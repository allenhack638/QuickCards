package com.quickcards.app.core.data.repository

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quickcards.app.core.data.database.QuickCardsDatabase
import com.quickcards.app.core.data.model.SeedData
import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.domain.CardField
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class NetworkRuleReviewTest {

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
    fun resettingVisaFlagsCardsSavedUnderTheChangedRuleWithoutTouchingThem() = runBlocking {
        val visa = repository.getNetwork("visa")!!
        repository.updateNetwork(visa.copy(cvvLength = 4))
        val holder = repository.createHolder("Aarav Mehta")
        repository.insertCard(
            VaultCard(number = "4111111111111111", expiry = "12/29", cvv = "1234", holderId = holder.id)
        ).getOrThrow()
        assertTrue(repository.cards.first().single().needsReview.isEmpty())

        repository.updateNetwork(visa)

        val card = repository.cards.first().single()
        assertEquals(setOf(CardField.CVV), card.needsReview)
        assertEquals("1234", card.cvv)

        repository.updateNetwork(visa.copy(cvvLength = 4))
        assertTrue(repository.cards.first().single().needsReview.isEmpty())
    }
}
