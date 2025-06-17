// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.base.extensions

import com.intellij.ide.BrowserUtil
import com.intellij.ide.browsers.WebBrowserUrlProvider
import io.mockk.every
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class LaunchTaskExtensionsTests {

    @BeforeEach
    fun setUp() {
        mockkStatic(BrowserUtil::class)
    }

    @AfterEach
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `openInBrowser delegates to BrowserUtil browse`() {
        val url = "https://www.example.com"
        every { BrowserUtil.browse(url) } just runs

        openInBrowser(url)

        verify(exactly = 1) { BrowserUtil.browse(url) }
    }

    @Test
    fun `openInBrowser propagates BrowserException`() {
        val url = "invalid://url"
        every { BrowserUtil.browse(url) } throws WebBrowserUrlProvider.BrowserException("Failed to open browser")

        assertThrows<WebBrowserUrlProvider.BrowserException> {
            openInBrowser(url)
        }

        verify(exactly = 1) { BrowserUtil.browse(url) }
    }
}
