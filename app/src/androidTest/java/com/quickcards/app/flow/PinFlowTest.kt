package com.quickcards.app.flow

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quickcards.app.core.data.model.VaultCard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Pin and unpin from the home actions sheet, the pinned group order, and the
 * disabled Pin action with its limit note once the pin limit is reached.
 */
@RunWith(AndroidJUnit4::class)
class PinFlowTest : FlowTestBase() {

    private val hdfc = "4532778103492211"
    private val axis = "4111111111111111"
    private val icici = "5555555555554444"

    @Test
    fun pinAndUnpinKeepThePinnedGroupFirst() {
        app.settings.setPinnedMax(3)
        val cards = seedThreeBanks()

        pin("HDFC Bank")
        awaitCardsWhere("HDFC Bank pinned") { list ->
            list.first { it.id == cards[0].id }.pinned
        }
        pin("Axis Bank")
        awaitCardsWhere("Axis Bank pinned") { list ->
            list.first { it.id == cards[1].id }.pinned
        }

        assertEquals(
            listOf(cards[0].id, cards[1].id, cards[2].id),
            cardsNow().map { it.id }
        )
        compose.onAllNodesWithContentDescription("Pinned").assertCountEquals(2)

        unpin("HDFC Bank")
        awaitCardsWhere("HDFC Bank unpinned") { list ->
            !list.first { it.id == cards[0].id }.pinned
        }

        val afterUnpin = cardsNow()
        assertEquals(
            listOf(cards[1].id, cards[0].id, cards[2].id),
            afterUnpin.map { it.id }
        )
        assertTrue(afterUnpin.first { it.id == cards[1].id }.pinned)
        assertFalse(afterUnpin.first { it.id == cards[0].id }.pinned)
        compose.onAllNodesWithContentDescription("Pinned").assertCountEquals(1)
    }

    @Test
    fun pinIsDisabledAtTheLimit() {
        app.settings.setPinnedMax(1)
        val cards = seedThreeBanks()

        pin("HDFC Bank")
        awaitCardsWhere("HDFC Bank pinned") { list ->
            list.first { it.id == cards[0].id }.pinned
        }

        openActions("Axis Bank")
        compose.onNodeWithText("1 of 1 pinned. Unpin a card or raise the limit in Settings.").assertExists()
        compose.onNodeWithText("Pin").performClick()
        assertFalse(cardsNow().first { it.id == cards[1].id }.pinned)
        assertTrue(cardsNow().first { it.id == cards[0].id }.pinned)
        compose.onAllNodesWithContentDescription("Pinned").assertCountEquals(1)
    }

    private fun seedThreeBanks(): List<VaultCard> {
        val holder = runBlockingSeed { app.repository.createHolder("Aarav Mehta") }
        val hdfcBank = runBlockingSeed { app.repository.createBank("HDFC Bank") }
        val axisBank = runBlockingSeed { app.repository.createBank("Axis Bank") }
        val iciciBank = runBlockingSeed { app.repository.createBank("ICICI Bank") }
        val cards = listOf(
            seedCard(hdfc, holder.id, hdfcBank.id, sortOrder = 0),
            seedCard(axis, holder.id, axisBank.id, sortOrder = 1),
            seedCard(icici, holder.id, iciciBank.id, sortOrder = 2)
        )
        revealCard("HDFC Bank")
        return cards
    }

    private fun openActions(title: String) {
        revealCard(title)
        compose.onNodeWithContentDescription("More for $title")
            .performScrollTo()
            .performClick()
        waitForText("Copy details")
    }

    private fun pin(title: String) {
        openActions(title)
        compose.onNodeWithText("Pin").performClick()
        waitForTextGone("Copy details")
    }

    private fun unpin(title: String) {
        openActions(title)
        compose.onNodeWithText("Unpin").performClick()
        waitForTextGone("Copy details")
    }

    private fun revealCard(title: String) {
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasContentDescription("More for $title"))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun <T> runBlockingSeed(block: suspend () -> T): T =
        kotlinx.coroutines.runBlocking { block() }
}
