// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.simplej.base.ProjectViewPopupMenuItem
import com.simplej.base.SimpleJAnAction
import com.simplej.base.extensions.executeBackgroundTask
import com.simplej.base.extensions.showError
import com.simplej.base.extensions.showNotification
import com.simplej.plugin.SimpleJConfig
import com.simplej.plugin.simpleJConfig
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
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return event.showError("No valid project found within the workspace.")
        val simpleJConfig = project.simpleJConfig() ?: return event.showError(
            "No valid `simplej-config.json` configuration file found within `${project.basePath}/config/simplej`!"
        )
        validateJava(project, simpleJConfig)
        validateSshConnection(project, simpleJConfig)
        validateAndroidBuildToolsVersion(project, simpleJConfig)
    }

    /**
     * Validates the Java environment configuration.
     *
     * Checks either the Java version or Java home directory based on the configuration. If the Java version is
     * specified, it takes precedence over the Java home directory validation.
     *
     * @param project The current project
     * @param simpleJConfig The SimpleJ configuration
     */
    private fun validateJava(project: Project, simpleJConfig: SimpleJConfig) {
        val requiredJavaVersion = simpleJConfig.workspaceCompat?.java?.version
        if (requiredJavaVersion == null) {
            validateJavaHome(project, simpleJConfig)
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
                project.showNotification("Unable to determine Java version", JAVA_VALIDATION_ERROR)
                return@executeBackgroundTask
            }

            if (requiredJavaVersion.matches(javaVersion)) {
                project.showNotification("Java version is valid: $javaVersion", JAVA_VALIDATION_SUCCESS)
            } else {
                project.showError(
                    "Java version value is not compatible with required: $requiredJavaVersion",
                    JAVA_VALIDATION_ERROR
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
     * @param project The current project
     * @param simpleJConfig The SimpleJ configuration
     */
    @Suppress("ReturnCount")
    private fun validateJavaHome(project: Project, simpleJConfig: SimpleJConfig) {
        val requiredJavaHome = simpleJConfig.workspaceCompat?.java?.home
        if (requiredJavaHome.isNullOrBlank()) {
            // Opt out of validation as no required java home has been configured
            return
        }

        val javaHome = System.getenv("JAVA_HOME") ?: System.getProperty("java.home")
        if (javaHome.isNullOrEmpty()) {
            project.showError("Java home variable is not set", JAVA_VALIDATION_ERROR)
            return
        }

        if (requiredJavaHome != javaHome) {
            project.showError(
                "Java home path is not compatible with required: $requiredJavaHome",
                JAVA_VALIDATION_ERROR
            )
            return
        }

        project.showNotification("Java home is valid: $javaHome", JAVA_VALIDATION_SUCCESS)
    }

    /**
     * Validates SSH connectivity to GitHub using the configured test endpoint.
     *
     * Tests the SSH connection by attempting to authenticate with GitHub using the configured SSH test endpoint.
     *
     * @param project The current project
     * @param simpleJConfig The SimpleJ configuration
     */
    private fun validateSshConnection(project: Project, simpleJConfig: SimpleJConfig) {
        val githubUrl = simpleJConfig.workspaceCompat?.ssh?.testRepo
        if (githubUrl.isNullOrBlank()) {
            // Opt out of the ssh check when no test endpoint has been configured
            return
        }

        if (!githubUrl.matches(Regex("git@github(.*)\\.com:.+/.+\\.git"))) {
            project.showNotification(
                "Invalid GitHub SSH URL format. Expected format: git@github.com:username/repo.git",
                SSH_VALIDATION_ERROR
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
                    project.showNotification(
                        "SSH connection to GitHub was successful.",
                        SSH_VALIDATION_SUCCESS
                    )
                } else {
                    showSshError(project)
                }
            } catch (e: Exception) {
                showSshError(project)
            }
        }
    }

    private fun showSshError(project: Project) {
        project.showError(
            """
                SSH connection failed. Please check your SSH configuration:<br>
                 1. Ensure SSH keys are generated (~/.ssh/id_rsa and ~/.ssh/id_rsa.pub)<br>
                 2. Verify your public key is added to GitHub<br>
                 3. Check if ssh-agent is running
            """.trimIndent(),
            SSH_VALIDATION_ERROR
        )
    }

    /**
     * This check is less than ideal but simpler than adding another platform dependency on 'android' to the IDE
     * Plugin for a one-off validation check. There is an assumption that Google continues pushing the 'sdk.dir'
     * property into a `local.properties` file at the root of every Android project.
     */
    @Suppress("ReturnCount")
    private fun validateAndroidBuildToolsVersion(project: Project, simpleJConfig: SimpleJConfig) {
        // Opt out of the buildTools check when as it hasn't been configured
        val requiredBuildTools = simpleJConfig.workspaceCompat?.android?.buildTools
            ?: return

        val localProperties = File("${project.basePath}/local.properties")
        if (!localProperties.exists()) {
            project.showError(
                "Unable to find local.properties file within the project workspace.",
                ANDROID_BUILD_TOOLS_VALIDATION_ERROR
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
            project.showNotification(
                "Android Build tools version is compatible.",
                ANDROID_BUILD_TOOLS_VALIDATION_SUCCESS
            )
        } else {
            project.showError(
                "Unable to find matching build tools installed in Android SDK: $requiredBuildTools",
                ANDROID_BUILD_TOOLS_VALIDATION_ERROR
            )
            return
        }
    }

    private companion object {
        private const val SSH_VALIDATION_ERROR = "SSH Validation Error"
        private const val SSH_VALIDATION_SUCCESS = "SSH Validation Success"
        private const val JAVA_VALIDATION_ERROR = "Java Validation Error"
        private const val JAVA_VALIDATION_SUCCESS = "Java Validation Success"
        private const val ANDROID_BUILD_TOOLS_VALIDATION_ERROR = "Android Build Tools Validation Error"
        private const val ANDROID_BUILD_TOOLS_VALIDATION_SUCCESS = "Android Build Tools Validation Success"
    }
}
