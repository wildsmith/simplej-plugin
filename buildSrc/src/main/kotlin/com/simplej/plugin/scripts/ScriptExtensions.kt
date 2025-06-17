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

/**
 * Extension property to access the source sets configuration of a Gradle project.
 *
 * @return The [SourceSetContainer] for this project
 */
internal val Project.sourceSets: SourceSetContainer
    get() = extensions["sourceSets"] as SourceSetContainer

/**
 * Extension property to get all sibling projects at the same level as the current one.
 *
 * @return A [Set] of sibling [Project]s, or empty set if this project has no siblings
 */
internal val Project.siblings: Set<Project>
    get() = parent?.subprojects?.filterTo(mutableSetOf()) { it != this } ?: setOf()

internal fun Project.getVersionCatalog(): VersionCatalog =
    extensions.getByType(VersionCatalogsExtension::class.java).named("libs")

/**
 * Gets the Java version string from the version catalog.
 *
 * @return The Java version as a string from 'java-lang' entry in version catalog
 *
 * @see getVersionCatalog
 */
internal fun Project.getSimpleJavaVersion(): String = getVersionCatalog().findVersion("java-lang").get().requiredVersion

/**
 * Gets the Java version as [JavaVersion] object from the version catalog.
 *
 * @return [JavaVersion] object corresponding to the project's Java version
 *
 * @see getVersionCatalog
 */
internal fun Project.getJavaVersion(): JavaVersion = JavaVersion.toVersion(getSimpleJavaVersion())

/**
 * Gets the Android target version from the version catalog.
 *
 * @return The Android target version as a string
 *
 * @see getVersionCatalog
 */
internal fun Project.getAndroidVersion(): String =
    getVersionCatalog().findVersion("android-target").get().requiredVersion

/**
 * Gets the IntelliJ IDEA release version from the version catalog.
 *
 * @return The IntelliJ IDEA release version as a string
 *
 * @see getVersionCatalog
 */
internal fun Project.getIntelliJReleaseVersion(): String =
    getVersionCatalog().findVersion("intellij-release").get().requiredVersion

/**
 * Gets the minimum compatible IntelliJ IDEA build version from the version catalog.
 *
 * @return The minimum IntelliJ IDEA build version as a string
 *
 * @see getVersionCatalog
 */
internal fun Project.getIntelliJSinceBuildVersion(): String =
    getVersionCatalog().findVersion("intellij-since-build").get().requiredVersion

/**
 * Gets the maximum compatible IntelliJ IDEA build version from the version catalog.
 *
 * @return The maximum IntelliJ IDEA build version as a string
 *
 * @see getVersionCatalog
 */
internal fun Project.getIntelliJUntilBuildVersion(): String =
    getVersionCatalog().findVersion("intellij-until-build").get().requiredVersion

/**
 * Gets the Detekt Gradle plugin version from the version catalog.
 *
 * @return The Detekt Gradle plugin version as a string
 *
 * @see getVersionCatalog
 */
internal fun Project.getDetektGradleVersion(): String =
    getVersionCatalog().findVersion("detekt-gradle-plugin").get().requiredVersion

/**
 * Configures Android-specific settings for the project. This extension function leverages the most basic extension
 * type leveraged by Android library, test, and application projects.
 *
 * @param configuration Lambda with Android configuration block
 */
internal fun Project.android(configuration: BaseExtension.() -> Unit) =
    extensions.configure(BaseExtension::class.java, configuration)

/**
 * Configures Android library-specific settings for the project.
 *
 * @param configuration Lambda with Android library configuration block
 */
internal fun Project.androidLibrary(configuration: LibraryExtension.() -> Unit) =
    extensions.configure(LibraryExtension::class.java, configuration)

/**
 * Configures Android lint options for the project.
 *
 * @param configuration Lambda with lint configuration block
 */
internal fun Project.lint(configuration: LintOptions.() -> Unit) =
    extensions.configure(LintOptions::class.java, configuration)

/**
 * Configures IntelliJ Platform-specific repository settings.
 *
 * @param configuration Lambda with IntelliJ Platform repositories configuration block
 */
internal fun RepositoryHandler.intellijPlatform(configuration: IntelliJPlatformRepositoriesExtension.() -> Unit) =
    configuration((this as ExtensionAware).extensions["intellijPlatform"] as IntelliJPlatformRepositoriesExtension)

/**
 * Configures IntelliJ Platform-specific dependency settings.
 *
 * @param configuration Lambda with IntelliJ Platform dependencies configuration block
 */
internal fun DependencyHandlerScope.intellijPlatform(configuration: IntelliJPlatformDependenciesExtension.() -> Unit) =
    configuration(extensions["intellijPlatform"] as IntelliJPlatformDependenciesExtension)

/**
 * Configures IntelliJ Platform-specific settings for the project.
 *
 * @param configuration Lambda with IntelliJ Platform configuration block
 */
internal fun Project.intellijPlatform(configuration: IntelliJPlatformExtension.() -> Unit) =
    configuration(extensions["intellijPlatform"] as IntelliJPlatformExtension)

/**
 * Configures Detekt static code analysis settings for the project.
 *
 * @param configuration Lambda with Detekt configuration block
 */
internal fun Project.detekt(configuration: DetektExtension.() -> Unit) =
    extensions.configure(DetektExtension::class.java, configuration)

/**
 * Adds a dependency to the 'testImplementation' configuration.
 *
 * This function is a convenience wrapper around the Gradle dependency handler's add method, specifically for test
 * dependencies. It is used to declare dependencies that are only required for testing the project, such as testing
 * frameworks and mocking libraries.
 *
 * Example usage:
 * ```
 * dependencies {
 *     testImplementation("org.junit.jupiter:junit-jupiter:5.8.1")
 *     testImplementation("io.mockk:mockk:1.12.0")
 * }
 * ```
 *
 * @param dependencyNotation The dependency to add, can be a String (e.g. "group:name:version"),
 *                          a Project instance, a File instance, or any other valid Gradle dependency notation
 */
internal fun DependencyHandlerScope.testImplementation(dependencyNotation: Any) =
    add("testImplementation", dependencyNotation)
