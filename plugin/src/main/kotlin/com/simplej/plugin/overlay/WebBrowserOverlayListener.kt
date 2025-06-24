// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.overlay

import com.intellij.openapi.diagnostic.thisLogger
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.simplej.plugin.actions.settings.SimpleJSettings
import com.simplej.plugin.simpleJConfig
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.util.concurrent.ConcurrentHashMap
import javax.swing.SwingUtilities

/**
 * Manages the lifecycle of [WebBrowserOverlay] instances within editors.
 *
 * This class listens for editor creation and release events via [EditorFactoryListener]. When an editor is created
 * for a file that has a corresponding URL mapping in the project's configuration, this listener adds a
 * semi-transparent browser overlay to that editor. It also handles the proper removal and disposal of these overlays
 * when editors are closed or when the file context changes.
 *
 * It is implemented as a singleton to provide a central point of management for all browser overlays.
 */
internal class WebBrowserOverlayListener : EditorFactoryListener {

    private val overlays = ConcurrentHashMap<Editor, WebBrowserOverlay>()

    /**
     * Called when a new editor is created in the IDE.
     *
     * This method checks if the inline browser feature is enabled and if the file opened in the editor has a URL
     * mapping. If so, it creates and attaches a [WebBrowserOverlay]. It also adds a component listener to handle
     * resizing of the editor, ensuring the overlay adjusts its bounds accordingly.
     *
     * @param event The event containing the newly created editor.
     */
    @Suppress("ReturnCount")
    override fun editorCreated(event: EditorFactoryEvent) {
        if (!SimpleJSettings.instance.state.inlineBrowserEnabled) return
        val editor = event.editor
        val project = editor.project ?: return
        val document = editor.document
        val file = FileDocumentManager.getInstance().getFile(document) ?: return

        // Check if this file has a URL mapping to our json file
        checkAndCreateOverlay(editor, project, file)

        // Add component listener to handle editor resizing
        editor.contentComponent.addComponentListener(
            object : ComponentAdapter() {
                override fun componentResized(e: ComponentEvent) {
                    overlays[editor]?.let { overlay ->
                        SwingUtilities.invokeLater {
                            overlay.updateBounds()
                            overlay.revalidate()
                            overlay.repaint()
                        }
                    }
                }
            }
        )
    }

    /**
     * Called when an editor is released (closed).
     *
     * This ensures that any browser overlay associated with the closed editor is properly disposed of and removed
     * from the UI to prevent memory leaks.
     *
     * @param event The event containing the editor being released.
     */
    override fun editorReleased(event: EditorFactoryEvent) =
        removeOverlay(event.editor)

    /**
     * Checks if the given file has a URL mapping and creates a browser overlay if it does.
     *
     * If a mapping exists, [createOverlay] is called. If not, [removeOverlay] is called to ensure no outdated
     * overlay persists for that editor instance.
     *
     * @param editor The editor to potentially add the overlay to.
     * @param project The current project.
     * @param file The file being displayed in the editor.
     */
    private fun checkAndCreateOverlay(editor: Editor, project: Project, file: VirtualFile) {
        val projectPath = project.basePath ?: return
        val filePath = file.path

        // Get URL for this file
        val url = project.simpleJConfig()?.webBrowserMappings?.getUrlForFile(projectPath, filePath)
        if (url != null) {
            SwingUtilities.invokeLater {
                createOverlay(editor, url)
            }
        } else {
            // Remove overlay if the file no longer has a mapping
            removeOverlay(editor)
        }
    }

    /**
     * Creates and attaches a [WebBrowserOverlay] to the specified editor.
     *
     * This method handles the instantiation of the browser component, adds it to the editor's content pane, and
     * ensures it is rendered correctly on top of the text. It includes error handling to prevent failures during
     * browser initialization from crashing the IDE.
     *
     * @param editor The target editor for the overlay.
     * @param url The URL to be loaded in the browser overlay.
     */
    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    private fun createOverlay(editor: Editor, url: String) {
        // Remove the existing overlay if present
        removeOverlay(editor)

        try {
            val overlay = WebBrowserOverlay(editor, url)
            overlays[editor] = overlay

            // Get the editor's content component (where the actual text is)
            val editorComponent = editor.contentComponent
            editorComponent.add(overlay)
            editorComponent.setComponentZOrder(overlay, 0) // Bring to the front

            // Ensure overlay is positioned correctly
            SwingUtilities.invokeLater {
                overlay.updateBounds()
                overlay.revalidate()
                overlay.repaint()
                editorComponent.revalidate()
                editorComponent.repaint()
            }

        } catch (e: Exception) {
            // Handle any issues with browser creation
            editor.project?.thisLogger()?.info(
                "Unable to create browser overlay. Please report this issue to the plugin " +
                        "author.",
                e
            )
        }
    }

    /**
     * Safely removes and disposes of a browser overlay from an editor.
     *
     * This method is crucial for preventing UI glitches and resource leaks. It ensures that the browser component is
     * properly disposed of and removed from the Swing component tree, then forces a repaint of the editor.
     *
     * @param editor The editor from which to remove the overlay.
     */
    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    private fun removeOverlay(editor: Editor) {
        overlays.remove(editor)?.let { overlay ->
            SwingUtilities.invokeLater {
                try {
                    overlay.dispose()
                    editor.contentComponent.remove(overlay)
                    editor.contentComponent.revalidate()
                    editor.contentComponent.repaint()
                } catch (e: Exception) {
                    // Handle disposal errors gracefully
                    editor.project?.thisLogger()?.info(
                        "Unable to dispose browser overlay. Please report this issue to the " +
                                "plugin author.",
                        e
                    )
                }
            }
        }
    }

    /**
     * Updates the overlay for an editor when its displayed file changes.
     *
     * This handles cases like switching tabs where the same editor component is reused for a different file. It
     * checks the new file for a URL mapping and updates the existing overlay's URL, creates a new overlay, or
     * removes the overlay as appropriate.
     *
     * @param editor The editor whose file context has changed.
     * @param file The new file being displayed in the editor.
     */
    fun updateOverlayForFile(editor: Editor, file: VirtualFile) {
        val project = editor.project ?: return
        val projectPath = project.basePath ?: return
        val filePath = file.path

        val url = project.simpleJConfig()?.webBrowserMappings?.getUrlForFile(projectPath, filePath)
        if (url != null) {
            val existingOverlay = overlays[editor]
            if (existingOverlay != null) {
                // Update the existing overlay with new URL
                SwingUtilities.invokeLater {
                    existingOverlay.updateUrl(url)
                }
            } else {
                // Create a new overlay
                SwingUtilities.invokeLater {
                    createOverlay(editor, url)
                }
            }
        } else {
            // Remove overlay if the new file doesn't have a mapping
            removeOverlay(editor)
        }
    }

    companion object {
        private val instance = WebBrowserOverlayListener()

        fun getInstance(): WebBrowserOverlayListener = instance
    }
}
