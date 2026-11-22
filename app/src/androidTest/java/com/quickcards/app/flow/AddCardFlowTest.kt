package com.quickcards.app.flow

import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
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

/** Add, reject bad Luhn, block a duplicate PAN, follow network CVV length. */
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

        awaitTile("HDFC Bank")
        compose.onNodeWithContentDescription("Copy HDFC Bank card number ending 2211").assertExists()
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

        waitForText("Check the digits")
        compose.onNode(hasText("Save card") and hasClickAction()).assertIsNotEnabled()
        assertTrue(cardsNow().isEmpty())
    }

    @Test
    fun duplicatePanIsBlockedWithTheExistingCardsBankAndHolder() {
        val holder = runBlockingSeed { app.repository.createHolder("Aarav Mehta") }
        val bank = runBlockingSeed { app.repository.createBank("HDFC Bank") }
        seedCard(visa, holderId = holder.id, bankId = bank.id)

        awaitTile("HDFC Bank")
        compose.onNodeWithContentDescription("Add card").performClick()
        waitForText("Save card")

        typeNumber(visa)
        field("Expiry").performTextInput("1229")
        field("CVV").performTextInput("381")
        selectBank("HDFC Bank")

        waitForText("Already saved as HDFC Bank •• 2211")
        compose.onNode(hasText("Save card") and hasClickAction()).assertIsNotEnabled()
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
        field("CVV").performTextInput("48211")
        assertEquals("••••", editableText("CVV"))

        field("Card number").performTextClearance()
        typeNumber(mastercard)
        assertEquals("•••", editableText("CVV"))

        field("Expiry").performTextInput("1229")
        selectBank("HDFC Bank")
        selectType("Credit")
        compose.onNodeWithText("Save card").performClick()

        val saved = awaitCards(1).single()
        assertEquals(mastercard, saved.number)
        assertEquals("482", saved.cvv)
        assertEquals(holder.id, saved.holderId)
        assertEquals(bank.id, saved.bankId)
    }

    @Test
    fun keyboardNextWalksTheTextFieldsAndTheNoteEndsWithDone() {
        runBlockingSeed { app.repository.createHolder("Aarav Mehta") }

        compose.waitUntil(10_000) {
            compose.onAllNodesWithText("Add your first card").fetchSemanticsNodes().isNotEmpty()
        }
        compose.onNodeWithText("Add card").performClick()
        waitForText("Save card")

        val chain = listOf("Card number", "Expiry", "CVV", "Due day (optional)", "Note (optional)")
        chain.zipWithNext().forEach { (from, to) ->
            assertEquals(ImeAction.Next, field(from).fetchSemanticsNode().config[SemanticsProperties.ImeAction])
            field(from).performClick().performImeAction()
            field(to).assertIsFocused()
        }
        assertEquals(ImeAction.Done, field("Note (optional)").fetchSemanticsNode().config[SemanticsProperties.ImeAction])
        field("Note (optional)").performImeAction()
        field("Note (optional)").assertIsNotFocused()
    }

    /** One character per event so the field sees typing, not a paste. */
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
        waitForContentDescription("Search banks")
        compose.onNodeWithText(name).performClick()
        awaitLookup("Bank", name)
    }

    private fun selectType(name: String) {
        compose.onNodeWithContentDescription("Type")
            .performScrollTo()
            .performClick()
        waitForContentDescription("Search types")
        compose.onNodeWithText(name).performClick()
        awaitLookup("Type", name)
    }

    private fun <T> runBlockingSeed(block: suspend () -> T): T =
        kotlinx.coroutines.runBlocking { block() }
}
