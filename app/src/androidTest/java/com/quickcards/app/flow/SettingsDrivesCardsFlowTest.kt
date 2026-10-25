package com.quickcards.app.flow

import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isDialog
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quickcards.domain.ExpiredPlacement
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/** Card list settings reach the Cards screen on return, without a restart. */
@RunWith(AndroidJUnit4::class)
class SettingsDrivesCardsFlowTest : FlowTestBase() {

    @Test
    fun expiredPlacementHidesAndRestoresTheListLive() {
        val holder = runBlocking { app.repository.createHolder("Aarav Mehta") }
        val bank = runBlocking { app.repository.createBank("HDFC Bank") }
        seedCard("4532778103492211", holder.id, bank.id, expiry = "01/20")
        waitForContentDescription("More for HDFC Bank")

        openCardListSection()
        choose("Expired cards", "Hide from the list")
        awaitSetting { app.settings.expiredCards.value == ExpiredPlacement.HIDE }

        openCards()
        waitForText("Expired cards are hidden")

        openSettingsTab()
        choose("Expired cards", "Keep in place")
        awaitSetting { app.settings.expiredCards.value == ExpiredPlacement.KEEP }

        openCards()
        waitForContentDescription("More for HDFC Bank")
    }

    @Test
    fun expiresSoonWindowRelabelsThePassLive() {
        val holder = runBlocking { app.repository.createHolder("Aarav Mehta") }
        val bank = runBlocking { app.repository.createBank("HDFC Bank") }
        val inTwoMonths = YearMonth.now().plusMonths(2)
        val expiry = inTwoMonths.format(DateTimeFormatter.ofPattern("MM/yy"))
        seedCard("4532778103492211", holder.id, bank.id, expiry = expiry)
        waitForContentDescription("More for HDFC Bank")

        openCardListSection()
        choose("Expires soon", "Within 3 months")
        awaitSetting { app.settings.expiresSoonMonths.value == 3 }

        openCards()
        waitForText("EXPIRES SOON", unmerged = true)

        openSettingsTab()
        choose("Expires soon", "Within 1 month")
        awaitSetting { app.settings.expiresSoonMonths.value == 1 }

        openCards()
        waitForTextGone("EXPIRES SOON", unmerged = true)
        waitForContentDescription("More for HDFC Bank")
    }

    @Test
    fun archivedCardsAppearInMainSearchWithTheArchivedLabel() {
        val holder = runBlocking { app.repository.createHolder("Aarav Mehta") }
        val active = runBlocking { app.repository.createBank("HDFC Bank") }
        val closed = runBlocking { app.repository.createBank("Closed Bank") }
        seedCard("4532778103492211", holder.id, active.id, sortOrder = 0)
        val archived = seedCard("5555555555554444", holder.id, closed.id, sortOrder = 1)
        runBlocking { app.repository.archive(archived.id, "Card closed") }
        waitForContentDescription("More for HDFC Bank")

        compose.onNodeWithContentDescription("Search").performClick()
        compose.onNodeWithContentDescription("Search cards").performTextInput("Closed")
        waitForContentDescription("More for Closed Bank")
        waitForText("ARCHIVED · CARD CLOSED", unmerged = true)
    }

    @Test
    fun showNotesSwitchUpdatesThePassLive() {
        val holder = runBlocking { app.repository.createHolder("Aarav Mehta") }
        val bank = runBlocking { app.repository.createBank("HDFC Bank") }
        seedCard("4532778103492211", holder.id, bank.id, note = "Renewal due in June")
        waitForText("Renewal due in June")

        openCardListSection()
        compose.onNode(hasText("Show notes on cards") and isToggleable())
            .performScrollTo()
            .performClick()
        awaitSetting { !app.settings.showNotes.value }

        openCards()
        waitForTextGone("Renewal due in June")
    }

    private fun openSettingsTab() {
        compose.onNode(hasText("Settings") and isSelectable()).performClick()
        compose.waitForIdle()
    }

    private fun openCards() {
        compose.onNode(hasText("Cards") and isSelectable()).performClick()
        compose.waitForIdle()
    }

    private fun openCardListSection() {
        openSettingsTab()
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasScrollAction()).fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNode(hasScrollAction()).performScrollToNode(hasText("Expired cards"))
    }

    private fun choose(row: String, option: String) {
        compose.onNode(hasScrollAction()).performScrollToNode(hasText(row))
        compose.onNodeWithText(row).performClick()
        waitForText(option)
        compose.onNodeWithText(option).performClick()
        compose.onNodeWithText("OK").performClick()
        compose.waitUntil(10_000) {
            compose.onAllNodes(isDialog()).fetchSemanticsNodes().isEmpty()
        }
    }

    private fun awaitSetting(predicate: () -> Boolean) {
        compose.waitUntil(10_000, condition = predicate)
    }
}
