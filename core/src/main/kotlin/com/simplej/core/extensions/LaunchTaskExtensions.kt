package com.simplej.core.extensions

import com.intellij.ide.BrowserUtil

fun openInBrowser(url: String) =
    BrowserUtil.browse(url)
