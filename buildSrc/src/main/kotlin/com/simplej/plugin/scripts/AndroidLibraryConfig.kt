package com.simplej.plugin.scripts

import org.gradle.api.JavaVersion
import org.gradle.api.Project

private const val VERSION = 1

internal fun Project.configureAndroidLibrary() {
    configureRepositories()
    val androidVersionInt = getAndroidVersion().toInt()
    android {
        compileSdkVersion(androidVersionInt)
        buildToolsVersion("$androidVersionInt.0.0")
        defaultConfig {
            minSdk = androidVersionInt
            targetSdk = androidVersionInt
            versionCode = VERSION
            versionName = VERSION.toString()
            val javaVersion = getJavaVersion()
            compileOptions {
                sourceCompatibility = javaVersion
                targetCompatibility = javaVersion
            }
        }
    }
}