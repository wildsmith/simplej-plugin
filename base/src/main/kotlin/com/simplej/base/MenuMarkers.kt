// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.base

/**
 * Marker interface for actions that should appear in the editor popup menu.
 *
 * Implementing this interface indicates that the action should be included in the context menu when right-clicking
 * within the editor window.
 *
 * This interface is typically used in conjunction with [com.intellij.openapi.actionSystem.AnAction].
 */
interface EditorPopupMenuItem

/**
 * Marker interface for actions that should appear in the project view popup menu.
 *
 * Implementing this interface indicates that the action should be included in the context menu when right-clicking
 * items in the project view tool window.
 *
 * This interface is typically used in conjunction with [com.intellij.openapi.actionSystem.AnAction].
 */
interface ProjectViewPopupMenuItem
