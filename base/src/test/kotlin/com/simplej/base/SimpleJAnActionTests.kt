// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.base

import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import com.intellij.openapi.project.Project
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class SimpleJAnActionTests {

    private lateinit var event: AnActionEvent
    private lateinit var project: Project
    private lateinit var presentation: Presentation

    @BeforeEach
    fun setup() {
        project = mockk()
        presentation = mockk(relaxed = true)
        event = mockk {
            every { project } returns this@SimpleJAnActionTests.project
            every { presentation } returns this@SimpleJAnActionTests.presentation
        }
    }

    @Test
    fun `getActionUpdateThread should return BGT`() {
        val action = FakeAction()
        assertEquals(ActionUpdateThread.BGT, action.actionUpdateThread)
    }

    @Test
    fun `update should hide action when project is null`() {
        val action = FakeAction()
        val eventWithNullProject = mockk<AnActionEvent> {
            every { project } returns null
            every { presentation } returns this@SimpleJAnActionTests.presentation
        }

        action.update(eventWithNullProject)

        verify { presentation.isVisible = false }
    }

    @Test
    fun `update should hide action when place is not supported`() {
        val action = FakeAction()
        every { event.place } returns "UNKNOWN_PLACE"

        action.update(event)

        verify { presentation.isVisible = false }
    }


    @Test
    fun `should be visible in project view for ProjectViewPopupMenuItem`() {
        val action = FakeMenuAction()
        every { event.place } returns ActionPlaces.PROJECT_VIEW_POPUP

        action.update(event)

        verify { presentation.isVisible = true }
    }

    @Test
    fun `should respect custom shouldShow implementation`() {
        val action = object : FakeMenuAction() {
            override fun shouldShow(event: AnActionEvent, project: Project) = false
        }
        every { event.place } returns ActionPlaces.PROJECT_VIEW_POPUP

        action.update(event)

        verify { presentation.isVisible = false }
    }

    @Test
    fun `should be visible in editor popup for EditorPopupMenuItem`() {
        val action = FakeEditorAction()
        every { event.place } returns ActionPlaces.EDITOR_POPUP

        action.update(event)

        verify { presentation.isVisible = true }
    }

    @Test
    fun `should not be visible in project view for EditorPopupMenuItem`() {
        val action = FakeEditorAction()
        every { event.place } returns ActionPlaces.PROJECT_VIEW_POPUP

        action.update(event)

        verify { presentation.isVisible = false }
    }

    @Test
    fun `should allow implementing both ProjectView and EditorPopup interfaces`() {
        val action = FakeMenuAndEditorAction()

        every { event.place } returns ActionPlaces.PROJECT_VIEW_POPUP
        action.update(event)
        verify { presentation.isVisible = true }

        every { event.place } returns ActionPlaces.EDITOR_POPUP
        action.update(event)
        verify { presentation.isVisible = true }
    }

    private class FakeAction : SimpleJAnAction() {
        override fun actionPerformed(p0: AnActionEvent) {
            TODO("Not yet implemented")
        }
    }

    private open class FakeMenuAction : SimpleJAnAction(), ProjectViewPopupMenuItem {
        override fun actionPerformed(p0: AnActionEvent) {
            TODO("Not yet implemented")
        }
    }

    private class FakeEditorAction : SimpleJAnAction(), EditorPopupMenuItem {
        override fun actionPerformed(p0: AnActionEvent) {
            TODO("Not yet implemented")
        }
    }

    private class FakeMenuAndEditorAction : SimpleJAnAction(), ProjectViewPopupMenuItem, EditorPopupMenuItem {
        override fun actionPerformed(p0: AnActionEvent) {
            TODO("Not yet implemented")
        }
    }
}
