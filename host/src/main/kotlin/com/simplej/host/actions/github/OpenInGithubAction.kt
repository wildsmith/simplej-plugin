package com.simplej.host.actions.github

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.simplej.core.extensions.currentFiles
import com.simplej.core.extensions.executeBackgroundTask
import com.simplej.core.extensions.findClosestProject
import com.simplej.core.extensions.showError

class OpenInGithubAction : TrackedCodeAction() {

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
            val projectFile = currentFile.findClosestProject(project) ?: return event.showError(
                "No valid project found within the workspace."
            )
            val editor = event.getData(PlatformDataKeys.EDITOR)
            project.openInBrowser(projectFile, currentFile, editor)
        }
    }

    private fun Project.openInBrowser(projectFile: VirtualFile, currentFile: VirtualFile, editor: Editor? = null) {
        executeBackgroundTask {
            getGithubUrl(editor, projectFile, currentFile)?.let {
                com.simplej.core.extensions.openInBrowser(it)
            }
        }
    }
}
