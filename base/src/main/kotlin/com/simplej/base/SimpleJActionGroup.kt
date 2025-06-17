// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.base

import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.actionSystem.DefaultActionGroup

/**
 * A specialized action group implementation for SimpleJ plugin actions.
 *
 * This action group extends IntelliJ's [DefaultActionGroup] with specific configurations:
 * - Performs action updates in the background thread for better UI responsiveness
 * - Automatically hides the group from menus when it contains no actions
 *
 * Use this class when grouping related SimpleJ plugin actions together in menus or toolbars to ensure consistent
 * behavior and optimal performance.
 */
@Suppress("ComponentNotRegistered")
class SimpleJActionGroup : DefaultActionGroup() {

    /**
     * Specifies that action updates should occur in the background thread.
     *
     * @return [ActionUpdateThread.BGT] to perform updates asynchronously
     */
    override fun getActionUpdateThread(): ActionUpdateThread =
        ActionUpdateThread.BGT

    /**
     * Updates the presentation of this action group.
     *
     * Configures the group to be hidden from the UI when it contains no actions, preventing empty menu sections.
     *
     * @param event The action event containing context information
     */
    override fun update(event: AnActionEvent) {
        event.presentation.isHideGroupIfEmpty = true
    }
}
