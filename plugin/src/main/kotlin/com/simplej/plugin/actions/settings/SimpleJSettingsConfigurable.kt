// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.actions.settings

import com.intellij.openapi.options.Configurable
import com.intellij.ui.dsl.builder.panel
import com.intellij.ui.dsl.builder.selected
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

    override fun getDisplayName(): String {
        return "SimpleJ Settings"
    }

    override fun createComponent(): JComponent =
        panel {
            group("Default Tasks") {
                row {
                    text(
                        "The following tasks are preloaded by SimpleJ and offered as default options " +
                                "within the 'Run...' action group."
                    )
                }
                uiBoundDefaultTasks.forEach { taskState ->
                    row {
                        checkBox(taskState.name)
                            .comment(taskState.description)
                            .selected(taskState.enabled)
                            .onChanged {
                                // For some reason `bindSelected` wasn't updating the value but this exlicit listener
                                // works
                                taskState.enabled = it.isSelected
                            }
                    }
                }
            }
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
