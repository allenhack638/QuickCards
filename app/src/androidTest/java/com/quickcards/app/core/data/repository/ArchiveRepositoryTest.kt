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
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ArchiveRepositoryTest {

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
    fun archiveUnpinsHidesAndRestoresTheCard() = runBlocking {
        val holder = repository.createHolder("Aarav Mehta")
        val card = repository.insertCard(
            VaultCard(
                number = "4111111111111111",
                expiry = "12/29",
                cvv = "123",
                holderId = holder.id,
                pinned = true,
                pinnedAt = 1_700_000_000_000L
            )
        ).getOrThrow()

        repository.archive(card.id, "  Card closed  ")

        val archived = repository.cards.first().single()
        assertTrue(archived.archived)
        assertEquals("Card closed", archived.archiveReason)
        assertFalse(archived.pinned)
        assertNull(archived.pinnedAt)
        assertTrue(archived.archivedAt != null)
        assertEquals(card.id, repository.observeArchived().first().single().id)

        repository.restore(card.id)

        val restored = repository.cards.first().single()
        assertFalse(restored.archived)
        assertNull(restored.archiveReason)
        assertNull(restored.archivedAt)
        assertTrue(repository.observeArchived().first().isEmpty())
    }

    @Test
    fun editingAnArchivedCardKeepsItsArchiveState() = runBlocking {
        val holder = repository.createHolder("Aarav Mehta")
        val card = repository.insertCard(
            VaultCard(
                number = "4111111111111111",
                expiry = "12/29",
                cvv = "123",
                holderId = holder.id
            )
        ).getOrThrow()
        repository.archive(card.id, "Replaced")

        val updated = repository.updateCard(
            card.copy(
                bankId = null,
                note = "Edited while archived",
                archived = false,
                archivedAt = null,
                archiveReason = null,
                pinned = false,
                pinnedAt = null
            )
        ).getOrThrow()

        assertTrue(updated.archived)
        assertEquals("Replaced", updated.archiveReason)
        val stored = repository.cards.first().single()
        assertTrue(stored.archived)
        assertEquals("Replaced", stored.archiveReason)
        assertEquals("Edited while archived", stored.note)
    }

    @Test
    fun archivingKeepsTheCardOutOfTheMainList() = runBlocking {
        val holder = repository.createHolder("Aarav Mehta")
        val card = repository.insertCard(
            VaultCard(
                number = "4111111111111111",
                expiry = "12/29",
                cvv = "123",
                holderId = holder.id
            )
        ).getOrThrow()

        repository.archive(card.id, null)

        val archived = repository.cards.first().single()
        assertTrue(archived.archived)
        assertNull(archived.archiveReason)
        assertEquals(card.id, repository.observeArchived().first().single().id)
    }
}
