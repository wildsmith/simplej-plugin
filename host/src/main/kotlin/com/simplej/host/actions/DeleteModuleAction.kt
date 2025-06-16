// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.host.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.simplej.core.ProjectViewPopupMenuItem
import com.simplej.core.SimpleJAnAction
import com.simplej.core.extensions.currentFile
import com.simplej.core.extensions.findClosestProject

class DeleteModuleAction : SimpleJAnAction(), ProjectViewPopupMenuItem {

    @Suppress("ReturnCount")
    override fun shouldShow(event: AnActionEvent, project: Project): Boolean {
        val projectFile = event.currentFile?.findClosestProject(project)
            ?: return super.shouldShow(event, project)
        val rootProject = ProjectRootManager.getInstance(project).contentRoots.firstOrNull()
            ?: return super.shouldShow(event, project)
        return projectFile.path != rootProject.path
    }

    override fun actionPerformed(event: AnActionEvent) {
        TODO("Not yet implemented")
    }
}
