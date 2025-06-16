// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.scripts.dsl

/**
 * A [DslMarker] interface that defines the scope for DSL elements used by the SimpleJ Gradle Plugins extension.
 */
@DslMarker
@Target(AnnotationTarget.CLASS, AnnotationTarget.TYPE)
annotation class DslScope