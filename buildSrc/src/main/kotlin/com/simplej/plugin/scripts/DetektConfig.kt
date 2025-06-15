package com.simplej.plugin.scripts

import io.gitlab.arturbosch.detekt.Detekt
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies

internal fun Project.configureDetekt() {
    apply(plugin = "io.gitlab.arturbosch.detekt")

    detekt {
        toolVersion = getDetektGradleVersion()
        config.setFrom(rootProject.file("config/detekt.yml"))
        parallel = true
    }

    tasks.withType(Detekt::class.java) {
        include("**/*.kt", "**/*.kts")
        exclude("$projectDir/build/**")
        reports {
            html.required.set(true)
        }
    }
}