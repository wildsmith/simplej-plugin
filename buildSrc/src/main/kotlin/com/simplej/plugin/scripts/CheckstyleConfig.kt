package com.simplej.plugin.scripts

import org.gradle.api.Project
import org.gradle.api.plugins.quality.Checkstyle
import org.gradle.kotlin.dsl.apply

internal fun Project.configureCheckstyle() {
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