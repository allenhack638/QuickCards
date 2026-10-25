package com.quickcards.app.core.ui.components

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayGuardTest {

    private val allowedTokens = mapOf(
        "QcDialog.kt" to setOf("Dialog"),
        "QcOverlays.kt" to setOf(
            "ModalBottomSheet",
            "DropdownMenu",
            "ExposedDropdownMenu",
            "Popup"
        )
    )

    private val overlayCall = Regex(
        "\\b(AlertDialog|ModalBottomSheet|ExposedDropdownMenu|DropdownMenu|Dialog|Popup)\\s*\\("
    )

    @Test
    fun overlayWindowsAreOnlyCreatedByTheLockAwareWrappers() {
        val root = sourceRoot()
        val violations = mutableListOf<String>()

        root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .sortedBy { it.path }
            .forEach { file ->
                val relative = file.relativeTo(root).invariantSeparatorsPath
                val allowed = allowedTokens[file.name].orEmpty()
                val source = stripComments(file.readText())
                source.lineSequence().forEachIndexed { index, line ->
                    overlayCall.findAll(line).forEach { match ->
                        val token = match.groupValues[1]
                        if (token !in allowed) {
                            violations += "$relative:${index + 1}: $token("
                        }
                    }
                }
            }

        assertTrue(
            "Overlay windows must only be created by QcDialog/QcOverlays:\n" +
                violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    private fun stripComments(source: String): String {
        val withoutBlockComments = source.replace(Regex("/\\*[\\s\\S]*?\\*/"), "")
        return withoutBlockComments.lineSequence()
            .joinToString("\n") { line -> line.substringBefore("//") }
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
