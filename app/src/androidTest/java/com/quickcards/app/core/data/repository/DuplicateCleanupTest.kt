package com.quickcards.app.core.data.repository

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quickcards.app.QuickCardsApplication
import com.quickcards.app.core.data.backup.BackupImporter
import com.quickcards.app.core.data.model.Card
import com.quickcards.app.core.data.model.CardTag
import com.quickcards.app.core.data.model.SeedData
import com.quickcards.app.core.security.EncryptionHelper
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Two stored copies of one number (possible before 2.2) become one on upgrade, keeping tags and note. */
@RunWith(AndroidJUnit4::class)
class DuplicateCleanupTest {

    @Test
    fun upgradeKeepsOneCopyAndMergesItsDetails() = runBlocking<Unit> {
        val app = ApplicationProvider.getApplicationContext<QuickCardsApplication>()
        BackupImporter.eraseAll(app.database)
        app.database.cardTypeDao().insertCardTypes(SeedData.cardTypes)
        val encryption = EncryptionHelper.getInstance()
        val travel = app.repository.createTag("Travel")
        val fuel = app.repository.createTag("Fuel")
        fun legacy(id: String, number: String, note: String, updatedAt: Long) = Card(
            id = id,
            cardNumber = encryption.encrypt(number),
            expiryDate = encryption.encrypt("12/29"),
            cvv = encryption.encrypt("381"),
            bankName = "",
            cardType = "",
            description = encryption.encryptMetadata(note),
            createdAt = 1,
            updatedAt = updatedAt
        )
        val cards = app.database.cardDao()
        cards.insertCard(legacy("old", "4532 7781 0349 2211", "Lounge access", updatedAt = 10))
        cards.insertCard(legacy("new", "4532778103492211", "", updatedAt = 20))
        cards.insertCard(legacy("other", "5555555555554444", "", updatedAt = 5))
        app.database.cardTagDao().insertLinks(listOf(CardTag("old", travel.id), CardTag("new", fuel.id)))

        DataMaintenance.run(app.database, app.repairSummaryStore)

        val left = cards.getAllCardsSync().map { it.id }.sorted()
        assertEquals(listOf("new", "other"), left)
        val kept = app.repository.cards.first().single { it.id == "new" }
        assertEquals("Lounge access", kept.note)
        assertEquals(setOf(travel.id, fuel.id), kept.tagIds.toSet())
    }
}
