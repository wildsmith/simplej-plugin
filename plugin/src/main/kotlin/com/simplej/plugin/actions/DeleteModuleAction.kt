// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.simplej.base.ProjectViewPopupMenuItem
import com.simplej.base.SimpleJAnAction
import com.simplej.base.extensions.currentFile
import com.simplej.base.extensions.findAllProjectRoots
import com.simplej.base.extensions.findClosestProject
import com.simplej.base.extensions.getRootProjectFile
import com.simplej.base.extensions.getBuildFile
import com.simplej.base.extensions.getCodeOwnersFile
import com.simplej.base.extensions.getGradlePath
import com.simplej.base.extensions.getSettingsFile
import com.simplej.base.extensions.gradleSync
import com.simplej.base.extensions.showError
import com.simplej.plugin.actions.github.LookupCodeOwnerAction.CodeOwnerIdentifier
import org.jetbrains.annotations.VisibleForTesting

/**
 * Action that handles the deletion of modules from a Gradle project.
 *
 * This action is available in the Project View popup menu and performs the following tasks:
 * - Checks for module references in other build files
 * - Updates settings files to remove module inclusion
 * - Updates CODEOWNERS file if needed
 * - Removes the module directory
 * - Triggers a Gradle sync
 */
internal class DeleteModuleAction : SimpleJAnAction(), ProjectViewPopupMenuItem {

    /**
     * Determines whether the action should be shown in the context menu.
     *
     * The action is shown only when:
     * - A valid project file is selected
     * - The selected project is not the root project
     *
     * @param event The action event
     * @param project The current project
     * @return true if the action should be shown, false otherwise
     */
    @Suppress("ReturnCount")
    override fun shouldShow(event: AnActionEvent, project: Project): Boolean {
        val projectFile = event.currentFile?.findClosestProject(project)
            ?: return super.shouldShow(event, project)
        val rootProjectFile = projectFile.getRootProjectFile(project)
            ?: return super.shouldShow(event, project)
        return projectFile.path != rootProjectFile.path
    }

    /**
     * Performs the module deletion action.
     *
     * Checks for module references and either shows a warning dialog or proceeds with deletion.
     *
     * @param event The action event containing the project and file context
     */
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return event.showError("No valid project found within the workspace.")
        val projectFile = event.currentFile?.findClosestProject(project) ?: return event.showError(
            "No valid project found within the workspace."
        )

        if (checkForBuildFileReferences(projectFile, project)) {
            promptWithReferenceDialog()
        } else {
            deleteModuleAndSync(projectFile, project, event)
        }
    }

    @VisibleForTesting
    internal fun checkForBuildFileReferences(projectFile: VirtualFile, project: Project): Boolean {
        val shorthandProjectPath = projectFile.getGradlePath(project)
        val projectRoots = projectFile.findAllProjectRoots(project)
        var containsReferences = false
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

    @VisibleForTesting
    internal fun promptWithReferenceDialog() {
        TODO("Not yet implemented")
    }

    @VisibleForTesting
    internal fun deleteModuleAndSync(projectFile: VirtualFile, project: Project, event: AnActionEvent) {
        val fileWrites = mutableListOf<FileUpdates?>()
        fileWrites.add(updateSettings(projectFile, project))
        fileWrites.add(updateCodeOwners(projectFile, project))
        WriteCommandAction.runWriteCommandAction(
            project,
            "Delete Module",
            null,
            {
                fileWrites
                    .filterNotNull()
                    .forEach { it() }
                projectFile.delete(this)
                event.gradleSync()
            }
        )
    }

    @VisibleForTesting
    internal fun updateSettings(
        projectFile: VirtualFile,
        project: Project
    ): (() -> Unit)? {
        val shorthandProjectPath = projectFile.getGradlePath(project)
        val fileLines = StringBuilder()
        val settingsFile = projectFile.getRootProjectFile(project)?.getSettingsFile() ?: return null
        settingsFile.useLines { lines ->
            for (line in lines) {
                val trimmedLine = line.trim()
                when {
                    trimmedLine == "include '$shorthandProjectPath'" ||
                            trimmedLine == "include(\"$shorthandProjectPath\")" ||
                            trimmedLine == "'$shorthandProjectPath'," ||
                            trimmedLine == "\"$shorthandProjectPath\"," ->
                        continue

                    line.contains("'$shorthandProjectPath',") -> {
                        line.replace("'$shorthandProjectPath',", "")
                        fileLines.appendLine(line)
                    }

                    line.contains("\"$shorthandProjectPath\",") -> {
                        line.replace("\"$shorthandProjectPath\",", "")
                        fileLines.appendLine(line)
                    }

                    else -> fileLines.appendLine(line)
                }
            }
        }
        return { settingsFile.writeText(fileLines.toString()) }
    }

    @VisibleForTesting
    internal fun updateCodeOwners(currentFile: VirtualFile, project: Project): (() -> Unit)? {
        val rootProjectFile = currentFile.getRootProjectFile(project) ?: return null
        val currentFilePathSegments = currentFile.path.substringAfter(rootProjectFile.path)
            .split("/")
            .drop(1)
        val codeOwnerFile = rootProjectFile.getCodeOwnersFile()
        val matchingCodeOwnerClaims = CodeOwnerIdentifier(codeOwnerFile)
            .findAllCodeOwnerRules(currentFile.path.substringAfter(project.name))
            .filter {
                var claimSegments = it.pattern.split("/")
                if (it.pattern.startsWith("/")) {
                    claimSegments = claimSegments.drop(1)
                }
                claimSegments[0] == currentFilePathSegments[0]
            }
        return if (matchingCodeOwnerClaims.isEmpty()) {
            null
        } else {
            {
                val lines = codeOwnerFile.readLines().toMutableList()
                matchingCodeOwnerClaims.forEach { codeOwnerClaim ->
                    lines.removeAt(codeOwnerClaim.humanReadableLineNumber - 1)
                }
                codeOwnerFile.writeText(lines.joinToString("\n"))
            }
        }
    }
}

private typealias FileUpdates = () -> Unit
