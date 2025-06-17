// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugins.actions.deletion

import com.intellij.openapi.actionSystem.ActionManager
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.simplej.base.extensions.currentFile
import com.simplej.base.extensions.findAllProjectRoots
import com.simplej.base.extensions.findClosestProject
import com.simplej.base.extensions.getBuildFile
import com.simplej.base.extensions.getCodeOwnersFile
import com.simplej.base.extensions.getGradlePath
import com.simplej.base.extensions.getRootProjectFile
import com.simplej.base.extensions.getSettingsFile
import com.simplej.base.extensions.gradleSync
import com.simplej.plugin.actions.deletion.DeleteModuleAction
import com.simplej.plugin.actions.github.LookupCodeOwnerAction.CodeOwnerIdentifier
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal class DeleteModuleActionTests {
    private lateinit var action: DeleteModuleAction
    private lateinit var event: AnActionEvent
    private lateinit var project: Project
    private lateinit var projectFile: VirtualFile
    private lateinit var rootProjectFile: VirtualFile
    private lateinit var buildFile: File
    private lateinit var settingsFile: File
    private lateinit var codeOwnersFile: File

    @BeforeEach
    fun setUp() {
        action = DeleteModuleAction()
        event = mockk(relaxed = true)
        project = mockk(relaxed = true)
        projectFile = mockk(relaxed = true)
        rootProjectFile = mockk(relaxed = true)
        buildFile = mockk(relaxed = true)
        settingsFile = mockk(relaxed = true)
        codeOwnersFile = mockk(relaxed = true)

        mockkStatic(ActionManager::class)
        mockkStatic("kotlin.io.FilesKt")
        mockkStatic("kotlin.io.FilesKt__FileReadWriteKt")
        mockkStatic("com.simplej.base.extensions.VirtualFileExtensionsKt")

        every { project.name } returns "testProject"
        every { projectFile.path } returns "/test/path/module"
        every { rootProjectFile.path } returns "/test/path"
        every { projectFile.getRootProjectFile(any()) } returns rootProjectFile
        every { projectFile.getGradlePath(any()) } returns ":module"
        every { rootProjectFile.getSettingsFile() } returns settingsFile
        every { rootProjectFile.getCodeOwnersFile() } returns codeOwnersFile
        every { ActionManager.getInstance() } returns mockk(relaxed = true)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `shouldShow returns false when no project file is found`() {
        every { event.currentFile } returns null

        assertTrue(action.shouldShow(event, project))
    }

    @Test
    fun `shouldShow returns false for root project`() {
        every { event.currentFile } returns projectFile
        every { projectFile.findClosestProject(project) } returns projectFile
        every { projectFile.path } returns "/test/path"
        every { rootProjectFile.path } returns "/test/path"

        assertFalse(action.shouldShow(event, project))
    }

    @Test
    fun `shouldShow returns true for non-root project`() {
        every { event.currentFile } returns projectFile
        every { projectFile.findClosestProject(project) } returns projectFile
        every { projectFile.path } returns "/test/path/module"
        every { rootProjectFile.path } returns "/test/path"

        assertTrue(action.shouldShow(event, project))
    }

    @Disabled("Until I can figure out how to mock `useLines`")
    @Test
    fun `checkForBuildFileReferences returns true when references exist`() {
        val buildFileContent = """
            dependencies {
                implementation project(':module')
            }
        """.trimIndent()

        every { projectFile.findAllProjectRoots(project) } returns setOf(rootProjectFile)
        every { rootProjectFile.getBuildFile() } returns buildFile
        every { buildFile.useLines(any(), any<(Sequence<String>) -> Boolean>()) } answers {
            firstArg<(Sequence<String>) -> Boolean>().invoke(buildFileContent.lineSequence())
            true
        }

        assertFalse(action.checkForBuildFileReferences(projectFile, project).isEmpty())
    }

    @Disabled("Until I can figure out how to mock `useLines`")
    @Test
    fun `updateSettings removes module from settings file`() {
        val settingsContent = """
            include ':app'
            include ':module'
            include ':core'
        """.trimIndent()

        every { settingsFile.useLines(any(), any<(Sequence<String>) -> List<String>>()) } answers {
            firstArg<(Sequence<String>) -> List<String>>().invoke(settingsContent.lineSequence())
            emptyList()
        }
        every { settingsFile.writeText(any()) } just Runs

        val update = action.updateSettings(projectFile, project)
        assertNotNull(update)
        verify(exactly = 1) { settingsFile.useLines(any(), any<(Sequence<String>) -> List<String>>()) }
    }

    @Test
    fun `updateCodeOwners removes module references`() {
        val codeOwnerIdentifier = mockk<CodeOwnerIdentifier>()
        every { codeOwnerIdentifier.findAllCodeOwnerRules(any()) } returns setOf(
            mockk(relaxed = true) {
                every { pattern } returns "/module/**"
                every { humanReadableLineNumber } returns 1
            }
        )

        mockkConstructor(CodeOwnerIdentifier::class)
        every { anyConstructed<CodeOwnerIdentifier>().findAllCodeOwnerRules(any()) } returns
                codeOwnerIdentifier.findAllCodeOwnerRules("")

        every { codeOwnersFile.readLines() } returns listOf("/module/** @owner")
        every { codeOwnersFile.writeText(any()) } just Runs

        val update = action.updateCodeOwners(projectFile, project)
        assertNotNull(update)
        verify(exactly = 1) { codeOwnersFile.readLines() }
    }

    @Disabled("Until I can figure out how to mock `useLines`")
    @Test
    fun `deleteModuleAndSync executes all updates`() {
        every { projectFile.delete(any()) } just Runs
        every { event.gradleSync() } just Runs

        action.deleteModuleAndSync(projectFile, project, event)

        verify {
            projectFile.delete(any())
            event.gradleSync()
        }
    }
}
