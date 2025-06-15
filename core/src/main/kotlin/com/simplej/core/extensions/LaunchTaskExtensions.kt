// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.core.extensions

import com.intellij.ide.BrowserUtil

/**
 * Opens the specified URL in the system's default web browser.
 *
 * This utility function provides a convenient wrapper around IntelliJ's [BrowserUtil] to open web links. The
 * function handles the URL opening in a platform-independent way, using the system's default browser.
 *
 * @param url The URL to open in the browser. Should be a valid URL string
 * @return The result of [BrowserUtil.browse] operation
 *
 * @throws [com.intellij.ide.BrowserException] if the URL cannot be opened
 * @see com.intellij.ide.BrowserUtil.browse
 */
fun openInBrowser(url: String) =
    BrowserUtil.browse(url)
