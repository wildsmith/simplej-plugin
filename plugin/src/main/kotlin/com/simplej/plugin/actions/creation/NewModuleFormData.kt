// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.actions.creation

import androidx.annotation.RestrictTo

/**
 * A data class that holds the form data collected from the user for creating a new module.
 *
 * This class serves as a container for all the information required by the module creation logic,
 * such as the module's name, its code owner, and the template to be used for its creation.
 * It also provides utility functions to format the raw user input into standardized formats.
 *
 * @property moduleName The raw, user-provided name for the new module.
 * @property githubTeamOrUser The user-provided GitHub team or username to be set as the code owner.
 * @property templateName The name of the template selected to create the new module.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
data class NewModuleFormData(
    var moduleName: String = "",
    var githubTeamOrUser: String = "",
    var templateName: String = ""
) {
    fun formattedModuleName(): String = moduleName
        .lowercase()
        .replace(" ", "-")
        .replace(".", "-")

    fun formattedGithubTeamOrUser(): String = githubTeamOrUser
        .lowercase()
        .replace(" ", "_")
        .replace(".", "_")
        .let {
            return if (it.startsWith("@")) {
                it
            } else {
                "@$it"
            }
        }
}
