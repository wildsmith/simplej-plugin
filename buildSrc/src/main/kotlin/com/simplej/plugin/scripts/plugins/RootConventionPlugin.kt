// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.scripts.plugins

import com.simplej.plugin.scripts.dsl.SimpleJOptions
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * A Gradle convention plugin that configures common settings.
 *
 * This plugin provides centralized configuration for multi-project builds by:
 * - Setting up common repositories and dependencies
 * - Configuring project-wide Gradle settings
 */
abstract class RootConventionPlugin : Plugin<Project> {

    final override fun apply(target: Project) {
        target.extensions.create("simpleJ", SimpleJOptions::class.java)
        applyInternal(target)
    }

    abstract fun applyInternal(target: Project)
}