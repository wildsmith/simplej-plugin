// Use of this source code is governed by the Apache 2.0 license.
package com.simplej.plugin.actions.github

import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.ide.CopyPasteManager
import com.intellij.openapi.project.Project
import com.simplej.base.extensions.currentFile
import com.simplej.base.extensions.currentFiles
import com.simplej.base.extensions.getRootProjectFile
import com.simplej.base.extensions.getCodeOwnersFile
import com.simplej.base.extensions.openInIde
import com.simplej.base.extensions.showError
import com.simplej.base.extensions.showNotification
import java.awt.datatransfer.StringSelection
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.PathMatcher
import java.nio.file.Paths

/**
 * An action that looks up code owners for the currently selected file based on the GitHub CODEOWNERS file.
 *
 * This action:
 * - Only shows when exactly one file is selected and a CODEOWNERS file exists in the .github directory
 * - Reads and parses the CODEOWNERS file to identify the code owners for the selected file
 * - Displays a notification with the code owners and provides a link to open the CODEOWNERS file
 *
 * The action follows GitHub's CODEOWNERS file format and pattern matching rules, supporting:
 * - Basic path patterns
 * - Directory wildcards (*)
 * - Recursive wildcards (**)
 * - Directory-specific patterns (ending with /)
 */
internal class LookupCodeOwnerAction : GithubTrackedCodeAction() {

    @Suppress("ReturnCount")
    override fun shouldShow(event: AnActionEvent, project: Project): Boolean {
        if (event.currentFiles.size != 1) {
            return false
        }
        val rootProjectFile = event.currentFile?.getRootProjectFile(project) ?: return false
        return rootProjectFile.getCodeOwnersFile().exists()
    }

    @Suppress("ReturnCount")
    override fun actionPerformed(event: AnActionEvent) {
        val currentFile = event.currentFile ?: return event.showError(
            "No valid file found within the project workspace."
        )
        val project = event.project ?: return event.showError(
            "No valid project found within the workspace."
        )
        val rootProjectFile = currentFile.getRootProjectFile(project) ?: return event.showError(
            "No valid project found within the workspace."
        )

        val codeOwnersFile = rootProjectFile.getCodeOwnersFile()
        val codeOwnerRule = CodeOwnerIdentifier(codeOwnersFile)
            .findCodeOwnerRule(currentFile.path.substringAfter(project.name))
            ?: return event.showError("Unable to find code owner rule for file: ${currentFile.path}")

        val codeOwners = codeOwnerRule.owners.joinToString(", ")
        project.showNotification(
            message = "Code Owners: $codeOwners",
            actions = mutableSetOf<AnAction>().apply {
                add(
                    NotificationAction.createSimpleExpiring("CODEOWNERS") {
                        event.openInIde(codeOwnersFile, codeOwnerRule.humanReadableLineNumber)
                    }
                )
                add(
                    NotificationAction.createSimpleExpiring("Copy") {
                        val copyPasteManager = CopyPasteManager.getInstance()
                        copyPasteManager.setContents(StringSelection(codeOwners))
                    }
                )
            }
        )
    }

    /**
     * Identifies code owners for a given file path based on a CODEOWNERS file.
     *
     * @param codeOwnersFile The CODEOWNERS file to parse. Defaults to ".github/CODEOWNERS".
     */
    internal class CodeOwnerIdentifier(private val codeOwnersFile: File) {

        private var rules: List<CodeOwnerRule>

        init {
            // Read lines, filter out comments and blank lines, and parse them into rules.
            // Reversing the list makes it easier to find the *last* match by just finding the *first* one.
            var lineNumber = 1
            rules = codeOwnersFile.readLines()
                .map { it.trim() }
                .mapNotNull {
                    val trimmedLine = it.trim()
                    val result = if (trimmedLine.isNotEmpty() && !trimmedLine.startsWith("#")) {
                        CodeOwnerRule.fromString(trimmedLine, lineNumber)
                    } else {
                        null
                    }
                    lineNumber++
                    result
                }
                .reversed()
        }

        /**
         * Finds the owners for a specific file path.
         *
         * @param filePath The path to the file to check (e.g., "src/main/kotlin/com/mycompany/core/User.kt").
         * @return A list of owner strings (e.g., ["@security-team", "@core-leads"]), or null if no owner is found.
         */
        fun findCodeOwnerRule(filePath: String): CodeOwnerRule? {
            // Find the first rule that matches, which corresponds to the last matching rule in the original file.
            return rules.firstOrNull { it.matches(filePath) }
        }

        /**
         * Finds all matching code owner rules for a specific file path.
         *
         * @param filePath The path to the file to check (e.g., "src/main/kotlin/com/mycompany/core/User.kt").
         * @return A list of owner strings (e.g., ["@security-team", "@core-leads"]), or null if no owner is found.
         */
        fun findAllCodeOwnerRules(filePath: String): Set<CodeOwnerRule> {
            // Collect all of the matching rules
            return rules.filterTo(mutableSetOf()) { it.matches(filePath) }
        }
    }

    /**
     * Represents a single rule from a CODEOWNERS file.
     *
     * @param pattern The file path pattern.
     * @param owners The list of owners for that pattern.
     */
    internal data class CodeOwnerRule(
        val pattern: String,
        val owners: List<String>,
        /**
         * This is the human-readable line number, subtract 1 from it to interact with
         * the actual line number in code.
         */
        val humanReadableLineNumber: Int
    ) {
        private val pathMatcher: PathMatcher

        init {
            // Convert CODEOWNERS pattern to glob pattern
            var globPattern = pattern
                // Remove the leading slash
                .trimStart('/')
                // Escape dots
                .replace(".", "\\.")
                // Temporarily replace **
                .replace("**", "{DOUBLE_STAR}")
                // ** matches anything including
                .replace("{DOUBLE_STAR}", ".*")
            if (globPattern == "*") {
                // Special case for fallback ownership
                globPattern = "**"
            }
            if (globPattern.endsWith('/')) {
                // CodeOwners claims that end with `/` indicate wildcard ownership
                // over all directories underneath the last path segment
                globPattern = "$globPattern**"
            }
            pathMatcher = FileSystems.getDefault().getPathMatcher("glob:$globPattern")
        }

        /**
         * Checks if a given file path matches this rule's pattern.
         */
        fun matches(filePath: String): Boolean {
            return if (pattern.endsWith('/')) {
                filePath.startsWith(pattern.removeSuffix("/"))
            } else {
                if (pattern.startsWith("*.")) {
                    // File type ownership claim
                    val extension = filePath.substringAfterLast('.')
                    extension == pattern.removePrefix("*.")
                } else {
                    pathMatcher.matches(Paths.get(filePath.removePrefix("/")))
                }
            }
        }

        companion object {

            /**
             * Parses a single line from a CODEOWNERS file into a [CodeOwnerRule].
             */
            fun fromString(line: String, lineNumber: Int): CodeOwnerRule? {
                val parts = line.split("\\s+".toRegex())
                // A rule must have a pattern and at least one owner
                if (parts.size < 2) {
                    return null
                }
                val pattern = parts[0]
                val owners = parts.drop(1)
                return CodeOwnerRule(pattern, owners, lineNumber)
            }
        }
    }
}
