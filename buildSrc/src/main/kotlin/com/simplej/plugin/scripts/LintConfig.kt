// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.scripts

import org.gradle.api.Project
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.withType

internal fun Project.configureLint() {
    if (plugins.hasPlugin("com.android.library")) {
        androidLib {
            lint {
                abortOnError = true
                warningsAsErrors = true
                checkReleaseBuilds = false
            }
        }
    } else {
        apply(plugin = "com.android.lint")
        @Suppress("DEPRECATION")
        lint {
            isAbortOnError = true
            isWarningsAsErrors = true
            isCheckReleaseBuilds = false
        }
        tasks.withType(JavaCompile::class.java).configureEach {
            options.compilerArgs.addAll(listOf("-Xlint:all", "-Werror"))
        }
    }
}