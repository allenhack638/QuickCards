package com.quickcards.app.feature.cardform

import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.domain.CardError
import com.quickcards.domain.CardField
import com.quickcards.domain.CardNumberRules
import com.quickcards.domain.DefaultNetworks
import com.quickcards.domain.ExistingCardRef
import com.quickcards.domain.Field
import com.quickcards.domain.NamedRef
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

/**
 * The editing rules live in CardFormSession so they can be exercised on the JVM.
 * CardFormViewModel only adapts Room/AppSettings to this state.
 */
class CardFormViewModelTest {

    private val today = YearMonth.of(2026, 10)

    private val holders = listOf(
        NamedRef("h1", "Aarav Mehta"),
        NamedRef("h2", "Priya Mehta")
    )
    private val banks = listOf(
        NamedRef("b1", "HDFC Bank"),
        NamedRef("b2", "ICICI Bank")
    )
    private val types = listOf(
        NamedRef("t1", "Credit"),
        NamedRef("t2", "Debit")
    )
    private val tags = listOf(
        NamedRef("g1", "Travel"),
        NamedRef("g2", "Online"),
        NamedRef("g3", "Fuel"),
        NamedRef("g4", "Bills")
    )

    private fun lists(
        duplicates: Map<String, ExistingCardRef> = emptyMap(),
        holders: List<NamedRef> = this.holders,
        banks: List<NamedRef> = this.banks
    ) = CardFormLists(
        holders = holders,
        banks = banks,
        networks = DefaultNetworks.all,
        types = types,
        tags = tags,
        tagLimit = 3,
        tagMaxLen = 16,
        duplicates = duplicates
    )

    private fun session(duplicates: Map<String, ExistingCardRef> = emptyMap()) =
        CardFormSession.forAdd(lists(duplicates), CardFormOptions(), today)

    private val visa = "453277810349221" + CardNumberRules.luhnCheckDigit("453277810349221")

    @Test
    fun addPrefillsFirstHolderWithoutANameField() {
        val state = session()

        assertEquals(CardFormMode.ADD, state.form.mode)
        assertEquals("h1", state.form.holderId)
        assertEquals("navy", state.form.bandColor)
        assertTrue(state.form.number.isEmpty())
        assertTrue(state.form.tagIds.isEmpty())
        assertTrue(state.form.errors.isEmpty())
        assertFalse(state.isDirty())
    }

    @Test
    fun editPrefillsEveryStoredField() {
        val card = VaultCard(
            id = "c1",
            number = visa,
            expiry = "12/29",
            cvv = "381",
            holderId = "h1",
            bankId = "b1",
            networkAuto = false,
            networkId = "visa",
            typeId = "t1",
            nameOnCard = "SOMEONE ELSE",
            cardName = "Regalia",
            note = "Everyday spending",
            bandColor = "navy",
            tagIds = listOf("g1", "g2")
        )

        val state = CardFormSession.forEdit(card, lists(), CardFormOptions(), today)

        assertEquals(CardFormMode.EDIT, state.form.mode)
        assertEquals("c1", state.form.editingId)
        assertEquals(visa, state.form.number)
        assertEquals("12/29", state.form.expiry)
        assertEquals("381", state.form.cvv)
        assertEquals("h1", state.form.holderId)
        assertEquals("b1", state.form.bankId)
        assertEquals("t1", state.form.typeId)
        assertEquals("Everyday spending", state.form.note)
        assertEquals("navy", state.form.bandColor)
        assertEquals(listOf("g1", "g2"), state.form.tagIds)
        assertEquals("Visa", state.form.network(state.lists.networks)?.name)
        assertFalse(state.isDirty())
    }

    @Test
    fun networkAlwaysFollowsTheNumberEvenWhenTheStoredCardWasManual() {
        val card = VaultCard(
            number = "378282246310005",
            holderId = "h1",
            bankId = "b1",
            networkAuto = false,
            networkId = "visa",
            cvv = "4821"
        )

        val state = CardFormSession.forEdit(card, lists(), CardFormOptions(), today)

        assertEquals("amex", state.form.network(state.lists.networks)?.id)
        assertEquals(4, state.form.cvvDigits(state.lists.networks))
        assertEquals("American Express", state.form.network(state.lists.networks)?.name)
    }

    @Test
    fun leavingAnUntouchedFieldShowsNoError() {
        val state = session()

        assertTrue(state.numberBlur().form.errors.isEmpty())
        assertTrue(state.expiryBlur().form.errors.isEmpty())
        assertTrue(state.cvvBlur().form.errors.isEmpty())
    }

    @Test
    fun aFieldWithTextValidatesWhenItIsLeft() {
        var state = session().updateExpiry("1")
        assertNull(state.form.errors[Field.EXPIRY])

        state = state.expiryBlur()
        assertEquals(CardError.ExpiryFormat, state.form.errors[Field.EXPIRY])

        state = state.updateExpiry("1229")
        assertEquals("12/29", state.form.expiry)
        assertNull(state.form.errors[Field.EXPIRY])
    }

    @Test
    fun saveAttemptShowsEveryRemainingRequiredErrorThenInputFixesThem() {
        var state = session().saveAttempt()

        assertTrue(state.form.tried)
        assertEquals(
            setOf(Field.NUMBER, Field.EXPIRY, Field.CVV, Field.BANK),
            state.form.errors.keys
        )
        assertEquals(CardError.NumberRequired, state.form.errors[Field.NUMBER])
        assertEquals(CardError.ExpiryFormat, state.form.errors[Field.EXPIRY])
        assertEquals(CardError.CvvUnknown, state.form.errors[Field.CVV])
        assertEquals(CardError.BankRequired, state.form.errors[Field.BANK])
        assertNull(state.form.errors[Field.HOLDER])
        assertEquals(Field.NUMBER, state.firstInvalid())

        state = state.updateNumber(visa).numberBlur()
        assertNull(state.form.errors[Field.NUMBER])

        state = state.updateExpiry("1229").updateCvv("381").selectBank("b1")
        assertNull(state.form.errors[Field.EXPIRY])
        assertNull(state.form.errors[Field.CVV])
        assertNull(state.form.errors[Field.BANK])
        assertNull(state.firstInvalid())
    }

    @Test
    fun untouchedEmptyFieldStaysCleanEvenAfterOtherFieldsWereTouched() {
        val state = session()
            .updateNumber(visa)
            .numberBlur()
            .updateExpiry("")
            .expiryBlur()

        assertNull(state.form.errors[Field.EXPIRY])
        assertNull(state.form.errors[Field.NUMBER])
    }

    @Test
    fun duplicateNumberIsNamedLikeThePrototype() {
        val duplicate = mapOf(
            visa to ExistingCardRef(title = "HDFC Bank", holderName = "Aarav Mehta")
        )
        val state = session(duplicate)
            .updateNumber(visa)
            .numberBlur()

        assertEquals(
            CardError.Duplicate("HDFC Bank", "Aarav Mehta"),
            state.form.errors[Field.NUMBER]
        )
    }

    @Test
    fun choosingAHolderOnlyChangesTheHolder() {
        var state = session()
        assertEquals("h1", state.form.holderId)

        state = state.selectHolder("h2")
        assertEquals("h2", state.form.holderId)
        assertTrue(state.isDirty())

        state = state.selectHolder("h1")
        assertFalse(state.isDirty())
    }

    /** A Room emission while the bank sheet is open must never replace the user's pick. */
    @Test
    fun selectedBankSurvivesAListEmission() {
        val state = session()
            .selectBank("b2")
            .withLists(
                lists(
                    banks = listOf(
                        NamedRef("b0", "American Express"),
                        NamedRef("b2", "ICICI Bank"),
                        NamedRef("b1", "HDFC Bank")
                    )
                ),
                CardFormOptions()
            )

        assertEquals("b2", state.form.bankId)
        assertEquals("ICICI Bank", state.lists.bankName(state.form.bankId))
        assertNull(state.form.errors[Field.BANK])
    }

    /**
     * The form can be created before Room's first emission; when holders arrive
     * it must adopt one (and stay clean) so a save is not silently blocked.
     */
    @Test
    fun formOpenedBeforeTheHolderListLoadedAdoptsTheFirstHolder() {
        val empty = CardFormSession.forAdd(
            lists(holders = emptyList()),
            CardFormOptions(),
            today
        )
        assertNull(empty.form.holderId)

        val loaded = empty.withLists(lists(), CardFormOptions())

        assertEquals("h1", loaded.form.holderId)
        assertFalse(loaded.isDirty())
        assertEquals(emptyMap<Field, CardError>(), loaded.saveAttempt().form.errors.filterKeys { it == Field.HOLDER })
    }

    @Test
    fun draftTracksOnlyEditableValues() {
        val state = session()
        assertFalse(state.isDirty())

        assertTrue(state.updateNumber(visa).isDirty())
        assertTrue(state.updateExpiry("12/29").isDirty())
        assertTrue(state.updateCvv("123").isDirty())
        assertTrue(state.selectBank("b1").isDirty())
        assertTrue(state.selectBand("rust").isDirty())
        assertTrue(state.updateNote("Everyday").isDirty())
        assertTrue(state.toggleTag("g1").isDirty())
    }

    @Test
    fun editIsCleanUntilAnEditableValueChanges() {
        val card = VaultCard(
            id = "c1",
            number = visa,
            expiry = "12/29",
            cvv = "381",
            holderId = "h1",
            bankId = "b1"
        )
        val state = CardFormSession.forEdit(card, lists(), CardFormOptions(), today)

        assertFalse(state.isDirty())
        assertEquals("b1", state.selectBank("b1").form.bankId)
        assertFalse(state.isDirty())
        assertTrue(state.selectBank("b2").isDirty())
        assertFalse(state.selectBank("b1").isDirty())
    }

    @Test
    fun tagLimitHidesFurtherSelectionAndRemovalAlwaysWorks() {
        var state = session()
            .toggleTag("g1")
            .toggleTag("g2")
            .toggleTag("g3")
        assertFalse(state.canAddTag())

        state = state.toggleTag("g4")
        assertEquals(listOf("g1", "g2", "g3"), state.form.tagIds)

        state = state.removeTag("g2")
        assertEquals(listOf("g1", "g3"), state.form.tagIds)
        assertTrue(state.canAddTag())
    }

    @Test
    fun numberInputKeepsDigitsOnlyAndRespectsTheNetworkLimit() {
        val state = session().updateNumber("3714 4963 5398 4331 extra")
        assertEquals("amex", state.form.network(state.lists.networks)?.id)
        assertEquals("371449635398433", state.form.number)

        val formatted = state.form.formattedNumber(state.lists.networks, typing = false)
        assertEquals("3714 496353 98433", formatted)
    }

    @Test
    fun expiryAcceptsSixDigitsAndNormalisesToMonthYear() {
        val state = session().updateExpiry("122029")
        assertEquals("12/29", state.form.expiry)
    }

    @Test
    fun cvvInputIsLimitedToTheNetworkLength() {
        val amex = session().updateNumber("3714496353984")
        assertEquals("1234", amex.updateCvv("12345").form.cvv)

        val visaState = session().updateNumber(visa)
        assertEquals("123", visaState.updateCvv("1234").form.cvv)
    }

    @Test
    fun changingTheNumberTrimsACvvThatIsNowTooLong() {
        val state = session().updateCvv("1234")
        assertEquals("1234", state.form.cvv)

        val visaState = state.updateNumber(visa)
        assertEquals("123", visaState.form.cvv)
    }

    @Test
    fun aReviewFieldOpensWithItsNormalValidationError() {
        val card = VaultCard(
            id = "legacy",
            number = visa,
            expiry = "11//22",
            cvv = "12a3",
            holderId = "h1",
            bankId = "b1",
            needsReview = setOf(CardField.EXPIRY, CardField.CVV)
        )

        val state = CardFormSession
            .forEdit(card, lists(), CardFormOptions(), today)
            .showReviewErrors(card.needsReview)

        assertEquals(CardError.ExpiryFormat, state.form.errors[Field.EXPIRY])
        assertEquals(CardError.CvvLength(3), state.form.errors[Field.CVV])
        assertNull(state.form.errors[Field.NUMBER])
        assertFalse(state.isDirty())
    }

    @Test
    fun undoingTheExpiryRepairRestoresTheOldValueAndFlagsIt() {
        val card = VaultCard(
            id = "legacy",
            number = visa,
            expiry = "11/22",
            cvv = "123",
            holderId = "h1",
            bankId = "b1"
        )
        val state = CardFormSession.forEdit(card, lists(), CardFormOptions(), today)

        val undone = state.restoreRepair(CardField.EXPIRY, "11//22")

        assertEquals("11//22", undone.form.expiry)
        assertEquals(CardError.ExpiryFormat, undone.form.errors[Field.EXPIRY])
        assertTrue(undone.isDirty())
    }

    @Test
    fun undoingTheNumberRepairKeepsTheFieldDigitsOnly() {
        val card = VaultCard(
            id = "legacy",
            number = visa,
            expiry = "12/29",
            cvv = "123",
            holderId = "h1",
            bankId = "b1"
        )
        val state = CardFormSession.forEdit(card, lists(), CardFormOptions(), today)

        val undone = state.restoreRepair(CardField.NUMBER, "4532 7781-0349 2211")

        assertEquals(visa, undone.form.number)
        assertNull(undone.form.errors[Field.NUMBER])
    }

    @Test
    fun fieldInputDropsPastedLettersAndExtraDigits() {
        assertEquals("1234", CardFieldInput.cvv("ab12xy345678", null))
        assertEquals(
            "3714 496353 98431",
            CardFieldInput.number(
                "ab3714xy49635398431zz",
                DefaultNetworks.all,
                typing = true
            )
        )
        assertEquals("12/29", CardFieldInput.expiry("ab12xy29"))
        assertEquals(80, CardFieldInput.note("9".repeat(100)).length)
    }
}
