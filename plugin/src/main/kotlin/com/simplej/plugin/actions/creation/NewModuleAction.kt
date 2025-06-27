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
import com.simplej.plugin.SimpleJConfig
import com.simplej.plugin.actions.github.LookupCodeOwnerAction.CodeOwnerIdentifier
import com.simplej.plugin.simpleJConfig

/**
 * Action that facilitates the creation of a new module within the project.
 *
 * This action orchestrates the entire process of scaffolding a new module. It handles:
 * - Creating the module's directory structure.
 * - Updating the Gradle `settings.gradle.kts` file to include the new module.
 * - Updating the `CODEOWNERS` file to assign ownership.
 * - Refreshing the Virtual File System (VFS) to reflect the changes.
 * - Triggering a Gradle sync to integrate the new module into the IDE and build process.
 *
 * It typically gathers the required module information from the user through a dialog.
 */
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
    override fun shouldShow(event: AnActionEvent, project: Project): Boolean {
        val currentFile = event.currentFile ?: return false
        return findAllProjectRoots(project)
            .any { it.path.startsWith(currentFile.path) }
    }

    @Suppress("ReturnCount")
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return event.showError("No valid project found within the workspace.")
        val simpleJConfig = project.simpleJConfig() ?: return event.showError(
            "No valid `simplej-config.json` configuration file found within `${project.basePath}/config/simplej`!"
        )
        if (simpleJConfig.newModuleTemplates.isNullOrEmpty()) return event.showError(
            "No new module templates listed within `simplej-config.json`!"
        )
        val projectFile = event.currentFile?.findClosestProject(project) ?: return event.showError(
            "No valid project found within the workspace."
        )

        // To view the Compose dialog, set this to false
        val useSwingDialog = true
        if (useSwingDialog) {
            NewModuleDialog(project, simpleJConfig) { formData ->
                createNewModule(event, project, projectFile, simpleJConfig, formData)
            }.show()
        } else {
            NewModuleDialogCompose(project, simpleJConfig) { formData ->
                createNewModule(event, project, projectFile, simpleJConfig, formData)
            }.show()
        }
    }

    /**
     * Orchestrates the creation of a new module by executing all necessary file modifications
     * and system commands within a single, undoable `WriteCommandAction`.
     *
     * This function handles the core logic of the [NewModuleAction].
     *
     * @param event The action event, used here to trigger a post-creation Gradle sync.
     * @param project The current project where the module will be created.
     * @param projectFile The virtual file representing the project, used as an anchor to find other configuration
     *                    files.
     * @param formData A data object containing the configuration for the new module (e.g., its name and owner).
     */
    private fun createNewModule(
        event: AnActionEvent,
        project: Project,
        projectFile: VirtualFile,
        simpleJConfig: SimpleJConfig,
        formData: NewModuleFormData
    ) = WriteCommandAction.runWriteCommandAction(
        project,
        "New Module",
        null,
        {
            val newModuleDir = createNewModule(project, projectFile, simpleJConfig, formData)
            val rootProjectFile = getRootProjectFile(project)!!
            updateSettings(project, rootProjectFile, newModuleDir)
            updateCodeOwners(project, rootProjectFile, newModuleDir, formData)

            projectFile.refresh(true, true)
            event.gradleSync()
        }
    )

    private fun createNewModule(
        project: Project,
        projectFile: VirtualFile,
        simpleJConfig: SimpleJConfig,
        formData: NewModuleFormData
    ): VirtualFile {
        val newModuleDir = projectFile.createChildDirectory(this, formData.formattedModuleName())
        simpleJConfig.newModuleTemplates!!.first { it.name == formData.templateName }.files.forEach { file ->
            var fileDir = newModuleDir
            if (file.relativePath.contains("/")) {
                fileDir = createChildDirectoryRecursively(
                    newModuleDir,
                    file.relativePath
                        .substringBeforeLast("/")
                        .split("/")
                )
            }
            fileDir.createChildData(this, file.relativePath.substringAfterLast("/"))
                .writeText(file.template(project.basePath!!).readText())
        }
        return newModuleDir
    }

    private fun createChildDirectoryRecursively(
        parentDir: VirtualFile,
        pathSegments: List<String>
    ): VirtualFile {
        return if (pathSegments.isEmpty()) {
            parentDir
        } else {
            val fileDir = parentDir.createChildDirectory(this, pathSegments.first())
            return createChildDirectoryRecursively(fileDir, pathSegments.drop(1))
        }
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
