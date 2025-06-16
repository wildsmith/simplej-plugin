// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.scripts

import io.gitlab.arturbosch.detekt.Detekt
import org.gradle.api.Project
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.repositories

/**
 * Configures basic project settings and quality tools for the project.
 *
 * This function sets up essential project configurations including:
 * - Repository configurations:
 *   - Maven Central repository
 *   - Google's Maven repository
 * - Code quality tools:
 *   - Checkstyle for Java code style checking
 *   - Lint configuration
 *   - Detekt for Kotlin static code analysis
 */
internal fun Project.configureBaseProject(isAndroidLibrary: Boolean) {
    repositories {
        mavenCentral()
        google()
    }
    configureCheckstyle()
    configureLint(isAndroidLibrary)
    configureDetekt()
}

private fun Project.configureCheckstyle() {
    apply(plugin = "checkstyle")

    tasks.register("checkstyle", Checkstyle::class.java) {
        source("src/main/java", "src/test/java", "src/androidTest/java")
        include("**/*.java")
        exclude("$projectDir/build/**")
        classpath = files()
        configFile = rootProject.file("config/checkstyle.xml")
        ignoreFailures = false
        isShowViolations = true
        maxErrors = 0
        maxWarnings = 0
    }
}

private fun Project.configureLint(isAndroidLibrary: Boolean) {
    if (isAndroidLibrary) {
        androidLibrary {
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

private fun Project.configureDetekt() {
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