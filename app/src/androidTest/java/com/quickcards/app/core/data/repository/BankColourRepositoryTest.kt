package com.quickcards.app.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quickcards.app.core.data.database.QuickCardsDatabase
import com.quickcards.app.core.data.model.BandColor
import com.quickcards.app.core.data.model.SeedData
import com.quickcards.app.core.data.model.VaultCard
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BankColourRepositoryTest {

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

    private suspend fun card(number: String, bankId: String, colour: String, manual: Boolean) =
        repository.insertCard(
            VaultCard(number = number, expiry = "12/29", cvv = "123", bankId = bankId, bandColor = colour, bandColorManual = manual)
        ).getOrThrow().id

    private suspend fun colourOf(id: String) = repository.cards.first().first { it.id == id }.bandColor

    private suspend fun hexOf(id: String) = database.cardDao().getCardById(id)!!.cardColor

    @Test
    fun aNewCardThatFollowsItsBankTakesTheBankColour() = runBlocking {
        val bank = repository.createBank("Corner Bank", "teal")
        val follower = card("4111111111111111", bank.id, "plum", manual = false)
        assertEquals("teal", colourOf(follower))
    }

    @Test
    fun updatingTheColourRepaintsFollowersAndLeavesManualCards() = runBlocking {
        val bank = repository.createBank("Corner Bank", "teal")
        val follower = card("4111111111111111", bank.id, "teal", manual = false)
        val manual = card("5555555555554444", bank.id, "plum", manual = true)
        val custom = card("4012888888881881", bank.id, "#0F5E5A", manual = true)

        repository.updateBank(bank.id, "Corner Bank", "rust")

        assertEquals("rust", colourOf(follower))
        assertEquals(BandColor.hexFor("rust"), hexOf(follower))
        assertEquals("plum", colourOf(manual))
        assertEquals("#0F5E5A", colourOf(custom))
        assertEquals("#0F5E5A", hexOf(custom))
        assertEquals("rust", repository.banks.first().single().color)
    }

    @Test
    fun renamingOntoABuiltInBankRepaintsFollowersInOneStep() = runBlocking {
        val bank = repository.createBank("Corner Bank", "teal")
        val follower = card("4111111111111111", bank.id, "teal", manual = false)
        val manual = card("5555555555554444", bank.id, "plum", manual = true)

        repository.updateBank(bank.id, "HDFC Bank", null)

        val stored = repository.banks.first().single()
        assertEquals("HDFC Bank", stored.name)
        assertNull(stored.color)
        assertEquals("navy", colourOf(follower))
        assertEquals("plum", colourOf(manual))
    }

    @Test
    fun anUnknownBankWithoutAColourLeavesItsCardsAlone() = runBlocking {
        val bank = repository.createBank("Corner Bank", "teal")
        val follower = card("4111111111111111", bank.id, "teal", manual = false)

        repository.updateBank(bank.id, "Corner Bank", null)

        assertEquals("teal", colourOf(follower))
    }
}
