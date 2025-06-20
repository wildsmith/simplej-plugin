// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.overlay

import kotlinx.serialization.Serializable

/**
 * Configuration data class for web browser overlay mappings.
 * Maps file paths to URLs for displaying in the editor browser overlay.
 *
 * The rest of this logic is in [WebBrowserConfigService]
 */
@Serializable
data class WebBrowserConfig(
    val fileMappings: Map<String, String> = mapOf()
)

