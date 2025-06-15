package com.simplej.plugin.scripts.plugins

import com.simplej.plugin.scripts.configureCheckstyle
import com.simplej.plugin.scripts.configureLint
import com.simplej.plugin.scripts.configureDetekt
import com.simplej.plugin.scripts.configureJavaLibrary
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

class JavaLibraryConventionPlugin : RootConventionPlugin() {

    override fun applyInternal(target: Project) {
        target.apply(plugin = "java-library")
        target.apply(plugin = "kotlin")
        target.configureJavaLibrary()
        target.configureCheckstyle()
        target.configureLint()
        target.configureDetekt()
    }
}