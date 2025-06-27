// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.actions.github

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.simplej.base.extensions.currentFiles
import com.simplej.base.extensions.executeBackgroundTask
import com.simplej.base.extensions.showError

/**
 * An action that opens the current file or selection in GitHub.
 *
 * This action:
 * - Opens GitHub in the default browser
 * - Navigates to the exact file
 *
 * When activated, it will:
 * - For a single file: Open the file in GitHub
 * - For multiple selected files: Open each file in a separate browser tab
 * - For a directory: Open the directory view in GitHub
 *
 * Note: This action requires:
 * - An active internet connection
 * - A properly configured GitHub remote in the Git repository
 * - Appropriate access rights to the GitHub repository
 */
internal class OpenInGithubAction : GithubTrackedCodeAction() {

    @Suppress("ReturnCount")
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return event.showError(
            "No valid project found within the workspace."
        )
        val currentFiles = event.currentFiles.ifEmpty {
            return event.showError(
                "No valid file found within the project workspace."
            )
        }

        currentFiles.forEach { currentFile ->
            val editor = event.getData(PlatformDataKeys.EDITOR)
            project.openInBrowser(currentFile, editor)
        }
    }

    private fun Project.openInBrowser(currentFile: VirtualFile, editor: Editor? = null) {
        executeBackgroundTask {
            getGithubUrl(editor, currentFile)?.let {
                com.simplej.base.extensions.openInBrowser(it)
            }
        }
    }
}
