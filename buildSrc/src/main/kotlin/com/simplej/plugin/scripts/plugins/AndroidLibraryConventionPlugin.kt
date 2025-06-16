// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.scripts.plugins

import com.simplej.plugin.scripts.configureAndroidLibrary
import com.simplej.plugin.scripts.configureCheckstyle
import com.simplej.plugin.scripts.configureDetekt
import com.simplej.plugin.scripts.configureLint
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

/**
 * A Gradle convention plugin that provides standardized configuration for Android library modules.
 *
 * This plugin applies the `com.android.library`, `kotlin-android` and many other "default" plugins for shared
 * multi-project functionality, including:
 * - Android SDK and build tools version configuration
 * - Common compilation settings and compatibility options
 * - Default dependencies and repositories
 *
 * Usage:
 * ```kotlin
 * plugins {
 *     id("com.example.convention.android-library")
 * }
 * ```
 */
class AndroidLibraryConventionPlugin : RootConventionPlugin() {

    override fun applyInternal(target: Project) {
        target.apply(plugin = "com.android.library")
        target.apply(plugin = "kotlin-android")
        target.configureAndroidLibrary()
        target.configureCheckstyle()
        target.configureLint()
        target.configureDetekt()
    }
}