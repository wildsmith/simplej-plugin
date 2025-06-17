// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.scripts.plugins

import com.simplej.plugin.scripts.configureJavaLibrary
import com.simplej.plugin.scripts.dsl.SimpleJOptions
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

/**
 * A Gradle convention plugin that provides standardized configuration for Java library projects.
 *
 * This plugin applies the `java-library`, `kotlin` and many other "default" plugins for shared multi-project
 * functionality, including:
 * - Standard source set layout (src/main/java, src/test/java)
 * - Common compilation settings and Java compatibility
 * - Default dependencies configuration
 * - JAR artifact generation
 *
 * Usage:
 * ```kotlin
 * plugins {
 *     id("simplej.java-library")
 * }
 * ```
 */
class JavaLibraryConventionPlugin : RootConventionPlugin() {

    override fun applyInternal(target: Project, simpleJOptions: SimpleJOptions) {
        target.configureJavaLibrary(simpleJOptions)
    }
}
