package com.quickcards.app.flow

import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Archive from the card actions sheet (hold a tile), Undo, and search + Restore. */
@RunWith(AndroidJUnit4::class)
class ArchiveFlowTest : FlowTestBase() {

    @Test
    fun archiveWithReasonAndUndoRestoresTheCard() {
        val holder = runBlocking { app.repository.createHolder("Aarav Mehta") }
        val bank = runBlocking { app.repository.createBank("HDFC Bank") }
        val card = seedCard(
            "4532778103492211",
            holder.id,
            bank.id,
            pinned = true,
            pinnedAt = 1_700_000_000_000L
        )
        openCardActions("HDFC Bank")
        compose.onNodeWithText("Archive").performClick()

        waitForText("Archive card?")
        compose.onNode(hasSetTextAction()).performTextInput("Card closed")
        compose.onNodeWithText("Archive").performClick()

        waitForText("Card archived")
        awaitCardsWhere("card archived") { list ->
            list.singleOrNull()?.archived == true
        }
        assertEquals("Card closed", cardsNow().single().archiveReason)
        assertFalse(cardsNow().single().pinned)
        awaitTileGone("HDFC Bank")

        compose.onNodeWithText("Undo").performClick()

        awaitCardsWhere("card restored") { list ->
            list.singleOrNull()?.archived == false
        }
        assertEquals(card.id, cardsNow().single().id)
        assertTrue(cardsNow().single().pinned)
    }

    @Test
    fun searchFindsAnArchivedCardAndOffersRestore() {
        val holder = runBlocking { app.repository.createHolder("Aarav Mehta") }
        val bank = runBlocking { app.repository.createBank("HDFC Bank") }
        val card = seedCard("4532778103492211", holder.id, bank.id)
        runBlocking { app.repository.archive(card.id, "Card closed") }
        awaitCardsWhere("card archived") { list -> list.singleOrNull()?.archived == true }

        compose.onNodeWithContentDescription("Search").performClick()
        compose.onNodeWithContentDescription("Search cards").performTextInput("HDFC")
        openCardActions("HDFC Bank")
        compose.onNodeWithText("Restore").performClick()

        waitForText("Card restored")
        awaitCardsWhere("card restored") { list ->
            list.singleOrNull()?.archived == false
        }
        assertFalse(cardsNow().single().archived)
        assertTrue(cardsNow().single().archiveReason == null)

        compose.onNodeWithContentDescription("Close search").performClick()
        waitForText("Cards")
    }
}
