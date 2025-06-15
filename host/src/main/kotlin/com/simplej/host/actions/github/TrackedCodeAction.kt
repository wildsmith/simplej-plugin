package com.simplej.host.actions.github

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.FileStatus
import com.intellij.openapi.vcs.FileStatusManager
import com.intellij.openapi.vfs.VirtualFile
import com.simplej.core.EditorPopupMenuItem
import com.simplej.core.ProjectViewPopupMenuItem
import com.simplej.core.SimpleJAnAction
import com.simplej.core.extensions.currentFiles
import git4idea.GitUtil

abstract class TrackedCodeAction : SimpleJAnAction(), ProjectViewPopupMenuItem, EditorPopupMenuItem {

    override fun shouldShow(event: AnActionEvent, project: Project): Boolean =
        event.currentFiles.ifEmpty {
            return false
        }.any {
            FileStatusManager.getInstance(project).getStatus(it) != FileStatus.IGNORED
        }

    protected fun Project.getGithubUrl(
        editor: Editor?,
        projectFile: VirtualFile,
        currentFile: VirtualFile
    ): String? {
        var linePath = ""
        editor?.let {
            val selectionModel = it.selectionModel
            val document = editor.document
            val startLine = document.getLineNumber(selectionModel.selectionStart) + 1
            val endLine = document.getLineNumber(selectionModel.selectionEnd) + 1

            linePath = if (endLine > startLine) {
                "#L$startLine-L$endLine"
            } else {
                "#L$startLine"
            }
        }
        val remoteUrl = GitUtil.getRepositoryForFile(this, projectFile)
            .remotes
            .firstOrNull { it.name == "origin" }
            ?.firstUrl
            ?.substringBefore(".git")
        return if (remoteUrl != null) {
            val lastSegment = remoteUrl.substringAfterLast("/")
            val pathSegment = currentFile.path.substringAfterLast(lastSegment)
            "$remoteUrl/blob/main$pathSegment$linePath"
        } else {
            null
        }
    }
}
