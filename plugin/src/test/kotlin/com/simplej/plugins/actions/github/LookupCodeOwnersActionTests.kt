// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugins.actions.github

import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.simplej.base.extensions.currentFile
import com.simplej.base.extensions.currentFiles
import com.simplej.base.extensions.getCodeOwnersFile
import com.simplej.base.extensions.getRootProjectFile
import com.simplej.base.extensions.showError
import com.simplej.base.extensions.showNotification
import com.simplej.plugin.actions.github.LookupCodeOwnerAction
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class LookupCodeOwnersActionTests {

    private lateinit var action: LookupCodeOwnerAction
    private lateinit var event: AnActionEvent
    private lateinit var project: Project
    private lateinit var currentFile: VirtualFile
    private lateinit var rootProjectFile: VirtualFile
    private lateinit var copyPasteManager: CopyPasteManager
    private lateinit var projectRootManager: ProjectRootManager

    @TempDir
    lateinit var tempDir: Path

    @BeforeEach
    fun setUp() {
        action = LookupCodeOwnerAction()
        event = mockk(relaxed = true)
        project = mockk(relaxed = true)
        currentFile = mockk(relaxed = true)
        rootProjectFile = mockk(relaxed = true)
        copyPasteManager = mockk(relaxed = true)
        projectRootManager = mockk(relaxed = true)

        mockkStatic(ProjectRootManager::class)
        every { ProjectRootManager.getInstance(eq(project)) } returns projectRootManager
        every { projectRootManager.contentRoots } returns arrayOf(currentFile)

        mockkStatic("com.simplej.base.extensions.AnActionEventExtensionsKt")
        every { event.showError(any()) } just Runs

        mockkStatic("com.simplej.base.extensions.ProjectExtensionsKt")
        every { project.showNotification(any(), any(), any(), any()) } just Runs

        mockkStatic("com.simplej.base.extensions.VirtualFileExtensionsKt")

        every { event.project } returns project
        every { event.currentFile } returns currentFile
        every { project.name } returns "testProject"
        every { currentFile.path } returns "/test/path/file.kt"
        every { currentFile.getRootProjectFile(any()) } returns rootProjectFile

        mockkStatic(CopyPasteManager::class)
        every { CopyPasteManager.getInstance() } returns copyPasteManager
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `shouldShow returns false when multiple files are selected`() {
        every { event.currentFiles } returns arrayOf(mockk(), mockk())

        assertFalse(action.shouldShow(event, project))
    }

    @Test
    fun `shouldShow returns false when no files are selected`() {
        every { event.currentFiles } returns arrayOf()

        assertFalse(action.shouldShow(event, project))
    }

    @Test
    fun `shouldShow returns false when CODEOWNERS file does not exist`() {
        every { event.currentFiles } returns arrayOf(currentFile)
        every { rootProjectFile.getCodeOwnersFile().exists() } returns false

        assertFalse(action.shouldShow(event, project))
    }

    @Test
    fun `shouldShow returns true when single file selected and CODEOWNERS exists`() {
        every { event.currentFiles } returns arrayOf(currentFile)
        every { rootProjectFile.getCodeOwnersFile().exists() } returns true

        assertTrue(action.shouldShow(event, project))
    }

    @Test
    fun `actionPerformed shows error when no current file`() {
        every { event.currentFile } returns null

        action.actionPerformed(event)

        verify {
            event.showError("No valid file found within the project workspace.")
        }
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
    fun `actionPerformed shows error when no root project file`() {
        every { currentFile.getRootProjectFile(any()) } returns null

        action.actionPerformed(event)

        verify {
            event.showError("No valid project found within the workspace.")
        }
    }

    @Test
    fun `actionPerformed shows notification with code owners`() {
        val codeOwnersFile = File(tempDir.toFile(), "CODEOWNERS").apply {
            writeText("*.kt @kotlin-team\n")
        }

        every { rootProjectFile.getCodeOwnersFile() } returns codeOwnersFile

        action.actionPerformed(event)

        verify {
            project.showNotification(
                message = "Code Owners: @kotlin-team",
                actions = match { actions ->
                    actions.size == 2 &&
                            actions.any { it is NotificationAction }
                }
            )
        }
    }

    @Test
    fun `CodeOwnerRule matches correct patterns`() {
        val rule = LookupCodeOwnerAction.CodeOwnerRule("*.kt", listOf("@team"), 1)

        assertTrue(rule.matches("file.kt"))
        assertFalse(rule.matches("file.java"))
    }

    @Test
    fun `CodeOwnerRule handles directory patterns`() {
        val rule = LookupCodeOwnerAction.CodeOwnerRule("src/", listOf("@team"), 1)

        assertTrue(rule.matches("src/file.kt"))
        assertTrue(rule.matches("src/nested/file.kt"))
        assertFalse(rule.matches("test/file.kt"))
    }

    @Test
    fun `CodeOwnerIdentifier finds most specific rule`() {
        val codeOwnersFile = File(tempDir.toFile(), "CODEOWNERS").apply {
            writeText(
                """
                * @default-team
                *.kt @kotlin-team
                src/main/ @main-team
            """.trimIndent()
            )
        }

        val identifier = LookupCodeOwnerAction.CodeOwnerIdentifier(codeOwnersFile)
        val rule = identifier.findCodeOwnerRule("src/main/file.kt")

        assertNotNull(rule)
        assertEquals("@main-team", rule.owners[0])
    }

    @Test
    fun `CodeOwnerIdentifier finds all matching rules`() {
        val codeOwnersFile = File(tempDir.toFile(), "CODEOWNERS").apply {
            writeText(
                """
                * @default-team
                *.kt @kotlin-team
                src/main/ @main-team
            """.trimIndent()
            )
        }

        val identifier = LookupCodeOwnerAction.CodeOwnerIdentifier(codeOwnersFile)
        val rules = identifier.findAllCodeOwnerRules("src/main/file.kt")

        assertEquals(3, rules.size)
        assertTrue(rules.any { "@main-team" in it.owners })
        assertTrue(rules.any { "@kotlin-team" in it.owners })
        assertTrue(rules.any { "@default-team" in it.owners })
    }
}
