// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.base.extensions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.CommonDataKeys
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VirtualFile
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class VirtualFileExtensionsTests {

    private lateinit var event: AnActionEvent
    private lateinit var virtualFile: VirtualFile
    private lateinit var virtualFiles: Array<VirtualFile>
    private lateinit var file: File
    private lateinit var localFileSystem: LocalFileSystem
    private lateinit var project: Project
    private lateinit var projectRootManager: ProjectRootManager
    private lateinit var contentRoots: Array<VirtualFile>
    private lateinit var buildFile: File
    private lateinit var settingsFile: File
    private lateinit var rootProject: VirtualFile

    @BeforeEach
    fun setUp() {
        event = mockk()
        virtualFile = mockk()
        virtualFiles = arrayOf(mockk(), mockk())
        file = mockk()
        localFileSystem = mockk()
        mockkStatic(LocalFileSystem::class)
        every { LocalFileSystem.getInstance() } returns localFileSystem

        project = mockk()
        projectRootManager = mockk()
        buildFile = mockk()
        contentRoots = arrayOf(mockk(), mockk())

        mockkStatic(ProjectRootManager::class)
        every { ProjectRootManager.getInstance(project) } returns projectRootManager
        every { projectRootManager.contentRoots } returns contentRoots
        every { virtualFile.path } returns "/test/path"
        every { virtualFile.name } returns "path"

        settingsFile = mockk()

        rootProject = mockk()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `currentFile returns virtual file from event data`() {
        every { event.getData(CommonDataKeys.VIRTUAL_FILE) } returns virtualFile

        val result = event.currentFile

        assertEquals(result, virtualFile)
        verify { event.getData(CommonDataKeys.VIRTUAL_FILE) }
    }

    @Test
    fun `currentFile returns null when no file is selected`() {
        every { event.getData(CommonDataKeys.VIRTUAL_FILE) } returns null

        val result = event.currentFile

        assertNull(result, null)
        verify { event.getData(CommonDataKeys.VIRTUAL_FILE) }
    }

    @Test
    fun `currentFiles returns array of virtual files from event data`() {
        every { event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) } returns virtualFiles

        val result = event.currentFiles

        assertTrue(result.contentEquals(virtualFiles))
        verify { event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) }
    }

    @Test
    fun `currentFiles returns empty array when no files are selected`() {
        every { event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) } returns null

        val result = event.currentFiles

        assertTrue(result.isEmpty())
        verify { event.getData(CommonDataKeys.VIRTUAL_FILE_ARRAY) }
    }


    @Test
    fun `toVirtualFile converts File to VirtualFile`() {
        every { localFileSystem.findFileByIoFile(file) } returns virtualFile

        val result = file.toVirtualFile()

        assertEquals(result, virtualFile)
        verify {
            LocalFileSystem.getInstance()
            localFileSystem.findFileByIoFile(file)
        }
    }

    @Test
    fun `toVirtualFile returns null when file is not found`() {
        every { localFileSystem.findFileByIoFile(file) } returns null

        val result = file.toVirtualFile()

        assertNull(result)
        verify {
            LocalFileSystem.getInstance()
            localFileSystem.findFileByIoFile(file)
        }
    }

    @Test
    fun `getBuildFile returns kotlin build file when it exists`() {
        mockkConstructor(File::class)
        every { anyConstructed<File>().exists() } returns true

        val result = virtualFile.getBuildFile()

        assertNotNull(result)
        verify { anyConstructed<File>().exists() }
    }

    @Test
    fun `getSettingsFile returns kotlin settings file when it exists`() {
        mockkConstructor(File::class)
        every { anyConstructed<File>().exists() } returns true

        val result = virtualFile.getSettingsFile()

        assertNotNull(result)
        verify { anyConstructed<File>().exists() }
    }

    @Test
    fun `getCodeOwnersFile returns file with correct path`() {
        val result = virtualFile.getCodeOwnersFile()

        assertEquals(result.path, "/test/path/.github/CODEOWNERS")
    }

    @Test
    fun `exists returns false for null file`() {
        val file: File? = null

        assertTrue(!file.exists())
    }

    @Test
    fun `exists returns true for existing file`() {
        val file: File? = mockk()
        every { file.exists() } returns true

        assertTrue(file.exists())
    }

    @Test
    fun `exists returns false for non-existing file`() {
        val file: File? = mockk()
        every { file.exists() } returns false

        assertTrue(!file.exists())
    }
}
