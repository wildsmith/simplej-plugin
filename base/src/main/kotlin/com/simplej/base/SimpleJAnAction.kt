// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.base

import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

/**
 * Base class for all SimpleJ plugin actions that provides standardized action handling.
 *
 * This abstract class extends IntelliJ's [AnAction] and implements common functionality:
 * - Background thread execution for action updates
 * - Context-aware visibility control based on action placement
 * - Support for [ProjectViewPopupMenuItem] and [EditorPopupMenuItem] interfaces
 *
 * To create a new action:
 * 1. Extend this class
 * 2. Implement required marker interfaces ([ProjectViewPopupMenuItem] and/or [EditorPopupMenuItem])
 * 3. Override [actionPerformed] to define the action's behavior
 * 4. Optionally override [shouldShow] to add custom visibility logic
 */
abstract class SimpleJAnAction : AnAction() {

    /**
     * Ensures all action updates occur in the background thread for better UI responsiveness.
     *
     * @return [ActionUpdateThread.BGT] to perform updates asynchronously
     */
    final override fun getActionUpdateThread(): ActionUpdateThread =
        ActionUpdateThread.BGT

    /**
     * Updates the action's visibility based on context and implemented interfaces.
     *
     * Automatically hides the action if:
     * - No project is open
     * - Action placement doesn't match implemented interfaces
     * - Custom visibility conditions aren't met
     *
     * @param event The action event containing context information
     */
    final override fun update(event: AnActionEvent) {
        val project = event.project
        if (project == null || !shouldShowInternal(event, project)) {
            event.presentation.isVisible = false
        }
    }

    /**
     * Internal visibility check that validates action placement against implemented interfaces.
     *
     * @param event The action event
     * @param project The current project
     * @return true if the action should be visible in the current context
     */
    private fun shouldShowInternal(event: AnActionEvent, project: Project): Boolean {
        val shouldShow = when (event.place) {
            ActionPlaces.PROJECT_VIEW_POPUP -> this is ProjectViewPopupMenuItem
            ActionPlaces.EDITOR_POPUP -> this is EditorPopupMenuItem
            else -> false
        }

        return if (shouldShow) {
            shouldShow(event, project)
        } else {
            false
        }
    }

    /**
     * Override this method to implement custom visibility logic.
     *
     * By default, the action is visible if all other conditions are met. Subclasses can override this to add
     * additional visibility requirements.
     *
     * @param event The action event
     * @param project The current project
     * @return true if the action should be visible, false otherwise
     */
    open fun shouldShow(event: AnActionEvent, project: Project): Boolean = true
}
