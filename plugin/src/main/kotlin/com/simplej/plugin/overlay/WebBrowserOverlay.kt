// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.overlay

import com.intellij.icons.AllIcons
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.ui.jcef.JBCefBrowser
import com.intellij.util.ui.UIUtil
import com.simplej.base.extensions.openInBrowser
import com.simplej.base.extensions.showNotification
import java.awt.BorderLayout
import java.awt.Color
import java.awt.Container
import java.awt.Cursor
import java.awt.Dimension
import java.awt.FlowLayout
import java.awt.Point
import java.awt.Rectangle
import java.awt.datatransfer.StringSelection
import java.awt.event.MouseAdapter
import java.awt.event.MouseEvent
import javax.swing.BorderFactory
import javax.swing.JButton
import javax.swing.JLabel
import javax.swing.JPanel
import javax.swing.SwingConstants
import javax.swing.SwingUtilities
import javax.swing.border.EmptyBorder

/**
 * Web browser overlay component that displays a collapsible, resizable mini browser in the bottom right corner of
 * the editor.
 */
@Suppress("TooManyFunctions", "MagicNumber")
internal class WebBrowserOverlay(
    private val editor: Editor,
    private val url: String
) : JPanel() {

    private val browser: JBCefBrowser = JBCefBrowser()
    private val globeIcon: JLabel
    private val browserPanel: JPanel
    private val headerPanel: JPanel
    private var isCollapsed = false
    private var overlayBounds = Rectangle(0, 0, 400, 300)

    init {
        // Make the overlay panel completely transparent and non-interfering
        layout = null
        isOpaque = false
        background = null
        border = null

        // Create globe icon for collapsed state
        globeIcon = JLabel(AllIcons.General.Web).apply {
            size = Dimension(COLLAPSED_SIZE, COLLAPSED_SIZE)
            preferredSize = Dimension(COLLAPSED_SIZE, COLLAPSED_SIZE)
            isOpaque = true
            background = UIUtil.getPanelBackground()
            border = BorderFactory.createRaisedBevelBorder()
            horizontalAlignment = SwingConstants.CENTER
            verticalAlignment = SwingConstants.CENTER
            cursor = Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)

            addMouseListener(object : MouseAdapter() {
                override fun mouseClicked(e: MouseEvent) {
                    expand()
                }
            })
        }

        browserPanel = JPanel(BorderLayout()).apply {
            border = BorderFactory.createRaisedBevelBorder()
            background = UIUtil.getPanelBackground()
            size = Dimension(overlayBounds.width, overlayBounds.height)
            preferredSize = Dimension(overlayBounds.width, overlayBounds.height)
        }

        headerPanel = createHeaderPanel()
        setupBrowser()
        collapse()

        // Add resize listener
        setupResizeHandling()
    }

    private fun createHeaderPanel(): JPanel {
        val header = JPanel(BorderLayout()).apply {
            background = UIUtil.getPanelBackground()
            border = EmptyBorder(2, 5, 2, 5)
            preferredSize = Dimension(0, 25)
        }

        // URL label (truncated)
        val urlLabel = JLabel(truncateUrl(url)).apply {
            foreground = UIUtil.getLabelForeground()
            font = UIUtil.getFont(UIUtil.FontSize.SMALL, null)
        }

        val buttonPanel = JPanel(FlowLayout(FlowLayout.RIGHT, 2, 0)).apply {
            isOpaque = false
        }

        // External browser button
        val externalButton = JButton(AllIcons.Ide.External_link_arrow).apply {
            isOpaque = false
            isBorderPainted = false
            isContentAreaFilled = false
            preferredSize = Dimension(20, 20)
            toolTipText = "Open in external browser"

            addActionListener {
                openInExternalBrowser()
                collapse()
            }
        }

        // Collapse button
        val collapseButton = JButton(AllIcons.General.CollapseComponent).apply {
            isOpaque = false
            isBorderPainted = false
            isContentAreaFilled = false
            preferredSize = Dimension(20, 20)
            toolTipText = "Collapse"

            addActionListener { collapse() }
        }

        buttonPanel.add(externalButton)
        buttonPanel.add(collapseButton)

        header.add(urlLabel, BorderLayout.CENTER)
        header.add(buttonPanel, BorderLayout.EAST)

        return header
    }

    private fun setupBrowser() {
        browser.loadURL(url)
        browserPanel.add(headerPanel, BorderLayout.NORTH)
        browserPanel.add(browser.component, BorderLayout.CENTER)
    }

    private fun setupResizeHandling() {
        val resizeHandle = JLabel().apply {
            preferredSize = Dimension(10, 10)
            cursor = Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR)
            isOpaque = true
            background = Color.GRAY
        }

        var startPoint: Point? = null
        var startBounds: Rectangle? = null

        val mouseAdapter = object : MouseAdapter() {
            override fun mousePressed(e: MouseEvent) {
                startPoint = Point(e.point)
                SwingUtilities.convertPointToScreen(startPoint!!, resizeHandle)
                startBounds = Rectangle(overlayBounds)
            }

            override fun mouseDragged(e: MouseEvent) {
                if (startPoint != null && startBounds != null) {
                    val currentPoint = Point(e.point)
                    SwingUtilities.convertPointToScreen(currentPoint, resizeHandle)
                    val deltaX = currentPoint.x - startPoint!!.x
                    val deltaY = currentPoint.y - startPoint!!.y

                    val newWidth = Math.max(MIN_WIDTH, startBounds!!.width + deltaX)
                    val newHeight = Math.max(MIN_HEIGHT, startBounds!!.height + deltaY)

                    overlayBounds.width = newWidth
                    overlayBounds.height = newHeight

                    updateBounds()
                }
            }
        }

        resizeHandle.addMouseListener(mouseAdapter)
        resizeHandle.addMouseMotionListener(mouseAdapter)

        browserPanel.add(resizeHandle, BorderLayout.SOUTH)
    }

    private fun collapse() {
        isCollapsed = true
        removeAll()
        add(globeIcon)
        updateBounds()
        revalidate()
        repaint()
    }

    private fun expand() {
        isCollapsed = false
        removeAll()
        add(browserPanel)
        updateBounds()
        revalidate()
        repaint()
    }

    fun updateBounds() {
        val editorComponent = editor.contentComponent
        val editorSize = editorComponent.size

        if (isCollapsed) {
            // Position globe icon in bottom-right corner of editor content
            bounds = Rectangle(
                editorSize.width - COLLAPSED_SIZE - 10,
                editorSize.height - COLLAPSED_SIZE - 10,
                COLLAPSED_SIZE,
                COLLAPSED_SIZE
            )
            // Globe icon fills the entire overlay
            globeIcon.bounds = Rectangle(0, 0, COLLAPSED_SIZE, COLLAPSED_SIZE)
        } else {
            // Position browser in bottom-right corner of editor content
            bounds = Rectangle(
                editorSize.width - overlayBounds.width - 10,
                editorSize.height - overlayBounds.height - 10,
                overlayBounds.width,
                overlayBounds.height
            )
            // Browser panel fills the entire overlay
            browserPanel.bounds = Rectangle(0, 0, overlayBounds.width, overlayBounds.height)
        }

        // Ensure the overlay stays within editor content bounds
        if (bounds.x < 0) bounds.x = 10
        if (bounds.y < 0) bounds.y = 10
        if (bounds.x + bounds.width > editorSize.width) {
            bounds.x = editorSize.width - bounds.width - 10
        }
        if (bounds.y + bounds.height > editorSize.height) {
            bounds.y = editorSize.height - bounds.height - 10
        }
    }

    @Suppress("SwallowedException", "TooGenericExceptionCaught")
    private fun openInExternalBrowser() {
        try {
            openInBrowser(url)
        } catch (e: Exception) {
            // Fallback: copy URL to clipboard
            CopyPasteManager.getInstance().setContents(StringSelection(url))
            editor.project?.showNotification("Unable to open in external browser. URL copied to clipboard instead.")
        }
    }

    /**
     * Long text will cause ui issues mortal eyes shouldn't see
     */
    private fun truncateUrl(url: String): String {
        return if (url.length > LONG_URL_CHAR_LIMIT) {
            url.take(LONG_URL_CHAR_DROP) + "..."
        } else {
            url
        }
    }

    /**
     * Updates the browser URL when switching to a different file.
     */
    fun updateUrl(newUrl: String) {
        browser.loadURL(newUrl)

        // Update header label
        val urlLabel = findUrlLabel(headerPanel)
        urlLabel?.text = truncateUrl(newUrl)
    }

    @Suppress("ReturnCount")
    private fun findUrlLabel(container: Container): JLabel? {
        for (component in container.components) {
            if (component is JLabel && component != globeIcon) {
                return component
            }
            if (component is Container) {
                findUrlLabel(component)?.let { return it }
            }
        }
        return null
    }

    /**
     * Properly dispose of the browser component to help with memory leaks (hopefully).
     */
    fun dispose() {
        browser.dispose()
    }

    private companion object {
        private const val COLLAPSED_SIZE = 30
        private const val MIN_WIDTH = 200
        private const val MIN_HEIGHT = 150
        private const val LONG_URL_CHAR_LIMIT = 50
        private const val LONG_URL_CHAR_DROP = 47
    }
}
