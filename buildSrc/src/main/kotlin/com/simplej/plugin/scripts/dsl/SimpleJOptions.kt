// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.scripts.dsl

import com.simplej.plugin.scripts.configureIntelliJPlugin
import org.gradle.api.Project

/**
 * Configuration options for a SimpleJ enhanced project. These options help inform SimpleJ's Gradle Plugins on
 * which build scripts and project attributes to customize.
 *
 * Example usage:
 * ```kotlin
 * plugins {
 *     id("simplej.java-library")
 * }
 *
 * simpleJ.intellijPlugin = true
 * ```
 */
@DslScope
open class SimpleJOptions internal constructor(private val project: Project) {

    /**
     * Configures the project with IntelliJ's platform SDK
     */
    var intellijPlugin: Boolean = false
        set(value) {
            field = value
            project.configureIntelliJPlugin()
        }

    internal var coverageMinimums: TestCoverageMinimums = TestCoverageMinimums()

    /**
     * Configures minimum test coverage thresholds for unit tests using JaCoCo.
     *
     * This function allows setting custom coverage requirements for different metrics of code coverage.
     * If coverage falls below these minimums during verification, the build will fail.
     *
     * Example usage:
     * ```kotlin
     * simpleJ {
     *     unitTestCoverageMinimums {
     *         instruction = 80
     *         branch = 75
     *         line = 80
     *         complexity = 75
     *         method = 80
     *         clazz = 90
     *     }
     * }
     * ```
     *
     * Available coverage metrics:
     * - instruction: Percentage of Java bytecode instructions covered
     * - branch: Percentage of branches covered (if/switch statements)
     * - line: Percentage of lines covered
     * - complexity: Percentage of cyclomatic complexity covered
     * - method: Percentage of methods covered
     * - clazz: Percentage of classes covered
     *
     * All values should be specified as percentages between 0 and 100.
     *
     * @param configuration Lambda with receiver that configures the coverage thresholds.
     *                     The receiver [TestCoverageMinimums] provides properties for each coverage metric.
     */
    fun unitTestCoverageMinimums(configuration: TestCoverageMinimums.() -> Unit) {
        coverageMinimums.configuration()
    }
}
