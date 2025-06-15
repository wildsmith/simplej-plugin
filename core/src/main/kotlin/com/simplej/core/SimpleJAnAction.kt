package com.simplej.core

import com.intellij.openapi.actionSystem.ActionPlaces
import com.intellij.openapi.actionSystem.ActionUpdateThread
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project

abstract class SimpleJAnAction : AnAction() {

    final override fun getActionUpdateThread(): ActionUpdateThread =
        ActionUpdateThread.BGT

    final override fun update(event: AnActionEvent) {
        val project = event.project
        if (project == null || !shouldShowInternal(event, project)) {
            event.presentation.isVisible = false
        }
    }

    private fun shouldShowInternal(event: AnActionEvent, project: Project): Boolean {
        val shouldShow = when (event.place) {
            ActionPlaces.PROJECT_VIEW_POPUP -> this is ProjectViewPopupMenuItem
            ActionPlaces.EDITOR_POPUP -> this is EditorPopupMenuItem
            else -> false
        }

        return if (shouldShow) {
            shouldShow(event, project)
        } else {
            false
        }
    }

    open fun shouldShow(event: AnActionEvent, project: Project): Boolean = true
}
