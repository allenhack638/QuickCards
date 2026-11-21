package com.quickcards.app.core.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.espresso.Espresso
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quickcards.app.core.ui.theme.QuickCardsTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The Colour sheet: card swatches, Bank colour, the Custom view and its 7:1 guard. */
@RunWith(AndroidJUnit4::class)
class BandColorPickerTest {

    @get:Rule
    val compose = createComposeRule()

    private fun showSheet(initial: String = "navy", bank: String? = "rust") {
        compose.setContent {
            QuickCardsTheme(darkTheme = true) {
                var selected by remember { mutableStateOf(initial) }
                Column {
                    Text(text = selected, modifier = Modifier.testTag("selected"))
                    CardColourSheet(
                        selected = selected,
                        bankColour = bank,
                        preview = ColourPreview("ICICI Bank"),
                        recent = emptyList(),
                        onPick = { selected = it },
                        onBankColour = { selected = bank.orEmpty() },
                        onDismiss = {}
                    )
                }
            }
        }
    }

    @Test
    fun aSwatchSelectsItsColour() {
        showSheet()
        compose.onNodeWithContentDescription("Plum").performClick()
        compose.onNodeWithTag("selected").assertTextEquals("plum")
    }

    @Test
    fun bankColourChipReturnsToTheBankColour() {
        showSheet(initial = "plum")
        compose.onNodeWithText("Bank colour").performClick()
        compose.onNodeWithTag("selected").assertTextEquals("rust")
    }

    @Test
    fun customHexBecomesTheCardColour() {
        showSheet()
        compose.onNodeWithContentDescription("Custom colour").performScrollTo().performClick()
        compose.onNodeWithTag("band_hex").performScrollTo().performTextClearance()
        compose.onNodeWithTag("band_hex").performTextInput("0F5E5A")
        compose.onNodeWithText("Use colour").performClick()
        compose.onNodeWithTag("selected").assertTextEquals("#0F5E5A")
    }

    @Test
    fun backFromCustomReturnsToTheSwatchGrid() {
        showSheet()
        compose.onNodeWithContentDescription("Custom colour").performScrollTo().performClick()
        compose.onNodeWithText("Use colour").assertExists()

        Espresso.pressBack()

        compose.onNodeWithText("Use colour").assertDoesNotExist()
        compose.onNodeWithContentDescription("Plum").assertExists()
    }

    @Test
    fun tooLightHexShowsTheGuardAndDisablesUse() {
        showSheet()
        compose.onNodeWithContentDescription("Custom colour").performScrollTo().performClick()
        compose.onNodeWithTag("band_hex").performScrollTo().performTextClearance()
        compose.onNodeWithTag("band_hex").performTextInput("FFFF00")
        compose.onNodeWithText("Too light. Pick a darker shade.").assertExists()
        compose.onNodeWithText("Use colour").assertIsNotEnabled()
    }
}
