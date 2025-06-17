// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin

import com.intellij.openapi.util.IconLoader
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import io.mockk.slot
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import javax.swing.Icon
import kotlin.test.assertNotNull
import kotlin.test.assertSame

internal class SimpleJIconsTests {

    private lateinit var mockIcon: Icon

    @BeforeEach
    fun setUp() {
        mockkStatic(IconLoader::class)
        mockIcon = mockk<Icon>()
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `test AndroidDevice icon is loaded, cached and reused`() {
        val slot = slot<String>()
        every { IconLoader.getIcon(capture(slot), any<Class<Any>>()) } returns mockIcon

        val firstAccess = SimpleJIcons.AndroidDevice

        assert(slot.captured.startsWith("/icons/expui/"))
        assert(slot.captured.endsWith(".svg"))
        assertNotNull(firstAccess)

        val secondAccess = SimpleJIcons.AndroidDevice
        assertNotNull(secondAccess)
        assertSame(firstAccess, secondAccess, "Icon instances should be the same due to caching")
        verify(exactly = 1) { IconLoader.getIcon("/icons/expui/androidDevice.svg", any<Class<Any>>()) }
    }
}
