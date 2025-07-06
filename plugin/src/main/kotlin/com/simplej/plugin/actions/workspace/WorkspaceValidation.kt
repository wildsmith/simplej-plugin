// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.actions.workspace

import kotlinx.coroutines.flow.MutableStateFlow

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
)

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
    class Unnecessary(message: String) : ValidationState("Unnecessary, $message")
}
