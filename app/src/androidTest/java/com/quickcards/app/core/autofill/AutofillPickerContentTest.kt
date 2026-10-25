package com.quickcards.app.core.autofill

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quickcards.app.core.ui.theme.QuickCardsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The picker searches like Home: tags, bank, holder, first digits or last four. */
@RunWith(AndroidJUnit4::class)
class AutofillPickerContentTest {
    @get:Rule val compose = createComposeRule()

    private val cards = listOf(
        PickerCard("1", "HDFC Bank", "Credit", "Aarav Mehta", "4532778103492211", "12/29", listOf("Travel"), "visa", false),
        PickerCard("2", "ICICI Bank", "", "Priya Sharma", "5555555555554444", "11/26", listOf("Groceries"), "mc", false),
        PickerCard("3", "Kotak Bank", "", "Aarav Mehta", "3530111333300000", "03/25", emptyList(), "jcb", true)
    )

    private fun show(onPick: (String) -> Unit = {}) = compose.setContent {
        QuickCardsTheme(darkTheme = true) {
            AutofillPickerContent(cards = cards, requester = "shop.example", onPick = onPick, onCancel = {})
        }
    }

    private fun search(text: String) =
        compose.onNodeWithContentDescription("Search cards").performTextInput(text)

    @Test
    fun everyCardIsListed() {
        show()
        compose.onAllNodesWithTag(PICKER_ROW).assertCountEquals(3)
    }

    @Test
    fun searchingATagNarrows() {
        show(); search("groc")
        compose.onAllNodesWithTag(PICKER_ROW).assertCountEquals(1)
        compose.onNodeWithText("ICICI Bank").assertExists()
    }

    @Test
    fun searchingTheFirstDigitsNarrows() {
        show(); search("4532")
        compose.onAllNodesWithTag(PICKER_ROW).assertCountEquals(1)
        compose.onNodeWithText("HDFC Bank Credit").assertExists()
    }

    @Test
    fun searchingTheLastFourNarrows() {
        show(); search("4444")
        compose.onAllNodesWithTag(PICKER_ROW).assertCountEquals(1)
    }

    @Test
    fun noMatchShowsTheEmptyState() {
        show(); search("zzzz")
        compose.onAllNodesWithTag(PICKER_ROW).assertCountEquals(0)
        compose.onNodeWithText("No cards match").assertExists()
    }

    @Test
    fun tappingACardPicksIt() {
        var picked: String? = null
        show { picked = it }
        compose.onNodeWithText("ICICI Bank").performClick()
        assertEquals("2", picked)
    }
}
