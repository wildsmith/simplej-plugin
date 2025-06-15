package com.simplej.plugin.scripts

import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.bundling.Zip
import org.gradle.jvm.tasks.Jar
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.repositories

internal fun Project.configureIntelliJPlugin() {
    if (!plugins.hasPlugin("org.jetbrains.intellij.platform") && !plugins.hasPlugin("org.jetbrains.intellij.platform.module")) {
        return
    }

    repositories {
        intellijPlatform {
            defaultRepositories()
        }
    }

    dependencies {
        intellijPlatform {
            bundledPlugins("com.intellij.gradle", "com.intellij.java", "Git4Idea")
            intellijIdeaCommunity(getIntelliJReleaseVersion())
        }
    }

    if (plugins.hasPlugin("org.jetbrains.intellij.platform")) {
        intellijPlatform {
            pluginConfiguration {
                ideaVersion {
                    sinceBuild.set(getIntelliJSinceBuildVersion())
                    untilBuild.set(getIntelliJUntilBuildVersion())
                }
            }
            buildSearchableOptions.set(false)
        }
        configureDistTasks()
//        configureUberJar()
    }
}

private const val COPY_NEW_DIST_ARTIFACT_TASK_NAME = "copyNewDistArtifact"
private const val DELETE_OLD_DIST_ARTIFACT_TASK_NAME = "deleteOldDistArtifact"
private const val ZIP_SUFFIX = ".zip"
private val EXCLUDED_FILE_PREFIXES = setOf(
    "gradle-",
    "groovy-",
    "javaparser-",
    "kotlin-reflect-"
)

private fun Project.configureDistTasks() {
    val distDir = "$projectDir/../dist"

    val projectName = project.name

    tasks.register(DELETE_OLD_DIST_ARTIFACT_TASK_NAME, Delete::class.java) {
        description = "Deletes the old dist artifact."
        group = "publishing"

        delete(distDir)

        finalizedBy(COPY_NEW_DIST_ARTIFACT_TASK_NAME)
    }

    tasks.named("buildPlugin", Zip::class.java).configure {
        exclude { file ->
            EXCLUDED_FILE_PREFIXES.any { file.name.startsWith(it) }
        }

        val matchingFiles = fileTree(distDir).matching { include("$projectName-*.zip") }
        if (matchingFiles.isEmpty) {
            version = "0.0.1"
        } else {
            val zipFileName = matchingFiles.last().name.substringBefore(ZIP_SUFFIX)
            val oldVersion = zipFileName.split("-").last().substringBefore("-")
            var index = 0
            version = oldVersion.split(".").joinToString(".") {
                index++
                if (index == 2) {
                    "${it.toInt() + 1}"
                } else {
                    it
                }
            }
        }

        dependsOn(DELETE_OLD_DIST_ARTIFACT_TASK_NAME)
        finalizedBy(COPY_NEW_DIST_ARTIFACT_TASK_NAME)
    }

    tasks.register(COPY_NEW_DIST_ARTIFACT_TASK_NAME, Copy::class.java) {
        description = "Copies the new artifact into dist."
        group = "publishing"

        from("${layout.buildDirectory.get()}/distributions/$projectName-$version$ZIP_SUFFIX")
        into(distDir)
    }
}

//private fun Project.configureUberJar() {
//    tasks.named("jar", Jar::class.java).configure {
//        from(sourceSets.named("main").get().output)
//        dependsOn(configurations.named("runtimeClasspath"))
//        from({
//            configurations.named("runtimeClasspath").get().filter {
//                it.path.contains(project.projectDir.parentFile.path) && it.name.endsWith("jar")
//            }.map {
//                zipTree(it)
//            }
//        })
//        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
//    }
//
//    tasks.named("instrumentedJar").configure {
//        project.siblings.filter { !it.path.endsWith("playground") }.forEach { sibling ->
//            dependsOn("${sibling.path}:jar")
//        }
//    }
//}