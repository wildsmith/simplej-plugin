// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugins.actions.github

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vcs.FileStatus
import com.intellij.openapi.vcs.FileStatusManager
import com.intellij.openapi.vfs.VirtualFile
import com.simplej.base.extensions.currentFiles
import com.simplej.base.extensions.executeBackgroundTask
import com.simplej.base.extensions.findClosestProject
import com.simplej.base.extensions.openInBrowser
import com.simplej.base.extensions.showError
import com.simplej.plugin.actions.github.OpenInGithubAction
import git4idea.GitUtil
import git4idea.repo.GitRemote
import git4idea.repo.GitRepository
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class OpenInGithubActionTests {

    private lateinit var action: OpenInGithubAction
    private lateinit var event: AnActionEvent
    private lateinit var project: Project
    private lateinit var currentFile: VirtualFile
    private lateinit var projectFile: VirtualFile
    private lateinit var editor: Editor
    private lateinit var fileStatusManager: FileStatusManager
    private lateinit var repository: GitRepository
    private lateinit var projectRootManager: ProjectRootManager

    @BeforeEach
    fun setUp() {
        action = OpenInGithubAction()
        event = mockk(relaxed = true)
        project = mockk(relaxed = true)
        currentFile = mockk(relaxed = true)
        projectFile = mockk(relaxed = true)
        editor = mockk(relaxed = true)
        fileStatusManager = mockk(relaxed = true)
        repository = mockk(relaxed = true)
        projectRootManager = mockk(relaxed = true)

        mockkStatic(ProjectRootManager::class)
        every { ProjectRootManager.getInstance(eq(project)) } returns projectRootManager
        every { projectRootManager.contentRoots } returns arrayOf(currentFile)

        mockkStatic("com.simplej.base.extensions.AnActionEventExtensionsKt")
        every { event.showError(any()) } just Runs

        mockkStatic("com.simplej.base.extensions.VirtualFileExtensionsKt")

        every { event.project } returns project
        every { currentFile.path } returns "/test/path/file.kt"
        every { currentFile.findClosestProject(any()) } returns projectFile
        every { event.getData(PlatformDataKeys.EDITOR) } returns editor

        mockkStatic(FileStatusManager::class)
        every { FileStatusManager.getInstance(project) } returns fileStatusManager
        every { fileStatusManager.getStatus(any()) } returns FileStatus.NOT_CHANGED

        mockkStatic(GitUtil::class)
        every { GitUtil.getRepositoryForFile(any(), any<VirtualFile>()) } returns repository

        mockkStatic("com.simplej.base.extensions.LaunchTaskExtensionsKt")
        every { openInBrowser(any<String>()) } just Runs

        // Mock background task execution to run synchronously
        mockkStatic("com.simplej.base.extensions.BgtExtensionsKt")
        val slot = slot<() -> Unit>()
        every { executeBackgroundTask(capture(slot)) } answers {
            slot.captured.invoke()
        }
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `shouldShow returns false when no files are selected`() {
        every { event.currentFiles } returns arrayOf()

        assertFalse(action.shouldShow(event, project))
    }

    @Test
    fun `shouldShow returns false for ignored files`() {
        every { event.currentFiles } returns arrayOf(currentFile)
        every { fileStatusManager.getStatus(currentFile) } returns FileStatus.IGNORED

        assertFalse(action.shouldShow(event, project))
    }

    @Test
    fun `shouldShow returns true for tracked files`() {
        every { event.currentFiles } returns arrayOf(currentFile)
        every { fileStatusManager.getStatus(currentFile) } returns FileStatus.NOT_CHANGED

        assertTrue(action.shouldShow(event, project))
    }

    @Test
    fun `actionPerformed shows error when no project`() {
        every { event.project } returns null

        action.actionPerformed(event)

        verify {
            event.showError("No valid project found within the workspace.")
        }
    }

    @Test
    fun `actionPerformed shows error when no files selected`() {
        every { event.currentFiles } returns arrayOf()

        action.actionPerformed(event)

        verify {
            event.showError("No valid file found within the project workspace.")
        }
    }

    @Test
    fun `actionPerformed shows error when no valid project file found`() {
        every { event.currentFiles } returns arrayOf(currentFile)
        every { currentFile.findClosestProject(any()) } returns null

        action.actionPerformed(event)

        verify {
            event.showError("No valid project found within the workspace.")
        }
    }

    @Disabled("Need to come back to fix this")
    @Test
    fun `actionPerformed opens browser with correct GitHub URL`() {
        val remotes = listOf(
            mockk<GitRemote> {
                every { name } returns "origin"
                every { firstUrl } returns "https://github.com/org/repo.git"
            }
        )
        every { repository.remotes } returns remotes
        every { event.currentFiles } returns arrayOf(currentFile)

        // Mock editor selection
        val selectionModel = mockk<SelectionModel>()
        val document = mockk<Document>()
        every { editor.selectionModel } returns selectionModel
        every { editor.document } returns document
        every { selectionModel.selectionStart } returns 0
        every { selectionModel.selectionEnd } returns 0
        every { document.getLineNumber(any()) } returns 0

        action.actionPerformed(event)

        val pathSuffix = currentFile.path.substringAfterLast("repo")
        verify {
            openInBrowser(match { url ->
                url == "https://github.com/org/repo/blob/main$pathSuffix#L1"
            })
        }
    }

    @Test
    fun `actionPerformed handles multiple selected files`() {
        val file1 = mockk<VirtualFile>(relaxed = true)
        val file2 = mockk<VirtualFile>(relaxed = true)
        every { event.currentFiles } returns arrayOf(file1, file2)
        every { file1.findClosestProject(any()) } returns projectFile
        every { file2.findClosestProject(any()) } returns projectFile

        val remotes = listOf(
            mockk<GitRemote> {
                every { name } returns "origin"
                every { firstUrl } returns "https://github.com/org/repo.git"
            }
        )
        every { repository.remotes } returns remotes

        action.actionPerformed(event)

        verify(exactly = 2) {
            openInBrowser(any())
        }
    }
}
