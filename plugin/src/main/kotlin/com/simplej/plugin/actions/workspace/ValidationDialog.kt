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
import com.simplej.plugin.SimpleJCoroutineService
import com.simplej.plugin.WorkspaceCompat
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

internal class ValidationDialog(
    project: Project,
    private val coroutineService: SimpleJCoroutineService,
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
        isOKActionEnabled = true
        isResizable = false
        init()
    }

    override fun createCenterPanel(): DialogPanel = panel {
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
                        rowComment =
                            "Passphrase required: <code>$passphraseEnabled</code><br>" +
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

    private fun Panel.rowChecks(
        rowLabel: String,
        rowComment: String,
        visibleStates: VisibleStates,
        stateFlow: MutableStateFlow<ValidationState>
    ) = row(rowLabel) {
        panel {
            row {
                cell(AsyncProcessIcon("Loading..."))
            }
        }.visibleIf(visibleStates.loading)
        panel {
            row {
                icon(AllIcons.RunConfigurations.TestPassed)
                text("Passed")
                    .bindText(visibleStates.message)
            }
        }.visibleIf(visibleStates.passed)
        panel {
            row {
                icon(AllIcons.RunConfigurations.TestFailed)
                text("Failed")
                    .bindText(visibleStates.message)
            }
        }.visibleIf(visibleStates.failed)
        panel {
            row {
                icon(AllIcons.Actions.Checked)
                text("Unnecessary")
                    .bindText(visibleStates.message)
            }
        }.visibleIf(visibleStates.unnecessary)
        jobs.add(
            coroutineService.scope.launch {
                stateFlow.collect(visibleStates)
            }
        )
        rowComment(rowComment)
    }

    private suspend fun MutableStateFlow<ValidationState>.collect(visibleStates: VisibleStates) {
        collect { state ->
            state.message?.let { message -> visibleStates.message.set(message) }
            if (state !is ValidationState.Loading) visibleStates.loading.set(false)
            when (state) {
                is ValidationState.Passed -> visibleStates.passed.set(true)
                is ValidationState.Failed -> visibleStates.failed.set(true)
                is ValidationState.Unnecessary -> visibleStates.unnecessary.set(true)
                else -> Unit
            }
        }
    }

    override fun dispose() {
        jobs.forEach { it.cancel() }
        super.dispose()
    }

    private data class VisibleStates(
        val loading: AtomicMutableBooleanProperty = AtomicBooleanProperty(true),
        val passed: AtomicMutableBooleanProperty = AtomicBooleanProperty(false),
        val failed: AtomicMutableBooleanProperty = AtomicBooleanProperty(false),
        val unnecessary: AtomicMutableBooleanProperty = AtomicBooleanProperty(false),
        val message: ObservableMutableProperty<String> = AtomicProperty("")
    )
}
