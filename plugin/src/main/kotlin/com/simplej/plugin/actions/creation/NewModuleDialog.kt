// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.actions.creation

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.simplej.plugin.SimpleJConfig
import com.simplej.plugin.simpleJConfig

/**
 * A dialog window that prompts the user for information needed to create a new module.
 *
 * This class uses the IntelliJ Platform's Kotlin UI DSL to construct a Swing-based form.
 * The form collects the module name, the GitHub team/user for code ownership, and the
 * desired template for the new module.
 *
 * Upon successful completion (clicking "OK"), it passes the collected data back
 * through a callback function.
 *
 * @param project The current IntelliJ project.
 * @param formData The data object used to pre-fill the dialog and to store user input.
 * @param okCallback A lambda function that is invoked with the completed [NewModuleFormData]
 *                   when the user clicks the "OK" button.
 */
internal class NewModuleDialog(
    private val project: Project,
    private val formData: NewModuleFormData = NewModuleFormData(),
    private val okCallback: (NewModuleFormData) -> Unit,
) : DialogWrapper(project, true) {

    private val simpleJConfig: SimpleJConfig by lazy {
        project.simpleJConfig()!!
    }

    init {
        title = "Create New Module"
        isOKActionEnabled = true
        isResizable = false
        init()
    }

    /**
     * Creates the main content panel of the dialog using the Kotlin UI DSL.
     *
     * The panel contains input fields for the module name and owner, as well as a
     * radio button group for selecting a module template. The available templates
     * are dynamically populated from the project's [SimpleJConfig].
     */
    override fun createCenterPanel(): DialogPanel = panel {
        row("Module name:") {
            textField()
                .bindText(formData::moduleName)
                .onChanged {
                    formData.moduleName = it.text
                }
                .focused()
        }
        row("Github team or user:") {
            textField()
                .bindText(formData::githubTeamOrUser)
                .onChanged {
                    formData.githubTeamOrUser = it.text
                }
        }
        buttonsGroup("Choose a template:") {
            simpleJConfig.newModuleTemplates!!.forEach { newModuleTemplate ->
                row {
                    radioButton(newModuleTemplate.name)
                        .onChanged {
                            if (it.isSelected) formData.templateName = newModuleTemplate.name
                        }
                }
            }
        }
    }

    /**
     * Executes when the user clicks the "OK" button.
     *
     * This method triggers the [okCallback], passing the populated [formData] object
     * to the caller, before closing the dialog.
     */
    override fun doOKAction() {
        okCallback(formData)
        super.doOKAction()
    }
}
