// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.base

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.Presentation
import io.mockk.every
import io.mockk.mockk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

internal class SimpleJActionGroupTests {

    private lateinit var actionGroup: SimpleJActionGroup
    private lateinit var mockEvent: AnActionEvent
    private lateinit var mockPresentation: Presentation

    @BeforeEach
    fun setup() {
        actionGroup = SimpleJActionGroup()
        mockEvent = mockk<AnActionEvent>(relaxed = true)
        mockPresentation = mockk<Presentation>(relaxed = true)
        every { mockEvent.presentation } returns mockPresentation
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test action update thread is background`() {
        assertEquals(ActionUpdateThread.BGT, actionGroup.actionUpdateThread)
    }

    @Test
    fun `test update sets hideGroupIfEmpty to true`() {
        actionGroup.update(mockEvent)

        verify(exactly = 1) { mockPresentation.isHideGroupIfEmpty = true }
    }

    @Test
    fun `test presentation property is accessed only once during update`() {
        actionGroup.update(mockEvent)

        verify(exactly = 1) { mockEvent.presentation }
    }

    @Test
    fun `test update behavior with multiple calls`() {
        actionGroup.update(mockEvent)
        actionGroup.update(mockEvent)
        actionGroup.update(mockEvent)

        verify(exactly = 3) { mockPresentation.isHideGroupIfEmpty = true }
        verify(exactly = 3) { mockEvent.presentation }
    }
}
