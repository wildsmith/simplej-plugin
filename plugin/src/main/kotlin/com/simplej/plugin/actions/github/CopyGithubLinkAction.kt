// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.actions.github

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.simplej.base.extensions.currentFile
import com.simplej.base.extensions.currentFiles
import com.simplej.base.extensions.executeBackgroundTask
import com.simplej.base.extensions.showError
import java.awt.datatransfer.StringSelection

/**
 * Action that copies a GitHub permalink to the current file or selected code.
 *
 * This action enables users to quickly share references to code by generating and copying GitHub URLs to the
 * clipboard. The URL format depends on the context:
 * - Single line: Links to the specific line
 * - Selection: Links to the selected line range
 * - File: Links to the entire file
 *
 * Features:
 * - Generates permanent links that remain valid after code changes
 * - Includes line numbers for precise reference
 * - Copies URL directly to system clipboard
 *
 * Prerequisites:
 * - Project must be a Git repository
 * - Repository must have a GitHub remote
 * - Only one file can be selected at a time
 *
 * The action is available through:
 * - Editor context menu
 * - Project view context menu
 * - VCS menu
 *
 * Usage examples:
 * - Sharing code snippets in pull request discussions
 * - Referencing code in documentation
 * - Creating links for issue references
 *
 * Note: The action will show an error notification if:
 * - No valid project is found
 * - No file is selected
 * - Multiple files are selected
 * - GitHub remote is not configured
 *
 * @see GithubTrackedCodeAction
 */
internal class CopyGithubLinkAction : GithubTrackedCodeAction() {

    override fun shouldShow(event: AnActionEvent, project: Project): Boolean {
        return event.currentFiles.size == 1
    }

    @Suppress("ReturnCount")
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return event.showError(
            "No valid project found within the workspace."
        )
        val currentFile = event.currentFile ?: return event.showError(
            "No valid file found within the project workspace."
        )

        val editor = event.getData(PlatformDataKeys.EDITOR)
        executeBackgroundTask {
            project.getGithubUrl(editor, currentFile)?.let {
                val copyPasteManager = CopyPasteManager.getInstance()
                copyPasteManager.setContents(StringSelection(it))
            }
        }
    }
}
