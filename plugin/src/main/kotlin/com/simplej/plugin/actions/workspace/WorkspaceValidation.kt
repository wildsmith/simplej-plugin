// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.actions.workspace

import com.intellij.openapi.project.Project
import com.simplej.base.extensions.runCommandForErrorStream
import com.simplej.base.extensions.runCommandForOutputStream
import com.simplej.plugin.WorkspaceCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.io.File
import java.util.Properties

/**
 * A data class that acts as a container for the real-time state of workspace validation checks.
 *
 * Each property in this class represents a specific validation check (e.g., Java version, SSH connection)
 * and holds its current state as a [MutableStateFlow]. This allows different parts of the
 * plugin, such as the [ValidationDialog], to observe and react to state changes as they happen.
 * All checks are initialized to the [ValidationState.Loading] state.
 *
 * @property javaVersionCheck The state of the Java version validation check.
 * @property javaHomeCheck The state of the Java home directory validation check.
 * @property sshConnectionCheck The state of the SSH connection validation check.
 * @property sshPassphraseCheck The state of the SSH key passphrase validation check.
 * @property androidBuildToolsCheck The state of the Android build tools version validation check.
 */
internal data class WorkspaceValidation(
    val javaVersionCheck: MutableStateFlow<ValidationState> = MutableStateFlow(ValidationState.Loading),
    val javaHomeCheck: MutableStateFlow<ValidationState> = MutableStateFlow(ValidationState.Loading),
    val sshConnectionCheck: MutableStateFlow<ValidationState> = MutableStateFlow(ValidationState.Loading),
    val sshPassphraseCheck: MutableStateFlow<ValidationState> = MutableStateFlow(ValidationState.Loading),
    val androidBuildToolsCheck: MutableStateFlow<ValidationState> = MutableStateFlow(ValidationState.Loading)
) {

    internal fun toJson(): JsonArray =
        JsonArray(
            this.javaClass.declaredFields
                .map { Pair(it, it.get(this)) }
                .filter { it.second is MutableStateFlow<*> }
                .map { fieldPair ->
                    val validationState = (fieldPair.second as MutableStateFlow<ValidationState>).value
                    JsonObject(
                        mapOf(
                            "name" to JsonPrimitive(fieldPair.first.name),
                            "state" to JsonPrimitive(validationState.javaClass.simpleName),
                            "message" to JsonPrimitive(validationState.message?.replace("\"", "'"))
                        )
                    )
                }
        )

    internal companion object {

        @JvmStatic
        internal fun populate(
            project: Project,
            workspaceCompat: WorkspaceCompat,
            scope: CoroutineScope
        ): Pair<WorkspaceValidation, List<Deferred<*>>> {
            val workspaceValidation = WorkspaceValidation()
            val deferred = listOf(
                scope.async { validateJavaVersion(workspaceCompat, workspaceValidation) },
                scope.async { validateJavaHome(workspaceCompat, workspaceValidation) },
                scope.async { validateSshConnection(workspaceCompat, workspaceValidation) },
                scope.async { validateSshPassphrase(workspaceCompat, workspaceValidation) },
                scope.async { validateAndroidBuildToolsVersion(project, workspaceCompat, workspaceValidation) }
            )
            return Pair(workspaceValidation, deferred)
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
                    "Unnecessary, no Java version has been configured."
                )
                return
            }

            val javaVersion = rawJavaVersion()
                ?: "Unknown"
            if (javaVersion.lineSequence().any { requiredJavaVersion.matches(it) }) {
                workspaceValidation.javaVersionCheck.value = ValidationState.Passed(
                    "Valid Java version found."
                )
            } else {
                workspaceValidation.javaVersionCheck.value = ValidationState.Failed(
                    "Java version value is not compatible with required: $requiredJavaVersion."
                )
            }
        }

        internal fun rawJavaVersion(): String? =
            runCommandForErrorStream("java -version")
                ?: System.getProperty("java.version")

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
                    "Unnecessary, Java version has been configured."
                )
                return
            }

            val requiredJavaHome = workspaceCompat.java?.home
            if (requiredJavaHome.isNullOrBlank()) {
                // Opt out of validation as no required java home has been configured
                workspaceValidation.javaHomeCheck.value = ValidationState.Unnecessary(
                    "Unnecessary, no Java home has been configured."
                )
                return
            }

            val javaHome = rawJavaHome()
            if (javaHome.isNullOrEmpty()) {
                workspaceValidation.javaHomeCheck.value = ValidationState.Failed("Java home variable is not set.")
                return
            }

            if (requiredJavaHome != javaHome) {
                workspaceValidation.javaHomeCheck.value = ValidationState.Failed(
                    "Java home path is not compatible with required: $requiredJavaHome."
                )
                return
            }

            workspaceValidation.javaHomeCheck.value = ValidationState.Passed("Java home is valid: $javaHome.")
        }

        internal fun rawJavaHome(): String? =
            System.getenv("JAVA_HOME") ?: System.getProperty("java.home")

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
                    "Unnecessary, no test endpoint has been configured."
                )
                return
            }

            if (!githubUrl.matches(Regex("git@github(.*)\\.com:.+/.+\\.git"))) {
                workspaceValidation.sshConnectionCheck.value = ValidationState.Failed(
                    "Invalid GitHub SSH URL format. Expected format: <code>git@github.com:username/repo.git</code>.",
                )
                return
            }

            @Suppress("TooGenericExceptionCaught", "SwallowedException")
            try {
                val hostname = githubUrl.substringAfter("@")
                    .substringBefore(":")

                // Test SSH connection
                val error = runCommandForErrorStream(
                    "ssh -T -o BatchMode=yes -o StrictHostKeyChecking=no git@$hostname"
                )

                // GitHub's SSH test always returns exit code 1 even on success
                // We need to check the error output for the expected message
                if (error?.contains("successfully authenticated", true) == true) {
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
                    "Unnecessary, no ssh passphrase state or keypath has been configured."
                )
                return
            }

            @Suppress("TooGenericExceptionCaught", "SwallowedException")
            try {
                // Validate passphrase presence
                val input = runCommandForOutputStream(
                    command = "ssh-keygen -y -P \"\" -f ${keyPath.removePrefix("/")}",
                    directory = File(System.getProperty("user.home"))
                )

                // If the output is the public key, then no passphrase was required
                val publicKeyPrefix = "ssh-${keyPath.substringAfterLast("/").substringAfter("_")}"
                if (!passphraseEnabled && input?.startsWith(publicKeyPrefix) == true) {
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
                    "Unnecessary, no Android build tools version has been configured."
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
                    "Unable to find matching build tools installed in Android SDK: $requiredBuildTools.",
                )
            }
        }
    }
}

/**
 * Represents the various states of a single validation check.
 *
 * This sealed interface is used to model the outcome of a validation process,
 * such as checking the Java version or SSH connectivity. Each state can hold an
 * optional message to provide more context to the user.
 */
internal sealed class ValidationState(internal val message: String? = null) {

    /**
     * Represents a validation check currently in progress.
     */
    data object Loading : ValidationState()

    /**
     * Represents a validation check that has successfully completed.
     * @param message A message detailing the successful validation (e.g., "Valid Java version found").
     */
    class Passed(message: String) : ValidationState(message)

    /**
     * Represents a validation check that has failed.
     *
     * @param message A message explaining the reason for the failure.
     */
    class Failed(message: String) : ValidationState(message)

    /**
     * Represents a validation check not performed because it was not required or configured.
     *
     * @param message A message explaining why the check was skipped.
     */
    class Unnecessary(message: String) : ValidationState(message)
}
