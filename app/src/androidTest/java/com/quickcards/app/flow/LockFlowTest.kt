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
        openCardActions("HDFC Bank")
        waitForText("Unpin")

        compose.runOnUiThread { AppLockController.lock() }
        waitForText("QuickCards is locked")
        compose.onAllNodesWithText("Unpin").assertCountEquals(0)
        compose.onAllNodesWithText("Delete").assertCountEquals(0)

        compose.runOnUiThread { AppLockController.unlock() }
        waitForTextGone("QuickCards is locked")

        openCardActions("Axis Bank")
        UnlockSession.unlockFor(60)
        compose.onNodeWithText("Delete").performClick()
        waitForText("Delete Axis Bank ••4444?")

        compose.runOnUiThread { AppLockController.lock() }
        waitForText("QuickCards is locked")
        compose.onAllNodesWithText("Delete Axis Bank ••4444?").assertCountEquals(0)

        compose.runOnUiThread { AppLockController.unlock() }
        waitForTextGone("QuickCards is locked")
    }

    @Test
    fun lockCoversTheArchiveDialogAndTheArchivedScreen() {
        val holder = runBlockingSeed { app.repository.createHolder("Aarav Mehta") }
        val bank = runBlockingSeed { app.repository.createBank("HDFC Bank") }
        val card = seedCard("4532778103492211", holder.id, bank.id)

        app.settings.setAppLockEnabled(true)
        openCardActions("HDFC Bank")
        compose.onNodeWithText("Archive").performClick()
        waitForText("Archive card?")

        compose.runOnUiThread { AppLockController.lock() }
        waitForText("QuickCards is locked")
        compose.onAllNodesWithText("Archive card?").assertCountEquals(0)

        compose.runOnUiThread { AppLockController.unlock() }
        waitForTextGone("QuickCards is locked")

        runBlockingSeed { app.repository.archive(card.id, "Card closed") }
        compose.onNode(hasText("Manage") and isSelectable()).performClick()
        waitForText("Archived")
        compose.onNodeWithText("Archived").performClick()
        waitForContentDescription("Search archived cards")
        waitForText("Archived · Card closed", unmerged = true)

        compose.runOnUiThread { AppLockController.lock() }
        waitForText("QuickCards is locked")
        compose.onAllNodesWithText("Archived cards").assertCountEquals(0)
        compose.onAllNodesWithText("Archived · Card closed").assertCountEquals(0)

        compose.runOnUiThread { AppLockController.unlock() }
        waitForTextGone("QuickCards is locked")
        waitForText("Archived · Card closed", unmerged = true)
    }

    private fun <T> runBlockingSeed(block: suspend () -> T): T =
        kotlinx.coroutines.runBlocking { block() }
}
