// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.preview.creation

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.simplej.plugin.actions.creation.NewModuleDialogContent
import com.simplej.plugin.actions.creation.NewModuleFormData

@Preview
@Composable
private fun PreviewNewModuleDialog() {
    val formData = NewModuleFormData()
    MaterialTheme {
        NewModuleDialogContent(
            templates = listOf("SimpleJ Java Library", "Java Library"),
            formData = formData,
        )
    }
}
