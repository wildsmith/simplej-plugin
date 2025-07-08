// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.actions.workspace

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.components.service
import com.simplej.base.ProjectViewPopupMenuItem
import com.simplej.base.SimpleJAnAction
import com.simplej.base.extensions.showError
import com.simplej.plugin.SimpleJCoroutineService
import com.simplej.plugin.simpleJConfig

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

        val coroutineScope = service<SimpleJCoroutineService>().scope
        val (workspaceValidation, _) = WorkspaceValidation.populate(
            project,
            workspaceCompat,
            coroutineScope
        )
        ValidationDialog(project, coroutineScope, workspaceCompat, workspaceValidation).show()
    }
}
