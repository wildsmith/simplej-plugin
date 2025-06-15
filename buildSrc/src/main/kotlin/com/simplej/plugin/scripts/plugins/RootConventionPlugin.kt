package com.simplej.plugin.scripts.plugins

import com.simplej.plugin.scripts.dsl.SimpleJOptions
import org.gradle.api.Plugin
import org.gradle.api.Project

abstract class RootConventionPlugin : Plugin<Project> {

    final override fun apply(target: Project) {
        target.extensions.create("simpleJ", SimpleJOptions::class.java)
        applyInternal(target)
    }

    abstract fun applyInternal(target: Project)
}