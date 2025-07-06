// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.actions.workspace

import kotlinx.coroutines.flow.MutableStateFlow

internal data class WorkspaceValidation(
    val javaVersionCheck: MutableStateFlow<ValidationState> = MutableStateFlow(ValidationState.Loading),
    val javaHomeCheck: MutableStateFlow<ValidationState> = MutableStateFlow(ValidationState.Loading),
    val sshConnectionCheck: MutableStateFlow<ValidationState> = MutableStateFlow(ValidationState.Loading),
    val sshPassphraseCheck: MutableStateFlow<ValidationState> = MutableStateFlow(ValidationState.Loading),
    val androidBuildToolsCheck: MutableStateFlow<ValidationState> = MutableStateFlow(ValidationState.Loading)
)

internal sealed class ValidationState(internal val message: String? = null) {
    data object Loading : ValidationState()
    class Passed(message: String) : ValidationState(message)
    class Failed(message: String) : ValidationState(message)
    class Unnecessary(message: String) : ValidationState("Unnecessary, $message")
}
