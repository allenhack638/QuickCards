package com.quickcards.app.core.ui.components

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * User-visible copy belongs in `res/values/strings_*.xml`. Literals without letters pass
 * automatically; other intentional ones go in the allow-list file.
 */
class HardcodedStringGuardTest {

    private val stringBody = "(?:\\\\.|[^\"\\\\])*"

    private val userVisibleLiteral = listOf(
        Regex("\\bText\\s*\\(\\s*(?:text\\s*=\\s*)?\"($stringBody)\""),
        Regex("\\bcontentDescription\\s*=\\s*\"($stringBody)\""),
        Regex("\\b(?:onClickLabel|onLongClickLabel)\\s*=\\s*\"($stringBody)\""),
        Regex("\\bshowSnackbar\\s*\\(\\s*(?:message\\s*=\\s*)?\"($stringBody)\""),
        Regex("\\bsnackbar\\s*\\.\\s*show\\s*\\(\\s*(?:message\\s*=\\s*)?\"($stringBody)\""),
        Regex("\\b(?:helper|label|title|subtitle|message|confirmLabel|cancelLabel)\\s*=\\s*\"($stringBody)\"")
    )

    @Test
    fun composablesUseStringResourcesForUserVisibleCopy() {
        val root = sourceRoot()
        val allowList = allowList()
        val violations = mutableListOf<String>()
        var scanned = 0

        root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .filter { file ->
                val relative = file.relativeTo(root).invariantSeparatorsPath
                relative.startsWith("com/quickcards/app/feature/") ||
                    relative.startsWith("com/quickcards/app/core/ui/")
            }
            .sortedBy { it.path }
            .forEach { file ->
                scanned++
                val relative = file.relativeTo(root).invariantSeparatorsPath
                val source = stripComments(file.readText())
                source.lineSequence().forEachIndexed { index, line ->
                    userVisibleLiteral.forEach { pattern ->
                        pattern.findAll(line).forEach { match ->
                            val literal = match.groupValues[1]
                            if (isAllowed(literal, allowList)) return@forEach
                            violations += "$relative:${index + 1}: \"$literal\""
                        }
                    }
                }
            }

        assertTrue("The guard found no composable sources", scanned > 0)
        assertTrue(
            "User-visible strings must come from string resources:\n" +
                violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    private fun isAllowed(literal: String, allowList: Set<String>): Boolean {
        if (literal.isBlank()) return true
        if (allowList.contains(literal)) return true
        return literal.none { it.isLetter() }
    }

    private fun allowList(): Set<String> {
        val stream = javaClass.classLoader
            ?.getResourceAsStream("hardcoded-strings-allowlist.txt")
            ?: return emptySet()
        return stream.bufferedReader().useLines { lines ->
            lines.map { it.trim() }
                .filter { it.isNotEmpty() && !it.startsWith("#") }
                .toSet()
        }
    }

    private fun stripComments(source: String): String {
        val builder = StringBuilder(source.length)
        var index = 0
        var inBlock = false
        var inLine = false
        var inString = false
        while (index < source.length) {
            val char = source[index]
            val next = source.getOrNull(index + 1)
            when {
                inBlock -> {
                    if (char == '*' && next == '/') {
                        builder.append("  ")
                        index += 2
                        inBlock = false
                        continue
                    }
                    builder.append(if (char == '\n') '\n' else ' ')
                }

                inLine -> {
                    if (char == '\n') {
                        builder.append('\n')
                        inLine = false
                    } else {
                        builder.append(' ')
                    }
                }

                inString -> {
                    builder.append(char)
                    if (char == '\\' && next != null) {
                        builder.append(next)
                        index += 2
                        continue
                    }
                    if (char == '"') inString = false
                }

                char == '/' && next == '/' -> {
                    builder.append("  ")
                    index += 2
                    inLine = true
                    continue
                }

                char == '/' && next == '*' -> {
                    builder.append("  ")
                    index += 2
                    inBlock = true
                    continue
                }

                char == '"' -> {
                    builder.append(char)
                    inString = true
                }

                else -> builder.append(char)
            }
            index++
        }
        return builder.toString()
    }

    private fun sourceRoot(): File {
        var directory: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
        while (directory != null) {
            val nested = File(directory, "app/src/main/java")
            if (nested.isDirectory) return nested
            val direct = File(directory, "src/main/java")
            if (direct.isDirectory) return direct
            directory = directory.parentFile
        }
        throw AssertionError("Could not find app/src/main/java from ${System.getProperty("user.dir")}")
    }
}
