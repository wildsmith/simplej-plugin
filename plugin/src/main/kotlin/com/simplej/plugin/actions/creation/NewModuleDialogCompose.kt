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
import javax.swing.JComponent

/**
 * A dialog window that prompts the user for new module information, built with Compose Desktop.
 *
 * This class demonstrates how to integrate a modern, declarative UI created with Compose
 * into the IntelliJ Platform's traditional Swing-based dialog system. It wraps the
 * composable content inside a `DialogWrapper`.
 *
 * @param project The current IntelliJ project.
 * @param formData The data object to bind to the UI fields and store user input.
 * @param okCallback A lambda function executed with the final [NewModuleFormData] when the
 *                   user confirms the dialog.
 */
internal class NewModuleDialogCompose(
    private val project: Project,
    private val simpleJConfig: SimpleJConfig,
    private val formData: NewModuleFormData = NewModuleFormData(),
    private val okCallback: (NewModuleFormData) -> Unit,
) : DialogWrapper(project, true) {

    init {
        title = "Create New Module"
        isOKActionEnabled = true
        isResizable = false
        init()
    }

    /**
     * Creates the center panel of the dialog by embedding a Composable.
     *
     * This method returns a [ComposePanel], which acts as a bridge between Swing and Compose,
     * allowing the [NewModuleDialogContent] composable to be rendered inside the dialog.
     *
     * @return A [JComponent] containing the Compose-based UI.
     */
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

    /**
     * Handles the user clicking the "OK" button.
     *
     * It invokes the [okCallback] with the populated [formData] and then closes the dialog.
     */
    override fun doOKAction() {
        okCallback(formData)
        super.doOKAction()
    }
}

/**
 * The main composable function that defines the UI for the new module dialog.
 *
 * This function is self-contained and declaratively builds the form fields for the module name,
 * owner, and a list of templates. It is stateless, with its state managed by the [formData] object.
 *
 * @param templates A list of template names to be displayed as radio button options.
 * @param formData The state object holding the user's input.
 */
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
        OutlinedTextField(
            value = formData.githubTeamOrUser,
            onValueChange = { formData.githubTeamOrUser = it },
            label = { Text("Github team or user") },
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
