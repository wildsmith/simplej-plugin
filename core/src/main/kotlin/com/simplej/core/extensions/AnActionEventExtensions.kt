package com.simplej.core.extensions

import com.intellij.ide.actions.OpenFileAction
import com.intellij.notification.NotificationType
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
fun AnActionEvent.showError(message: String) {
    showNotification(message, type = NotificationType.ERROR)
}

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
) {
    project.showNotification(message, title, type, actions)
}

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
