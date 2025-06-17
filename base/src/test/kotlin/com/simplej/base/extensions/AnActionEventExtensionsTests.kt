// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.base.extensions

import com.intellij.notification.NotificationType
import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class AnActionEventExtensionsTests {

    private lateinit var event: AnActionEvent
    private lateinit var project: Project
    private lateinit var actionManager: ActionManager

    @BeforeEach
    fun setUp() {
        event = mockk()
        project = mockk()
        every { event.project } returns project

        actionManager = mockk()
        mockkStatic(ActionManager::class)
        every { ActionManager.getInstance() } returns actionManager
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `showError displays error notification with correct parameters`() {
        val errorMessage = "Test error message"

        mockkStatic("com.simplej.base.extensions.ProjectExtensionsKt")
        every {
            project.showNotification(
                message = errorMessage,
                title = PLUGIN_NAME,
                type = NotificationType.ERROR,
                actions = setOf()
            )
        } just runs

        event.showError(errorMessage)

        verify {
            project.showNotification(
                message = errorMessage,
                title = PLUGIN_NAME,
                type = NotificationType.ERROR,
                actions = setOf()
            )
        }
    }

    @Test
    fun `showNotification displays notification with custom parameters`() {
        val message = "Test message"
        val title = "Custom Title"
        val type = NotificationType.WARNING
        val action = mockk<AnAction>()
        val actions = setOf(action)

        mockkStatic("com.simplej.base.extensions.ProjectExtensionsKt")
        every {
            project.showNotification(
                message = message,
                title = title,
                type = type,
                actions = actions
            )
        } just runs

        event.showNotification(message, title, type, actions)

        verify {
            project.showNotification(
                message = message,
                title = title,
                type = type,
                actions = actions
            )
        }
    }

    @Test
    fun `gradleSync executes Gradle refresh action when available`() {
        val refreshAction = mockk<AnAction>()
        every { actionManager.getAction("Gradle.RefreshAllProjects") } returns refreshAction
        every { refreshAction.actionPerformed(event) } just runs

        event.gradleSync()

        verify {
            refreshAction.actionPerformed(event)
        }
    }

    @Test
    fun `gradleSync falls back to Android sync when Gradle refresh is not available`() {
        val androidSyncAction = mockk<AnAction>()
        every { actionManager.getAction("Gradle.RefreshAllProjects") } returns null
        every { actionManager.getAction("Android.SyncProject") } returns androidSyncAction
        every { androidSyncAction.actionPerformed(event) } just runs

        event.gradleSync()

        verify {
            androidSyncAction.actionPerformed(event)
        }
    }
}
