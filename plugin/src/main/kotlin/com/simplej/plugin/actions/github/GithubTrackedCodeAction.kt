// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.actions.github

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.openapi.vcs.FileStatus
import com.intellij.openapi.vcs.FileStatusManager
import com.intellij.openapi.vfs.VirtualFile
import com.simplej.base.EditorPopupMenuItem
import com.simplej.base.ProjectViewPopupMenuItem
import com.simplej.base.SimpleJAnAction
import com.simplej.base.extensions.currentFiles
import git4idea.GitUtil
import org.jetbrains.annotations.VisibleForTesting

/**
 * An abstract base class for GitHub-specific actions that integrate with GitHub functionality.
 *
 * This class provides specialized functionality for actions that interact with GitHub repositories and features.
 */
internal abstract class GithubTrackedCodeAction : SimpleJAnAction(), ProjectViewPopupMenuItem, EditorPopupMenuItem {

    override fun shouldShow(event: AnActionEvent, project: Project): Boolean =
        event.currentFiles.ifEmpty {
            return false
        }.any {
            FileStatusManager.getInstance(project).getStatus(it) != FileStatus.IGNORED
        }
}

@VisibleForTesting
internal fun Project.getGithubUrl(
    editor: Editor?,
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
    val repo = GitUtil.getRepositoryForFile(this, currentFile)
    val remoteUrl = repo
        .remotes
        .firstOrNull { it.name == "origin" }
        ?.firstUrl
        ?.substringBefore(".git")
        ?.substringAfter("@")
        ?.replace(".com:", ".com/")
        ?.substringAfter("://")
    return if (remoteUrl != null) {
        val lastSegment = remoteUrl.substringAfterLast("/")
        val pathSegment = currentFile.path.substringAfterLast(lastSegment)
        val render = if (currentFile.isDirectory) {
            "tree"
        } else {
            "blob"
        }
        "https://$remoteUrl/$render/${repo.currentBranchName}$pathSegment$linePath"
    } else {
        null
    }
}
