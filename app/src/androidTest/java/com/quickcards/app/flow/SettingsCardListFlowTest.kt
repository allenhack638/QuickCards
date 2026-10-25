package com.quickcards.app.flow

import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quickcards.app.core.data.settings.AppSettingsDefaults
import com.quickcards.domain.ArchivedVisibility
import com.quickcards.domain.ExpiredPlacement
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The Card list section writes its four settings through
 * the same radio-dialog and switch rows as the rest of Settings.
 */
@RunWith(AndroidJUnit4::class)
class SettingsCardListFlowTest : FlowTestBase() {

    @Before
    fun resetCardListSettings() {
        app.settings.setExpiredCards(AppSettingsDefaults.EXPIRED_CARDS)
        app.settings.setExpiresSoonMonths(AppSettingsDefaults.EXPIRES_SOON_MONTHS)
        app.settings.setArchivedCards(AppSettingsDefaults.ARCHIVED_CARDS)
        app.settings.setShowNotes(AppSettingsDefaults.SHOW_NOTES)
    }

    @Test
    fun cardListSectionPersistsEveryChoice() {
        openCardListSection()

        compose.onNodeWithText("Expired cards").performClick()
        waitForText("Keep in place")
        waitForText("Move to the end")
        compose.onNodeWithText("Hide from the list").performClick()
        compose.onNodeWithText("OK").performClick()
        awaitDialogClosed()
        compose.waitUntil(10_000) {
            app.settings.expiredCards.value == ExpiredPlacement.HIDE
        }
        waitForText("Hide from the list")

        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Expires soon"))
        compose.onNodeWithText("Expires soon").performClick()
        waitForText("Within 3 months")
        compose.onNodeWithText("Within 3 months").performClick()
        compose.onNodeWithText("OK").performClick()
        awaitDialogClosed()
        compose.waitUntil(10_000) { app.settings.expiresSoonMonths.value == 3 }

        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Archived cards"))
        compose.onNodeWithText("Archived cards").performClick()
        waitForText("Also in search results")
        compose.onNodeWithText("Also in search results").performClick()
        compose.onNodeWithText("OK").performClick()
        awaitDialogClosed()
        compose.waitUntil(10_000) {
            app.settings.archivedCards.value == ArchivedVisibility.IN_SEARCH
        }

        compose.onNode(hasScrollAction()).performScrollToNode(
            hasText("Show notes on cards")
        )
        compose.onNode(hasText("Show notes on cards") and isToggleable()).performClick()
        compose.waitUntil(10_000) { !app.settings.showNotes.value }
        assertFalse(app.settings.showNotes.value)
        assertEquals(3, app.settings.expiresSoonMonths.value)
    }

    private fun openCardListSection() {
        compose.onNode(hasText("Settings") and isSelectable()).performClick()
        compose.waitForIdle()
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasScrollAction()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Card list"))
        waitForText("Expired cards")
    }

    private fun awaitDialogClosed() {
        compose.waitUntil(10_000) {
            compose.onAllNodes(isDialog()).fetchSemanticsNodes().isEmpty()
        }
    }
}
