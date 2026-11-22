package com.quickcards.app.flow

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.getOrNull
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quickcards.app.core.security.AppLockController
import com.quickcards.app.core.security.UnlockSession
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** A revealed number never outlives a lock: back on the card it is masked and needs a new unlock. */
@RunWith(AndroidJUnit4::class)
class DetailsRevealFlowTest : FlowTestBase() {

    @Test
    fun aLockHidesARevealedNumber() {
        val bank = kotlinx.coroutines.runBlocking { app.repository.createBank("HDFC Bank") }
        seedCard("4532778103492211", bankId = bank.id)
        awaitTile("HDFC Bank")
        tile("HDFC Bank").performClick()
        compose.waitUntil(10_000) { hidden().isNotEmpty() }

        UnlockSession.unlockFor(60)
        app.settings.setAppLockEnabled(true)
        // The reveal drains on the frame clock; hold it so the 5 s timeout cannot be what hides it.
        compose.mainClock.autoAdvance = false
        try {
            compose.onNode(revealNumber).performSemanticsAction(SemanticsActions.OnLongClick)
            compose.mainClock.advanceTimeBy(FRAMES)
            assertTrue("The hold reveals the number", hidden().isEmpty())

            compose.runOnUiThread { AppLockController.lock() }
            compose.mainClock.advanceTimeBy(FRAMES)
            compose.runOnUiThread { AppLockController.unlock() }
            UnlockSession.unlockFor(60)
            compose.mainClock.advanceTimeBy(FRAMES)
            assertTrue("Back from the lock the number is masked again", hidden().isNotEmpty())
        } finally {
            compose.mainClock.autoAdvance = true
            app.settings.setAppLockEnabled(false)
        }
    }

    private val revealNumber = SemanticsMatcher("holds to show the card number") {
        it.config.getOrNull(SemanticsActions.OnLongClick)?.label == "Show card number"
    }

    private fun hidden() = compose.onAllNodes(hasContentDescription(HIDDEN, substring = true)).fetchSemanticsNodes()

    private companion object {
        const val HIDDEN = "Card number ending 2211, hidden"
        const val FRAMES = 200L
    }
}
