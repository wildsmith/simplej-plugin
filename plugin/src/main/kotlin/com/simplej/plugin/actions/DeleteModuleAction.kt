// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.simplej.base.ProjectViewPopupMenuItem
import com.simplej.base.SimpleJAnAction
import com.simplej.base.extensions.currentFile
import com.simplej.base.extensions.findAllProjectRoots
import com.simplej.base.extensions.findClosestProject
import com.simplej.base.extensions.getBuildFile
import com.simplej.base.extensions.getGradlePath
import com.simplej.base.extensions.gradleSync
import com.simplej.base.extensions.showError
import java.io.File

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
        val project = event.project ?: return event.showError("No valid project found within the workspace.")
        val projectFile = event.currentFile?.findClosestProject(project) ?: return event.showError(
            "No valid project found within the workspace."
        )

        val referencesFound = checkForBuildFileReferences(projectFile, project)
        if (referencesFound) {
            promptWithReferenceDialog()
        } else {
            deleteModuleAndSync(projectFile, project, event)
        }
    }

    private fun checkForBuildFileReferences(currentProjectRoot: VirtualFile, project: Project): Boolean {
        val shorthandProjectPath = currentProjectRoot.getGradlePath(project)
        var containsReferences = false
        val projectRoots = currentProjectRoot.findAllProjectRoots(project)
        for (projectRoot in projectRoots) {
            var withinDependenciesBlock = false
            projectRoot.getBuildFile()?.useLines { lines ->
                for (line in lines) {
                    if (line.contains("dependencies {")) {
                        withinDependenciesBlock = true
                    }
                    if (withinDependenciesBlock) {
                        if (line.contains("project(':") || line.contains("project(\":")) {
                            val dependency = line.substringAfter("project('")
                                .substringAfter("project(\"")
                                .substringBefore("'")
                                .substringBefore("\"")
                            if (dependency == shorthandProjectPath) {
                                containsReferences = true
                                break
                            }
                        }
                    }
                }

            }
        }
        return containsReferences
    }

    private fun promptWithReferenceDialog() {
        TODO("Not yet implemented")
    }

    private fun deleteModuleAndSync(projectFile: VirtualFile, project: Project, event: AnActionEvent) {
        // TODO add a copy to the codeowners notification
//        updateSettings()
//        updateCodeOwners()
        WriteCommandAction.runWriteCommandAction(
            project,
            "Delete Module",
            null,
            {
                projectFile.delete(this)
//                projectFile.refresh(true, true)
                event.gradleSync()
            }
        )
    }
}
