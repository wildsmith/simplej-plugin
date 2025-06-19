// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.actions.settings

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.PersistentStateComponent
import com.intellij.openapi.components.State
import com.intellij.openapi.components.Storage

/**
 * Supports storing the application settings in a persistent way. The [State] and [Storage] annotations define the
 * name of the data and the filename where these persistent application settings are stored.
 */
@State(
    name = "com.simplej.plugin.actions.settings.SimpleJSettings",
    storages = [Storage("SimpleJPlugin.xml")]
)
internal class SimpleJSettings : PersistentStateComponent<SimpleJSettings.State> {

    private var myState = State()

    override fun getState(): State {
        return myState
    }

    override fun loadState(state: State) {
        myState = state
    }

    companion object {
        /**
         * Provides access to the singleton instance of the `SimpleJSettings` service.
         *
         * This instance is used to retrieve and modify the stored application settings for the plugin. The settings are
         * persistently stored and can be accessed and updated across multiple components.
         *
         * Use this instance to interact with the `State` of `SimpleJSettings`, which contains configuration details
         * such as default tasks and their properties.
         */
        @JvmStatic
        val instance: SimpleJSettings
            get() = ApplicationManager.getApplication().getService(SimpleJSettings::class.java)
    }

    internal class State {

        var inlineBrowserEnabled = true

        var defaultTasks: Set<TaskState> = setOf(
            TaskState(
                name = "checkstyle",
                description = "Static code analysis for Java.",
                enabled = true
            ),
            TaskState(
                name = "detekt",
                description = "Static code analysis for Kotlin.",
                enabled = true
            ),
            TaskState(
                name = "lint",
                description = "Run Android Lint analysis to check for errors and warnings.",
                enabled = true
            ),
            TaskState(
                name = "check",
                description = "run the Gradle 'check' task which performs *some* project validation including, " +
                        "compilation, testing, and packaging.",
                enabled = true
            ),
            TaskState(
                name = "build",
                description = "Runs the Gradle 'build' task which performs a complete project build including, " +
                        "compilation, testing, verification, and packaging.",
                enabled = false
            ),
            TaskState(
                name = "connectedAndroidTest",
                description = "Runs all instrumentation tests for enabled flavors on connected devices.",
                enabled = true
            ),
            TaskState(
                name = "assemble",
                description = "Runs the Gradle 'assemble' task which compiles and packages the project.",
                enabled = true
            ),
            TaskState(
                name = "clean",
                description = "Execute Gradle's 'clean' task which removes build outputs and temporary files.",
                enabled = false
            )
        )

        internal data class TaskState(
            var name: String = "",
            var description: String = "",
            var enabled: Boolean = true
        )
    }
}
