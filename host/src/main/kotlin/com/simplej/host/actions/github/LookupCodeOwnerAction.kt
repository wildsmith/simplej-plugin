package com.simplej.host.actions.github

import com.intellij.notification.NotificationAction
import com.intellij.openapi.actionSystem.AnAction
import com.intellij.openapi.actionSystem.AnActionEvent
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.VirtualFile
import com.simplej.core.extensions.currentFile
import com.simplej.core.extensions.currentFiles
import com.simplej.core.extensions.openInIde
import com.simplej.core.extensions.showError
import com.simplej.core.extensions.showNotification
import java.io.File
import java.nio.file.FileSystems
import java.nio.file.PathMatcher
import java.nio.file.Paths

class LookupCodeOwnerAction : TrackedCodeAction() {

    @Suppress("ReturnCount")
    override fun shouldShow(event: AnActionEvent, project: Project): Boolean {
        if (event.currentFiles.size != 1) {
            return false
        }
        val projectFile = ProjectRootManager.getInstance(project).contentRoots.firstOrNull() ?: return false
        return getCodeOwnersFile(projectFile).exists()
    }

    @Suppress("ReturnCount")
    override fun actionPerformed(event: AnActionEvent) {
        val project = event.project ?: return event.showError(
            "No valid project found within the workspace."
        )
        val projectFile = ProjectRootManager.getInstance(project).contentRoots.firstOrNull() ?: return event.showError(
            "No valid project found within the workspace."
        )
        val currentFile = event.currentFile ?: return event.showError(
            "No valid file found within the project workspace."
        )

        val codeOwnersFile = getCodeOwnersFile(projectFile)
        val codeOwnerRule = CodeOwnerIdentifier(getCodeOwnersFile(projectFile))
            .findCodeOwnerRule(currentFile.path.substringAfter(project.name))
            ?: return event.showError("Unable to find code owner rule for file: ${currentFile.path}")

        project.showNotification(
            message = "Code Owners: ${codeOwnerRule.owners.joinToString(", ")}",
            actions = mutableSetOf<AnAction>().apply {
                add(
                    NotificationAction.createSimpleExpiring("CODEOWNERS") {
                        event.openInIde(codeOwnersFile, codeOwnerRule.lineNumber)
                    }
                )
            }
        )
    }

    private fun getCodeOwnersFile(projectFile: VirtualFile) = File("${projectFile.path}/.github/CODEOWNERS")

    /**
     * Identifies code owners for a given file path based on a CODEOWNERS file.
     *
     * @param codeOwnersFile The CODEOWNERS file to parse. Defaults to ".github/CODEOWNERS".
     */
    private class CodeOwnerIdentifier(private val codeOwnersFile: File) {

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
    }

    /**
     * Represents a single rule from a CODEOWNERS file.
     *
     * @param pattern The file path pattern.
     * @param owners The list of owners for that pattern.
     */
    private data class CodeOwnerRule(
        val pattern: String,
        val owners: List<String>,
        val lineNumber: Int
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
        fun matches(filePath: String): Boolean =
            if (pattern.endsWith('/')) {
                filePath.startsWith(pattern.removeSuffix("/"))
            } else {
                pathMatcher.matches(Paths.get(filePath.removePrefix("/")))
            }

        companion object {
            /**
             * Parses a single line from a CODEOWNERS file into a CodeownerRule.
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
