package com.quickcards.app.flow

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/** Archived count in Manage, search across every field (reason included), and Restore. */
@RunWith(AndroidJUnit4::class)
class ArchivedCardsFlowTest : FlowTestBase() {

    @Test
    fun emptyArchiveShowsTheEmptyState() {
        openManage()

        compose.onNodeWithText("Archived").performClick()

        waitForText("No archived cards")
        waitForText("Hold a card to archive it")
    }

    @Test
    fun archivedCardsAreCountedSearchedByReasonAndRestored() {
        val holder = runBlocking { app.repository.createHolder("Aarav Mehta") }
        val bank = runBlocking { app.repository.createBank("HDFC Bank") }
        val closed = seedCard("4111111111111111", holder.id, bank.id, sortOrder = 0)
        val replaced = seedCard("5555555555554444", holder.id, bank.id, sortOrder = 1)
        runBlocking {
            app.repository.archive(closed.id, "Card closed")
            app.repository.archive(replaced.id, "Replaced")
        }
        compose.waitUntil(10_000) { archivedNow() == 2 }

        openManage()

        compose.waitUntil(10_000) {
            compose.onAllNodes(hasText("Archived") and hasText("2")).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Archived").performClick()

        waitForContentDescription("Search archived cards")
        compose.onNodeWithContentDescription("Search archived cards").performClick()

        waitForText("Search archived cards")
        waitForContentDescription("Archived · Card closed", substring = true)
        waitForContentDescription("Archived · Replaced", substring = true)

        compose.onNode(
            hasSetTextAction() and hasContentDescription("Search archived cards")
        ).performTextInput("replaced")

        waitForContentDescription("Archived · Replaced", substring = true)
        awaitDescriptionGone("Archived · Card closed")

        openCardActions("HDFC Bank")
        compose.onNodeWithText("Restore").performClick()

        waitForText("Card restored")
        compose.waitUntil(10_000) { archivedNow() == 1 }
        awaitDescriptionGone("Archived · Replaced")
        waitForText("No archived cards match", substring = true)

        compose.onNodeWithContentDescription("Close search").performClick()
        compose.onNodeWithContentDescription("Back").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasText("Archived") and hasText("1")).fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(1, archivedNow())
    }

    private fun openManage() {
        compose.onNodeWithText("Manage").performClick()
        waitForText("Archived")
    }

    private fun awaitDescriptionGone(description: String) {
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasContentDescription(description, substring = true))
                .fetchSemanticsNodes().isEmpty()
        }
    }

    private fun archivedNow(): Int = runBlocking {
        app.repository.observeArchived().first().size
    }
}
