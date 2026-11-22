package com.quickcards.app.flow

import android.content.Intent
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.assertCountEquals
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quickcards.app.core.reminders.OpenCardRequest
import com.quickcards.app.core.security.AppLockController
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** A reminder tap opens that card's details, and only after the lock screen is passed. */
@RunWith(AndroidJUnit4::class)
class ReminderOpenFlowTest : FlowTestBase() {

    @Test
    fun aReminderTapOpensTheCardOnlyOnceUnlocked() {
        val bank = kotlinx.coroutines.runBlocking { app.repository.createBank("HDFC Bank") }
        val card = seedCard("4532778103492211", bankId = bank.id)
        awaitTile("HDFC Bank")

        app.settings.setAppLockEnabled(true)
        compose.runOnUiThread { AppLockController.lock() }
        waitForContentDescription(LOCKED)

        compose.runOnUiThread {
            OpenCardRequest.offer(Intent().putExtra(OpenCardRequest.EXTRA_CARD_ID, card.id))
        }
        compose.waitForIdle()
        compose.onAllNodesWithContentDescription(EDIT).assertCountEquals(0)

        compose.runOnUiThread { AppLockController.unlock() }
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasContentDescription(EDIT)).fetchSemanticsNodes().isNotEmpty()
        }
        assertNull(OpenCardRequest.cardId.value)
        app.settings.setAppLockEnabled(false)
    }

    private companion object {
        const val LOCKED = "QuickCards, locked"
        const val EDIT = "Edit card"
    }
}
