// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.actions.workspace

import com.intellij.icons.AllIcons
import com.intellij.openapi.observable.properties.AtomicBooleanProperty
import com.intellij.openapi.observable.properties.AtomicMutableBooleanProperty
import com.intellij.openapi.observable.properties.AtomicProperty
import com.intellij.openapi.observable.properties.ObservableMutableProperty
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.Panel
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.intellij.util.ui.AsyncProcessIcon
import com.simplej.plugin.WorkspaceCompat
import com.simplej.plugin.actions.settings.SimpleJSettingsConfigurable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.awt.event.ActionEvent
import javax.swing.Action

/**
 * A dialog that displays the real-time status of various workspace validation checks.
 *
 * This dialog presents a dynamic view of ongoing validation processes, such as Java version, SSH connectivity, and
 * Android configuration checks. It updates the UI as each validation completes, showing whether it passed, failed,
 * or was unnecessary.
 *
 * @param project The current IntelliJ project.
 * @param coroutineScope The scope used to launch and manage coroutines for observing validation state.
 * @param workspaceCompat The configuration object that specifies which validations to perform.
 * @param workspaceValidation The object that holds the state of each validation check.
 */
internal class ValidationDialog(
    private val project: Project,
    private val coroutineScope: CoroutineScope,
    private val workspaceCompat: WorkspaceCompat,
    private val workspaceValidation: WorkspaceValidation
) : DialogWrapper(project, true) {

    private val jobs = mutableListOf<Job>()
    private val javaVersionPanel = VisibleStates()
    private val javaHomePanel = VisibleStates()
    private val sshConfigPanel = VisibleStates()
    private val sshPassphrasePanel = VisibleStates()
    private val androidBuildToolsPanel = VisibleStates()

    init {
        title = "Validating Workspace"
        isResizable = false
        init()
    }

    override fun createActions(): Array<Action> {
        val copyAction = CopyAndCloseAction()
        copyAction.putValue(DEFAULT_ACTION, true)
        return arrayOf(copyAction, CloseAction())
    }

    /**
     * Creates the main content panel of the dialog using the IntelliJ UI DSL.
     *
     * It dynamically builds the UI by adding validation rows based on the settings defined in the [WorkspaceCompat]
     * configuration.
     *
     * @return The [DialogPanel] containing all the UI elements.
     */
    override fun createCenterPanel(): DialogPanel = panel {
        row {
            text(SimpleJSettingsConfigurable.WORKSPACE_COMPAT_INFO)
        }
        workspaceCompat.java?.let { java ->
            group("Java Checks") {
                java.version?.let { version ->
                    rowChecks(
                        rowLabel = "Version:",
                        rowComment = "Preferred: <code>$version</code>",
                        visibleStates = javaVersionPanel,
                        stateFlow = workspaceValidation.javaVersionCheck
                    )
                }
                java.home?.let { home ->
                    rowChecks(
                        rowLabel = "Home:",
                        rowComment = "Preferred: <code>$home</code>",
                        visibleStates = javaHomePanel,
                        stateFlow = workspaceValidation.javaHomeCheck
                    )
                }
            }
        }
        workspaceCompat.ssh?.let { ssh ->
            group("SSH Checks") {
                ssh.testRepo?.let { testRepo ->
                    rowChecks(
                        rowLabel = "Connection:",
                        rowComment = "Test repo: <code>$testRepo</code>",
                        visibleStates = sshConfigPanel,
                        stateFlow = workspaceValidation.sshConnectionCheck
                    )
                }
                ssh.passphraseEnabled?.let { passphraseEnabled ->
                    rowChecks(
                        rowLabel = "Passphrase:",
                        rowComment = "Passphrase required: <code>$passphraseEnabled</code><br>" +
                                "Key path: <code>${ssh.keyPath}</code>",
                        visibleStates = sshPassphrasePanel,
                        stateFlow = workspaceValidation.sshPassphraseCheck
                    )
                }
            }
        }
        group("Other Checks") {
            workspaceCompat.android?.buildTools.let { buildTools ->
                rowChecks(
                    rowLabel = "Android Build Tools:",
                    rowComment = "Preferred: <code>$buildTools</code>",
                    visibleStates = androidBuildToolsPanel,
                    stateFlow = workspaceValidation.androidBuildToolsCheck
                )
            }
        }
    }

    /**
     * Creates a standardized UI row for a single validation check.
     *
     * This extension function builds a row containing a label, a comment, and a status indicator that dynamically
     * changes based on the [ValidationState]. It shows a loading spinner initially, then updates to a passed,
     * failed, or unnecessary icon. It also launches a coroutine to collect updates from the provided [stateFlow].
     *
     * @param rowLabel The text label for the validation check.
     * @param rowComment A descriptive comment, often showing the expected value.
     * @param visibleStates The UI state holder that controls the visibility of status icons.
     * @param stateFlow The flow that emits updates for the validation state.
     */
    private fun Panel.rowChecks(
        rowLabel: String, rowComment: String, visibleStates: VisibleStates, stateFlow: MutableStateFlow<ValidationState>
    ) = row(rowLabel) {
        panel {
            row {
                cell(AsyncProcessIcon("Loading..."))
            }
        }.visibleIf(visibleStates.loading)
        panel {
            row {
                icon(AllIcons.RunConfigurations.TestPassed)
                text("Passed").bindText(visibleStates.message)
            }
        }.visibleIf(visibleStates.passed)
        panel {
            row {
                icon(AllIcons.RunConfigurations.TestFailed)
                text("Failed").bindText(visibleStates.message)
            }
        }.visibleIf(visibleStates.failed)
        panel {
            row {
                icon(AllIcons.Actions.Checked)
                text("Unnecessary").bindText(visibleStates.message)
            }
        }.visibleIf(visibleStates.unnecessary)
        jobs.add(
            coroutineScope.launch {
                stateFlow.collect(visibleStates)
            })
        rowComment(rowComment)
    }

    /**
     * Collects [ValidationState] updates and applies them to the UI's [VisibleStates].
     *
     * This suspend function observes a [MutableStateFlow] and updates the corresponding [VisibleStates] properties
     * to reflect the current validation status (e.g., loading, passed, failed), ensuring the dialog's UI is always
     * in sync with the validation logic.
     *
     * @param visibleStates The state holder for the UI row to be updated.
     */
    private suspend fun MutableStateFlow<ValidationState>.collect(visibleStates: VisibleStates) {
        collect { state ->
            if (state !is ValidationState.Loading) visibleStates.loading.set(false)
            if (state.message != null) visibleStates.message.set(state.message)
            when (state) {
                is ValidationState.Passed -> visibleStates.passed.set(true)
                is ValidationState.Failed -> visibleStates.failed.set(true)
                is ValidationState.Unnecessary -> visibleStates.unnecessary.set(true)
                else -> Unit
            }
        }
    }

    /**
     * Disposes of the dialog and cancels any running coroutines.
     *
     * This is overridden to ensure that all background jobs observing validation states are canceled when the dialog
     * is closed, preventing resource leaks.
     */
    override fun dispose() {
        jobs.forEach { it.cancel() }
        super.dispose()
    }

    /**
     * A data class that holds the observable properties for controlling the UI state of a single validation row.
     *
     * @property loading Controls the visibility of the loading indicator.
     * @property passed Controls the visibility of the "passed" icon and message.
     * @property failed Controls the visibility of the "failed" icon and message.
     * @property unnecessary Controls the visibility of the "unnecessary" icon and message.
     * @property message The text to be displayed alongside the status icon.
     */
    private data class VisibleStates(
        val loading: AtomicMutableBooleanProperty = AtomicBooleanProperty(true),
        val passed: AtomicMutableBooleanProperty = AtomicBooleanProperty(false),
        val failed: AtomicMutableBooleanProperty = AtomicBooleanProperty(false),
        val unnecessary: AtomicMutableBooleanProperty = AtomicBooleanProperty(false),
        val message: ObservableMutableProperty<String> = AtomicProperty("")
    )

    /**
     * Represents an action that performs two primary tasks:
     * - Copies the relevant data or content to a target (e.g., clipboard, file, etc.).
     * - Closes the associated context, such as a dialog or process, after completing the copy operation.
     *
     * This action is typically used to streamline workflows where users need to extract and save information and
     * subsequently close the UI or tool associated with the action.
     */
    private inner class CopyAndCloseAction : DialogWrapperAction("Copy and Close") {

        override fun doAction(event: ActionEvent?) {
            CopyWorkspaceInfoAction.copyWorkspaceInfo(project, workspaceCompat)
            close(OK_EXIT_CODE)
        }
    }

    /**
     * Represents an action that closes a specific UI component, dialog, or process.
     *
     * This action is typically used to simplify workflows where a user-triggered event (e.g., button press or menu
     * selection) is meant to signal the closure of an active item, such as a window, tool window, or editor session.
     */
    private inner class CloseAction : DialogWrapperAction("Close") {

        override fun doAction(event: ActionEvent?) {
            close(OK_EXIT_CODE)
        }
    }
}
