package com.quickcards.app.flow

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Add a card, reject a bad Luhn digit, block a duplicate PAN and
 * follow the detected network's CVV length (Amex 4, Mastercard 3).
 */
@RunWith(AndroidJUnit4::class)
class AddCardFlowTest : FlowTestBase() {

    private val visa = "4532778103492211"
    private val amex = "378282246310005"
    private val mastercard = "5555555555554444"

    @Test
    fun addCardHappyPathSavesEncryptedCardAndShowsItOnHome() {
        val holder = runBlockingSeed { app.repository.createHolder("Aarav Mehta") }
        val bank = runBlockingSeed { app.repository.createBank("HDFC Bank") }

        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Add your first card").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Add card").performClick()
        waitForText("Save card")

        typeNumber(visa)
        field("Expiry").performTextInput("1229")
        field("CVV").performTextInput("381")
        selectBank("HDFC Bank")
        selectType("Credit")

        compose.onNodeWithText("Save card").performClick()

        val saved = awaitCards(1).single()
        assertEquals(visa, saved.number)
        assertEquals("12/29", saved.expiry)
        assertEquals("381", saved.cvv)
        assertEquals(holder.id, saved.holderId)
        assertEquals(bank.id, saved.bankId)
        assertTrue(saved.networkAuto)

        waitForContentDescription("More for HDFC Bank", substring = true)
        compose.onNodeWithContentDescription("Card number", substring = true)
            .assertExists()
        compose.onNodeWithText("HDFC Bank").assertExists()
    }

    @Test
    fun luhnErrorIsShownOnSaveAndNothingIsStored() {
        runBlockingSeed { app.repository.createHolder("Aarav Mehta") }
        runBlockingSeed { app.repository.createBank("HDFC Bank") }

        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Add your first card").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Add card").performClick()
        waitForText("Save card")

        typeNumber("4111111111111112")
        field("Expiry").performTextInput("1229")
        field("CVV").performTextInput("123")
        selectBank("HDFC Bank")

        compose.onNodeWithText("Save card").performClick()

        waitForText("This number doesn’t look right. Check the digits.")
        compose.onNodeWithText("This number doesn’t look right. Check the digits.")
            .assertExists()
        assertTrue(cardsNow().isEmpty())
    }

    @Test
    fun duplicatePanIsBlockedWithTheExistingCardsBankAndHolder() {
        val holder = runBlockingSeed { app.repository.createHolder("Aarav Mehta") }
        val bank = runBlockingSeed { app.repository.createBank("HDFC Bank") }
        seedCard(visa, holderId = holder.id, bankId = bank.id)

        waitForContentDescription("More for HDFC Bank")
        compose.onNodeWithContentDescription("Add card").performClick()
        waitForText("Save card")

        typeNumber(visa)
        field("Expiry").performTextInput("1229")
        field("CVV").performTextInput("381")
        selectBank("HDFC Bank")

        compose.onNodeWithText("Save card").performClick()

        waitForText("Already saved as HDFC Bank for Aarav Mehta")
        compose.onNodeWithText("Already saved as HDFC Bank for Aarav Mehta").assertExists()
        assertEquals(1, cardsNow().size)
    }

    @Test
    fun detectedNetworkChangesTheCvvLengthAndTrimsTheValue() {
        val holder = runBlockingSeed { app.repository.createHolder("Aarav Mehta") }
        val bank = runBlockingSeed { app.repository.createBank("HDFC Bank") }

        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Add your first card").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Add card").performClick()
        waitForText("Save card")

        typeNumber(amex)
        waitForText("4 digits")
        field("CVV").performTextInput("48211")
        assertEquals("••••", editableText("CVV"))

        field("Card number").performTextClearance()
        typeNumber(mastercard)
        waitForText("3 digits")
        assertEquals("•••", editableText("CVV"))

        field("Expiry").performTextInput("1229")
        selectBank("HDFC Bank")
        compose.onNodeWithText("Save card").performClick()

        val saved = awaitCards(1).single()
        assertEquals(mastercard, saved.number)
        assertEquals("482", saved.cvv)
        assertEquals(holder.id, saved.holderId)
        assertEquals(bank.id, saved.bankId)
    }

    /** One character per event, so the field sees typing rather than a paste. */
    private fun typeNumber(text: String) {
        text.forEach { digit -> field("Card number").performTextInput(digit.toString()) }
    }

    private fun editableText(label: String): String =
        field(label).fetchSemanticsNode()
            .config[SemanticsProperties.EditableText]
            .text

    private fun selectBank(name: String) {
        compose.onNodeWithContentDescription("Bank")
            .performScrollTo()
            .performClick()
        waitForText("Choose bank")
        compose.onNodeWithText(name).performClick()
        waitForContentDescription("Bank, $name")
    }

    private fun selectType(name: String) {
        compose.onNodeWithContentDescription("Card type (optional)")
            .performScrollTo()
            .performClick()
        waitForText("Choose card type")
        compose.onNodeWithText(name).performClick()
        waitForContentDescription("Card type (optional), $name")
    }

    private fun <T> runBlockingSeed(block: suspend () -> T): T =
        kotlinx.coroutines.runBlocking { block() }
}
