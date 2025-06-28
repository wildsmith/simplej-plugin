// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin

import com.intellij.openapi.project.Project
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.decodeFromStream
import java.io.File

private val jsonSerializer: Json by lazy {
    Json { ignoreUnknownKeys = true }
}

/**
 * Loads and parses the SimpleJ configuration from a JSON file.
 *
 * @param simpleJConfigFile The configuration file to load. Defaults to 'config/simplej/simplej-config.json' in the
 * project's base path.
 * @return The parsed [SimpleJConfig] object, or null if the file doesn't exist.
 */
internal fun Project.simpleJConfig(
    simpleJConfigFile: File = getSimpleJFile()
): SimpleJConfig? {
    if (!simpleJConfigFile.exists()) {
        return null
    }

    return simpleJConfigFile.inputStream().use {
        @OptIn(ExperimentalSerializationApi::class)
        jsonSerializer.decodeFromStream<SimpleJConfig>(it)
    }
}

/**
 * Returns the `simplej-config.json` [File]
 *
 * Note: this file may or may not exist, it's best to check its existence before performing any actions.
 *
 * @return the `simplej-config.json` [File]
 */
internal fun Project.getSimpleJFile() =
    File("$basePath/config/simplej/simplej-config.json")

/**
 * Root configuration class for SimpleJ plugin settings.
 *
 * @property workspaceCompat Configuration for workspace compatibility settings.
 */
@Serializable
internal data class SimpleJConfig(
    val workspaceCompat: WorkspaceCompat? = null,
    val webBrowserMappings: WebBrowserMappings? = null,
    val newModuleTemplates: List<NewModuleTemplate>? = null,
)

/**
 * Workspace compatibility configuration containing various environment settings.
 *
 * @property ssh SSH-related configuration settings
 * @property java Java environment configuration
 * @property android Android-specific configuration
 */
@Serializable
internal data class WorkspaceCompat(
    val ssh: SshConfig? = null,
    val java: JavaConfig? = null,
    val android: AndroidConfig? = null,
)

/**
 * SSH configuration settings.
 *
 * @property testRepo The SSH endpoint used for testing connectivity
 */
@Serializable
internal data class SshConfig(
    val testRepo: String? = null
)

/**
 * Java environment configuration settings.
 *
 * @property version The required Java version specification
 * @property home The path to Java home directory
 */
@Serializable
internal data class JavaConfig(
    val version: SemanticVersion? = null,
    val home: String? = null,
)

/**
 * Android-specific configuration settings.
 *
 * @property buildTools The required Android build tools version
 */
@Serializable
internal data class AndroidConfig(
    val buildTools: SemanticVersion? = null,
)

/**
 * Represents a semantic version number with major, minor, and patch components.
 *
 * @property major The major version number (required)
 * @property minor The minor version number (optional)
 * @property patch The patch version number (optional)
 */
@Serializable
internal data class SemanticVersion(
    val major: Int,
    val minor: Int? = null,
    val patch: Int? = null,
) {

    /**
     * Returns a string representation of the version, using 'x' for unspecified components.
     *
     * For example, "1.x.x" or "1.2.x" or "1.2.3"
     */
    override fun toString(): String {
        return "$major${minor?.let { ".$minor" } ?: ".x"}${patch?.let { ".$patch" } ?: ".x"}"
    }

    /**
     * Checks if this version matches a given version string.
     *
     * @param other The version strings to match against
     * @return true if the versions match, considering wildcards
     */
    fun matches(other: String): Boolean {
        val minor = minor.asRegexString()
        val patch = patch.asRegexString()
        return other.matches(Regex("(.*)$major$minor$patch(.*)"))
    }

    /**
     * Returns a compatibility string representation of the version. Omits unspecified components rather than using 'x'.
     *
     * For example, "1" or "1.2" or "1.2.3"
     */
    fun toCompatString(): String {
        return "$major${minor?.let { ".$minor" } ?: ""}${patch?.let { ".$patch" } ?: ""}"
    }

    /**
     * Converts an optional integer to its regex string representation. Returns ".*" for null values or the exact
     * version number prefixed with a dot.
     */
    private fun Int?.asRegexString(): String = if (this == null) {
        "(.*)"
    } else {
        ".$this"
    }
}

/**
 * Configuration data class for web browser overlay mappings. Maps file paths to URLs for displaying in the editor
 * browser overlay.
 */
@Serializable
internal data class WebBrowserMappings(
    val fileMappings: Map<String, String>? = null
) {

    /**
     * Gets the URL for a given file path from the configuration.
     */
    fun getUrlForFile(projectPath: String, filePath: String): String? {
        // Try the exact match first
        fileMappings?.get(filePath)?.let { return it }

        // Try relative path matches
        val relativePath = filePath.substringAfter(projectPath).removePrefix("/")
        return fileMappings?.get(relativePath)
    }
}

@Serializable
internal data class NewModuleTemplate(
    val name: String,
    val files: List<NewFileTemplate>,
)

@Serializable
internal data class NewFileTemplate(
    val relativePath: String,
    val templateLocation: String,
) {

    fun template(basePath: String): File =
        File("$basePath/config/simplej/templates/$templateLocation")
}
