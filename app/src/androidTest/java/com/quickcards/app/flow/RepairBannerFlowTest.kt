package com.quickcards.app.flow

import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quickcards.app.core.data.repository.RepairSummary
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith

/** The one-time repair banner, Review filter and Dismiss. */
@RunWith(AndroidJUnit4::class)
class RepairBannerFlowTest : FlowTestBase() {

    private val bannerText =
        "Fixed the format of 12 cards. 3 need a quick check."

    @Test
    fun reviewShowsTheNeedsCheckChipAndHidesTheBanner() {
        val holder = runBlocking { app.repository.createHolder("Aarav Mehta") }
        val bank = runBlocking { app.repository.createBank("Kotak Mahindra Bank") }
        seedCard(
            "552199001137441",
            holder.id,
            bank.id,
            expiry = "13/22"
        )
        awaitCardsWhere("card needs a check") { list ->
            list.singleOrNull()?.needsReview?.isNotEmpty() == true
        }

        app.repairSummaryStore.record(RepairSummary(fixedCards = 12, needsCheck = 3))
        compose.waitUntil(10_000) {
            runBlocking { app.repository.repairSummary.first() } != null
        }
        awaitBanner(bannerText)

        compose.onNodeWithText("Review").performClick()

        waitForText("Needs check (1)")
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasText(bannerText)).fetchSemanticsNodes().isEmpty()
        }
        compose.waitUntil(10_000) {
            compose.onAllNodes(
                hasText("CHECK DETAILS", substring = true),
                useUnmergedTree = true
            ).fetchSemanticsNodes().isNotEmpty()
        }
    }

    @Test
    fun dismissClearsTheSummary() {
        val holder = runBlocking { app.repository.createHolder("Aarav Mehta") }
        seedCard("4532778103492211", holder.id)
        awaitCardsWhere("card seeded") { list -> list.size == 1 }

        app.repairSummaryStore.record(RepairSummary(fixedCards = 4, needsCheck = 0))
        awaitBanner()

        compose.onNodeWithText("Dismiss").performClick()

        compose.waitUntil(10_000) {
            compose.onAllNodes(hasText("Dismiss")).fetchSemanticsNodes().isEmpty()
        }
        assertNull(runBlocking { app.repairSummaryStore.summary.first() })
    }

    private fun awaitBanner(text: String = "Fixed the format of") {
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasText(text, substring = true))
                .fetchSemanticsNodes().isNotEmpty()
        }
    }
}
