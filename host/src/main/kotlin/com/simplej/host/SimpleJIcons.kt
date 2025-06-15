package com.simplej.host

import com.intellij.openapi.util.IconLoader
import javax.swing.Icon

/**
 * Centralizes *static* icon management for the SimpleJ plugin. This is to be used as an example, the alternative
 * is a direct path reference within the `plugin.xml` file.
 *
 * This object provides access to all icons used throughout the plugin. Icons are loaded lazily using IntelliJ's
 * [IconLoader] and cached for reuse. All icons should be placed in the `/icons/expui/` resources directory as SVG
 * files.
 */
@Suppress("unused")
object SimpleJIcons {

    /**
     * Icon representing an Android device. Used in UI elements related to Android device operations. The icon is
     * loaded from `/icons/expui/androidDevice.svg`.
     */
    @JvmField
    val AndroidDevice: Icon = load("androidDevice")

    /**
     * Loads an icon from the plugin's resources.
     *
     * @param name The base name of the icon file without extension
     * @return The loaded [Icon] instance
     */
    private fun load(name: String): Icon =
        IconLoader.getIcon("/icons/expui/$name.svg", javaClass)
}
