// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.overlay

import com.intellij.openapi.fileEditor.FileEditorManager
import com.intellij.openapi.fileEditor.FileEditorManagerEvent
import com.intellij.openapi.fileEditor.FileEditorManagerListener
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile

/**
 * Listener for file editor changes to update web browser overlays when switching between files.
 */
internal class FileChangeListener(private val project: Project) : FileEditorManagerListener {
    
    override fun selectionChanged(event: FileEditorManagerEvent) {
        // a bunch of different checks to validate that there is no issue with the new context being selected
        val newFile = event.newFile ?: return
        val editor = event.manager.selectedTextEditor ?: return
        if (editor.project != project) return
        
        val overlayListener = WebBrowserOverlayListener.getInstance()
        overlayListener.updateOverlayForFile(editor, newFile)
    }
    
    override fun fileOpened(source: FileEditorManager, file: VirtualFile) {
        // get the editor object so that our overlaylistener can update accordingly
        val editor = source.selectedTextEditor ?: return
        if (editor.project != project) return
        
        val overlayListener = WebBrowserOverlayListener.getInstance()
        overlayListener.updateOverlayForFile(editor, file)
    }
} 