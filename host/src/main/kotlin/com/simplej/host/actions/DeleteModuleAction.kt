// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.host.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.simplej.core.ProjectViewPopupMenuItem
import com.simplej.core.SimpleJAnAction

class DeleteModuleAction : SimpleJAnAction(), ProjectViewPopupMenuItem {

    override fun shouldShow(event: AnActionEvent, project: Project): Boolean {
        TODO("Check to ensure the module is a module that can be deleted, unlike gradle/dist/config/etc")
    }

    override fun actionPerformed(event: AnActionEvent) {
        TODO("Not yet implemented")
    }
}
