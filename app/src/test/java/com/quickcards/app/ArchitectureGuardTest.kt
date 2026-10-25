package com.quickcards.app

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Source-scan guard for the package structure:
 *
 * - Feature packages may use core packages and `:domain`, but never another
 *   feature. `app/navigation` is the only place that wires features together.
 * - Core packages never depend on feature packages.
 * - Composables never reach past the repository/view model into Room DAOs or
 *   `@Entity` classes.
 * - The old v1 packages stay gone.
 * - `:domain` stays a pure Kotlin module (no Android, no `:app` imports).
 */
class ArchitectureGuardTest {

    private val packagePattern = Regex("""(?m)^\s*package\s+([\w.]+)""")
    private val importPattern = Regex("""(?m)^\s*import\s+([\w.]+)""")
    private val classPattern = Regex("""\b(?:data\s+)?class\s+(\w+)""")

    private val appPrefix = "com.quickcards.app"
    private val featurePrefix = "$appPrefix.feature."
    private val corePrefix = "$appPrefix.core."
    private val navigationPrefix = "$appPrefix.navigation"

    private val retiredPackages = listOf(
        "$appPrefix.data.",
        "$appPrefix.security.",
        "$appPrefix.notifications.",
        "$appPrefix.utils.",
        "$appPrefix.viewmodel.",
        "$appPrefix.ui.",
        "$appPrefix.domain."
    )

    @Test
    fun featuresOnlyDependOnCoreDomainAndThemselves() {
        val violations = mutableListOf<String>()

        appSources().forEach { source ->
            val ownFeature = featureOf(source.packageName) ?: return@forEach
            source.imports.forEach { import ->
                val importedFeature = featureOf(import) ?: return@forEach
                if (importedFeature != ownFeature) {
                    violations += "${source.relative}: imports feature '$importedFeature' from '$ownFeature'"
                }
            }
        }

        assertTrue(
            "feature/* must not import another feature/* (wire it in app/navigation):\n" +
                violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    @Test
    fun coreNeverDependsOnFeaturesOrNavigation() {
        val violations = mutableListOf<String>()

        appSources().forEach { source ->
            if (!source.packageName.startsWith(corePrefix)) return@forEach
            source.imports.forEach { import ->
                if (import.startsWith(featurePrefix) || import.startsWith("$navigationPrefix.")) {
                    violations += "${source.relative}: imports $import"
                }
            }
        }

        assertTrue(
            "core/* must not import feature/* or app/navigation:\n" +
                violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    @Test
    fun uiCodeDoesNotImportDaosOrRoomEntities() {
        val entityImports = roomEntityNames().map { "$appPrefix.core.data.model.$it" }.toSet()
        val violations = mutableListOf<String>()

        appSources().forEach { source ->
            val isUiPackage = source.packageName.startsWith(featurePrefix) ||
                source.packageName.startsWith("$appPrefix.core.ui.")
            if (!isUiPackage) return@forEach
            val isComposable = source.text.contains("@Composable")
            source.imports.forEach { import ->
                val reachesDao = import.startsWith("$appPrefix.core.data.dao.")
                val reachesEntity = isComposable &&
                    (import in entityImports || import.startsWith("androidx.room."))
                if (reachesDao || reachesEntity) {
                    violations += "${source.relative}: imports $import"
                }
            }
        }

        assertTrue(
            "Feature/UI code must use the repository/view model, never DAOs or Room entities:\n" +
                violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    @Test
    fun prePhase12PackagesAreGone() {
        val violations = mutableListOf<String>()

        appSources().forEach { source ->
            val wrongPackage = retiredPackages.firstOrNull { source.packageName.startsWith(it) }
            if (wrongPackage != null) {
                violations += "${source.relative}: still declares package ${source.packageName}"
            }
            source.imports.forEach { import ->
                if (retiredPackages.any { import.startsWith(it) }) {
                    violations += "${source.relative}: imports retired package $import"
                }
            }
        }

        assertTrue(
            "Every source must use the core/feature/navigation packages:\n" + violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    @Test
    fun domainModuleStaysPureKotlin() {
        val violations = mutableListOf<String>()
        val root = domainRoot()

        root.walkTopDown()
            .filter { it.isFile && it.extension == "kt" }
            .sortedBy { it.path }
            .forEach { file ->
                val relative = file.relativeTo(root).invariantSeparatorsPath
                val source = stripComments(file.readText())
                importPattern.findAll(source).forEach { match ->
                    val import = match.groupValues[1]
                    if (import.startsWith("android.") ||
                        import.startsWith("androidx.") ||
                        import.startsWith(appPrefix) ||
                        import.startsWith("java.awt.")
                    ) {
                        violations += "$relative: imports $import"
                    }
                }
            }

        assertTrue(
            ":domain must stay a pure Kotlin module:\n" + violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    @Test
    fun guardSeesTheSourceTrees() {
        val appCount = appSources().size
        val domainCount = domainRoot().walkTopDown().count { it.isFile && it.extension == "kt" }
        val entities = roomEntityNames()

        assertTrue("The guard found no app sources", appCount > 50)
        assertTrue("The guard found no domain sources", domainCount >= 12)
        assertTrue(
            "Room entities were not discovered: $entities",
            entities.containsAll(listOf("Card", "Bank", "Holder", "CardNetwork", "CardType", "Tag", "CardTag"))
        )
    }

    private fun appSources(): List<SourceFile> {
        val root = appRoot()
        return root.walkTopDown()
            .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
            .sortedBy { it.path }
            .map { file ->
                val text = stripComments(file.readText())
                SourceFile(
                    relative = file.relativeTo(root).invariantSeparatorsPath,
                    packageName = packagePattern.find(text)?.groupValues?.get(1).orEmpty(),
                    imports = importPattern.findAll(text).map { it.groupValues[1] }.toList(),
                    text = text
                )
            }
            .toList()
    }

    private fun roomEntityNames(): Set<String> {
        val modelDir = File(appRoot(), "com/quickcards/app/core/data/model")
        if (!modelDir.isDirectory) return emptySet()
        return modelDir.listFiles { file -> file.isFile && file.extension == "kt" }
            .orEmpty()
            .asSequence()
            .filter { it.readText().contains("@Entity") }
            .mapNotNull { file -> classPattern.find(stripComments(file.readText()))?.groupValues?.get(1) }
            .toSet()
    }

    private fun featureOf(packageOrImport: String): String? {
        if (!packageOrImport.startsWith(featurePrefix)) return null
        return packageOrImport.removePrefix(featurePrefix).substringBefore('.')
    }

    private fun stripComments(source: String): String {
        val withoutBlockComments = source.replace(Regex("/\\*[\\s\\S]*?\\*/"), "")
        return withoutBlockComments.lineSequence()
            .joinToString("\n") { line -> line.substringBefore("//") }
    }

    private fun appRoot(): File = File(repositoryRoot(), "app/src/main/java")

    private fun domainRoot(): File = File(repositoryRoot(), "domain/src/main/kotlin")

    private fun repositoryRoot(): File {
        var directory: File? = File(System.getProperty("user.dir") ?: ".").absoluteFile
        while (directory != null) {
            if (File(directory, "settings.gradle").isFile) return directory
            directory = directory.parentFile
        }
        throw AssertionError("Could not find settings.gradle from ${System.getProperty("user.dir")}")
    }

    private data class SourceFile(
        val relative: String,
        val packageName: String,
        val imports: List<String>,
        val text: String
    )
}
