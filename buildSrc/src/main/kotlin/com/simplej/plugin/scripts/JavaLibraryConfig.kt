// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.scripts

import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile

internal fun Project.configureJavaLibrary() {
    configureRepositories()
    val javaVersion = "${getJavaVersion()}"
    tasks.withType(JavaCompile::class.java).configureEach {
        sourceCompatibility = javaVersion
        targetCompatibility = javaVersion
    }
}