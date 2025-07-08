// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.base.extensions

import java.io.File

/**
 * Runs a given command via the local workspaces shell access within the [directory] specified; if `null` then the
 * current directory
 *
 * @param command a specified system command.
 * @param directory the working directory of the subprocess, or null if the subprocess should inherit the working
 * directory of the current process.
 */
@Suppress("TooGenericExceptionCaught", "SwallowedException")
fun runCommandForOutputStream(
    command: String,
    directory: File? = null,
): String? = runCommand(command, directory).first

/**
 * Runs a given command via the local workspaces shell access within the [directory] specified; if `null` then the
 * current directory
 *
 * @param command a specified system command.
 * @param directory the working directory of the subprocess, or null if the subprocess should inherit the working
 * directory of the current process.
 */
@Suppress("TooGenericExceptionCaught", "SwallowedException")
fun runCommandForErrorStream(
    command: String,
    directory: File? = null,
): String? = runCommand(command, directory).second

/**
 * Runs a given command via the local workspaces shell access within the [directory] specified; if `null` then the
 * current directory
 *
 * @param command a specified system command.
 * @param directory the working directory of the subprocess, or null if the subprocess should inherit the working
 * directory of the current process.
 */
@Suppress("TooGenericExceptionCaught", "SwallowedException")
fun runCommand(
    command: String,
    directory: File? = null,
): Pair<String?, String?> {
    return try {
        val process = Runtime.getRuntime().exec(
            command,
            null,
            directory
        )
        val output = process.inputStream?.bufferedReader()?.readText()?.trim()
        val error = process.errorStream?.bufferedReader()?.readText()?.trim()
        process.waitFor()
        Pair(output, error)
    } catch (e: Exception) {
        Pair(null, null)
    }
}
