package com.quickcards.app.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quickcards.app.core.data.database.QuickCardsDatabase
import com.quickcards.app.core.data.model.SeedData
import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.app.core.security.PanHasher
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Duplicate detection is the Keystore HMAC in `cards.panHash`, never plaintext. */
@RunWith(AndroidJUnit4::class)
class VaultDedupTest {

    private lateinit var database: QuickCardsDatabase
    private lateinit var repository: VaultRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, QuickCardsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = VaultRepository(database)
        runBlocking {
            database.networkDao().insertNetworks(SeedData.networks)
            database.cardTypeDao().insertCardTypes(SeedData.cardTypes)
        }
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun duplicatePanIsBlockedOnInsert() = runBlocking {
        val first = repository.insertCard(card(VISA)).getOrThrow()

        val error = repository.insertCard(card(VISA)).exceptionOrNull()

        assertTrue(error is DuplicateCardException)
        assertEquals(first.id, (error as DuplicateCardException).existingCardId)
        assertEquals(1, storedCount())
    }

    @Test
    fun duplicatePanIsBlockedOnUpdate() = runBlocking {
        repository.insertCard(card(VISA)).getOrThrow()
        val other = repository.insertCard(card(MASTERCARD)).getOrThrow()

        val error = repository.updateCard(other.copy(number = VISA)).exceptionOrNull()

        assertTrue(error is DuplicateCardException)
        assertEquals(2, storedCount())
        assertEquals(MASTERCARD, repository.getCard(other.id)?.number)
    }

    @Test
    fun updatingACardKeepsItsOwnPan() = runBlocking {
        val saved = repository.insertCard(card(VISA)).getOrThrow()

        val updated = repository.updateCard(saved.copy(cvv = "456"))

        assertTrue(updated.isSuccess)
        assertEquals("456", repository.getCard(saved.id)?.cvv)
    }

    @Test
    fun missingPanHashIsBackfilledByMaintenance() = runBlocking {
        repository.insertCard(card(VISA)).getOrThrow()
        val stored = database.cardDao().getAllCardsSync().single()
        assertNotNull(stored.panHash)
        database.cardDao().updateCard(stored.copy(panHash = null))

        DataMaintenance.run(database, InMemoryRepairSummaryStore())

        assertEquals(PanHasher.hash(VISA), database.cardDao().getAllCardsSync().single().panHash)
    }

    private suspend fun storedCount() = database.cardDao().getAllCardsSync().size

    private fun card(number: String) = VaultCard(number = number, expiry = "12/30", cvv = "123")

    private companion object {
        const val VISA = "4111111111111111"
        const val MASTERCARD = "5555555555554444"
    }
}
