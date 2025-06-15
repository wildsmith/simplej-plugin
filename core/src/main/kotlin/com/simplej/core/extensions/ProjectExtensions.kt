package com.simplej.core.extensions

import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project

const val PLUGIN_NAME = "SimpleJ Plugin"

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
