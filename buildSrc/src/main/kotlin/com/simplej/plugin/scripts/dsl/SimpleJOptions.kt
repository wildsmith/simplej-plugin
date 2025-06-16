// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.scripts.dsl

import com.simplej.plugin.scripts.configureIntelliJPlugin
import org.gradle.api.Project

/**
 * Configuration options for a SimpleJ enhanced project. These options help inform SimpleJ's Gradle Plugins on
 * which build scripts and project attributes to customize.
 *
 * Example usage:
 * ```kotlin
 * plugins {
 *     id("simplej.java-library")
 * }
 *
 * simpleJ.intellijPlugin = true
 * ```
 */
@DslScope
open class SimpleJOptions internal constructor(private val project: Project) {

    /**
     * Configures the project with IntelliJ's platform SDK
     */
    var intellijPlugin: Boolean = false
        set(value) {
            field = value
            project.configureIntelliJPlugin()
        }
}