package com.quickcards.app.feature.cardform

import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.domain.CardError
import com.quickcards.domain.CardNumberRules
import com.quickcards.domain.DefaultNetworks
import com.quickcards.domain.Field
import com.quickcards.domain.NamedRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

/** Daylight form rules: validate as you type, the Save line, Expired as a warning, card colour. */
class CardFormDaylightTest {

    private val today = YearMonth.of(2026, 11)
    private val visa = "453277810349221" + CardNumberRules.luhnCheckDigit("453277810349221")

    private val lists = CardFormLists(
        holders = listOf(NamedRef("h1", "Aarav Mehta")),
        banks = listOf(NamedRef("b1", "HDFC Bank"), NamedRef("b2", "ICICI Bank")),
        bankColours = mapOf("b1" to "navy", "b2" to "rust"),
        networks = DefaultNetworks.all,
        types = listOf(NamedRef("t1", "Credit"))
    )

    private fun empty() = CardFormSession.forAdd(lists.copy(holders = emptyList()), CardFormOptions(), today)
        .withLists(lists.copy(holders = emptyList()), CardFormOptions())

    private fun add() = CardFormSession.forAdd(lists, CardFormOptions(), today)

    private fun complete() = add().updateNumber(visa).updateExpiry("1229").updateCvv("123")
        .selectBank("b1").selectType("t1")

    @Test
    fun saveNamesTheFirstMissingItemInFormOrder() {
        assertEquals(SaveIssue.HOLDER, empty().saveIssue())
        var state = add()
        assertEquals(SaveIssue.NUMBER_MISSING, state.saveIssue())
        state = state.updateNumber("4111")
        assertEquals(SaveIssue.NUMBER_BAD, state.saveIssue())
        state = state.updateNumber(visa)
        assertEquals(SaveIssue.EXPIRY_MISSING, state.saveIssue())
        state = state.updateExpiry("13")
        assertEquals(SaveIssue.EXPIRY_BAD, state.saveIssue())
        state = state.updateExpiry("1229")
        assertEquals(SaveIssue.CVV_MISSING, state.saveIssue())
        state = state.updateCvv("12")
        assertEquals(SaveIssue.CVV_MISSING, state.saveIssue())
        state = state.updateCvv("123")
        assertEquals(SaveIssue.BANK, state.saveIssue())
        state = state.selectBank("b1")
        assertEquals(SaveIssue.TYPE, state.saveIssue())
        state = state.selectType("t1")
        assertNull(state.saveIssue())
        assertTrue(state.canSave())
    }

    @Test
    fun aCompleteNumberIsCheckedWhileTyping() {
        val bad = visa.dropLast(1) + ((visa.last().digitToInt() + 1) % 10)
        val state = add().updateNumber(bad)
        assertEquals(CardError.Luhn, state.form.errors[Field.NUMBER])
        assertNull(add().updateNumber(visa.take(12)).form.errors[Field.NUMBER])
    }

    @Test
    fun aMonthOverTwelveSaysMonthOneToTwelve() {
        val state = add().updateExpiry("1428")
        assertEquals(CardError.ExpiryMonth, state.form.errors[Field.EXPIRY])
    }

    @Test
    fun aPastExpiryWarnsButNeverBlocksSave() {
        val state = complete().updateExpiry("0926")
        assertEquals(CardError.Expired, state.form.errors[Field.EXPIRY])
        assertTrue(state.expiryWarning)
        assertNull(state.saveIssue())
        assertNull(state.saveAttempt().firstInvalid())
    }

    @Test
    fun aShortCvvOnlyErrorsWhenLeft() {
        val state = complete().updateCvv("12")
        assertNull(state.form.errors[Field.CVV])
        assertEquals(CardError.CvvLength(3), state.cvvBlur().form.errors[Field.CVV])
    }

    @Test
    fun theEmptyFormIsGraphiteAndPickingABankAppliesItsColour() {
        val state = add()
        assertEquals("graphite", state.form.bandColor)
        val picked = state.selectBank("b2")
        assertEquals("rust", picked.form.bandColor)
        assertFalse(picked.form.bandColorManual)
        assertEquals("navy", picked.selectBank("b1").form.bandColor)
    }

    @Test
    fun aColourChosenByHandSurvivesABankChange() {
        val state = add().selectBank("b1").selectBand("plum")
        assertTrue(state.form.bandColorManual)
        val moved = state.selectBank("b2")
        assertEquals("plum", moved.form.bandColor)
        assertTrue(moved.form.bandColorManual)
    }

    @Test
    fun pickingTheBanksOwnColourOrBankColourFollowsTheBankAgain() {
        val own = add().selectBank("b2").selectBand("plum").selectBand("rust")
        assertFalse(own.form.bandColorManual)
        val reset = add().selectBank("b2").selectBand("plum").followBankColour()
        assertEquals("rust", reset.form.bandColor)
        assertFalse(reset.form.bandColorManual)
    }

    @Test
    fun aRecolouredBankRepaintsCardsThatFollowIt() {
        val follow = add().selectBank("b1")
        val manual = add().selectBank("b1").selectBand("plum")
        val recoloured = lists.copy(bankColours = mapOf("b1" to "teal", "b2" to "rust"))
        assertEquals("teal", follow.withLists(recoloured, CardFormOptions()).form.bandColor)
        assertEquals("plum", manual.withLists(recoloured, CardFormOptions()).form.bandColor)
    }

    @Test
    fun editSavesOnlyAfterARealChange() {
        val card = VaultCard(
            id = "c1", number = visa, expiry = "12/29", cvv = "123", holderId = "h1", bankId = "b1",
            typeId = "t1", bandColor = "navy"
        )
        val state = CardFormSession.forEdit(card, lists, CardFormOptions(), today)
        assertNull(state.saveIssue())
        assertFalse(state.canSave())
        assertTrue(state.updateExpiry("1230").canSave())
        assertFalse(state.updateExpiry("1230").updateExpiry("1229").canSave())
    }

    @Test
    fun theNewTagFieldBlocksEmptyAndDuplicateNames() {
        val tagged = add().copy(lists = add().lists.copy(tags = listOf(NamedRef("g1", "Fuel")))).addTag("g1")
        assertEquals(TagInput.EMPTY, tagInput("  ", tagged))
        assertEquals(TagInput.DUPLICATE, tagInput("fuel", tagged))
        assertEquals(TagInput.DUPLICATE, tagInput("#Fuel", tagged))
        assertEquals(TagInput.SHORT, tagInput("a", tagged))
        assertEquals(TagInput.OK, tagInput("Travel", tagged))
    }
}
