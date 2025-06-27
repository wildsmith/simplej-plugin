// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.scripts

import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Delete
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.registering
import org.gradle.kotlin.dsl.repositories
import org.gradle.process.CommandLineArgumentProvider
import java.io.File

/**
 * Configures the project as an IntelliJ Platform plugin.
 *
 * This function performs plugin configuration only if either the 'org.jetbrains.intellij.platform'
 * or 'org.jetbrains.intellij.platform.module' plugin is applied. The configuration includes:
 *
 * - Setting up IntelliJ Platform repositories
 * - Configuring plugin dependencies:
 *   - Bundled plugins (Gradle, Java, Git4Idea)
 *   - IntelliJ IDEA Community Edition
 * - Configuring plugin metadata (when using org.jetbrains.intellij.platform):
 *   - Compatible IDE versions (sinceBuild, untilBuild)
 *   - Disabling searchable options build
 *   - Distribution tasks setup
 */
internal fun Project.configureIntelliJPlugin() {
    val hasPlatformPlugin = plugins.hasPlugin("org.jetbrains.intellij.platform")
    val hasPlatformModulePlugin = plugins.hasPlugin("org.jetbrains.intellij.platform.module")
    if (!hasPlatformPlugin && !hasPlatformModulePlugin) {
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

    intellijPlatformTesting {
        runIde.registering {
            task {
                jvmArgumentProviders += CommandLineArgumentProvider {
                    listOf(
                        "-Xmx8g",
                        "-Dkotlin.daemon.jvm.options=-XX:MaxMetaspaceSize=1g"
                    )
                }
            }
        }
    }

    if (hasPlatformPlugin) {
        intellijPlatform {
            pluginConfiguration {
                ideaVersion {
                    sinceBuild.set(getIntelliJSinceBuildVersion())
                    untilBuild.set(getIntelliJUntilBuildVersion())
                }
            }
            buildSearchableOptions.set(false)
            publishing {
                token.set(provider { properties.getOrDefault("PUBLISH_TOKEN", "").toString() })
            }
            signing {
                password.set(provider { properties.getOrDefault("PRIVATE_KEY_PASSWORD", "").toString() })
                privateKeyFile.set(layout.projectDirectory.file("config/signing/private.pem"))
                certificateChainFile.set(layout.projectDirectory.file("config/signing/chain.crt"))
            }
        }
        configureArtifactTasks()
    }
}

private const val COPY_NEW_ARTIFACT_TASK_NAME = "copyNewArtifact"
private const val DELETE_OLD_ARTIFACT_TASK_NAME = "deleteOldArtifact"
private const val ZIP_SUFFIX = ".zip"

private fun Project.configureArtifactTasks() {
    val artifactDir = "$projectDir/../artifact"

    tasks.register(DELETE_OLD_ARTIFACT_TASK_NAME, Delete::class.java) {
        description = "Deletes the old zip from the artifact directory."
        group = "publishing"

        delete(artifactDir)

        finalizedBy(COPY_NEW_ARTIFACT_TASK_NAME)
    }

    val projectName = project.name
    tasks.named("buildPlugin", Zip::class.java).configure {
        // Call this during task configuration to avoid impacting the configuration phase. If the task isn't
        // configured then the project version will not be set.
        setProjetVersion(artifactDir, projectName)

        dependsOn(DELETE_OLD_ARTIFACT_TASK_NAME)
        finalizedBy(COPY_NEW_ARTIFACT_TASK_NAME)
    }

    tasks.register(COPY_NEW_ARTIFACT_TASK_NAME, Copy::class.java) {
        description = "Copies the new zip into the artifact directory."
        group = "publishing"

        from("${layout.buildDirectory.get()}/distributions/$projectName-$version$ZIP_SUFFIX")
        into(artifactDir)
    }
}

private fun Project.setProjetVersion(artifactDir: String, projectName: String?) {
    val existingArtifact = rootProject.layout.projectDirectory.dir(artifactDir).asFile.listFiles()?.firstOrNull {
        it.name.startsWith("$projectName-") && it.name.endsWith(ZIP_SUFFIX)
    }
    if (existingArtifact == null) {
        version = "1.0.0"
    } else {
        val oldVersion = existingArtifact.name
            .substringBefore(ZIP_SUFFIX)
            .substringAfter("-")
        var index = 0
        version = oldVersion.split(".").joinToString(".") {
            index++
            if (index == 3) {
                "${it.toInt() + 1}"
            } else {
                it
            }
        }
    }
}
