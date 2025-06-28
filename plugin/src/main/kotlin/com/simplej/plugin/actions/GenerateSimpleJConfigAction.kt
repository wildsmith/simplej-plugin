// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.actions

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.command.WriteCommandAction
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.writeText
import com.simplej.base.ProjectViewPopupMenuItem
import com.simplej.base.SimpleJAnAction
import com.simplej.base.extensions.createChildOrGet
import com.simplej.base.extensions.createDirOrGet
import com.simplej.base.extensions.showError
import com.simplej.base.extensions.toVirtualFile
import com.simplej.plugin.getSimpleJFile
import com.simplej.plugin.simpleJConfig
import java.io.File

internal class GenerateSimpleJConfigAction : SimpleJAnAction(), ProjectViewPopupMenuItem {

    override fun shouldShow(event: AnActionEvent, project: Project): Boolean {
        return project.simpleJConfig() == null
    }

    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return event.showError("No valid project found within the workspace.")
        val projectFile = File(project.basePath!!).toVirtualFile() ?: return event.showError(
            "No valid project found within the workspace."
        )
        val simpleJFile = project.getSimpleJFile()
        if (simpleJFile.exists()) return event.showError("simplej-config.json already exists.")
        WriteCommandAction.runWriteCommandAction(
            project,
            "Create SimpleJ Config",
            null,
            {
                val simpleJDir = projectFile.createDirOrGet("config")
                    .createDirOrGet("simplej")

                simpleJDir.createChildOrGet("simplej-config.json")
                    .writeText(SIMPLEJ_CONFIG_JSON_TEMPLATE)

                val templatesDir = simpleJDir.createDirOrGet("templates")

                val javaModuleDir = templatesDir.createDirOrGet("java-module")
                javaModuleDir.createChildOrGet("build.gradle.kts")
                    .writeText(JAVA_MODULE_TEMPLATE)

                val simpleJJavaModuleDir = templatesDir.createDirOrGet("simplej-java-module")
                simpleJJavaModuleDir.createChildOrGet("build.gradle.kts")
                    .writeText(SIMPLEJ_JAVA_MODULE_TEMPLATE)
            }
        )
    }

    companion object {

        private const val DEFAULT_TEST_REPO = "git@github.com:wildsmith/simplej-plugin.git"
        private const val DEFAULT_JAVA_VERSION = 17
        private const val DEFAULT_JAVA_HOME = "/Applications/Android Studio.app/Contents/jbr/Contents/Home"
        private val DEFAULT_ANDROID_BUILD_TOOLS_VERSION = "35.0.0".split(".")

        @JvmStatic
        private val SIMPLEJ_CONFIG_JSON_TEMPLATE = """
            {
              "workspaceCompat": {
                "ssh": {
                  "testRepo": "$DEFAULT_TEST_REPO"
                },
                "java": {
                  "home": "$DEFAULT_JAVA_HOME",
                  "version": {
                    "major": $DEFAULT_JAVA_VERSION
                  }
                },
                "android": {
                  "buildTools": {
                    "major": ${DEFAULT_ANDROID_BUILD_TOOLS_VERSION[0]},
                    "minor": ${DEFAULT_ANDROID_BUILD_TOOLS_VERSION[1]},
                    "patch": ${DEFAULT_ANDROID_BUILD_TOOLS_VERSION[2]}
                  }
                }
              },
              "webBrowserMappings": {
                "fileMappings": {
                  "README.md": "https://github.com/wildsmith/simplej-plugin/blob/main/README.md",
                  "plugin/src/main/kotlin/com/simplej/plugin/actions/settings/SimpleJSettingsConfigurable.kt": "https://plugins.jetbrains.com/docs/intellij/kotlin-ui-dsl-version-2.html#cellbind"
                }
              },
              "newModuleTemplates": [
                {
                  "name": "SimpleJ Java Module",
                  "files": [
                    {
                      "relativePath": "build.gradle.kts",
                      "templateLocation": "simplej-java-module/build.gradle.kts"
                    }
                  ]
                },
                {
                  "name": "Java Module",
                  "files": [
                    {
                      "relativePath": "build.gradle.kts",
                      "templateLocation": "java-module/build.gradle.kts"
                    }
                  ]
                }
              ]
            }
        """.trimIndent()

        @JvmStatic
        private val JAVA_MODULE_TEMPLATE = """
            plugins { `java-library` }

        """.trimIndent()

        @JvmStatic
        private val SIMPLEJ_JAVA_MODULE_TEMPLATE = """
            plugins {
                id("simplej.java-library")
                id("org.jetbrains.intellij.platform.module")
            }

            simpleJ.intellijPlugin = true

        """.trimIndent()
    }
}