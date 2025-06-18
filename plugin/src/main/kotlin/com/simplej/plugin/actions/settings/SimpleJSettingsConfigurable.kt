// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.actions.settings

import com.intellij.openapi.options.Configurable
import com.intellij.openapi.project.ProjectManager
import com.intellij.ui.dsl.builder.COLUMNS_LARGE
import com.intellij.ui.dsl.builder.Row
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
import com.simplej.plugin.SimpleJConfig
import com.simplej.plugin.simpleJConfig
import javax.swing.JComponent

/**
 * Configuration interface for SimpleJ plugin settings.
 *
 * This class manages the settings UI and state for the SimpleJ plugin, implementing the [Configurable]
 * interface to integrate with IntelliJ's settings system. It provides functionality to:
 * - Display and edit default task configurations
 * - Manage task enable/disable states
 * - Handle settings persistence
 */
internal class SimpleJSettingsConfigurable : Configurable {

    private val simpleJConfig: SimpleJConfig? by lazy {
        // This is a hack, find a safer way if one exists
        ProjectManager.getInstance().openProjects.firstOrNull()?.simpleJConfig()
    }

    private val settings: SimpleJSettings by lazy {
        SimpleJSettings.instance
    }

    /**
     * Mutable set of task states that are bound to UI components. This set maintains the UI state independently of
     * the persisted settings until changes are explicitly applied.
     */
    private var uiBoundDefaultTasks = SimpleJSettings.instance.state.defaultTasks.mapTo(mutableSetOf()) {
        SimpleJSettings.State.TaskState(
            name = it.name,
            description = it.description,
            enabled = it.enabled
        )
    }

    override fun getDisplayName(): String = "SimpleJ Settings"

    @Suppress("LongMethod")
    override fun createComponent(): JComponent =
        panel {
            simpleJConfig?.let { simpleJConfig ->
                simpleJConfig.workspaceCompat?.let { workspaceCompat ->
                    group("Workspace Compat", indent = false) {
                        row {
                            text(
                                "Workspace compatability is determined by <code>config > simplej.json</code>. Talk " +
                                        "with the file owner before making changes."
                            ).applyToComponent {
                                insets.left = 0
                            }
                        }
                        indent {
                            workspaceCompat.ssh?.let { ssh ->
                                if (!ssh.testRepo.isNullOrBlank()) {
                                    row("SSH test repo:") {
                                        customTextField(ssh.testRepo)
                                        rowComment("This endpoint will be used to validate proper SSH configuration.")
                                    }
                                }
                            }
                            workspaceCompat.java?.let { java ->
                                if (java.version != null) {
                                    row("Java version:") {
                                        customTextField(java.version.toString())
                                        rowComment(
                                            "Java version is the preferred field to validate Java " +
                                                    "compatibility. When not specified, the home directory will be " +
                                                    "used as a fallback."
                                        )
                                    }
                                }
                                if (!java.home.isNullOrBlank()) {
                                    row("Java home directory:") {
                                        customTextField(java.home)
                                        rowComment(
                                            "If Java version is not specified the home directory will be " +
                                                    "used as a fallback."
                                        )
                                    }
                                }
                            }
                            workspaceCompat.android?.let { android ->
                                if (android.buildTools != null) {
                                    row("Android build tools:") {
                                        customTextField(android.buildTools.toString())
                                        rowComment(
                                            "Build tool misalignment can often lead to PKIX (public " +
                                                    "key infrastructure) errors. The build tools version should match."
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            group("Gradle Tasks", indent = false) {
                row {
                    text(
                        "The following tasks are preloaded by SimpleJ and offered as options " +
                                "within the 'Run...' action group."
                    )
                }
                indent {
                    uiBoundDefaultTasks.forEach { taskState ->
                        row {
                            checkBox(taskState.name)
                                .comment(taskState.description)
                                .selected(taskState.enabled)
                                .onChanged {
                                    // For some reason `bindSelected` wasn't updating the value but this explicit
                                    // listener works
                                    taskState.enabled = it.isSelected
                                }
                        }
                    }
                }
            }
        }

    private fun Row.customTextField(text: String) =
        textField().applyToComponent {
            setText(text)
            columns = COLUMNS_LARGE
            isEditable = false
        }

    /**
     * Checks if the current UI state differs from the persisted settings.
     *
     * @return true if the settings have been modified, false otherwise
     */
    override fun isModified(): Boolean {
        return uiBoundDefaultTasks != settings.state.defaultTasks
    }

    /**
     * Applies the current UI state to the persisted settings. This method is called when the user clicks "Apply" or
     * "OK" in the settings dialog.
     */
    override fun apply() {
        uiBoundDefaultTasks.syncTaskStatesTo(settings.state.defaultTasks)
    }

    /**
     * Resets the UI state to match the current persisted settings. This method is called when the user clicks
     * "Reset" in the settings dialog.
     */
    override fun reset() {
        settings.state.defaultTasks.syncTaskStatesTo(uiBoundDefaultTasks)
    }

    private fun Set<SimpleJSettings.State.TaskState>.syncTaskStatesTo(
        taskStates: Set<SimpleJSettings.State.TaskState>
    ): Unit = taskStates.forEach { taskState ->
        find { it.name == taskState.name }?.let {
            taskState.enabled = it.enabled
        }
    }
}
