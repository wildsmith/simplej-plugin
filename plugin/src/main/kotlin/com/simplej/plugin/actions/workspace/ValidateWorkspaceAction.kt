// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.actions.workspace

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.intellij.openapi.project.Project
import com.simplej.base.ProjectViewPopupMenuItem
import com.simplej.base.SimpleJAnAction
import com.simplej.base.extensions.executeBackgroundTask
import com.simplej.base.extensions.showError
import com.simplej.plugin.SimpleJCoroutineService
import com.simplej.plugin.WorkspaceCompat
import com.simplej.plugin.simpleJConfig
import kotlinx.coroutines.launch
import java.io.File
import java.util.Properties

/**
 * Action that validates the workspace configuration against the requirements specified in `simplej.json`.
 *
 * This action performs validation checks for:
 * - Java environment (version and home directory)
 * - SSH connectivity (for GitHub repositories)
 * - Android build tools version
 *
 * The action is available in the project view popup menu and executes validation tasks based on the configuration
 * specified in the project's `simplej.json` file.
 */
internal class ValidateWorkspaceAction : SimpleJAnAction(), ProjectViewPopupMenuItem {

    /**
     * Executes the workspace validation action when triggered.
     *
     * Performs all configured validation checks and displays the results through notifications.
     *
     * @param event The action event containing the project context
     */
    @Suppress("ReturnCount")
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return event.showError("No valid project found within the workspace.")
        val simpleJConfig = project.simpleJConfig() ?: return event.showError(
            "No valid `simplej-config.json` configuration file found within `${project.basePath}/config/simplej`!"
        )
        val workspaceCompat = simpleJConfig.workspaceCompat ?: return event.showError(
            "No `workspaceCompat` configuration found within `simplej-config.json`!"
        )

        val workspaceValidation = WorkspaceValidation()
        val coroutineService = service<SimpleJCoroutineService>()
        listOf(
            { validateJavaVersion(workspaceCompat, workspaceValidation) },
            { validateJavaHome(workspaceCompat, workspaceValidation) },
            { validateSshConnection(workspaceCompat, workspaceValidation) },
            { validateSshPassphrase(workspaceCompat, workspaceValidation) },
            { validateAndroidBuildToolsVersion(project, workspaceCompat, workspaceValidation) }
        ).forEach {
            coroutineService.scope.launch { it() }
        }
        ValidationDialog(project, coroutineService, workspaceCompat, workspaceValidation).show()
    }

    /**
     * Validates the Java environment configuration.
     *
     * Checks either the Java version or Java home directory based on the configuration. If the Java version is
     * specified, it takes precedence over the Java home directory validation.
     *
     * @param workspaceCompat The WorkspaceCompat configuration
     */
    private fun validateJavaVersion(
        workspaceCompat: WorkspaceCompat,
        workspaceValidation: WorkspaceValidation
    ) {
        val requiredJavaVersion = workspaceCompat.java?.version
        if (requiredJavaVersion == null) {
            // Opt out of validation as no required java version has been configured
            workspaceValidation.javaVersionCheck.value = ValidationState.Unnecessary(
                "no Java version has been configured."
            )
            return
        }

        @Suppress("TooGenericExceptionCaught", "SwallowedException")
        executeBackgroundTask {
            val javaVersion: String
            try {
                // When there's a discrepancy between `java.version` and `java --version` and on macOS, the
                // `java --version` command takes precedence because it reflects the actual Java executable that will
                // be used in the command line/workspace environment.
                val process = Runtime.getRuntime().exec("java -version")
                val output = process.errorStream.bufferedReader().readLines()
                process.waitFor()
                javaVersion = output.firstOrNull()?.trim() ?: System.getProperty("java.version") ?: "Unknown"
            } catch (e: Exception) {
                workspaceValidation.javaVersionCheck.value = ValidationState.Failed("Unable to determine Java version")
                return@executeBackgroundTask
            }

            if (requiredJavaVersion.matches(javaVersion)) {
                workspaceValidation.javaVersionCheck.value = ValidationState.Passed(
                    "Valid Java version found, <code>$javaVersion</code>."
                )
            } else {
                workspaceValidation.javaVersionCheck.value = ValidationState.Failed(
                    "Java version value is not compatible with required: $requiredJavaVersion"
                )
            }
        }
    }

    /**
     * Validates the Java home directory against the configured requirement.
     *
     * Checks if the system's `JAVA_HOME` environment variable or `java.home` property matches the configured
     * requirement in `simplej.json`.
     *
     * @param workspaceCompat The WorkspaceCompat configuration
     */
    @Suppress("ReturnCount")
    private fun validateJavaHome(
        workspaceCompat: WorkspaceCompat,
        workspaceValidation: WorkspaceValidation
    ) {
        val requiredJavaVersion = workspaceCompat.java?.version
        if (requiredJavaVersion != null) {
            // Opt out of validation as the java version has been configured
            workspaceValidation.javaHomeCheck.value = ValidationState.Unnecessary(
                "Java version has been configured."
            )
            return
        }

        val requiredJavaHome = workspaceCompat.java?.home
        if (requiredJavaHome.isNullOrBlank()) {
            // Opt out of validation as no required java home has been configured
            workspaceValidation.javaHomeCheck.value = ValidationState.Unnecessary(
                "no Java home has been configured."
            )
            return
        }

        val javaHome = System.getenv("JAVA_HOME") ?: System.getProperty("java.home")
        if (javaHome.isNullOrEmpty()) {
            workspaceValidation.javaHomeCheck.value = ValidationState.Failed("Java home variable is not set")
            return
        }

        if (requiredJavaHome != javaHome) {
            workspaceValidation.javaHomeCheck.value = ValidationState.Failed(
                "Java home path is not compatible with required: $requiredJavaHome"
            )
            return
        }

        workspaceValidation.javaHomeCheck.value = ValidationState.Passed("Java home is valid: $javaHome")
    }

    /**
     * Validates SSH connectivity to GitHub using the configured test endpoint.
     *
     * Tests the SSH connection by attempting to authenticate with GitHub using the configured SSH test endpoint.
     *
     * @param workspaceCompat The WorkspaceCompat configuration
     */
    private fun validateSshConnection(workspaceCompat: WorkspaceCompat, workspaceValidation: WorkspaceValidation) {
        val githubUrl = workspaceCompat.ssh?.testRepo
        if (githubUrl.isNullOrBlank()) {
            // Opt out of the ssh check when no test endpoint has been configured
            workspaceValidation.sshConnectionCheck.value = ValidationState.Unnecessary(
                "no test endpoint has been configured."
            )
            return
        }

        if (!githubUrl.matches(Regex("git@github(.*)\\.com:.+/.+\\.git"))) {
            workspaceValidation.sshConnectionCheck.value = ValidationState.Failed(
                "Invalid GitHub SSH URL format. Expected format: git@github.com:username/repo.git",
            )
            return
        }

        @Suppress("TooGenericExceptionCaught", "SwallowedException")
        executeBackgroundTask {
            try {
                val hostname = githubUrl.substringAfter("@")
                    .substringBefore(":")

                // Test SSH connection
                val process = Runtime.getRuntime().exec(
                    "ssh -T -o BatchMode=yes -o StrictHostKeyChecking=no git@$hostname"
                )
                val error = process.errorStream.bufferedReader().readLine()
                process.waitFor()

                // GitHub's SSH test always returns exit code 1 even on success
                // We need to check the error output for the expected message
                if (error.contains("successfully authenticated", true)) {
                    workspaceValidation.sshConnectionCheck.value = ValidationState.Passed(
                        "SSH connection to GitHub was successful.",
                    )
                } else {
                    showSshConnectionError(workspaceValidation)
                }
            } catch (e: Exception) {
                showSshConnectionError(workspaceValidation)
            }
        }
    }

    private fun showSshConnectionError(workspaceValidation: WorkspaceValidation) {
        workspaceValidation.sshConnectionCheck.value = ValidationState.Failed(
            """
                SSH connection failed. Please check your SSH configuration:<br>
                 1. Ensure SSH keys are generated (~/.ssh/id_rsa and ~/.ssh/id_rsa.pub)<br>
                 2. Verify your public key is added to GitHub<br>
                 3. Check if ssh-agent is running
            """.trimIndent(),
        )
    }

    /**
     * Validates the SSH key's passphrase protection against the `simplej-config.json` settings.
     *
     * This function checks if the SSH key specified by `workspaceCompat.ssh.keyPath` is passphrase-protected
     * and compares this status against the `workspaceCompat.ssh.passphraseEnabled` setting.
     *
     * It uses the `ssh-keygen -y` command to attempt reading the public key from the private key file
     * with an empty passphrase.
     *
     * - A success notification is shown if the configuration expects no passphrase (`passphraseEnabled = false`)
     *   and the key is successfully read without one.
     * - An error notification is shown if the actual state of the key mismatches the configuration, or if
     *   the validation command fails for any reason.
     * - The validation is skipped if `passphraseEnabled` or `keyPath` is not defined in the configuration.
     *
     * @param workspaceCompat The parsed WorkspaceCompat configuration containing the SSH settings to validate.
     */
    private fun validateSshPassphrase(workspaceCompat: WorkspaceCompat, workspaceValidation: WorkspaceValidation) {
        val passphraseEnabled = workspaceCompat.ssh?.passphraseEnabled
        val keyPath = workspaceCompat.ssh?.keyPath
        if (passphraseEnabled == null || keyPath.isNullOrBlank()) {
            // Opt out of the ssh check when no state for passphrase or keypath have been configured
            workspaceValidation.sshPassphraseCheck.value = ValidationState.Unnecessary(
                "no ssh passphrase state or keypath has been configured."
            )
            return
        }

        @Suppress("TooGenericExceptionCaught", "SwallowedException")
        executeBackgroundTask {
            try {
                // Validate passphrase presence
                val process = Runtime.getRuntime().exec(
                    "ssh-keygen -y -P \"\" -f ${keyPath.removePrefix("/")}",
                    null,
                    File(System.getProperty("user.home"))
                )
                val input = process.inputStream.bufferedReader().readLine()
                process.waitFor()

                // If the output is the public key, then no passphrase was required
                val publicKeyPrefix = "ssh-${keyPath.substringAfterLast("/").substringAfter("_")}"
                if (!passphraseEnabled && input.startsWith(publicKeyPrefix)) {
                    workspaceValidation.sshPassphraseCheck.value = ValidationState.Passed(
                        "SSH key does not require a passphrase."
                    )
                } else {
                    workspaceValidation.sshPassphraseCheck.value = ValidationState.Failed(
                        "SSH key requires a passphrase when it shouldn't."
                    )
                }
            } catch (e: Exception) {
                workspaceValidation.sshPassphraseCheck.value = ValidationState.Failed(
                    "SSH passphrase validation failed."
                )
            }
        }
    }

    /**
     * This check is less than ideal but simpler than adding another platform dependency on 'android' to the IDE
     * Plugin for a one-off validation check. There is an assumption that Google continues pushing the 'sdk.dir'
     * property into a `local.properties` file at the root of every Android project.
     */
    @Suppress("ReturnCount")
    private fun validateAndroidBuildToolsVersion(
        project: Project,
        workspaceCompat: WorkspaceCompat,
        workspaceValidation: WorkspaceValidation
    ) {
        val requiredBuildTools = workspaceCompat.android?.buildTools
        if (requiredBuildTools == null) {
            // Opt out of the buildTools check when as it hasn't been configured
            workspaceValidation.androidBuildToolsCheck.value = ValidationState.Unnecessary(
                "no Android build tools version has been configured."
            )
            return
        }

        val localProperties = File("${project.basePath}/local.properties")
        if (!localProperties.exists()) {
            workspaceValidation.androidBuildToolsCheck.value = ValidationState.Failed(
                "Unable to find local.properties file within the project workspace."
            )
            return
        }

        val properties = Properties()
        properties.load(localProperties.inputStream())

        val sdkDir = properties.getProperty("sdk.dir")
        val buildToolsDir = File("$sdkDir/build-tools")

        val hasMatchingBuildToolsInstalled = buildToolsDir.listFiles()
            ?.filter { it.isDirectory }
            ?.any { requiredBuildTools.matches(it.name) }
        if (hasMatchingBuildToolsInstalled == true) {
            workspaceValidation.androidBuildToolsCheck.value = ValidationState.Passed(
                "Build tools version is compatible."
            )
        } else {
            workspaceValidation.androidBuildToolsCheck.value = ValidationState.Failed(
                "Unable to find matching build tools installed in Android SDK: $requiredBuildTools",
            )
            return
        }
    }
}
