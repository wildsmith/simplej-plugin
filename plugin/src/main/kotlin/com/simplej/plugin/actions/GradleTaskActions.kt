// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.actions

import com.intellij.execution.executors.DefaultRunExecutor
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.externalSystem.model.execution.ExternalSystemTaskExecutionSettings
import com.intellij.openapi.externalSystem.service.execution.ProgressExecutionMode
import com.intellij.openapi.externalSystem.task.TaskCallback
import com.intellij.openapi.externalSystem.util.ExternalSystemUtil
import com.intellij.openapi.project.Project
import com.simplej.base.EditorPopupMenuItem
import com.simplej.base.ProjectViewPopupMenuItem
import com.simplej.base.SimpleJAnAction
import com.simplej.base.extensions.currentFile
import com.simplej.base.extensions.findClosestProject
import com.simplej.base.extensions.showError
import org.jetbrains.plugins.gradle.util.GradleConstants
import java.io.File

/**
 * Abstract base class for actions that execute Gradle tasks within the IDE.
 *
 * This class provides the foundation for creating actions that can run one or more Gradle tasks in the background.
 * It integrates with both the Project View and Editor context menus, allowing task execution from multiple UI entry
 * points.
 *
 * The action automatically:
 * - Validates the current file and project context
 * - Finds the closest Gradle project root
 * - Executes specified Gradle tasks asynchronously
 *
 * Example usage:
 * ```kotlin
 * class BuildDebugTask : GradleTaskAction("assembleDebug")
 * ```
 *
 * @property tasks Set of Gradle task names to be executed
 *
 * @constructor Creates a new GradleTaskAction with the specified tasks
 * @param tasks Variable number of task names to execute
 */
abstract class GradleTaskAction private constructor(
    private val tasks: Set<String>?,
    private val complexTasks: Set<GradleTaskAction>?
) : SimpleJAnAction(), ProjectViewPopupMenuItem, EditorPopupMenuItem {

    /**
     * Creates a new GradleTaskAction with the specified task names.
     *
     * @param tasks Variable number of task names to be executed
     */
    constructor(vararg tasks: String) : this(tasks.toSet(), null)

    /**
     * Creates a new GradleTaskAction by combining tasks from other GradleTaskActions.
     *
     * This constructor allows composition of multiple Gradle task actions into a single action that will execute all
     * tasks from the provided actions.
     *
     * @param tasks Variable number of GradleTaskActions whose tasks should be combined
     */
    constructor(vararg tasks: GradleTaskAction) : this(null, tasks.toSet())

    @Suppress("ReturnCount")
    override fun actionPerformed(event: AnActionEvent) {
        val currentFile = event.currentFile ?: return event.showError(
            "No valid file found within the project workspace."
        )
        val project = event.project ?: return event.showError(
            "No valid project found within the workspace."
        )
        val projectFile = currentFile.findClosestProject(project) ?: return event.showError(
            "Unable to find closest valid project within the workspace."
        )
        val tasks = getTasks(event, project)
        ExternalSystemUtil.runTask(
            ExternalSystemTaskExecutionSettings().apply {
                externalProjectPath = projectFile.path
                taskNames = tasks.toList()
                scriptParameters = ""
                vmOptions = ""
                externalSystemIdString = GradleConstants.SYSTEM_ID.id
            },
            DefaultRunExecutor.EXECUTOR_ID,
            project,
            GradleConstants.SYSTEM_ID,
            object : TaskCallback {
                override fun onSuccess() {
                    // no op
                }

                override fun onFailure() {
                    // no op
                }
            },
            ProgressExecutionMode.IN_BACKGROUND_ASYNC,
            true
        )
    }

    private fun getTasks(event: AnActionEvent, project: Project): Set<String> {
        return when {
            tasks != null -> tasks
            complexTasks != null -> complexTasks
                .filter { it.shouldShow(event, project) }
                .flatMap { it.tasks!! }
                .toSet()

            else -> emptySet()
        }
    }
}

/**
 * Action to execute Gradle's 'clean' task which removes build outputs and temporary files.
 *
 * This singleton action executes Gradle's built-in 'clean' task that deletes the build directory and all generated
 * files from previous builds. This includes:
 * - Compiled classes
 * - Generated sources
 * - Processed resources
 * - Test reports
 * - Temporary files
 * - Built artifacts (JARs, AARs, APKs)
 *
 * The action is available through:
 * - Project view context menu
 * - Editor context menu
 *
 * The task runs asynchronously in the background, and progress can be monitored in the Build tool window.
 *
 * This action is useful when you need to:
 * - Ensure a clean build state
 * - Resolve build issues
 * - Free up disk space
 * - Remove stale build outputs
 *
 * @see GradleTaskAction
 * @see <a href="https://docs.gradle.org/current/userguide/base_plugin.html#sec:base_tasks">Gradle Base Plugin Tasks</a>
 */

object CleanTaskAction : GradleTaskAction("clean")

/**
 * Action to run the Detekt static code analysis tool via Gradle.
 *
 * This singleton action executes the 'detekt' Gradle task which performs static code analysis on Kotlin source files
 * using the Detekt tool. The task analyzes the code for potential issues, code smells, and style violations
 * according to the project's Detekt configuration.
 *
 * The action is available through:
 * - Project view context menu
 * - Editor context menu
 *
 * The analysis runs asynchronously in the background, and the results will be displayed in the Build tool window.
 *
 * @see GradleTaskAction
 * @see <a href="https://detekt.dev/">Detekt Documentation</a>
 */
object DetektTaskAction : GradleTaskAction("detekt")

/**
 * Action to run Checkstyle code analysis via Gradle.
 *
 * This singleton action executes the 'checkstyleMain' Gradle task which performs static code analysis on Java source
 * files using Checkstyle. The task validates code against a defined set of coding standards and style rules
 * specified in the project's Checkstyle configuration.
 *
 * The action is available through:
 * - Project view context menu
 * - Editor context menu
 *
 * The analysis runs asynchronously in the background, and results will be displayed in the Build tool window. Any
 * style violations or warnings will be reported according to the severity levels defined in the Checkstyle
 * configuration.
 *
 * @see GradleTaskAction
 * @see <a href="https://checkstyle.sourceforge.io/">Checkstyle Documentation</a>
 */
object CheckstyleTaskAction : GradleTaskAction("checkstyleMain")

/**
 * Action to run Android Lint analysis via Gradle.
 *
 * This singleton action executes the 'lint' Gradle task which performs static code analysis specifically designed
 * for Android projects. Android Lint checks for potential bugs, performance issues, security vulnerabilities,
 * accessibility problems, and other Android-specific concerns in both source code and resource files.
 *
 * The action is available through:
 * - Project view context menu
 * - Editor context menu
 *
 * The analysis runs asynchronously in the background, and results will be displayed in the Build tool window. The
 * lint report includes:
 * - Code structure issues
 * - Resource optimization suggestions
 * - Layout performance recommendations
 * - XML resource problems
 * - App compatibility issues
 * - Gradle configuration warnings
 *
 * @see GradleTaskAction
 * @see <a href="https://developer.android.com/studio/write/lint">Android Lint Documentation</a>
 */
object LintTaskAction : GradleTaskAction("lint")

/**
 * Action to run all available static code analysis tools via Gradle.
 *
 * This singleton action combines multiple static analysis tasks into a single execution, including:
 * - Checkstyle (Java code style checker)
 * - Detekt (Kotlin static code analysis)
 * - Android Lint (Android-specific analysis)
 *
 * The action provides a convenient way to perform comprehensive code quality checks across all supported languages
 * and frameworks in the project. It executes all analysis tools sequentially in the background.
 *
 * The action is available through:
 * - Project view context menu
 * - Editor context menu
 *
 * Results from all analysis tools will be aggregated and displayed in the Build tool window, providing a complete
 * overview of:
 * - Code style violations
 * - Potential bugs and code smells
 * - Performance issues
 * - Best practice violations
 * - Android-specific problems
 *
 * @see CheckstyleTaskAction
 * @see DetektTaskAction
 * @see LintTaskAction
 * @see GradleTaskAction
 */
object AllStaticCodeAnalysisTaskAction : GradleTaskAction(
    CheckstyleTaskAction,
    DetektTaskAction,
    LintTaskAction
)

/**
 * Action to run the Gradle 'check' task which performs project verification.
 *
 * This singleton action executes Gradle's built-in 'check' task that aggregates all verification tasks in the
 * project, including:
 * - Unit tests
 * - Integration tests
 * - Static analysis tasks
 * - Code quality checks
 * - Other custom verification tasks configured in the project
 *
 * The action serves as a comprehensive quality gate that ensures the project meets all defined quality criteria and
 * tests pass successfully.
 *
 * The action is available through:
 * - Project view context menu
 * - Editor context menu
 *
 * The task runs asynchronously in the background, and results will be displayed in the Build tool window, showing:
 * - Test execution results
 * - Code analysis findings
 * - Build verification outcomes
 * - Any other configured verification reports
 *
 * @see GradleTaskAction
 * @see <a href="https://docs.gradle.org/current/userguide/java_plugin.html#lifecycle_tasks">Lifecycle Tasks</a>
 */
object CheckTaskAction : GradleTaskAction("check")

/**
 * Action to run the Gradle 'build' task which performs a complete project build.
 *
 * This singleton action executes Gradle's 'build' task that performs a full build of the project, including
 * compilation, testing, verification, and packaging. It combines the functionality of both [AssembleTaskAction] and
 * [CheckTaskAction].
 *
 * The task performs the following operations in order:
 * 2. Compilation and resource processing
 * 3. Unit tests execution
 * 4. Code quality checks and static analysis
 * 7. Packaging and artifact generation
 *
 * For Android projects, additional steps include:
 * - Manifest processing and merging
 * - Resource compilation and merging
 * - DEX compilation
 * - APK packaging and signing
 *
 * The action is available through:
 * - Project view context menu
 * - Editor context menu
 *
 * The task runs asynchronously in the background, and results will be displayed in the Build tool window, showing:
 * - Test execution results
 * - Code analysis findings
 * - Build verification outcomes
 * - Any other configured verification reports
 *
 * Note: This is the most comprehensive build task and may take longer to complete compared to individual tasks like
 * [AssembleTaskAction] or [CheckTaskAction].
 *
 * @see GradleTaskAction
 * @see AssembleTaskAction
 * @see CheckTaskAction
 */
object BuildTaskAction : GradleTaskAction("build")

/**
 * Action to run Android instrumented tests on a connected device or emulator.
 *
 * This singleton action executes the 'connectedAndroidTest' Gradle task which runs instrumentation tests that
 * require an Android device or emulator. These tests verify app behavior in a real Android environment and can
 * interact with:
 * - Android Framework APIs
 * - UI components
 * - System services
 * - Hardware sensors
 * - Device features
 *
 * Prerequisites:
 * - At least one Android device or emulator must be connected
 * - Device/emulator must be authorized for development
 * - Screen should be unlocked
 *
 * The action is available through:
 * - Project view context menu
 * - Editor context menu
 *
 * Tests run asynchronously and results are displayed in:
 * - Build tool window
 * - Android Studio's Test Runner window
 * - Generated test reports
 *
 * Note: These tests run significantly slower than local unit tests as they require actual device interaction and app
 * installation.
 *
 * @see GradleTaskAction
 * @see <a href="https://developer.android.com/studio/test/test-in-android-studio">Testing in Android Studio</a>
 */
@Suppress("ReturnCount")
object ConnectedAndroidTestTaskAction : GradleTaskAction("connectedAndroidTest") {

    override fun shouldShow(event: AnActionEvent, project: Project): Boolean {
        val projectFile = event.currentFile?.findClosestProject(project) ?: return super.shouldShow(event, project)
        val buildFile = File("${projectFile.path}/build.gradle.kts")
        if (!buildFile.exists()) {
            File("${projectFile.path}/build.gradle")
            if (!buildFile.exists()) {
                return super.shouldShow(event, project)
            }
        }
        var isLikelyAndroidProject = false
        buildFile.useLines { lines ->
            for (line in lines) {
                if (line.contains(".android.") || line.contains("android {")) {
                    isLikelyAndroidProject = true
                    break
                }
            }
        }
        return isLikelyAndroidProject
    }
}

/**
 * Action to run all types of tests in the project via Gradle.
 *
 * This singleton action combines multiple test tasks into a single execution, providing comprehensive test coverage
 * across different test types:
 * - Unit tests (`test` task)
 * - Integration tests
 * - Android instrumented tests (`connectedAndroidTest` task)
 * - Android unit tests (`testDebug` task)
 *
 * The action offers a convenient way to verify the entire test suite before commits or releases. Tests are executed
 * in order of increasing complexity and execution time:
 * 1. Local unit tests (fastest)
 * 2. Android-specific unit tests
 * 3. Integration tests
 * 4. Instrumented device tests (slowest)
 *
 * Prerequisites for Android instrumented tests:
 * - Connected Android device or running emulator
 * - Device must be authorized for development
 * - Screen should be unlocked
 *
 * The action is available through:
 * - Project view context menu
 * - Editor context menu
 *
 * Results are aggregated and displayed in:
 * - Build tool window
 * - Test Runner window
 * - Generated HTML test reports
 *
 * @see BuildTaskAction
 * @see ConnectedAndroidTestTaskAction
 * @see GradleTaskAction
 */
object AllTestTypesTaskAction : GradleTaskAction(
    BuildTaskAction,
    ConnectedAndroidTestTaskAction
)

/**
 * Action to run the Gradle 'assemble' task which compiles and packages the project.
 *
 * This singleton action executes Gradle's 'assemble' task that creates the project's distributable packages without
 * running tests or checks. For Android projects, it builds all configured build variants (debug, release, etc.).
 *
 * The task performs the following operations:
 * - Compiles source code
 * - Processes resources
 * - Packages compiled code and resources
 * - Generates build artifacts such as:
 *   - JAR/AAR files for Java/Android libraries
 *   - APK files for Android applications
 *   - Distribution archives for Java applications
 *
 * For Android projects, this includes:
 * - Merging manifest files
 * - Processing resources and assets
 * - DEX compilation for Android bytecode
 * - APK packaging and signing (if configured)
 *
 * The action is available through:
 * - Project view context menu
 * - Editor context menu
 *
 * Build progress and results are displayed in:
 * - Build tool window
 * - Event log
 * - Build artifacts are placed in the project's build directory
 *
 * Note: This task does not run tests or quality checks. Use [CheckTaskAction] for verification or [BuildTaskAction]
 * for a complete build with tests.
 *
 * @see GradleTaskAction
 * @see BuildTaskAction
 * @see CheckTaskAction
 * @see <a href="https://docs.gradle.org/current/userguide/java_plugin.html#sec:java_tasks">Gradle Java Plugin Tasks</a>
 */
object AssembleTaskAction : GradleTaskAction("assemble")
