// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.actions.creation

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.vfs.writeText
import com.simplej.base.ProjectViewPopupMenuItem
import com.simplej.base.SimpleJAnAction
import com.simplej.base.extensions.currentFile
import com.simplej.base.extensions.findAllProjectRoots
import com.simplej.base.extensions.findClosestProject
import com.simplej.base.extensions.getCodeOwnersFile
import com.simplej.base.extensions.getGradlePath
import com.simplej.base.extensions.getRootProjectFile
import com.simplej.base.extensions.getSettingsFile
import com.simplej.base.extensions.gradleSync
import com.simplej.base.extensions.showError
import com.simplej.base.extensions.toVirtualFile
import com.simplej.plugin.actions.github.LookupCodeOwnerAction.CodeOwnerIdentifier
import com.simplej.plugin.simpleJConfig

internal class NewModuleAction : SimpleJAnAction(), ProjectViewPopupMenuItem {

    /**
     * Determines whether the action should be shown in the context menu.
     *
     * The action is shown only when:
     * - A valid project file is selected
     *
     * @param event The action event
     * @param project The current project
     * @return true if the action should be shown, false otherwise
     */
    @Suppress("ReturnCount")
    override fun shouldShow(event: AnActionEvent, project: Project): Boolean {
        if (project.simpleJConfig()?.newModuleTemplates?.isEmpty() == true) {
            return false
        }
        val currentFile = event.currentFile ?: return false
        return currentFile.findAllProjectRoots(project)
            .any { it.path.startsWith(currentFile.path) }
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return event.showError("No valid project found within the workspace.")
        val projectFile = event.currentFile?.findClosestProject(project) ?: return event.showError(
            "No valid project found within the workspace."
        )

        // To view the Compose dialog, set this to false
        val useSwingDialog = true
        if (useSwingDialog) {
            NewModuleDialog(project) { formData ->
                createNewModule(event, project, projectFile, formData)
            }.show()
        } else {
            NewModuleDialogCompose(project) { formData ->
                createNewModule(event, project, projectFile, formData)
            }.show()
        }
    }

    private fun createNewModule(
        event: AnActionEvent,
        project: Project,
        projectFile: VirtualFile,
        formData: NewModuleFormData
    ) = WriteCommandAction.runWriteCommandAction(
        project,
        "New Module",
        null,
        {
            val newModuleDir = createNewModule(project, projectFile, formData)
            val rootProjectFile = projectFile.getRootProjectFile(project)!!
            updateSettings(project, rootProjectFile, newModuleDir)
            updateCodeOwners(project, rootProjectFile, newModuleDir, formData)

            projectFile.refresh(true, true)
            event.gradleSync()
        }
    )

    private fun createNewModule(
        project: Project,
        projectFile: VirtualFile,
        formData: NewModuleFormData
    ): VirtualFile {
        val newModuleDir = projectFile.createChildDirectory(this, formData.formattedModuleName())
        val simpleJConfig = project.simpleJConfig()
        simpleJConfig!!.newModuleTemplates!!.first { it.name == formData.templateName }.files.forEach { file ->
            newModuleDir.createChildData(this, file.relativePath)
                .writeText(file.template(project.basePath!!).readText())
        }
        return newModuleDir
    }

    private fun updateSettings(
        project: Project,
        projectFile: VirtualFile,
        newModuleDir: VirtualFile
    ) {
        projectFile.getSettingsFile()?.let { settingsFile ->
            val includeContents = if (settingsFile.name.endsWith(".kts")) {
                "\"${newModuleDir.getGradlePath(project)}\""
            } else {
                "'${newModuleDir.getGradlePath(project)}'"
            }
            settingsFile.appendText("\ninclude($includeContents)")
        }
    }

    private fun updateCodeOwners(
        project: Project,
        projectFile: VirtualFile,
        newModuleDir: VirtualFile,
        formData: NewModuleFormData
    ) {
        val codeOwnersFile = projectFile.getCodeOwnersFile()
        val newModulePath = newModuleDir.path.substringAfter(project.name)
        val matchingOwners = CodeOwnerIdentifier(codeOwnersFile)
            .findAllCodeOwnerRules(newModulePath)
        val ownershipClaim = "$newModulePath/ ${formData.formattedGithubTeamOrUser()}"
        if (codeOwnersFile.exists()) {
            if (matchingOwners.isEmpty()) {
                codeOwnersFile.appendText(ownershipClaim)
            } else {
                codeOwnersFile.readText()
                    .split("\n")
                    .toMutableList()
                    .apply { add(matchingOwners.last().humanReadableLineNumber, ownershipClaim) }
                    .joinToString("\n")
                    .let { codeOwnersFile.writeText(it) }
            }
        } else {
            codeOwnersFile.parentFile
                .toVirtualFile()!!
                .createChildData(this, "CODEOWNERS")
                .writeText(ownershipClaim)
        }
    }
}
