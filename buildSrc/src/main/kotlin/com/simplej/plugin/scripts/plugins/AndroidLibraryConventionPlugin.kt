// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.scripts.plugins

import com.simplej.plugin.scripts.configureAndroidLibrary
import com.simplej.plugin.scripts.dsl.SimpleJOptions
import org.gradle.api.Project

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

    override fun applyInternal(target: Project, simpleJOptions: SimpleJOptions) {
        target.configureAndroidLibrary(simpleJOptions)
    }
}
