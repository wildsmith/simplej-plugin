// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.actions.deletion

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.panel
import com.simplej.base.extensions.openInIde

/**
 * A dialog that warns users about existing references to a module before deletion. This dialog displays a list of
 * build files that reference the module and allows users to either proceed with or cancel the deletion.
 *
 * @property project The current IntelliJ project
 * @property codeReferences A set of code references where the module is being used in build files
 */
internal class DeletionWarningDialog(
    private val project: Project,
    private val codeReferences: Set<DeleteModuleAction.CodeReference>,
    private val okCallback: () -> Unit,
) : DialogWrapper(project, true) {

    init {
        title = "Build File References Found"
        isOKActionEnabled = true
        isResizable = false
        init()
    }

    override fun createCenterPanel(): DialogPanel = panel {
        row {
            text("This module is references in other build files:")
        }
        codeReferences.forEach { codeReference ->
            row {
                link("  • $codeReference") {
                    project.openInIde(codeReference.buildFile, codeReference.lineNumber)
                }
            }
        }
        row {
            text("Do you want to proceed?")
        }
    }

    override fun doOKAction() {
        okCallback()
        super.doOKAction()
    }
}
