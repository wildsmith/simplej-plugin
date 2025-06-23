// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.actions.creation

import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogPanel
import com.intellij.openapi.ui.DialogWrapper
import com.intellij.ui.dsl.builder.bindText
import com.intellij.ui.dsl.builder.panel
import com.simplej.plugin.SimpleJConfig
import com.simplej.plugin.simpleJConfig

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

    override fun doOKAction() {
        okCallback(formData)
        super.doOKAction()
    }
}
