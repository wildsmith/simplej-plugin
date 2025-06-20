// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.overlay

import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.StartupActivity

/**
 * Startup activity to register the file change listener for web browser overlays.
 */
internal class WebBrowserStartupActivity : StartupActivity {

    override fun runActivity(project: Project) {
        // Register the file change listener at once
        project.messageBus.connect().subscribe(
            com.intellij.openapi.fileEditor.FileEditorManagerListener.FILE_EDITOR_MANAGER,
            FileChangeListener(project)
        )
    }
}
