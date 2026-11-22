package com.quickcards.app.feature.details

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quickcards.app.core.data.database.QuickCardsDatabase
import com.quickcards.app.core.data.model.SeedData
import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.app.core.data.repository.VaultRepository
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

/** Every card action's Undo puts the card back exactly as it was. */
@RunWith(AndroidJUnit4::class)
class CardActionsViewModelTest {

    private lateinit var database: QuickCardsDatabase
    private lateinit var repository: VaultRepository
    private lateinit var viewModel: CardActionsViewModel

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, QuickCardsDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = VaultRepository(database)
        viewModel = CardActionsViewModel(repository)
        runBlocking { database.networkDao().insertNetworks(SeedData.networks) }
    }

    @After
    fun tearDown() {
        database.close()
    }

    private suspend fun stored(id: String) = repository.cards.first().single { it.id == id }

    private suspend fun seed(pinned: Boolean = false): VaultCard {
        val tag = repository.createTag("Travel")
        return repository.insertCard(
            VaultCard(
                number = "4532778103492211",
                expiry = "12/29",
                cvv = "381",
                note = "Fuel",
                bandColor = "plum",
                bandColorManual = true,
                tagIds = listOf(tag.id),
                pinned = pinned,
                pinnedAt = if (pinned) 1_700_000_000_000L else null
            )
        ).getOrThrow()
    }

    @Test
    fun undoingAnArchiveRestoresThePin() = runBlocking {
        val card = seed(pinned = true)

        val outcome = viewModel.archive(card.id, "Card closed")
        assertTrue(stored(card.id).archived)
        assertFalse(stored(card.id).pinned)

        viewModel.undoArchive(outcome)
        val back = stored(card.id)
        assertFalse(back.archived)
        assertNull(back.archiveReason)
        assertTrue(back.pinned)
        assertEquals(1_700_000_000_000L, back.pinnedAt)
    }

    @Test
    fun undoingADeleteRestoresTagsColourAndNote() = runBlocking {
        val card = seed()

        viewModel.delete(card.id)
        assertTrue(repository.cards.first().isEmpty())

        viewModel.undoDelete(card)
        val back = stored(card.id)
        assertEquals(card.tagIds, back.tagIds)
        assertEquals("plum", back.bandColor)
        assertTrue(back.bandColorManual)
        assertEquals("Fuel", back.note)
        assertEquals("381", back.cvv)
    }

    @Test
    fun undoingAPinPutsTheOldStateBack() = runBlocking {
        val card = seed()

        val outcome = viewModel.togglePin(card.id, pin = true)!!
        assertTrue(outcome.pinned)
        assertTrue(stored(card.id).pinned)

        viewModel.undoPin(outcome)
        assertFalse(stored(card.id).pinned)
        assertNull(stored(card.id).pinnedAt)
    }

    @Test
    fun undoingARestoreArchivesAgainWithTheReason() = runBlocking {
        val card = seed()
        repository.archive(card.id, "Lost")

        viewModel.restore(card.id)
        assertFalse(stored(card.id).archived)

        viewModel.rearchive(card.id, "Lost")
        assertTrue(stored(card.id).archived)
        assertEquals("Lost", stored(card.id).archiveReason)
    }

    @Test
    fun pinningAMissingCardDoesNothing() = runBlocking {
        assertNull(viewModel.togglePin("missing", pin = true))
    }
}
