// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.base.extensions

import androidx.annotation.RestrictTo
import com.intellij.openapi.application.ApplicationManager

/**
 * Executes a given task on IntelliJ's background thread pool.
 *
 * This function provides a convenient way to run tasks in the background without blocking the IDE's UI thread. The
 * task is executed on IntelliJ's pooled thread executor.
 *
 * Example usage:
 * ```
 * executeBackgroundTask {
 *     // Perform long-running operation
 * }
 * ```
 *
 * @param task A lambda containing the code to be executed in the background.
 *             The task should be self-contained and not require direct UI updates.
 *
 * @see ApplicationManager.getApplication
 * @see com.intellij.openapi.application.Application.executeOnPooledThread
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
fun executeBackgroundTask(task: () -> Unit) {
    ApplicationManager.getApplication().executeOnPooledThread {
        task()
    }
}
