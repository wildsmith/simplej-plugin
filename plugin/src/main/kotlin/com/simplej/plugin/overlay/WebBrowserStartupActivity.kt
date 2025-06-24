// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.overlay

import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.startup.ProjectActivity

/**
 * A project startup activity responsible for initializing the web browser overlay feature.
 *
 * This activity runs once when a project is opened. Its primary role is to register
 * a [FileChangeListener] on the project's message bus. This listener is crucial for
 * dynamically showing, hiding, or updating the [WebBrowserOverlay] as the user switches
 * between different files in the editor.
 *
 * By setting up this listener at startup, the plugin ensures that the browser overlay
 * functionality remains responsive to the user's navigation throughout the IDE session.
 */
internal class WebBrowserStartupActivity : ProjectActivity {

    /**
     * Executes the startup logic for the web browser feature.
     *
     * This method connects to the project's message bus and subscribes a new
     * [FileChangeListener] instance to file editor events.
     *
     * @param project The project being opened.
     */
    override suspend fun execute(project: Project) {
        // Register the file change listener at once
        project.messageBus.connect().subscribe(
            FileEditorManagerListener.FILE_EDITOR_MANAGER,
            FileChangeListener(project)
        )
    }
}
