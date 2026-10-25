package com.quickcards.app.flow

import com.quickcards.app.core.security.UnlockSession
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quickcards.app.core.security.AppLockController
import org.junit.Test
import org.junit.runner.RunWith

/** Lock now covers the app and removes open overlays; unlocking brings Home back. */
@RunWith(AndroidJUnit4::class)
class LockFlowTest : FlowTestBase() {

    @Test
    fun lockCoversAnOpenSheetAndAnOpenDialog() {
        val holder = runBlockingSeed { app.repository.createHolder("Aarav Mehta") }
        val hdfc = runBlockingSeed { app.repository.createBank("HDFC Bank") }
        val axis = runBlockingSeed { app.repository.createBank("Axis Bank") }
        val hdfcCard = seedCard("4532778103492211", holder.id, hdfc.id, sortOrder = 0)
        seedCard("5555555555554444", holder.id, axis.id, sortOrder = 1)
        runBlockingSeed { app.repository.pin(hdfcCard.id) }

        app.settings.setAppLockEnabled(true)
        app.settings.setPinnedMax(1)
        waitForContentDescription("More for HDFC Bank")

        compose.onNodeWithContentDescription("More for HDFC Bank").performClick()
        waitForText("Unpin")

        compose.runOnUiThread { AppLockController.lock() }
        waitForText("QuickCards is locked")
        compose.onAllNodesWithText("Unpin").assertCountEquals(0)
        compose.onAllNodesWithText("Delete card").assertCountEquals(0)

        compose.runOnUiThread { AppLockController.unlock() }
        waitForTextGone("QuickCards is locked")

        waitForContentDescription("More for Axis Bank")
        compose.onNodeWithContentDescription("More for Axis Bank").performClick()
        UnlockSession.unlockFor(60)
        waitForText("Delete card")
        compose.onNodeWithText("Delete card").performClick()
        waitForText("Delete this card?")

        compose.runOnUiThread { AppLockController.lock() }
        waitForText("QuickCards is locked")
        compose.onAllNodesWithText("Delete this card?").assertCountEquals(0)

        compose.runOnUiThread { AppLockController.unlock() }
        waitForTextGone("QuickCards is locked")
    }

    @Test
    fun lockCoversTheArchiveDialogAndTheArchivedScreen() {
        val holder = runBlockingSeed { app.repository.createHolder("Aarav Mehta") }
        val bank = runBlockingSeed { app.repository.createBank("HDFC Bank") }
        val card = seedCard("4532778103492211", holder.id, bank.id)

        app.settings.setAppLockEnabled(true)
        waitForContentDescription("More for HDFC Bank")

        compose.onNodeWithContentDescription("More for HDFC Bank").performClick()
        waitForText("Archive")
        compose.onNodeWithText("Archive").performClick()
        waitForText("Archive this card?")

        compose.runOnUiThread { AppLockController.lock() }
        waitForText("QuickCards is locked")
        compose.onAllNodesWithText("Archive this card?").assertCountEquals(0)

        compose.runOnUiThread { AppLockController.unlock() }
        waitForTextGone("QuickCards is locked")

        runBlockingSeed { app.repository.archive(card.id, "Card closed") }
        compose.onNode(hasText("Manage") and isSelectable()).performClick()
        waitForText("Archived cards")
        compose.onNodeWithText("Archived cards").performClick()
        waitForContentDescription("Search archived cards")
        waitForText("ARCHIVED · CARD CLOSED", unmerged = true)

        compose.runOnUiThread { AppLockController.lock() }
        waitForText("QuickCards is locked")
        compose.onAllNodesWithText("Archived cards").assertCountEquals(0)
        compose.onAllNodesWithText("ARCHIVED · CARD CLOSED").assertCountEquals(0)

        compose.runOnUiThread { AppLockController.unlock() }
        waitForTextGone("QuickCards is locked")
        waitForText("ARCHIVED · CARD CLOSED", unmerged = true)
    }

    private fun <T> runBlockingSeed(block: suspend () -> T): T =
        kotlinx.coroutines.runBlocking { block() }
}
