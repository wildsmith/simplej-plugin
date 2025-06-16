// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.base.extensions

import com.intellij.ide.actions.OpenFileAction
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.fileEditor.FileEditorManager
import java.io.File

/**
 * Shows an error notification in the IDE.
 *
 * @param message The error message to be displayed in the notification
 */
fun AnActionEvent.showError(message: String) =
    showNotification(message, type = NotificationType.ERROR)

/**
 * Shows a notification in the IDE with customizable parameters.
 *
 * @param message The message to be displayed in the notification
 * @param title The title of the notification (defaults to PLUGIN_NAME)
 * @param type The type of notification to show (defaults to INFORMATION)
 * @param actions Set of actions that can be performed from the notification (defaults to empty set)
 */
fun AnActionEvent.showNotification(
    message: String,
    title: String = PLUGIN_NAME,
    type: NotificationType = NotificationType.INFORMATION,
    actions: Set<AnAction> = setOf()
) = project.showNotification(message, title, type, actions)

/**
 * Opens a file in the IDE editor and optionally positions the caret at a specific line.
 *
 * This extension function for [AnActionEvent] provides a convenient way to:
 * - Open a file in the IDE's editor
 * - Optionally move the caret to a specific line number
 *
 * If the file cannot be converted to a [com.intellij.openapi.vfs.VirtualFile] or if no project is available, the
 * function will silently return without performing any action.
 *
 * @param file The file to be opened in the IDE
 * @param line Optional line number where the caret should be positioned (1-based indexing).
 *             If null, the caret position remains unchanged
 */
fun AnActionEvent.openInIde(file: File, line: Int? = null) {
    val virtualFile = file.toVirtualFile() ?: return
    val project = project ?: return
    OpenFileAction.openFile(virtualFile, project)

    if (line != null) {
        FileEditorManager
            .getInstance(project)
            .selectedTextEditor
            ?.caretModel
            ?.moveToLogicalPosition(LogicalPosition(line - 1, 0))
    }
}

/**
 * Synchronizes the Gradle project in the IDE.
 *
 * This extension function attempts to refresh/sync the Gradle project by:
 * 1. First trying to execute the "Gradle.RefreshAllProjects" action
 * 2. If that's not available, falling back to "Android.SyncProject" action
 *
 * This is useful when programmatic changes require the Gradle project to be resynced to reflect the updates in the IDE.
 */
fun AnActionEvent.gradleSync() {
    ActionManager.getInstance().getAction("Gradle.RefreshAllProjects")?.actionPerformed(this)
        ?: ActionManager.getInstance().getAction("Android.SyncProject")?.actionPerformed(this)
}
