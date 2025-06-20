// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.overlay

import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.event.EditorFactoryEvent
import com.intellij.openapi.editor.event.EditorFactoryListener
import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.simplej.base.extensions.showError
import com.simplej.plugin.actions.settings.SimpleJSettings
import com.simplej.plugin.simpleJConfig
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.util.concurrent.ConcurrentHashMap
import javax.swing.SwingUtilities

internal class WebBrowserOverlayListener : EditorFactoryListener {

    private val overlays = ConcurrentHashMap<Editor, WebBrowserOverlay>()

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

    override fun editorReleased(event: EditorFactoryEvent) =
        removeOverlay(event.editor)

    /**
     * Checks if the given file has a URL mapping and creates an overlay if it does.
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
     * Browser builder
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
            editor.project?.showError(
                "Unable to create browser overlay. Please report this issue to the plugin " +
                        "author."
            )
        }
    }

    /**
     * Without this, the browser can get stuck, so we want to revalidate the component often to make sure its not borked
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
                    editor.project?.showError(
                        "Unable to dispose browser overlay. Please report this issue to the " +
                                "plugin author."
                    )
                }
            }
        }
    }

    /**
     * Updates the overlay URL when switching to a different file that also has a mapping.
     * This is called when the same editor shows a different file.
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
