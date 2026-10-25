package com.quickcards.app.core.ui.theme

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/** Colour literals live only in `core/ui/theme`; other code uses MaterialTheme or PassColors. */
class ColorGuardTest {

    private val colorLiteral = Regex("\\bColor\\s*\\(\\s*0x[0-9A-Fa-f]+")

    @Test
    fun coloursOnlyComeFromTheThemePackage() {
        val root = sourceRoot()
        val violations = mutableListOf<String>()
        var scanned = 0
        var themeLiterals = 0

        root.walkTopDown()
            .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
            .sortedBy { it.path }
            .forEach { file ->
                scanned++
                val relative = file.relativeTo(root).invariantSeparatorsPath
                val source = stripComments(file.readText())
                val matches = colorLiteral.findAll(source).toList()
                if (matches.isEmpty()) return@forEach

                if (relative.startsWith("com/quickcards/app/core/ui/theme/")) {
                    themeLiterals += matches.size
                    return@forEach
                }

                source.lineSequence().forEachIndexed { index, line ->
                    colorLiteral.findAll(line).forEach { match ->
                        violations += "$relative:${index + 1}: ${match.value}"
                    }
                }
            }

        assertTrue("The guard found no app sources", scanned > 0)
        assertTrue("The theme package defines no colour literals", themeLiterals > 0)
        assertTrue(
            "Colour literals must live in core/ui/theme:\n" +
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
