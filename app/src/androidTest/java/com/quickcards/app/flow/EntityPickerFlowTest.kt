package com.quickcards.app.flow

import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.isFocused
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Picker sheets select an entry; "+ Add" creates and selects a new item. */
@RunWith(AndroidJUnit4::class)
class EntityPickerFlowTest : FlowTestBase() {

    @Test
    fun addCardHolderCreatesAndSelectsIt() {
        openAddCardForm()

        compose.onNodeWithContentDescription("Card holder")
            .performScrollTo()
            .performClick()
        waitForText("Choose card holder")
        compose.onNodeWithText("Add card holder").performClick()

        waitForText("New card holder")
        field("Name").performTextInput("Priya Mehta")
        compose.onNodeWithText("Add").performClick()

        waitForContentDescription("Card holder, Priya Mehta")
        val holders = runBlocking { app.repository.holders.first() }
        assertTrue(holders.any { it.name == "Priya Mehta" })
    }

    @Test
    fun addBankCreatesAndSelectsIt() {
        openAddCardForm()

        compose.onNodeWithContentDescription("Bank")
            .performScrollTo()
            .performClick()
        waitForText("Choose bank")
        compose.onNodeWithText("Add a new bank").performClick()

        waitForText("New bank")
        field("Name").performTextInput("Axis Bank")
        compose.onNodeWithText("Add").performClick()

        waitForContentDescription("Bank, Axis Bank")
        val banks = runBlocking { app.repository.banks.first() }
        assertTrue(banks.any { it.name == "Axis Bank" })
    }

    @Test
    fun cardTypeSheetSelectsAnExistingType() {
        openAddCardForm()

        compose.onNodeWithContentDescription("Card type (optional)")
            .performScrollTo()
            .performClick()
        waitForText("Choose card type")
        compose.onNodeWithText("Credit").performClick()

        waitForContentDescription("Card type (optional), Credit")
    }

    private fun openAddCardForm() {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithContentDescription("Add card").fetchSemanticsNodes().isNotEmpty() ||
                compose.onAllNodesWithText("Add your first card").fetchSemanticsNodes().isNotEmpty()
        }
        if (runBlocking { app.repository.cards.first() }.isEmpty()) {
            compose.onNodeWithText("Add card").performClick()
        } else {
            compose.onNodeWithContentDescription("Add card").performClick()
        }
        waitForText("Save card")
        // The form focuses (300 ms) then scrolls (350 ms) the number field; let it settle
        // so taps land where the field ends up.
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasTestTag("form_number") and isFocused())
                .fetchSemanticsNodes().isNotEmpty()
        }
        compose.mainClock.advanceTimeBy(FOCUS_SCROLL_SETTLE_MS)
        compose.waitForIdle()
        // With the IME closed the form stops resizing under the taps.
        Espresso.closeSoftKeyboard()
        compose.waitForIdle()
    }

    private companion object {
        const val FOCUS_SCROLL_SETTLE_MS = 500L
    }
}
