package com.simplej.plugin.scripts

import org.gradle.api.Project
import org.gradle.kotlin.dsl.repositories

internal fun Project.configureRepositories() {
    repositories {
        mavenCentral()
        google()
    }
}