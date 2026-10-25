package com.quickcards.app.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quickcards.app.core.data.database.QuickCardsDatabase
import com.quickcards.app.core.data.model.SeedData
import com.quickcards.app.core.data.model.VaultCard
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** Pinned cards sort first in pin order; lowering the limit unpins the newest extras. */
@RunWith(AndroidJUnit4::class)
class PinOrderingTest {

    private lateinit var database: QuickCardsDatabase
    private lateinit var repository: VaultRepository

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, QuickCardsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = VaultRepository(database)
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun seed(): List<String> {
        database.networkDao().insertNetworks(SeedData.networks)
        return listOf("4532778103492219", "378282246310005", "524199001137448")
            .map { number ->
                repository.insertCard(
                    VaultCard(
                        number = number,
                        expiry = "12/29",
                        cvv = "381",
                        nameOnCard = "AARAV MEHTA",
                        networkAuto = true,
                        bandColor = "navy"
                    )
                ).getOrThrow().id
            }
    }

    @Test
    fun pinnedCardsSortFirstInPinOrderThenTheExistingOrder() = runBlocking {
        val (first, second, third) = seed()

        repository.pin(first)
        repository.pin(second)
        repository.pin(third)

        val ordered = repository.cards.first()
        assertEquals(listOf(first, second, third), ordered.map { it.id })
        assertTrue(ordered.take(3).all { it.pinned })
    }

    @Test
    fun repinningMovesACardToTheEndOfThePinnedGroup() = runBlocking {
        val (first, second, third) = seed()

        repository.pin(first)
        repository.pin(second)
        repository.pin(third)
        repository.unpin(first)
        repository.pin(first)

        val ordered = repository.cards.first().map { it.id }
        assertEquals(listOf(second, third, first), ordered)
    }

    @Test
    fun loweringTheLimitUnpinsTheMostRecentlyPinnedExtras() = runBlocking {
        val (first, second, third) = seed()

        repository.pin(first)
        repository.pin(second)
        repository.pin(third)

        assertEquals(2, repository.trimPinned(1))

        val ordered = repository.cards.first()
        assertEquals(first, ordered.first().id)
        assertTrue(ordered.first().pinned)
        assertFalse(ordered.first { it.id == second }.pinned)
        assertFalse(ordered.first { it.id == third }.pinned)
    }
}
