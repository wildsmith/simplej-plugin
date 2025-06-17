// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.scripts.dsl

/**
 * Defines minimum coverage thresholds for different code coverage metrics in JaCoCo test reports.
 * 
 * This class encapsulates various code coverage metrics used to enforce quality standards in unit tests.
 * Each property represents a different aspect of code coverage and is specified as a percentage (0-100).
 *
 * Example usage:
 * ```kotlin
 * simpleJ {
 *     unitTestCoverageMinimums {
 *         instruction = 80  // 80% instruction coverage required
 *         branch = 75      // 75% branch coverage required
 *         line = 80        // 80% line coverage required
 *         complexity = 75  // 75% complexity coverage required
 *         method = 80      // 80% method coverage required
 *         clazz = 90       // 90% class coverage required
 *     }
 * }
 * ```
 *
 * Properties:
 * @property instruction Percentage of Java bytecode instructions that must be covered by tests.
 *                      Represents the most fine-grained coverage metric.
 *
 * @property branch Percentage of branch points (if/switch statements) that must be covered by tests.
 *                 Ensures different code paths are tested.
 *
 * @property line Percentage of source code lines that must be covered by tests.
 *               Provides a human-readable metric of code coverage.
 *
 * @property complexity Percentage of cyclomatic complexity that must be covered by tests.
 *                     Ensures complex code paths are adequately tested.
 *
 * @property method Percentage of methods/functions that must be covered by tests.
 *                 Ensures comprehensive testing of class behaviors.
 *
 * @property clazz Percentage of classes that must be covered by tests.
 *                Ensures no classes are completely untested.
 *
 * All properties accept values from 0 to 100, representing percentage coverage requirements.
 * The build will fail if actual coverage falls below any of these thresholds during verification.
 */
class TestCoverageMinimums {
    var instruction: Int = 0
    var branch: Int = 0
    var line: Int = 0
    var complexity: Int = 0
    var method: Int = 0
    var clazz: Int = 0
}