package com.quickcards.app.flow

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quickcards.app.core.data.settings.AppSettingsDefaults
import com.quickcards.domain.ExpiredPlacement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The Settings › Cards group writes its list settings; each choice is a sheet that applies on tap. */
@RunWith(AndroidJUnit4::class)
class SettingsCardListFlowTest : FlowTestBase() {

    @Before
    fun resetCardListSettings() {
        app.settings.setExpiredCards(AppSettingsDefaults.EXPIRED_CARDS)
        app.settings.setExpiresSoonMonths(AppSettingsDefaults.EXPIRES_SOON_MONTHS)
        app.settings.setShowNotes(AppSettingsDefaults.SHOW_NOTES)
    }

    @Test
    fun appearanceNoLongerOffersSystemColours() {
        openCardsGroup()

        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Theme"))
        waitForText("Theme")
        compose.onAllNodes(hasText("Use system colours", substring = true)).assertCountEquals(0)
    }

    @Test
    fun cardsGroupPersistsEveryChoice() {
        openCardsGroup()

        compose.onNodeWithText("Expired cards").performScrollTo().performClick()
        waitForText("Keep in place")
        waitForText("At the end")
        compose.onNodeWithText("Hide from the list").performClick()
        awaitSheetClosed("At the end")
        compose.waitUntil(10_000) {
            app.settings.expiredCards.value == ExpiredPlacement.HIDE
        }
        waitForText("Hide from the list")

        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Expires soon"))
        compose.onNodeWithText("Expires soon").performClick()
        waitForText("Within 3 months")
        compose.onNodeWithText("Within 3 months").performClick()
        awaitSheetClosed("Within 2 months")
        compose.waitUntil(10_000) { app.settings.expiresSoonMonths.value == 3 }

        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Notes on cards"))
        compose.onNode(hasText("Notes on cards") and isToggleable()).performClick()
        compose.waitUntil(10_000) { !app.settings.showNotes.value }
        assertFalse(app.settings.showNotes.value)
        assertEquals(3, app.settings.expiresSoonMonths.value)
    }

    private fun openCardsGroup() {
        compose.onNode(hasText("Settings") and isSelectable()).performClick()
        compose.waitForIdle()
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasScrollAction()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Cards"))
        waitForText("Expired cards")
    }

    /** The sheet is gone once an option that is not the chosen value has left the screen. */
    private fun awaitSheetClosed(otherOption: String) {
        waitForTextGone(otherOption)
    }
}
