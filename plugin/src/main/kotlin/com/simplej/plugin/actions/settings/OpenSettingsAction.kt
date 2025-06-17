// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.actions.settings

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.options.ShowSettingsUtil
import com.simplej.base.EditorPopupMenuItem
import com.simplej.base.ProjectViewPopupMenuItem
import com.simplej.base.SimpleJAnAction

/**
 * An action that opens the SimpleJ plugin settings dialog.
 *
 * This action is available in both the project view popup menu and editor popup menu contexts. When triggered, it
 * displays the SimpleJ settings configuration dialog for the current project.
 */
internal class OpenSettingsAction : SimpleJAnAction(), ProjectViewPopupMenuItem, EditorPopupMenuItem {

    override fun actionPerformed(event: AnActionEvent) {
        ShowSettingsUtil.getInstance().showSettingsDialog(
            event.project,
            SimpleJSettingsConfigurable::class.java
        )
    }
}
