package com.quickcards.app.flow

import android.content.Intent
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.app.core.reminders.OpenCardRequest
import com.quickcards.app.core.security.AppLockController
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A reminder tap opens that card's details, only after the lock screen is passed, and never
 * silently drops an unsaved Add/Edit draft.
 */
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

    @Test
    fun aReminderForADeletedCardStaysOnHomeAndSaysSo() {
        val bank = kotlinx.coroutines.runBlocking { app.repository.createBank("HDFC Bank") }
        seedCard("4532778103492211", bankId = bank.id)
        awaitTile("HDFC Bank")

        compose.runOnUiThread {
            OpenCardRequest.offer(Intent().putExtra(OpenCardRequest.EXTRA_CARD_ID, "deleted-card"))
        }
        waitForText("Card not found")
        compose.onAllNodesWithContentDescription(EDIT).assertCountEquals(0)
        awaitTile("HDFC Bank")
        assertNull(OpenCardRequest.cardId.value)
    }

    @Test
    fun aReminderOverAnUnsavedFormAsksAndKeepEditingStays() {
        val card = seedOneAndOpenAddWithDraft()

        offer(card.id)
        waitForText(DISCARD_TITLE)
        compose.onNodeWithText("Keep editing").performClick()
        waitForTextGone(DISCARD_TITLE)

        compose.onNodeWithText(SAVE).assertExists()
        compose.onAllNodesWithContentDescription(EDIT).assertCountEquals(0)
        assertNull(OpenCardRequest.cardId.value)
        compose.waitForIdle()
        compose.onAllNodesWithText(DISCARD_TITLE).assertCountEquals(0)
    }

    @Test
    fun aReminderOverAnUnsavedFormOpensTheCardOnDiscard() {
        val card = seedOneAndOpenAddWithDraft()

        offer(card.id)
        waitForText(DISCARD_TITLE)
        compose.onNodeWithText("Discard").performClick()

        waitForContentDescription(EDIT)
        compose.onAllNodesWithText(SAVE).assertCountEquals(0)
        assertNull(OpenCardRequest.cardId.value)
    }

    @Test
    fun aReminderOverACleanFormOpensTheCardWithoutAsking() {
        val bank = kotlinx.coroutines.runBlocking { app.repository.createBank("HDFC Bank") }
        val card = seedCard("4532778103492211", bankId = bank.id)
        awaitTile("HDFC Bank")
        compose.onNodeWithContentDescription("Add card").performClick()
        waitForText(SAVE)

        offer(card.id)
        waitForContentDescription(EDIT)
        compose.onAllNodesWithText(DISCARD_TITLE).assertCountEquals(0)
        compose.onAllNodesWithText(SAVE).assertCountEquals(0)
    }

    private fun seedOneAndOpenAddWithDraft(): VaultCard {
        val bank = kotlinx.coroutines.runBlocking { app.repository.createBank("HDFC Bank") }
        val card = seedCard("4532778103492211", bankId = bank.id)
        awaitTile("HDFC Bank")
        compose.onNodeWithContentDescription("Add card").performClick()
        waitForText(SAVE)
        field("Card number").performTextInput("4")
        compose.waitForIdle()
        return card
    }

    private fun offer(cardId: String) {
        compose.runOnUiThread {
            OpenCardRequest.offer(Intent().putExtra(OpenCardRequest.EXTRA_CARD_ID, cardId))
        }
    }

    private companion object {
        const val DISCARD_TITLE = "Discard changes?"
        const val SAVE = "Save card"
        const val LOCKED = "QuickCards, locked"
        const val EDIT = "Edit card"
    }
}
