// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.base.extensions

import androidx.annotation.RestrictTo
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File

/**
 * Shows an error notification in the IDE.
 *
 * @param message The error message to be displayed in the notification
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
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
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun AnActionEvent.openInIde(file: File, line: Int? = null) =
    project.openInIde(file, line)

/**
 * Synchronizes the Gradle project in the IDE.
 *
 * This extension function attempts to refresh/sync the Gradle project.
 *
 * This is useful when programmatic changes require the Gradle project to be resynced to reflect the updates in the IDE.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun AnActionEvent.gradleSync() {
    val project = project ?: return
    val rootProjectFile = getRootProjectFile(project) ?: return
    ExternalSystemUtil.refreshProject(
        project,
        GradleConstants.SYSTEM_ID,
        rootProjectFile.path,
        false,
        ProgressExecutionMode.IN_BACKGROUND_ASYNC
    )
}
