package com.quickcards.app.flow

import com.quickcards.app.core.security.UnlockSession
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.lifecycle.Lifecycle
import com.quickcards.app.core.security.AppLockController
import com.quickcards.app.core.security.BiometricAuthHelper
import org.junit.Assert.assertFalse
import org.junit.Assume.assumeTrue
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
        waitForContentDescription(LOCKED)
        compose.onAllNodesWithText("Unpin").assertCountEquals(0)
        compose.onAllNodesWithText("Delete").assertCountEquals(0)

        compose.runOnUiThread { AppLockController.unlock() }
        waitForLockGone()

        openCardActions("Axis Bank")
        UnlockSession.unlockFor(60)
        compose.onNodeWithText("Delete").performClick()
        waitForText("Delete Axis Bank •• 4444?")

        compose.runOnUiThread { AppLockController.lock() }
        waitForContentDescription(LOCKED)
        compose.onAllNodesWithText("Delete Axis Bank •• 4444?").assertCountEquals(0)

        compose.runOnUiThread { AppLockController.unlock() }
        waitForLockGone()
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
        waitForContentDescription(LOCKED)
        compose.onAllNodesWithText("Archive card?").assertCountEquals(0)

        compose.runOnUiThread { AppLockController.unlock() }
        waitForLockGone()

        runBlockingSeed { app.repository.archive(card.id, "Card closed") }
        compose.onNode(hasText("Manage") and isSelectable()).performClick()
        waitForText("Archived")
        compose.onNodeWithText("Archived").performClick()
        waitForContentDescription("Search archived cards")
        waitForText("Card closed", unmerged = true)

        compose.runOnUiThread { AppLockController.lock() }
        waitForContentDescription(LOCKED)
        compose.onAllNodesWithContentDescription("Search archived cards").assertCountEquals(0)
        compose.onAllNodesWithText("Card closed").assertCountEquals(0)

        compose.runOnUiThread { AppLockController.unlock() }
        waitForLockGone()
        waitForText("Card closed", unmerged = true)
    }

    @Test
    fun aQuickTripToTheShareSheetStaysUnlocked() {
        assumeTrue(BiometricAuthHelper(app).isAuthenticationAvailable())
        app.settings.setAppLockEnabled(true)
        app.settings.setLockOnLeave(true)

        leaveForTheShareSheet(awayMillis = 2_000)

        assertFalse(AppLockController.isLocked.value)
    }

    @Test
    fun comingBackAfterTheShareGraceLocks() {
        assumeTrue(BiometricAuthHelper(app).isAuthenticationAvailable())
        app.settings.setAppLockEnabled(true)
        app.settings.setLockOnLeave(true)
        AppLockController.shareSheetGraceMs = 2_000
        try {
            // The stop lands inside the grace (no lock yet); the return comes after it ends.
            leaveForTheShareSheet(awayMillis = 4_000)
            waitForContentDescription(LOCKED)
        } finally {
            AppLockController.shareSheetGraceMs = 60_000
            compose.runOnUiThread { AppLockController.unlock() }
            app.settings.setAppLockEnabled(false)
        }
    }

    /** The app goes to the background as the share sheet opens, and comes back [awayMillis] later. */
    private fun leaveForTheShareSheet(awayMillis: Long) {
        compose.runOnUiThread { AppLockController.beginShareSheet() }
        compose.activityRule.scenario.moveToState(Lifecycle.State.CREATED)
        Thread.sleep(awayMillis)
        compose.activityRule.scenario.moveToState(Lifecycle.State.RESUMED)
        compose.waitForIdle()
    }

    private fun waitForLockGone() {
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasContentDescription(LOCKED)).fetchSemanticsNodes().isEmpty()
        }
    }

    private fun <T> runBlockingSeed(block: suspend () -> T): T =
        kotlinx.coroutines.runBlocking { block() }

    private companion object {
        const val LOCKED = "QuickCards, locked"
    }
}
