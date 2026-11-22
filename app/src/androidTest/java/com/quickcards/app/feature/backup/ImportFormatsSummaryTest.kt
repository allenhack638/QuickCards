package com.quickcards.app.feature.backup

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quickcards.app.core.ui.theme.QuickCardsTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** The import review's Formats row and its exact copy. */
@RunWith(AndroidJUnit4::class)
class ImportFormatsSummaryTest {

    @get:Rule
    val compose = createComposeRule()

    @Test
    fun showsBothCountsWithTheAgreedCopy() {
        compose.setContent {
            QuickCardsTheme(darkTheme = true) {
                ImportFormatsRow(formatsFixed = 12, needCheck = 3)
            }
        }

        compose.onNodeWithText("Formats").assertExists()
        compose.onNodeWithText("12 fixed · 3 to check").assertExists()
    }

    @Test
    fun hiddenWhenNothingChanged() {
        compose.setContent {
            QuickCardsTheme(darkTheme = true) {
                ImportFormatsRow(formatsFixed = 0, needCheck = 0)
            }
        }

        compose.onNodeWithText("Formats").assertDoesNotExist()
    }
}
