package com.simplej.plugin.scripts.dsl

import com.simplej.plugin.scripts.configureIntelliJPlugin
import org.gradle.api.Project

@DslScope
open class SimpleJOptions internal constructor(private val project: Project) {

    var intellijPlugin: Boolean = false
        set(value) {
            field = value
            project.configureIntelliJPlugin()
        }
}