// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.actions.creation

import androidx.annotation.RestrictTo

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

    fun formattedGithubTeamOrUser(): String {
        githubTeamOrUser.lowercase()
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
}
