package com.quickcards.app.core.data.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        QuickCardsDatabase::class.java
    )

    @Test
    fun migrate6To7_preservesCardsBanksAndTags() {
        helper.createDatabase(TEST_DB, 6).apply {
            execSQL(
                """
                INSERT INTO cards
                (id, cardNumber, expiryDate, cvv, bankName, cardType, cardIssuer, cardVariant,
                 owner, tags, description, cardColor, createdAt, updatedAt)
                VALUES
                ('card-1', 'encrypted-number', 'encrypted-expiry', 'encrypted-cvv', 'Test Bank',
                 'Credit', 'Visa', 'Standard', 'Test Owner', '[]', 'note', '#2196F3', 1, 1)
                """.trimIndent()
            )
            execSQL("INSERT INTO banks (id, name, createdAt) VALUES ('bank-1', 'Test Bank', 1)")
            execSQL("INSERT INTO tags (id, name, color, createdAt) VALUES ('tag-1', 'Travel', '#2196F3', 1)")
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB,
            7,
            true,
            QuickCardsDatabase.MIGRATION_6_7
        )

        db.query("SELECT COUNT(*) FROM cards").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM banks").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM tags").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
        db.close()
    }

    @Test
    fun migrate7To8_addsNullablePanHashAndPreservesRows() {
        helper.createDatabase(TEST_DB_7_8, 7).apply {
            execSQL(
                """
                INSERT INTO cards
                (id, cardNumber, expiryDate, cvv, bankName, cardType, cardIssuer, cardVariant,
                 owner, tags, description, cardColor, createdAt, updatedAt)
                VALUES
                ('card-2', 'encrypted-number', 'encrypted-expiry', 'encrypted-cvv', 'Test Bank',
                 'Credit', 'Visa', 'Standard', 'Test Owner', '[]', 'note', '#2196F3', 1, 1)
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB_7_8,
            8,
            true,
            QuickCardsDatabase.MIGRATION_7_8
        )

        db.query("SELECT panHash FROM cards WHERE id = 'card-2'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.count)
            assertEquals(true, cursor.isNull(0))
        }
        db.close()
    }

    @Test
    fun migrate8To9_removesTagsAndPreservesCards() {
        helper.createDatabase(TEST_DB_8_9, 8).apply {
            execSQL(
                """
                INSERT INTO cards
                (id, cardNumber, expiryDate, cvv, bankName, cardType, cardIssuer, cardVariant,
                 owner, tags, description, cardColor, createdAt, updatedAt, panHash)
                VALUES
                ('card-3', 'encrypted-number', 'encrypted-expiry', 'encrypted-cvv', 'Test Bank',
                 'Credit', 'Visa', 'Standard', 'Test Owner', '["Travel"]', 'note', '#2196F3', 1, 1, 'hash-3')
                """.trimIndent()
            )
            execSQL("INSERT INTO banks (id, name, createdAt) VALUES ('bank-3', 'Test Bank', 1)")
            execSQL("INSERT INTO tags (id, name, color, createdAt) VALUES ('tag-3', 'Travel', '#2196F3', 1)")
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB_8_9,
            9,
            true,
            QuickCardsDatabase.MIGRATION_8_9
        )

        db.query("SELECT cardNumber, bankName, panHash FROM cards WHERE id = 'card-3'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.count)
            assertEquals("encrypted-number", cursor.getString(0))
            assertEquals("Test Bank", cursor.getString(1))
            assertEquals("hash-3", cursor.getString(2))
        }
        db.query("SELECT COUNT(*) FROM banks").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'tags'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        db.close()
    }

    @Test
    fun migrate9To10_createsVaultTablesSeedsRulesAndDefaultsColumns() {
        helper.createDatabase(TEST_DB_9_10, 9).apply {
            execSQL("INSERT INTO banks (id, name, createdAt) VALUES ('bank-10', 'Test Bank', 1)")
            execSQL(
                """
                INSERT INTO cards
                (id, cardNumber, expiryDate, cvv, bankName, cardType, cardIssuer, cardVariant,
                 owner, description, cardColor, createdAt, updatedAt, panHash)
                VALUES
                ('card-10', 'encrypted-number-10', 'encrypted-expiry-10', 'encrypted-cvv-10',
                 'encrypted-bank-10', 'Credit', 'Visa', 'Regalia',
                 'encrypted-owner-10', 'encrypted-note-10', '#1E3A8A', 10, 10, 'hash-10')
                """.trimIndent()
            )
            execSQL(
                """
                INSERT INTO cards
                (id, cardNumber, expiryDate, cvv, bankName, cardType, cardIssuer, cardVariant,
                 owner, description, cardColor, createdAt, updatedAt, panHash)
                VALUES
                ('card-11', 'encrypted-number-11', 'encrypted-expiry-11', 'encrypted-cvv-11',
                 'encrypted-bank-11', 'Debit', 'RuPay', 'Select',
                 'encrypted-owner-11', 'encrypted-note-11', '#14532D', 20, 20, 'hash-11')
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB_9_10,
            10,
            true,
            QuickCardsDatabase.MIGRATION_9_10
        )

        db.query(
            """
            SELECT cardNumber, expiryDate, cvv, bankName, cardType, cardIssuer, cardVariant,
                   owner, description, cardColor, createdAt, updatedAt, panHash,
                   holderId, bankId, networkId, networkAuto, typeId, nameOnCard, bandColor, sortOrder
            FROM cards WHERE id = 'card-10'
            """.trimIndent()
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.count)
            assertEquals("encrypted-number-10", cursor.getString(0))
            assertEquals("encrypted-expiry-10", cursor.getString(1))
            assertEquals("encrypted-cvv-10", cursor.getString(2))
            assertEquals("encrypted-bank-10", cursor.getString(3))
            assertEquals("Credit", cursor.getString(4))
            assertEquals("Visa", cursor.getString(5))
            assertEquals("Regalia", cursor.getString(6))
            assertEquals("encrypted-owner-10", cursor.getString(7))
            assertEquals("encrypted-note-10", cursor.getString(8))
            assertEquals("#1E3A8A", cursor.getString(9))
            assertEquals(10L, cursor.getLong(10))
            assertEquals(10L, cursor.getLong(11))
            assertEquals("hash-10", cursor.getString(12))
            assertEquals(true, cursor.isNull(13))
            assertEquals(true, cursor.isNull(14))
            assertEquals(true, cursor.isNull(15))
            assertEquals(1, cursor.getInt(16))
            assertEquals(true, cursor.isNull(17))
            assertEquals("", cursor.getString(18))
            assertEquals("navy", cursor.getString(19))
            assertEquals(0, cursor.getInt(20))
        }

        db.query("SELECT sortOrder FROM cards WHERE id = 'card-11'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }

        db.query("SELECT COUNT(*) FROM holders").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM tags").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM card_tags").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        db.query("SELECT COUNT(*) FROM networks").use { cursor ->
            cursor.moveToFirst()
            assertEquals(10, cursor.getInt(0))
        }
        db.query(
            "SELECT name, code, prefixes, lengths, cvvLength, luhn, groups, sortOrder FROM networks WHERE id = 'amex'"
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals("American Express", cursor.getString(0))
            assertEquals("AMEX", cursor.getString(1))
            assertEquals("34,37", cursor.getString(2))
            assertEquals("15", cursor.getString(3))
            assertEquals(4, cursor.getInt(4))
            assertEquals(1, cursor.getInt(5))
            assertEquals("4-6-5", cursor.getString(6))
            assertEquals(2, cursor.getInt(7))
        }
        db.query("SELECT luhn FROM networks WHERE id = 'unionpay'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(0, cursor.getInt(0))
        }
        db.query("SELECT id, name FROM card_types ORDER BY sortOrder").use { cursor ->
            val ids = mutableListOf<String>()
            val names = mutableListOf<String>()
            while (cursor.moveToNext()) {
                ids.add(cursor.getString(0))
                names.add(cursor.getString(1))
            }
            assertEquals(
                listOf(
                    "b0a1c2d3-0001-4e5f-8a9b-000000000001",
                    "b0a1c2d3-0002-4e5f-8a9b-000000000002",
                    "b0a1c2d3-0003-4e5f-8a9b-000000000003",
                    "b0a1c2d3-0004-4e5f-8a9b-000000000004"
                ),
                ids
            )
            assertEquals(listOf("Credit", "Debit", "Prepaid", "Forex"), names)
        }
        db.query("SELECT name FROM banks WHERE id = 'bank-10'").use { cursor ->
            cursor.moveToFirst()
            assertEquals("Test Bank", cursor.getString(0))
        }
        db.close()
    }

    @Test
    fun migrate10To11_addsPinColumnsAndKeepsRowsUnpinned() {
        helper.createDatabase(TEST_DB_10_11, 10).apply {
            execSQL(
                """
                INSERT INTO cards
                (id, cardNumber, expiryDate, cvv, bankName, cardType, cardIssuer, cardVariant,
                 owner, description, cardColor, createdAt, updatedAt, panHash,
                 networkAuto, nameOnCard, bandColor, sortOrder)
                VALUES
                ('card-11a', 'encrypted-number-11a', 'encrypted-expiry-11a', 'encrypted-cvv-11a',
                 'encrypted-bank-11a', 'Credit', 'Visa', 'Regalia',
                 'encrypted-owner-11a', 'encrypted-note-11a', '#1E3A8A', 11, 11, 'hash-11a',
                 1, '', 'navy', 0)
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB_10_11,
            11,
            true,
            QuickCardsDatabase.MIGRATION_10_11
        )

        db.query(
            "SELECT cardNumber, pinned, pinnedAt, sortOrder FROM cards WHERE id = 'card-11a'"
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.count)
            assertEquals("encrypted-number-11a", cursor.getString(0))
            assertEquals(0, cursor.getInt(1))
            assertEquals(true, cursor.isNull(2))
            assertEquals(0, cursor.getInt(3))
        }
        db.close()
    }

    @Test
    fun migrate12To13_addsDueDayAndKeepsCards() {
        helper.createDatabase(TEST_DB_12_13, 12).apply {
            execSQL(
                """
                INSERT INTO cards
                (id, cardNumber, expiryDate, cvv, bankName, cardType, cardIssuer, cardVariant,
                 owner, description, cardColor, createdAt, updatedAt, panHash,
                 holderId, bankId, networkId, networkAuto, typeId, nameOnCard, bandColor,
                 sortOrder, pinned, pinnedAt, archived, archivedAt, archiveReason, needsReview)
                VALUES
                ('card-13a', 'encrypted-number-13a', 'encrypted-expiry-13a',
                 'encrypted-cvv-13a', 'encrypted-bank-13a', 'Credit', 'Visa', 'Regalia',
                 'encrypted-owner-13a', 'encrypted-note-13a', '#1E3A8A', 13, 13, 'hash-13a',
                 'holder-13a', 'bank-13a', 'visa', 1, 'type-credit', '', 'navy',
                 2, 0, NULL, 0, NULL, NULL, 0)
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB_12_13,
            13,
            true,
            QuickCardsDatabase.MIGRATION_12_13
        )

        db.query("SELECT cardNumber, expiryDate, cvv, sortOrder, dueDay FROM cards WHERE id = 'card-13a'").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.count)
            assertEquals("encrypted-number-13a", cursor.getString(0))
            assertEquals("encrypted-expiry-13a", cursor.getString(1))
            assertEquals("encrypted-cvv-13a", cursor.getString(2))
            assertEquals(2, cursor.getInt(3))
            assertTrue(cursor.isNull(4))
        }
    }

    @Test
    fun migrate13To14_addsCvvOptionalOnForBuiltInMaestroOnly() {
        helper.createDatabase(TEST_DB_13_14, 13).apply {
            listOf(
                "('maestro', 'Maestro', 'MAESTRO', '6759', '12-19', 3, 1, '', 7, 1)",
                "('visa', 'Visa', 'VISA', '4', '16', 3, 1, '', 0, 1)",
                "('custom-1', 'Maestro Local', 'MLOC', '6799', '16', 3, 1, '', 10, 0)"
            ).forEach { row ->
                execSQL(
                    "INSERT INTO networks (id, name, code, prefixes, lengths, cvvLength, luhn, " +
                        "`groups`, sortOrder, builtIn) VALUES $row"
                )
            }
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB_13_14,
            14,
            true,
            QuickCardsDatabase.MIGRATION_13_14
        )

        db.query("SELECT id, cvvOptional, cvvLength FROM networks ORDER BY id").use { cursor ->
            val rows = buildMap {
                while (cursor.moveToNext()) put(cursor.getString(0), cursor.getInt(1) to cursor.getInt(2))
            }
            assertEquals(1 to 3, rows["maestro"])
            assertEquals(0 to 3, rows["visa"])
            assertEquals(0 to 3, rows["custom-1"])
        }
        db.close()
    }

    @Test
    fun migrate11To12_addsArchiveAndReviewColumnsAndCreatesRepairs() {
        helper.createDatabase(TEST_DB_11_12, 11).apply {
            execSQL(
                """
                INSERT INTO cards
                (id, cardNumber, expiryDate, cvv, bankName, cardType, cardIssuer, cardVariant,
                 owner, description, cardColor, createdAt, updatedAt, panHash,
                 holderId, bankId, networkId, networkAuto, typeId, nameOnCard, bandColor,
                 sortOrder, pinned, pinnedAt)
                VALUES
                ('card-12a', 'encrypted-number-12a', 'encrypted-expiry-12a',
                 'encrypted-cvv-12a', 'encrypted-bank-12a', 'Credit', 'Visa', 'Regalia',
                 'encrypted-owner-12a', 'encrypted-note-12a', '#1E3A8A', 12, 12, 'hash-12a',
                 'holder-12a', 'bank-12a', 'visa', 1, 'type-credit', '', 'navy',
                 4, 1, 1200)
                """.trimIndent()
            )
            close()
        }

        val db = helper.runMigrationsAndValidate(
            TEST_DB_11_12,
            12,
            true,
            QuickCardsDatabase.MIGRATION_11_12
        )

        db.query(
            """
            SELECT cardNumber, expiryDate, cvv, bankName, owner, description, sortOrder,
                   pinned, pinnedAt, archived, archivedAt, archiveReason, needsReview
            FROM cards WHERE id = 'card-12a'
            """.trimIndent()
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.count)
            assertEquals("encrypted-number-12a", cursor.getString(0))
            assertEquals("encrypted-expiry-12a", cursor.getString(1))
            assertEquals("encrypted-cvv-12a", cursor.getString(2))
            assertEquals("encrypted-bank-12a", cursor.getString(3))
            assertEquals("encrypted-owner-12a", cursor.getString(4))
            assertEquals("encrypted-note-12a", cursor.getString(5))
            assertEquals(4, cursor.getInt(6))
            assertEquals(1, cursor.getInt(7))
            assertEquals(1200L, cursor.getLong(8))
            assertEquals(0, cursor.getInt(9))
            assertEquals(true, cursor.isNull(10))
            assertEquals(true, cursor.isNull(11))
            assertEquals(0, cursor.getInt(12))
        }

        db.query(
            "SELECT COUNT(*) FROM sqlite_master WHERE type = 'table' AND name = 'card_repairs'"
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }
        db.query(
            "SELECT COUNT(*) FROM sqlite_master WHERE type = 'index' " +
                "AND name = 'index_card_repairs_cardId'"
        ).use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.getInt(0))
        }

        db.execSQL(
            """
            INSERT INTO card_repairs (cardId, field, before, after, at)
            VALUES ('card-12a', 'EXPIRY', 'encrypted-before', 'encrypted-after', 12345)
            """.trimIndent()
        )
        db.query("SELECT field, before, after, at FROM card_repairs").use { cursor ->
            cursor.moveToFirst()
            assertEquals(1, cursor.count)
            assertEquals("EXPIRY", cursor.getString(0))
            assertEquals("encrypted-before", cursor.getString(1))
            assertEquals("encrypted-after", cursor.getString(2))
            assertEquals(12345L, cursor.getLong(3))
        }
        db.close()
    }

    private companion object {
        const val TEST_DB = "quickcards-migration-test"
        const val TEST_DB_7_8 = "quickcards-migration-7-8-test"
        const val TEST_DB_8_9 = "quickcards-migration-8-9-test"
        const val TEST_DB_9_10 = "quickcards-migration-9-10-test"
        const val TEST_DB_10_11 = "quickcards-migration-10-11-test"
        const val TEST_DB_11_12 = "quickcards-migration-11-12-test"
        const val TEST_DB_12_13 = "quickcards-migration-12-13-test"
        const val TEST_DB_13_14 = "quickcards-migration-13-14-test"
    }
}
