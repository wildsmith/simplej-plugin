// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin

import com.intellij.openapi.components.Service
import kotlinx.coroutines.CoroutineScope

/**
 * Provides a managed [CoroutineScope] for the SimpleJ plugin.
 *
 * This service is responsible for creating and managing a coroutine scope that is tied to the lifecycle of the
 * application or project. Coroutines launched within this scope will be automatically cancelled when the
 * corresponding lifecycle ends (e.g., when the IDE is closed or the project is unloaded).
 *
 * This helps prevent memory leaks and ensures that long-running tasks do not outlive their intended context.
 *
 * ### Usage:
 * To use this service, inject it into your component (e.g., an Action, a Tool Window, or another Service)
 * and launch your coroutines using the provided [scope].
 *
 * ```kotlin
 * class MyAction : AnAction() {
 *     override fun actionPerformed(e: AnActionEvent) {
 *         val project = e.project ?: return
 *         val coroutineService = project.service<SimpleJCoroutineService>()
 *         coroutineService.scope.launch {
 *             // Your background task here...
 *         }
 *     }
 * }
 * ```
 *
 * @property scope The [CoroutineScope] managed by this service. Use this scope to launch lifecycle-aware coroutines.
 */
@Service
class SimpleJCoroutineService(val scope: CoroutineScope)
