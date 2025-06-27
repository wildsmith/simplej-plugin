// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugins.actions.github

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.PlatformDataKeys
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.simplej.base.extensions.currentFile
import com.simplej.base.extensions.currentFiles
import com.simplej.base.extensions.executeBackgroundTask
import com.simplej.base.extensions.findClosestProject
import com.simplej.base.extensions.showError
import com.simplej.plugin.actions.github.CopyGithubLinkAction
import com.simplej.plugin.actions.github.getGithubUrl
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
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.StringSelection
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class CopyGithubLinkActionTests {

    private lateinit var action: CopyGithubLinkAction
    private lateinit var event: AnActionEvent
    private lateinit var project: Project
    private lateinit var currentFile: VirtualFile
    private lateinit var projectFile: VirtualFile
    private lateinit var editor: Editor
    private lateinit var copyPasteManager: CopyPasteManager
    private lateinit var projectRootManager: ProjectRootManager

    @BeforeEach
    fun setUp() {
        action = CopyGithubLinkAction()
        event = mockk(relaxed = true)
        project = mockk(relaxed = true)
        currentFile = mockk(relaxed = true)
        projectFile = mockk(relaxed = true)
        editor = mockk(relaxed = true)
        copyPasteManager = mockk(relaxed = true)
        projectRootManager = mockk(relaxed = true)

        mockkStatic(ProjectRootManager::class)
        every { ProjectRootManager.getInstance(eq(project)) } returns projectRootManager
        every { projectRootManager.contentRoots } returns arrayOf(currentFile)

        mockkStatic("com.simplej.base.extensions.AnActionEventExtensionsKt")
        every { event.showError(any()) } just Runs

        mockkStatic("com.simplej.base.extensions.VirtualFileExtensionsKt")

        every { event.project } returns project
        every { event.currentFile } returns currentFile
        every { currentFile.findClosestProject(any()) } returns projectFile
        every { event.getData(PlatformDataKeys.EDITOR) } returns editor

        mockkStatic(CopyPasteManager::class)
        every { CopyPasteManager.getInstance() } returns copyPasteManager

        // Mock executeBackgroundTask to run synchronously for testing
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

    @Disabled("Need to come back to fix this")
    @Test
    fun `shouldShow returns true when exactly one file is selected`() {
        every { event.currentFiles } returns arrayOf(currentFile)

        assertTrue(action.shouldShow(event, project))
    }

    @Disabled("Need to come back to fix this")
    @Test
    fun `shouldShow returns false when multiple files are selected`() {
        every { event.currentFiles } returns arrayOf(currentFile, mockk<VirtualFile>())

        assertFalse(action.shouldShow(event, project))
    }

    @Test
    fun `shouldShow returns false when no files are selected`() {
        every { event.currentFiles } returns emptyArray()

        assertFalse(action.shouldShow(event, project))
    }

    @Test
    fun `actionPerformed shows error when no project is available`() {
        every { event.project } returns null

        action.actionPerformed(event)

        verify {
            event.showError("No valid project found within the workspace.")
        }
        verify(exactly = 0) {
            CopyPasteManager.getInstance()
        }
    }

    @Test
    fun `actionPerformed shows error when no current file is available`() {
        every { event.currentFile } returns null

        action.actionPerformed(event)

        verify {
            event.showError("No valid file found within the project workspace.")
        }
        verify(exactly = 0) {
            CopyPasteManager.getInstance()
        }
    }

    @Disabled("Need to come back to fix this")
    @Test
    fun `actionPerformed shows error when no project file is found`() {
        every { currentFile.findClosestProject(any()) } returns null

        action.actionPerformed(event)

        verify {
            event.showError("No valid project found within the workspace.")
        }
        verify(exactly = 0) {
            CopyPasteManager.getInstance()
        }
    }

    @Test
    fun `actionPerformed copies GitHub URL to clipboard when available`() {
        val githubUrl = "https://github.com/org/repo/blob/main/file.kt"

        mockkStatic("com.simplej.plugin.actions.github.GithubTrackedCodeActionKt")
        every {
            project.getGithubUrl(editor, currentFile)
        } returns githubUrl

        action.actionPerformed(event)

        verify {
            copyPasteManager.setContents(match {
                (it as StringSelection).getTransferData(DataFlavor.stringFlavor).toString() == githubUrl
            })
        }
    }

    @Test
    fun `actionPerformed does nothing when GitHub URL is not available`() {
        mockkStatic("com.simplej.plugin.actions.github.GithubTrackedCodeActionKt")
        every {
            project.getGithubUrl(editor, currentFile)
        } returns null

        action.actionPerformed(event)

        verify(exactly = 0) {
            copyPasteManager.setContents(any())
        }
    }
}
