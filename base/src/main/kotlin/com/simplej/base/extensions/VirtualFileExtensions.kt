// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.base.extensions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtil
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
    get() = getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: arrayOf()

/**
 * Converts a Java [File] to IntelliJ's [VirtualFile].
 *
 * This extension function bridges the gap between Java IO and IntelliJ's virtual file system, allowing easy
 * conversion from standard Java files to IntelliJ's virtual file representation.
 *
 * @return The corresponding [VirtualFile] or null if the file cannot be found in the virtual file system
 */
fun File.toVirtualFile(): VirtualFile? =
    LocalFileSystem.getInstance().findFileByIoFile(this)

fun VirtualFile.toFile(): File =
    VfsUtil.virtualToIoFile(this)

fun VirtualFile.findAllProjectRoots(project: Project): Set<VirtualFile> =
    ProjectRootManager.getInstance(project).contentRoots
        .filterTo(mutableSetOf()) {
            name != "buildSrc" && File(it.path, "build.gradle.kts").exists()
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
fun VirtualFile.findClosestProject(project: Project): VirtualFile? =
    findAllProjectRoots(project)
        .sortedBy { it.path.length }
        .reversed()
        .firstOrNull { path.startsWith(it.path) }

fun VirtualFile.getBuildFile(): File? {
    var buildFile = File("$path/build.gradle.kts")
    if (!buildFile.exists()) {
        buildFile = File("$path/build.gradle")
        if (!buildFile.exists()) {
            return null
        }
    }
    return buildFile
}

fun VirtualFile.getGradlePath(project: Project): String {
    val rootProject = findAllProjectRoots(project).minByOrNull { it.path.length }
    return path.substring(rootProject?.path?.length ?: 0).replace("/", ":")
}

fun File?.exists(): Boolean = this != null && exists()
