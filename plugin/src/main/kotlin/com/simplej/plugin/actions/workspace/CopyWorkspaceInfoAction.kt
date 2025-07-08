// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.actions.workspace

import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.components.service
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.SystemInfo
import com.simplej.base.ProjectViewPopupMenuItem
import com.simplej.base.SimpleJAnAction
import com.simplej.base.extensions.runCommandForOutputStream
import com.simplej.base.extensions.showError
import com.simplej.plugin.SimpleJCoroutineService
import com.simplej.plugin.WorkspaceCompat
import com.simplej.plugin.simpleJConfig
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import org.gradle.util.GradleVersion
import java.awt.datatransfer.StringSelection
import java.io.File
import java.io.IOException

/**
 * An action that collects comprehensive information about the user's workspace environment and copies it to the
 * system clipboard as a JSON string.
 *
 * This action is designed to simplify the process of gathering diagnostic data for debugging, support requests, or
 * environment replication. It aggregates details about the user, hardware, software, and the results of workspace
 * validation checks into a structured format.
 *
 * ### Information Collected:
 * - **User:** The current user's name.
 * - **Hardware:** System hardware details.
 * - **Software:** Operating system and other relevant software versions.
 * - **Workspace Validation:** A set of validation results checking the workspace configuration.
 *
 * The collected data is serialized into a `WorkspaceInfo` object and then copied to the clipboard.
 */
internal class CopyWorkspaceInfoAction : SimpleJAnAction(), ProjectViewPopupMenuItem {

    /**
     * Executes the action to gather workspace information and copy it to the clipboard.
     *
     * This method orchestrates the collection of hardware, software, and user data, serializes it into a JSON string
     * using the [WorkspaceInfo] format, and places it on the system clipboard. A notification is shown to the user
     * upon successful completion.
     *
     * @param event The [AnActionEvent] providing context for the action.
     */
    @Suppress("ReturnCount")
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return event.showError("No valid project found within the workspace.")
        val simpleJConfig = project.simpleJConfig() ?: return event.showError(
            "No valid `simplej-config.json` configuration file found within `${project.basePath}/config/simplej`!"
        )
        val workspaceCompat = simpleJConfig.workspaceCompat ?: return event.showError(
            "No `workspaceCompat` configuration found within `simplej-config.json`!"
        )

        copyWorkspaceInfo(project, workspaceCompat)
    }

    companion object {

        private const val BYTES_IN_GIGABYTE = 1_073_741_824L

        @JvmStatic
        internal fun copyWorkspaceInfo(
            project: Project,
            workspaceCompat: WorkspaceCompat,
            workspaceValidation: WorkspaceValidation? = null
        ) = service<SimpleJCoroutineService>().scope.launch {
            val applicationInfo = ApplicationInfo.getInstance()
            val copyPasteManager = CopyPasteManager.getInstance()
            @Suppress("JSON_FORMAT_REDUNDANT")
            copyPasteManager.setContents(
                StringSelection(
                    Json { prettyPrint = true }.encodeToString(
                        WorkspaceInfo(
                            userName = System.getProperty("user.name"),
                            hardware = Hardware(
                                cpuArchitecture = SystemInfo.OS_ARCH,
                                cpuCores = Runtime.getRuntime().availableProcessors(),
                                cpuFrequency = Hardware.getCpuFrequency(),
                                cpuModel = runCommandForOutputStream("sysctl -n machdep.cpu.brand_string"),
                                ramTotal = runCommandForOutputStream("sysctl -n hw.memsize")
                                    ?.toDouble()
                                    ?.div(BYTES_IN_GIGABYTE),
                                ramUsed = Hardware.getTotalMemoryUsed(),
                                model = runCommandForOutputStream("sysctl -n hw.model")
                            ),
                            software = Software(
                                osName = SystemInfo.OS_NAME,
                                osVersion = SystemInfo.OS_VERSION,
                                ideName = applicationInfo.fullApplicationName,
                                ideVersion = applicationInfo.apiVersion,
                                jdkVersion = WorkspaceValidation.rawJavaVersion(),
                                jdkHome = WorkspaceValidation.rawJavaHome(),
                                gradleVersion = GradleVersion.current().toString(),
                                androidGradlePluginVersion = Software.getAgpVersion(project)
                            ),
                            workspaceValidation = if (workspaceValidation == null) {
                                val bundle = WorkspaceValidation.populate(project, workspaceCompat, this@launch)
                                bundle.second.awaitAll()
                                bundle.first
                            } else {
                                workspaceValidation
                            }.toJson()
                        )
                    )
                )
            )
        }
    }

    /**
     * A data class representing the structured information collected about the workspace. This object is serialized
     * to JSON before being copied to the clipboard.
     *
     * @property userName The name of the current system user.
     * @property hardware Contains details about the system's hardware configuration.
     * @property software Contains details about the operating system and other software.
     * @property workspaceValidation A JSON array containing results from workspace configuration checks.
     */
    @Serializable
    private data class WorkspaceInfo(
        val userName: String? = null,
        val hardware: Hardware,
        val software: Software,
        val workspaceValidation: JsonArray
    )

    @Serializable
    private data class Hardware(
        val cpuArchitecture: String,
        val cpuCores: Int,
        val cpuFrequency: Double? = null,
        val cpuModel: String? = null,
        val ramTotal: Double? = null,
        val ramUsed: Double? = null,
        val model: String? = null
    ) {
        companion object {

            private const val HERTZ_IN_GIGAHERTZ = 1_000_000_000.0
            private const val MEGA_HERTZ_IN_GIGAHERTZ = 1_000.0
            private const val BYTES_TO_GIGABYTES = 1_073_741_824.0

            /**
             *  Page size in bytes on macOS
             */
            private const val PAGE_SIZE = 4096

            fun getCpuFrequency(): Double? {
                val os = System.getProperty("os.name")
                return when {
                    os.contains("Mac", ignoreCase = true) -> {
                        val output = runCommandForOutputStream("sysctl hw.cpufrequency")
                        // Parse output like: "hw.cpufrequency: 2400000000"
                        val value = output?.split(":")?.lastOrNull()?.trim()?.toDoubleOrNull()
                        value?.div(HERTZ_IN_GIGAHERTZ)
                    }

                    os.contains("Linux", ignoreCase = true) -> {
                        val output = runCommandForOutputStream("lscpu | grep 'MHz'")
                        // Parse output like: "CPU MHz: 2394.567"
                        val value = output?.split(":")?.lastOrNull()?.trim()?.toDoubleOrNull()
                        value?.div(MEGA_HERTZ_IN_GIGAHERTZ)
                    }

                    os.contains("Windows", ignoreCase = true) -> {
                        val output = runCommandForOutputStream(
                            "powershell.exe Get-WmiObject Win32_Processor | " +
                                    "Select-Object -ExpandProperty MaxClockSpeed"
                        )
                        // Parse PowerShell output like "2900" for 2.9GHz
                        val value = output?.trim()?.toDoubleOrNull()
                        value?.div(MEGA_HERTZ_IN_GIGAHERTZ)
                    }

                    else -> null
                }
            }

            @Suppress("TooGenericExceptionCaught", "SwallowedException")
            fun getTotalMemoryUsed(): Double? {
                try {
                    var activePages = 0L
                    var wiredPages = 0L
                    var compressedPages = 0L
                    runCommandForOutputStream("vm_stat")?.lineSequence()?.forEach { line ->
                        when {
                            line.contains("Pages active:") -> activePages = extractPages(line)
                            line.contains("Pages wired down:") -> wiredPages = extractPages(line)
                            line.contains("Pages stored in compressor:") -> compressedPages = extractPages(line)
                        }
                    }

                    val totalUsedBytes = (activePages + wiredPages + compressedPages) * PAGE_SIZE
                    return totalUsedBytes.div(BYTES_TO_GIGABYTES)
                } catch (e: Exception) {
                    return null
                }
            }

            private fun extractPages(line: String): Long =
                line.split(":")
                    .lastOrNull()
                    ?.trim()
                    ?.removeSuffix(".")
                    ?.toLongOrNull()
                    ?: 0L
        }
    }

    @Serializable
    private data class Software(
        val osName: String,
        val osVersion: String,
        val ideName: String,
        val ideVersion: String,
        val jdkVersion: String? = null,
        val jdkHome: String? = null,
        val gradleVersion: String,
        val androidGradlePluginVersion: String? = null
    ) {
        companion object {

            @JvmStatic
            private val agpRegex = listOf(
                // For libs.versions.toml (e.g., agp = "8.2.0")
                "agp\\s*=\\s*\"([^\"]+)\"".toRegex(),
                // For libs.versions.toml (e.g., android-gradle-plugin = "8.2.0")
                "android-gradle-plugin\\s*=\\s*\"([^\"]+)\"".toRegex(),
                // For build.gradle.kts (e.g., id("com.android.application") version "8.2.0")
                "id\\(\"com\\.android\\.(?:application|library)\"\\)\\s*version\\s*\"([^\"]+)\"".toRegex(),
                // For build.gradle (e.g., id 'com.android.application' version '8.2.0')
                "id\\s+'com\\.android\\.(?:application|library)'\\s*version\\s*'([^']+)".toRegex(),
                // For older build.gradle (e.g., classpath 'com.android.tools.build:gradle:7.2.1')
                "classpath\\s*['\"]com\\.android\\.tools\\.build:gradle:([^'\"]+)['\"]".toRegex()
            )

            @Suppress("NestedBlockDepth", "SwallowedException")
            @JvmStatic
            fun getAgpVersion(project: Project): String? {
                val searchFiles = listOf(
                    File(project.basePath, "gradle/libs.versions.toml"),
                    File(project.basePath, "libs.versions.toml"),
                    File(project.basePath, "build.gradle.kts"),
                    File(project.basePath, "build.gradle")
                )

                for (file in searchFiles) {
                    if (file.exists()) {
                        try {
                            val content = file.readText()
                            for (pattern in agpRegex) {
                                val match = pattern.find(content)
                                if (match != null) {
                                    return match.groupValues[1]
                                }
                            }
                        } catch (e: IOException) {
                            // Continue to the next file if one is unreadable
                        }
                    }
                }
                return null
            }
        }
    }
}
