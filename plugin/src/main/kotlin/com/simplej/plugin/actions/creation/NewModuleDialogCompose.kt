// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.actions.creation

import androidx.annotation.RestrictTo
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposePanel
import androidx.compose.ui.unit.dp
import com.intellij.openapi.project.Project
import com.intellij.openapi.ui.DialogWrapper
import com.simplej.plugin.SimpleJConfig
import com.simplej.plugin.simpleJConfig
import javax.swing.JComponent

internal class NewModuleDialogCompose(
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

    override fun createCenterPanel(): JComponent {
        return ComposePanel().apply {
            setContent {
                MaterialTheme {
                    NewModuleDialogContent(
                        templates = simpleJConfig.newModuleTemplates?.map { it.name } ?: emptyList(),
                        formData = formData,
                    )
                }
            }
        }
    }

    override fun doOKAction() {
        okCallback(formData)
        super.doOKAction()
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
@Composable
fun NewModuleDialogContent(
    templates: List<String>,
    formData: NewModuleFormData
) {
    Column(modifier = Modifier.padding(16.dp).width(IntrinsicSize.Max)) {
        OutlinedTextField(
            value = formData.moduleName,
            onValueChange = { formData.moduleName = it },
            label = { Text("Module name") },
            modifier = Modifier.fillMaxWidth()
        )
        templates.forEach { template ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { formData.templateName = template }
                    .padding(vertical = 4.dp)
            ) {
                RadioButton(
                    selected = (template == formData.templateName),
                    onClick = { formData.templateName = template }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(template)
            }
        }
    }
}
