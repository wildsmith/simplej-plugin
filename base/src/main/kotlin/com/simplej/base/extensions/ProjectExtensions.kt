// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.base.extensions

import androidx.annotation.RestrictTo
import com.intellij.ide.actions.OpenFileAction
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import java.io.File

internal const val PLUGIN_NAME = "SimpleJ Plugin"

/**
 * Shows a notification in the IDE for the current project.
 *
 * Displays a balloon notification in the event log with the specified message, title, type, and optional actions.
 * The notification will be associated with the current project context.
 *
 * Example usage:
 * ```kotlin
 * project.showNotification(
 *     message = "Operation completed successfully",
 *     type = NotificationType.INFORMATION
 * )
 * ```
 *
 * @param message The content text to be displayed in the notification
 * @param title The title text of the notification (defaults to PLUGIN_NAME)
 * @param type The notification type that determines its appearance and severity:
 *            - INFORMATION for general messages
 *            - WARNING for warning messages
 *            - ERROR for error messages
 * @param actions Optional set of actions that will be displayed as clickable
 *               links in the notification
 *
 * @see com.intellij.notification.NotificationType
 * @see com.intellij.notification.Notification
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun Project?.showNotification(
    message: String,
    title: String = PLUGIN_NAME,
    type: NotificationType = NotificationType.INFORMATION,
    actions: Set<AnAction> = setOf()
) {
    NotificationGroupManager.getInstance()
        .getNotificationGroup("MainNotificationGroup")
        .createNotification(message, type)
        .apply {
            if (title.isNotBlank()) setTitle(title)
            for (action in actions) addAction(action)
        }
        .notify(this)
}

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
fun Project?.openInIde(file: File, line: Int? = null) {
    val virtualFile = file.toVirtualFile() ?: return
    val project = this ?: return
    project.openInIde(virtualFile, line)
}

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
fun Project.openInIde(file: VirtualFile, line: Int? = null) {
    OpenFileAction.openFile(file, this)

    if (line != null) {
        FileEditorManager
            .getInstance(this)
            .selectedTextEditor
            ?.caretModel
            ?.moveToLogicalPosition(LogicalPosition(line - 1, 0))
    }
}
