// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.scripts

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.plugins.ExtensionAware
import org.gradle.kotlin.dsl.DependencyHandlerScope
import org.gradle.kotlin.dsl.get
import org.jetbrains.intellij.platform.gradle.extensions.IntelliJPlatformDependenciesExtension
import org.jetbrains.intellij.platform.gradle.extensions.IntelliJPlatformRepositoriesExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.internal.dsl.LintOptions
import io.gitlab.arturbosch.detekt.extensions.DetektExtension
import org.gradle.api.JavaVersion
import org.gradle.api.artifacts.VersionCatalog
import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.api.tasks.SourceSetContainer
import org.jetbrains.intellij.platform.gradle.extensions.IntelliJPlatformExtension

internal val Project.sourceSets: SourceSetContainer
    get() = extensions["sourceSets"] as SourceSetContainer

internal val Project.siblings: Set<Project>
    get() = parent?.subprojects?.filterTo(mutableSetOf()) { it != this } ?: setOf()

private fun Project.getVersionCatalog(): VersionCatalog =
    extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

internal fun Project.getSimpleJavaVersion() =
    getVersionCatalog().findVersion("java-lang").get().requiredVersion

internal fun Project.getJavaVersion(): JavaVersion =
    JavaVersion.toVersion(getSimpleJavaVersion())

internal fun Project.getAndroidVersion() =
    getVersionCatalog().findVersion("android-target").get().requiredVersion

internal fun Project.getIntelliJReleaseVersion() =
    getVersionCatalog().findVersion("intellij-release").get().requiredVersion

internal fun Project.getIntelliJSinceBuildVersion() =
    getVersionCatalog().findVersion("intellij-since-build").get().requiredVersion

internal fun Project.getIntelliJUntilBuildVersion() =
    getVersionCatalog().findVersion("intellij-until-build").get().requiredVersion

internal fun Project.getDetektGradleVersion() =
    getVersionCatalog().findVersion("detekt-gradle-plugin").get().requiredVersion

internal fun Project.android(configuration: BaseExtension.() -> Unit) =
    extensions.configure(BaseExtension::class.java, configuration)

internal fun Project.androidLib(configuration: LibraryExtension.() -> Unit) =
    extensions.configure(LibraryExtension::class.java, configuration)

internal fun Project.lint(configuration: LintOptions.() -> Unit) =
    extensions.configure(LintOptions::class.java, configuration)

internal fun RepositoryHandler.intellijPlatform(configuration: IntelliJPlatformRepositoriesExtension.() -> Unit) =
    configuration((this as ExtensionAware).extensions["intellijPlatform"] as IntelliJPlatformRepositoriesExtension)

internal fun DependencyHandlerScope.intellijPlatform(configuration: IntelliJPlatformDependenciesExtension.() -> Unit) =
    configuration(extensions["intellijPlatform"] as IntelliJPlatformDependenciesExtension)

internal fun Project.intellijPlatform(configuration: IntelliJPlatformExtension.() -> Unit) =
    configuration(extensions["intellijPlatform"] as IntelliJPlatformExtension)

internal fun Project.detekt(configuration: DetektExtension.() -> Unit) =
    extensions.configure(DetektExtension::class.java, configuration)