// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.simplej.base.ProjectViewPopupMenuItem
import com.simplej.base.SimpleJAnAction
import com.simplej.base.extensions.currentFile
import com.simplej.base.extensions.findClosestProject

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
