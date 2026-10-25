package com.quickcards.app.flow

import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
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

        waitForText("Cards you’ve put away")
        waitForText("0")
        compose.onNodeWithText("Archived cards").performClick()

        waitForText("No archived cards")
        waitForText("Archive a card from its ⋮ menu to keep it out of your list.")
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

        waitForText("Archived cards")
        waitForText("2")
        compose.onNodeWithText("Archived cards").performClick()

        waitForContentDescription("Search archived cards")
        compose.onNodeWithContentDescription("Search archived cards").performClick()

        waitForText("Search archived cards")
        waitForText("ARCHIVED · CARD CLOSED", unmerged = true)
        waitForText("ARCHIVED · REPLACED", unmerged = true)

        compose.onNode(
            hasSetTextAction() and hasContentDescription("Search archived cards")
        ).performTextInput("replaced")

        waitForText("ARCHIVED · REPLACED", unmerged = true)
        waitForTextGone("ARCHIVED · CARD CLOSED", unmerged = true)

        compose.onNodeWithContentDescription("More for HDFC Bank").performClick()
        waitForText("Restore")
        compose.onNodeWithText("Restore").performClick()

        waitForText("Card restored")
        compose.waitUntil(10_000) { archivedNow() == 1 }
        waitForTextGone("ARCHIVED · REPLACED", unmerged = true)
        waitForText("No archived cards match", substring = true)

        compose.onNodeWithContentDescription("Close search").performClick()
        compose.onNodeWithContentDescription("Back").performClick()
        waitForText("1")
        assertEquals(1, archivedNow())
    }

    private fun openManage() {
        compose.onNodeWithText("Manage").performClick()
        waitForText("Archived cards")
    }

    private fun archivedNow(): Int = runBlocking {
        app.repository.observeArchived().first().size
    }
}
