package com.simplej.plugin.overlay

import kotlinx.serialization.json.Json
import java.io.File

/**
 * Service for managing web browser configuration.
 */
object WebBrowserConfigService {
    private const val CONFIG_FILE_NAME = "web-browser-mappings.json"

    private val json = Json {
        prettyPrint = true
        ignoreUnknownKeys = true
    }

    /**
     * Loads the web browser configuration from the project root.
     * Returns an empty configuration if the file doesn't exist.
     */
    fun loadConfig(projectPath: String): WebBrowserConfig {
        val configFile = File(projectPath, CONFIG_FILE_NAME)

        return if (configFile.exists()) {
            try {
                val content = configFile.readText()
                json.decodeFromString<WebBrowserConfig>(content)
            } catch (e: Exception) {
                WebBrowserConfig()
            }
        } else {
            WebBrowserConfig()
        }
    }

    /**
     * Gets the URL for a given file path from the configuration.
     */
    fun getUrlForFile(projectPath: String, filePath: String): String? {
        val config = loadConfig(projectPath)

        // Try exact match first
        config.fileMappings[filePath]?.let { return it }

        // Try relative path matches
        val relativePath = filePath.substringAfter(projectPath).removePrefix("/")
        return config.fileMappings[relativePath]
    }
}