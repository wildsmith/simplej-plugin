// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugins.actions

import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.simplej.base.extensions.currentFile
import com.simplej.base.extensions.findClosestProject
import com.simplej.base.extensions.showError
import com.simplej.base.extensions.showNotification
import com.simplej.plugin.actions.GradleTaskAction
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.jetbrains.plugins.gradle.util.GradleConstants
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class GradleTaskActionsTests {

    private lateinit var event: AnActionEvent
    private lateinit var project: Project
    private lateinit var currentFile: VirtualFile
    private lateinit var projectFile: VirtualFile
    private lateinit var projectRootManager: ProjectRootManager

    @BeforeEach
    fun setUp() {
        event = mockk(relaxed = true)
        project = mockk(relaxed = true)
        currentFile = mockk(relaxed = true)
        projectFile = mockk(relaxed = true)
        projectRootManager = mockk(relaxed = true)

        mockkStatic(ProjectRootManager::class)
        every { ProjectRootManager.getInstance(eq(project)) } returns projectRootManager
        every { projectRootManager.contentRoots } returns arrayOf(currentFile)

        mockkStatic("com.simplej.base.extensions.ProjectExtensionsKt")
        every { project.showNotification(any(), any(), any(), any()) } just Runs

        mockkStatic("com.simplej.base.extensions.AnActionEventExtensionsKt")
        every { event.showError(any()) } just Runs

        every { event.project } returns project
        every { event.currentFile } returns currentFile
        mockkStatic("com.simplej.base.extensions.VirtualFileExtensionsKt")
        every { currentFile.findClosestProject(any()) } returns projectFile
        every { projectFile.path } returns "/test/project/path"

        mockkStatic(ExternalSystemUtil::class)
        every {
            ExternalSystemUtil.runTask(any(), any(), any(), any(), any(), any(), any())
        } just Runs
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `action with single task executes correctly`() {
        val testAction = object : GradleTaskAction("testTask") {}

        testAction.actionPerformed(event)

        verify {
            ExternalSystemUtil.runTask(
                match<ExternalSystemTaskExecutionSettings> {
                    it.taskNames == listOf("testTask") &&
                            it.externalProjectPath == "/test/project/path" &&
                            it.externalSystemIdString == GradleConstants.SYSTEM_ID.id
                },
                DefaultRunExecutor.EXECUTOR_ID,
                project,
                GradleConstants.SYSTEM_ID,
                any(),
                any(),
                true
            )
        }
    }

    @Test
    fun `action with multiple tasks executes all tasks`() {
        val testAction = object : GradleTaskAction("task1", "task2", "task3") {}

        testAction.actionPerformed(event)

        verify {
            ExternalSystemUtil.runTask(
                match<ExternalSystemTaskExecutionSettings> {
                    it.taskNames == listOf("task1", "task2", "task3") &&
                            it.externalProjectPath == "/test/project/path"
                },
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun `composed action executes combined tasks`() {
        val action1 = object : GradleTaskAction("task1") {}
        val action2 = object : GradleTaskAction("task2") {}
        val composedAction = object : GradleTaskAction(action1, action2) {}

        composedAction.actionPerformed(event)

        verify {
            ExternalSystemUtil.runTask(
                match<ExternalSystemTaskExecutionSettings> {
                    it.taskNames.containsAll(listOf("task1", "task2"))
                },
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun `action shows error when no current file`() {
        every { event.currentFile } returns null

        val testAction = object : GradleTaskAction("testTask") {}
        testAction.actionPerformed(event)

        verify {
            event.showError("No valid file found within the project workspace.")
        }
        verify(exactly = 0) {
            ExternalSystemUtil.runTask(any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `action shows error when no project`() {
        every { event.project } returns null

        val testAction = object : GradleTaskAction("testTask") {}
        testAction.actionPerformed(event)

        verify {
            event.showError("No valid project found within the workspace.")
        }
        verify(exactly = 0) {
            ExternalSystemUtil.runTask(any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `action shows error when no closest project found`() {
        every { currentFile.findClosestProject(any()) } returns null

        val testAction = object : GradleTaskAction("testTask") {}
        testAction.actionPerformed(event)

        verify {
            event.showError("Unable to find closest valid project within the workspace.")
        }
        verify(exactly = 0) {
            ExternalSystemUtil.runTask(any(), any(), any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `complex tasks are filtered by shouldShow`() {
        val action1 = object : GradleTaskAction("task1") {
            override fun shouldShow(event: AnActionEvent, project: Project): Boolean {
                return true
            }
        }
        val action2 = object : GradleTaskAction("task2") {
            override fun shouldShow(event: AnActionEvent, project: Project): Boolean {
                return false
            }
        }

        val composedAction = object : GradleTaskAction(action1, action2) {}
        composedAction.actionPerformed(event)

        verify {
            ExternalSystemUtil.runTask(
                match<ExternalSystemTaskExecutionSettings> {
                    it.taskNames == listOf("task1")
                },
                any(),
                any(),
                any(),
                any(),
                any(),
                any()
            )
        }
    }
}
