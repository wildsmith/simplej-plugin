package com.simplej.core.extensions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.LocalFileSystem
import java.io.File

val AnActionEvent.currentFile: VirtualFile?
    get() = getData(CommonDataKeys.VIRTUAL_FILE)

val AnActionEvent.currentFiles: Array<VirtualFile>
    get() = getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) ?: arrayOf()

fun File.toVirtualFile(): VirtualFile? =
    LocalFileSystem.getInstance().findFileByIoFile(this)

fun VirtualFile.findClosestProject(project: Project): VirtualFile? =
    ProjectRootManager.getInstance(project).contentRoots.filter {
        File(it.path, "build.gradle.kts").exists()
    }
        .sortedBy { it.path.length }
        .reversed()
        .firstOrNull {
            path.startsWith(it.path)
        }
