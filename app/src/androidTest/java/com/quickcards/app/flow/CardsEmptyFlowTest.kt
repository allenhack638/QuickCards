package com.quickcards.app.flow

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quickcards.domain.ExpiredPlacement
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** A vault with cards but nothing visible keeps app-bar actions and offers a way back. */
@RunWith(AndroidJUnit4::class)
class CardsEmptyFlowTest : FlowTestBase() {

    @Test
    fun hiddenExpiredCardsOfferTheExpiredFilter() {
        app.settings.setExpiredCards(ExpiredPlacement.HIDE)
        val holder = runBlocking { app.repository.createHolder("Aarav Mehta") }
        val bank = runBlocking { app.repository.createBank("HDFC Bank") }
        seedCard("4532778103492211", holder.id, bank.id, expiry = "01/20")

        waitForText("Expired cards are hidden")
        waitForText("Search, or turn on the Expired filter to see them.")
        waitForText("Show expired")
        compose.onNodeWithText("Add your first card").assertDoesNotExist()
        compose.onNodeWithContentDescription("Search").assertExists()
        compose.onNodeWithContentDescription("Filter cards").assertExists()
        compose.onNodeWithContentDescription("Add card").assertExists()

        compose.onNodeWithText("Show expired").performClick()

        waitForContentDescription("More for HDFC Bank")
        waitForText("Expired")
    }

    @Test
    fun anArchiveOnlyVaultLinksToArchivedCards() {
        val holder = runBlocking { app.repository.createHolder("Aarav Mehta") }
        val bank = runBlocking { app.repository.createBank("HDFC Bank") }
        val card = seedCard("4532778103492211", holder.id, bank.id)
        runBlocking { app.repository.archive(card.id, "Card closed") }

        waitForText("All your cards are archived")
        waitForText("Find them in Manage › Archived cards.")
        compose.onNodeWithText("Add your first card").assertDoesNotExist()

        compose.onNodeWithText("Open Archived cards").performClick()

        waitForText("Archived cards")
        waitForText("ARCHIVED · CARD CLOSED", unmerged = true)
        assertEquals(1, cardsNow().count { it.archived })
    }
}
