// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin

import com.intellij.openapi.components.Service
import kotlinx.coroutines.CoroutineScope

@Service
class SimpleJCoroutineService(val scope: CoroutineScope)
