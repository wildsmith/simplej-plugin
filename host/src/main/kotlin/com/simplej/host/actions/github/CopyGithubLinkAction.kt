package com.simplej.host.actions.github

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.simplej.core.extensions.currentFile
import com.simplej.core.extensions.currentFiles
import com.simplej.core.extensions.executeBackgroundTask
import com.simplej.core.extensions.findClosestProject
import com.simplej.core.extensions.showError
import java.awt.datatransfer.StringSelection

class CopyGithubLinkAction : TrackedCodeAction() {

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
        val projectFile = currentFile.findClosestProject(project) ?: return event.showError(
            "No valid project found within the workspace."
        )

        val editor = event.getData(PlatformDataKeys.EDITOR)
        executeBackgroundTask {
            project.getGithubUrl(editor, projectFile, currentFile)?.let {
                val copyPasteManager = CopyPasteManager.getInstance()
                copyPasteManager.setContents(StringSelection(it))
            }
        }
    }
}
