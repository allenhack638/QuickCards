package com.quickcards.app.core.data.backup

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quickcards.app.core.data.database.QuickCardsDatabase
import com.quickcards.app.core.data.model.Bank
import com.quickcards.app.core.data.model.SeedData
import com.quickcards.app.core.data.repository.VaultRepository
import com.quickcards.domain.BackupBank
import com.quickcards.domain.BackupCard
import com.quickcards.domain.BackupContent
import com.quickcards.domain.MergeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** How a restore colours new banks and the cards that follow them. */
@RunWith(AndroidJUnit4::class)
class BackupColourImportTest {

    private lateinit var database: QuickCardsDatabase
    private lateinit var repository: VaultRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, QuickCardsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = VaultRepository(database)
        runBlocking { database.networkDao().insertNetworks(SeedData.networks) }
    }

    @After
    fun tearDown() {
        database.close()
    }

    private fun import(vararg banks: BackupBank, cards: List<BackupCard>) = runBlocking {
        BackupImporter.import(database = database, content = BackupContent(banks = banks.toList(), cards = cards), mode = MergeMode.UPDATE)
    }

    private fun bank(name: String) = runBlocking { repository.banks.first().single { it.name == name } }

    private fun cardOf(number: String) = runBlocking { repository.cards.first().single { it.number == number } }

    @Test
    fun aNewBankTakesTheBackupColour() {
        import(BackupBank(id = "b1", name = "Corner Bank", color = "teal"), cards = emptyList())
        assertEquals("teal", bank("Corner Bank").color)
    }

    @Test
    fun aNewUnknownBankWithoutAColourGetsTheAutoPick() {
        runBlocking { database.bankDao().insertBank(Bank(name = "Mine", color = "navy")) }
        import(BackupBank(id = "b1", name = "Corner Bank"), cards = emptyList())
        assertEquals("ocean", bank("Corner Bank").color)
    }

    @Test
    fun aNewCatalogBankWithoutAColourKeepsItsBuiltInDefault() {
        import(BackupBank(id = "b1", name = "HDFC Bank"), cards = emptyList())
        assertNull(bank("HDFC Bank").color)
    }

    @Test
    fun aCardThatFollowsItsBankTakesTheBankColour() {
        import(
            BackupBank(id = "b1", name = "Corner Bank", color = "teal"),
            cards = listOf(BackupCard(number = "4111111111111111", exp = "12/29", bankId = "b1", color = "plum", colorManual = false))
        )
        val card = cardOf("4111111111111111")
        assertEquals("teal", card.bandColor)
        assertFalse(card.bandColorManual)
    }

    @Test
    fun aManualCardKeepsItsOwnColour() {
        import(
            BackupBank(id = "b1", name = "Corner Bank", color = "teal"),
            cards = listOf(BackupCard(number = "4111111111111111", exp = "12/29", bankId = "b1", color = "plum", colorManual = true))
        )
        val card = cardOf("4111111111111111")
        assertEquals("plum", card.bandColor)
        assertTrue(card.bandColorManual)
    }

    @Test
    fun aCardFollowsTheColourOfABankAlreadyOnThePhone() {
        val existing = runBlocking { repository.createBank("Corner Bank", "rust") }
        import(
            BackupBank(id = "b1", name = "Corner Bank", color = "teal"),
            cards = listOf(BackupCard(number = "4111111111111111", exp = "12/29", bankId = "b1", color = "plum", colorManual = false))
        )
        assertEquals("rust", bank("Corner Bank").color)
        assertEquals(existing.id, cardOf("4111111111111111").bankId)
        assertEquals("rust", cardOf("4111111111111111").bandColor)
    }
}
