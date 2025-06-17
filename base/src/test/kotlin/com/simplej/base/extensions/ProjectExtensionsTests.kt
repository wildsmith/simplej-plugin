// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.base.extensions

import com.intellij.notification.Notification
import com.intellij.notification.NotificationGroup
import com.intellij.notification.NotificationGroupManager
import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.project.Project
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verifySequence
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class ProjectExtensionsTests {

    private lateinit var project: Project
    private lateinit var notificationGroupManager: NotificationGroupManager
    private lateinit var notificationGroup: NotificationGroup
    private lateinit var notification: Notification

    @BeforeEach
    fun setUp() {
        project = mockk()
        notificationGroupManager = mockk()
        notificationGroup = mockk()
        notification = mockk(relaxed = true)

        mockkStatic(NotificationGroupManager::class)
        every { NotificationGroupManager.getInstance() } returns notificationGroupManager
        every { notificationGroupManager.getNotificationGroup("MainNotificationGroup") } returns notificationGroup
        every { notificationGroup.createNotification(any(), any<NotificationType>()) } returns notification
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `showNotification creates notification with default parameters`() {
        val message = "Test message"

        project.showNotification(message)

        verifySequence {
            NotificationGroupManager.getInstance()
            notificationGroupManager.getNotificationGroup("MainNotificationGroup")
            notificationGroup.createNotification(message, NotificationType.INFORMATION)
            notification.setTitle(PLUGIN_NAME)
            notification.notify(project)
        }
    }

    @Test
    fun `showNotification creates notification with custom title`() {
        val message = "Test message"
        val customTitle = "Custom Title"

        project.showNotification(
            message = message,
            title = customTitle
        )

        verifySequence {
            NotificationGroupManager.getInstance()
            notificationGroupManager.getNotificationGroup("MainNotificationGroup")
            notificationGroup.createNotification(message, NotificationType.INFORMATION)
            notification.setTitle(customTitle)
            notification.notify(project)
        }
    }

    @Test
    fun `showNotification creates notification with custom type`() {
        val message = "Test message"
        val type = NotificationType.WARNING

        project.showNotification(
            message = message,
            type = type
        )

        verifySequence {
            NotificationGroupManager.getInstance()
            notificationGroupManager.getNotificationGroup("MainNotificationGroup")
            notificationGroup.createNotification(message, type)
            notification.setTitle(PLUGIN_NAME)
            notification.notify(project)
        }
    }

    @Test
    fun `showNotification creates notification with actions`() {
        val message = "Test message"
        val action1 = mockk<AnAction>()
        val action2 = mockk<AnAction>()
        val actions = setOf(action1, action2)

        project.showNotification(
            message = message,
            actions = actions
        )

        verifySequence {
            NotificationGroupManager.getInstance()
            notificationGroupManager.getNotificationGroup("MainNotificationGroup")
            notificationGroup.createNotification(message, NotificationType.INFORMATION)
            notification.setTitle(PLUGIN_NAME)
            notification.addAction(action1)
            notification.addAction(action2)
            notification.notify(project)
        }
    }

    @Test
    fun `showNotification works with null project`() {
        val message = "Test message"
        val nullProject: Project? = null

        nullProject.showNotification(message)

        verifySequence {
            NotificationGroupManager.getInstance()
            notificationGroupManager.getNotificationGroup("MainNotificationGroup")
            notificationGroup.createNotification(message, NotificationType.INFORMATION)
            notification.setTitle(PLUGIN_NAME)
            notification.notify(null)
        }
    }

    @Test
    fun `showNotification handles empty title`() {
        val message = "Test message"
        val emptyTitle = ""

        project.showNotification(
            message = message,
            title = emptyTitle
        )

        verifySequence {
            NotificationGroupManager.getInstance()
            notificationGroupManager.getNotificationGroup("MainNotificationGroup")
            notificationGroup.createNotification(message, NotificationType.INFORMATION)
            notification.notify(project)
        }
    }

    @Test
    fun `showNotification creates error notification`() {
        val message = "Error message"

        project.showNotification(
            message = message,
            type = NotificationType.ERROR
        )

        verifySequence {
            NotificationGroupManager.getInstance()
            notificationGroupManager.getNotificationGroup("MainNotificationGroup")
            notificationGroup.createNotification(message, NotificationType.ERROR)
            notification.setTitle(PLUGIN_NAME)
            notification.notify(project)
        }
    }
}
