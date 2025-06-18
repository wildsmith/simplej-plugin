// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.scripts

import com.simplej.plugin.scripts.dsl.SimpleJOptions
import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.apply

/**
 * Configures the project as a Java library with Kotlin support.
 *
 * This function:
 * - Applies the 'java-library' and 'kotlin' plugins
 * - Sets up base project configuration
 * - Configures Java compilation tasks with appropriate source and target compatibility
 *
 * The Java version used for compilation is determined by the `java-lang` value in the version catalog.
 */
internal fun Project.configureJavaLibrary(simpleJOptions: SimpleJOptions) {
    apply(plugin = "java-library")
    apply(plugin = "kotlin")

    configureBaseProject(simpleJOptions)

    val javaVersion = "${getJavaVersion()}"
    tasks.withType(JavaCompile::class.java).configureEach {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
}
