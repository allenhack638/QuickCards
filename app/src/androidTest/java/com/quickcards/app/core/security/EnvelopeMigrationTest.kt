package com.quickcards.app.core.security

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quickcards.app.QuickCardsApplication
import com.quickcards.app.core.data.backup.BackupImporter
import com.quickcards.app.core.data.model.SeedData
import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.app.core.data.repository.DataMaintenance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Rows written before envelope encryption must survive the upgrade and read back unchanged. */
@RunWith(AndroidJUnit4::class)
class EnvelopeMigrationTest {

    private lateinit var app: QuickCardsApplication

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        runBlocking {
            BackupImporter.eraseAll(app.database)
            app.database.cardTypeDao().insertCardTypes(SeedData.cardTypes)
        }
    }

    @Test
    fun legacyRowsAreRewrappedAndStillDecrypt() = runBlocking<Unit> {
        val encryption = EncryptionHelper.getInstance()
        val holder = app.repository.createHolder("Aarav Mehta")
        val card = app.repository.insertCard(
            VaultCard(number = "4532778103492211", expiry = "12/29", cvv = "381", holderId = holder.id, note = "Lounge")
        ).getOrThrow()

        val cardDao = app.database.cardDao()
        val holderDao = app.database.holderDao()
        val entity = cardDao.getCardById(card.id)!!
        cardDao.updateCard(
            entity.copy(
                cardNumber = encryption.encryptLegacy("4532778103492211"),
                expiryDate = encryption.encryptLegacy("12/29"),
                cvv = encryption.encryptLegacy("381"),
                description = "md1:" + encryption.encryptLegacy("Lounge")
            )
        )
        holderDao.updateHolder(holder.copy(name = "md1:" + encryption.encryptLegacy("Aarav Mehta")))
        assertFalse(EncryptionHelper.isEnvelope(cardDao.getCardById(card.id)!!.cardNumber))

        DataMaintenance.run(app.database, app.repairSummaryStore)

        val migrated = cardDao.getCardById(card.id)!!
        assertTrue(EncryptionHelper.isEnvelope(migrated.cardNumber))
        assertTrue(EncryptionHelper.isEnvelope(migrated.expiryDate))
        assertTrue(EncryptionHelper.isEnvelope(migrated.cvv))
        assertTrue(migrated.description.startsWith("md1:v2:"))
        assertTrue(holderDao.getAllHoldersSync().single().name.startsWith("md1:v2:"))

        val read = app.repository.cards.first().single()
        assertEquals("4532778103492211", read.number)
        assertEquals("12/29", read.expiry)
        assertEquals("381", read.cvv)
        assertEquals("Lounge", read.note)
        assertEquals("Aarav Mehta", app.repository.holders.first().single().name)
    }

    @Test
    fun legacyValuesDecryptWithoutMigration() {
        val encryption = EncryptionHelper.getInstance()
        assertEquals("secret", encryption.decrypt(encryption.encryptLegacy("secret")))
        assertTrue(EncryptionHelper.isEnvelope(encryption.encrypt("secret")))
        assertEquals("secret", encryption.decrypt(encryption.encrypt("secret")))
    }
}
