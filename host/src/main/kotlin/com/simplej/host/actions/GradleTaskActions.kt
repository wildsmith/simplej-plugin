package com.simplej.host.actions

import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.simplej.core.EditorPopupMenuItem
import com.simplej.core.ProjectViewPopupMenuItem
import com.simplej.core.SimpleJAnAction
import com.simplej.core.extensions.currentFile
import com.simplej.core.extensions.findClosestProject
import com.simplej.core.extensions.showError
import org.jetbrains.plugins.gradle.util.GradleConstants


abstract class GradleTaskAction private constructor(
    private val tasks: Set<String>
) : SimpleJAnAction(), ProjectViewPopupMenuItem, EditorPopupMenuItem {

    constructor(vararg tasks: String) : this(tasks.toSet())

    constructor(vararg tasks: GradleTaskAction) : this(tasks.flatMapTo(mutableSetOf()) { it.tasks })

    @Suppress("ReturnCount")
    override fun actionPerformed(event: AnActionEvent) {
        val currentFile = event.currentFile ?: return event.showError(
            "No valid file found within the project workspace."
        )
        val project = event.project ?: return event.showError(
            "No valid project found within the workspace."
        )
        val projectFile = currentFile.findClosestProject(project) ?: return event.showError(
            "Unable to find closest valid project within the workspace."
        )
        ExternalSystemUtil.runTask(
            ExternalSystemTaskExecutionSettings().apply {
                externalProjectPath = projectFile.path
                taskNames = tasks.toList()
                scriptParameters = ""
                vmOptions = ""
                externalSystemIdString = GradleConstants.SYSTEM_ID.id
            },
            DefaultRunExecutor.EXECUTOR_ID,
            project,
            GradleConstants.SYSTEM_ID,
            object : TaskCallback {
                override fun onSuccess() {
                    // no op
                }

                override fun onFailure() {
                    // no op
                }
            },
            ProgressExecutionMode.IN_BACKGROUND_ASYNC,
            true
        )
    }
}

object DetektTaskAction : GradleTaskAction("detekt")

object CheckstyleTaskAction : GradleTaskAction("checkstyle")

object LintTaskAction : GradleTaskAction("lint")

object AllStaticCodeAnalysisTaskAction : GradleTaskAction(DetektTaskAction, CheckstyleTaskAction, LintTaskAction)

object CheckTaskAction : GradleTaskAction("check")

object ConnectedAndroidTestTaskAction : GradleTaskAction("connectedAndroidTest")

object AllTestTypesTaskAction : GradleTaskAction(CheckTaskAction, ConnectedAndroidTestTaskAction)

object AssembleTaskAction : GradleTaskAction("assemble")
