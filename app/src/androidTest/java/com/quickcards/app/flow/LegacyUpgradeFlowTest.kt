package com.quickcards.app.flow

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quickcards.app.core.data.repository.InMemoryRepairSummaryStore
import com.quickcards.app.core.data.repository.LegacyRepair
import com.quickcards.app.core.security.EncryptionHelper
import com.quickcards.app.core.security.PanHasher
import com.quickcards.domain.CardField
import com.quickcards.domain.ExpiredPlacement
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/** Upgrade repair fixes what it can prove, flags the rest, and the banner leads to those cards. */
@RunWith(AndroidJUnit4::class)
class LegacyUpgradeFlowTest : FlowTestBase() {

    @Test
    fun upgradeRepairFixesFormatsAndFlagsTheRest() {
        app.settings.setExpiredCards(ExpiredPlacement.KEEP)
        val holder = runBlocking { app.repository.createHolder("Aarav Mehta") }
        val bank = runBlocking { app.repository.createBank("HDFC Bank") }
        val expiryCard = seedCard("4111111111111111", holder.id, bank.id, sortOrder = 0)
        val cvvCard = seedCard("5555555555554444", holder.id, bank.id, sortOrder = 1)
        val numberCard = seedCard("6011111111111117", holder.id, bank.id, sortOrder = 2)

        corrupt(expiryCard.id, "4111111111111111", "11//22", "381")
        corrupt(cvvCard.id, "5555555555554444", "12/29", "12a3")
        corrupt(numberCard.id, "4532 7781-0349 2211", "12/29", "381")

        val summary = runBlocking {
            LegacyRepair.run(app.database, InMemoryRepairSummaryStore())
        }
        assertEquals(2, summary?.fixedCards)
        assertEquals(1, summary?.needsCheck)
        app.repairSummaryStore.record(summary!!)

        val repaired = runBlocking { app.repository.cards.first() }
        assertEquals("11/22", repaired.single { it.id == expiryCard.id }.expiry)
        assertEquals(
            "4532778103492211",
            repaired.single { it.id == numberCard.id }.number
        )
        val flagged = repaired.single { it.id == cvvCard.id }
        assertEquals("12a3", flagged.cvv)
        assertEquals(setOf(CardField.CVV), flagged.needsReview)

        waitForText("2 fixed · 1 to check")
        waitForContentDescription("HDFC Bank, ending 1111, Expired", substring = true)
        waitForContentDescription("HDFC Bank, ending 4444, Check details", substring = true)

        compose.onNodeWithText("Review").performClick()

        waitForText("Needs check (1)")
        compose.waitUntil(10_000) {
            compose.onAllNodes(hasContentDescription("HDFC Bank, ending", substring = true) and hasClickAction())
                .fetchSemanticsNodes().size == 1
        }
        assertTrue(
            compose.onAllNodesWithContentDescription("HDFC Bank, ending 4444", substring = true)
                .fetchSemanticsNodes().isNotEmpty()
        )
    }

    private fun corrupt(cardId: String, number: String, expiry: String, cvv: String) {
        runBlocking {
            val dao = app.database.cardDao()
            val entity = dao.getCardById(cardId) ?: error("card $cardId missing")
            val encryption = EncryptionHelper.getInstance()
            dao.updateCard(
                entity.copy(
                    cardNumber = encryption.encrypt(number),
                    expiryDate = encryption.encrypt(expiry),
                    cvv = encryption.encrypt(cvv),
                    panHash = PanHasher.hash(number)
                )
            )
        }
    }
}
