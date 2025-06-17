// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.base.extensions

import androidx.annotation.RestrictTo
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

/**
 * Gets the currently selected or focused virtual file in the IDE.
 *
 * This extension property provides easy access to the virtual file associated with
 * the current action event context. Commonly used in actions that operate on
 * the currently selected/active file.
 *
 * @return The current [VirtualFile] or null if no file is selected
 */
val AnActionEvent.currentFile: VirtualFile?
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    get() = getData(CommonDataKeys.VIRTUAL_FILE)

/**
 * Gets an array of currently selected virtual files in the IDE.
 *
 * This extension property is useful for actions that operate on multiple selected files, such as bulk operations in
 * the Project view or other file-centric contexts.
 *
 * @return An array of selected [VirtualFile]s. Returns an empty array if no files are selected
 */
val AnActionEvent.currentFiles: Array<VirtualFile>
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    get() = getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: arrayOf()

/**
 * Converts a Java [File] to IntelliJ's [VirtualFile].
 *
 * This extension function bridges the gap between Java IO and IntelliJ's virtual file system, allowing easy
 * conversion from standard Java files to IntelliJ's virtual file representation.
 *
 * @return The corresponding [VirtualFile] or null if the file cannot be found in the virtual file system
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun File.toVirtualFile(): VirtualFile? =
    LocalFileSystem.getInstance().findFileByIoFile(this)

/**
 * Gets the root project file for the workspace.
 *
 * @param project The IntelliJ project context
 * @return The root [VirtualFile] with the shortest path length, or null if no root is found
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun VirtualFile.getRootProjectFile(project: Project): VirtualFile? =
    findAllProjectRoots(project).minByOrNull { it.path.length }

/**
 * Finds all Gradle project roots in the given project.
 *
 * Identifies directories that:
 * - Are content roots in the project
 * - Are not named "buildSrc"
 * - Contain a build.gradle.kts file
 *
 * @param project The IntelliJ project context
 * @return Set of [VirtualFile]s representing all valid Gradle project roots
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun VirtualFile.findAllProjectRoots(project: Project): Set<VirtualFile> =
    ProjectRootManager.getInstance(project).contentRoots
        .filterTo(mutableSetOf()) {
            name != "buildSrc" && it.getBuildFile().exists()
        }

/**
 * Finds the closest Gradle project root directory containing the current virtual file.
 *
 * This function searches upwards through the file hierarchy to find the nearest directory that contains a
 * 'build.gradle.kts' file, indicating a Gradle project root. If multiple project roots are found, it returns the one
 * with the longest path that contains the current file.
 *
 * @param project The current IntelliJ project context
 * @return The closest [VirtualFile] representing a Gradle project root, or null if none found
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun VirtualFile.findClosestProject(project: Project): VirtualFile? =
    findAllProjectRoots(project)
        .sortedBy { it.path.length }
        .reversed()
        .firstOrNull { path.startsWith(it.path) }

/**
 * Locates and returns the Gradle build file (build.gradle.kts or build.gradle) in the current directory.
 *
 * @return The build file as a [File] object, or null if neither build.gradle.kts nor build.gradle exists
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun VirtualFile.getBuildFile(): File? {
    var settingsFile = File("$path/build.gradle.kts")
    if (!settingsFile.exists()) {
        settingsFile = File("$path/build.gradle")
        if (!settingsFile.exists()) {
            return null
        }
    }
    return settingsFile
}

/**
 * Locates and returns the Gradle settings file (settings.gradle.kts or settings.gradle) in the current directory.
 *
 * @return The settings file as a [File] object, or null if neither settings.gradle.kts nor settings.gradle exists
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun VirtualFile.getSettingsFile(): File? {
    var buildFile = File("$path/settings.gradle.kts")
    if (!buildFile.exists()) {
        buildFile = File("$path/settings.gradle")
        if (!buildFile.exists()) {
            return null
        }
    }
    return buildFile
}

/**
 * Gets the path to the CODEOWNERS file in the project's .github directory.
 *
 * @return A [File] object representing the path to the CODEOWNERS file
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun VirtualFile.getCodeOwnersFile(): File =
    File("$path/.github/CODEOWNERS")

/**
 * Converts the absolute file path to a Gradle project path notation.
 *
 * This function:
 * - Removes the root project path prefix
 * - Converts directory separators (/) to Gradle path separators (:)
 *
 * @param project The IntelliJ project context
 * @return The Gradle path notation as a String
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun VirtualFile.getGradlePath(project: Project): String =
    path.substring(getRootProjectFile(project)?.path?.length ?: 0).replace("/", ":")

/**
 * Safely checks if a nullable File exists.
 *
 * @return true if the file is not null and exists, false otherwise
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun File?.exists(): Boolean = this != null && exists()
