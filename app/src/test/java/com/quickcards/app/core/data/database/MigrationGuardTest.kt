package com.quickcards.app.core.data.database

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Source-scan guard for the Room rules in AGENTS.md and PLAN.md:
 *
 * - `exportSchema = true` and a committed schema JSON for every version;
 * - no destructive fallback anywhere in the app;
 * - a non-destructive migration exists for every consecutive version step;
 * - every declared migration is registered with the database builder.
 */
class MigrationGuardTest {

    private val versionPattern = Regex("\\bversion\\s*=\\s*(\\d+)")
    private val exportSchemaPattern = Regex("\\bexportSchema\\s*=\\s*(true|false)")
    private val migrationDeclaration = Regex(
        "MIGRATION_(\\d+)_(\\d+)\\s*=\\s*object\\s*:\\s*Migration\\(\\s*(\\d+)\\s*,\\s*(\\d+)\\s*\\)"
    )
    private val migrationReference = Regex("MIGRATION_(\\d+)_(\\d+)")

    @Test
    fun schemaIsExportedForEveryVersion() {
        val database = databaseSource()
        val version = databaseVersion(database)
        assertTrue("exportSchema must stay true", exportsSchema(database))

        val schemaDir = schemaDirectory()
        val schemas = schemaVersions(schemaDir)
        assertTrue("No schema JSON found in ${schemaDir.path}", schemas.isNotEmpty())

        val oldest = schemas.min()
        for (step in oldest..version) {
            assertTrue(
                "Missing schema JSON for version $step in ${schemaDir.path}",
                step in schemas
            )
        }
        assertEquals(
            "The highest schema JSON must match the database version",
            version,
            schemas.max()
        )

        schemas.sorted().forEach { schemaVersion ->
            val file = File(schemaDir, "$schemaVersion.json")
            val declared = Regex("\"version\"\\s*:\\s*(\\d+)")
                .find(file.readText())
                ?.groupValues?.get(1)?.toIntOrNull()
            assertEquals(
                "Schema $schemaVersion.json declares version $declared",
                schemaVersion,
                declared
            )
        }
    }

    @Test
    fun everyVersionStepHasANonDestructiveMigration() {
        val source = databaseSource()
        val version = databaseVersion(source)
        val oldest = schemaVersions(schemaDirectory()).min()
        val declarations = migrationDeclaration.findAll(source).map { match ->
            val (from, to, declaredFrom, declaredTo) = match.destructured
            assertEquals("MIGRATION_${from}_$to declares $declaredFrom->$declaredTo", from, declaredFrom)
            assertEquals("MIGRATION_${from}_$to declares $declaredFrom->$declaredTo", to, declaredTo)
            from.toInt() to to.toInt()
        }.toSet()

        for (step in oldest until version) {
            assertTrue(
                "No migration for $step -> ${step + 1}",
                (step to step + 1) in declarations
            )
        }

        val registered = migrationReference.findAll(
            source.substringAfter("addMigrations(", "")
        ).map { it.groupValues[1].toInt() to it.groupValues[2].toInt() }.toSet()

        declarations.forEach { step ->
            assertTrue("Migration ${step.first}->${step.second} is not registered", step in registered)
        }
    }

    @Test
    fun noDestructiveFallbackExists() {
        val violations = mutableListOf<String>()
        appRoot().walkTopDown()
            .filter { it.isFile && (it.extension == "kt" || it.extension == "java") }
            .sortedBy { it.path }
            .forEach { file ->
                val relative = file.relativeTo(appRoot()).invariantSeparatorsPath
                file.readLines().forEachIndexed { index, line ->
                    if (line.contains("fallbackToDestructiveMigration")) {
                        violations += "$relative:${index + 1}"
                    }
                }
            }

        assertTrue(
            "Room must never fall back to a destructive migration:\n" +
                violations.joinToString("\n"),
            violations.isEmpty()
        )
    }

    private fun databaseSource(): String = File(
        appRoot(),
        "com/quickcards/app/core/data/database/QuickCardsDatabase.kt"
    ).readText()

    private fun databaseVersion(source: String): Int =
        versionPattern.find(source.substringBefore("abstract class"))?.groupValues?.get(1)?.toInt()
            ?: throw AssertionError("Could not read the database version")

    private fun exportsSchema(source: String): Boolean =
        exportSchemaPattern.find(source)?.groupValues?.get(1) == "true"

    private fun schemaDirectory(): File {
        val appDir = appRoot().parentFile!!.parentFile!!.parentFile!!
        val schemasRoot = File(appDir, "schemas")
        val directory = schemasRoot.walkTopDown()
            .maxDepth(2)
            .firstOrNull { it.isDirectory && it.name.endsWith("QuickCardsDatabase") }
        if (directory == null) {
            throw AssertionError("Could not find the exported schema directory under ${schemasRoot.path}")
        }
        return directory
    }

    private fun schemaVersions(directory: File): List<Int> = directory
        .listFiles { file -> file.isFile && file.extension == "json" }
        .orEmpty()
        .mapNotNull { it.nameWithoutExtension.toIntOrNull() }

    private fun appRoot(): File {
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
