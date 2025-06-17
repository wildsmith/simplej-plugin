// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.scripts

import com.simplej.plugin.scripts.dsl.SimpleJOptions
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

private const val VERSION = 1

/**
 * Configures the project as an Android library with Kotlin support.
 *
 * This function sets up a comprehensive Android library configuration including:
 *
 * - Applies required plugins:
 *   - `com.android.library`
 *   - `kotlin-android`
 * - Configures base project settings with Android-specific lint rules
 * - Sets up Android SDK configuration:
 *   - Compile SDK version
 *   - Build tools version
 *   - Default configuration for:
 *     - Minimum SDK version
 *     - Target SDK version
 *     - Version code and name
 *     - Java compatibility settings
 *
 * The Android version used for SDK configuration is determined values with the version catalog.
 */
internal fun Project.configureAndroidLibrary(simpleJOptions: SimpleJOptions) {
    apply(plugin = "com.android.library")
    apply(plugin = "kotlin-android")

    configureBaseProject(simpleJOptions, true)

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
