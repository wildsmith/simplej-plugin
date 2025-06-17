// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.base.extensions

import androidx.annotation.RestrictTo
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project

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
