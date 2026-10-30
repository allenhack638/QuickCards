package com.quickcards.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Test
import java.time.YearMonth

class CardValidatorTest {

    private val networks = DefaultNetworks.all
    private val today = YearMonth.of(2026, 10)

    private fun draft(
        holderId: String? = "h1",
        number: String = "4111111111111111",
        expiry: String = "12/29",
        cvv: String = "123",
        bankId: String? = "b1",
        typeId: String? = null,
        tagIds: List<String> = emptyList(),
        note: String = ""
    ): CardDraft = CardDraft(
        holderId = holderId,
        number = number,
        expiry = expiry,
        cvv = cvv,
        bankId = bankId,
        typeId = typeId,
        tagIds = tagIds,
        note = note
    )

    private fun errors(
        draft: CardDraft,
        existing: (String) -> ExistingCardRef? = { null }
    ): Map<Field, CardError> = CardValidator.validate(
        draft = draft,
        networks = networks,
        today = today,
        existing = existing
    )

    private fun unknownNumber(length: Int): String {
        val partial = "9".repeat(length - 1)
        return partial + CardNumberRules.luhnCheckDigit(partial)
    }

    @Test
    fun completeDraftHasNoErrors() {
        assertEquals(emptyMap<Field, CardError>(), errors(draft()))
        assertEquals(emptyMap<Field, CardError>(), errors(draft(number = "378282246310005", cvv = "4821")))
    }

    @Test
    fun holderAndBankAreRequired() {
        assertEquals(
            CardError.HolderRequired,
            errors(draft(holderId = null))[Field.HOLDER]
        )
        assertEquals(
            CardError.HolderRequired,
            errors(draft(holderId = ""))[Field.HOLDER]
        )
        assertEquals(CardError.BankRequired, errors(draft(bankId = null))[Field.BANK])
        assertEquals(CardError.BankRequired, errors(draft(bankId = ""))[Field.BANK])
    }

    @Test
    fun cardNumberIsRequired() {
        val result = errors(draft(number = ""))
        assertEquals(CardError.NumberRequired, result[Field.NUMBER])
    }

    @Test
    fun numberLengthFollowsTheDetectedNetwork() {
        assertEquals(
            CardError.NetworkLength("Visa", "13, 16 or 19"),
            errors(draft(number = "41111111111111"))[Field.NUMBER]
        )
        assertEquals(
            CardError.NetworkLength("Mastercard", "16"),
            errors(draft(number = "555555555555444"))[Field.NUMBER]
        )
        assertEquals(
            CardError.NetworkLength("American Express", "15"),
            errors(draft(number = "37828224631000"))[Field.NUMBER]
        )
        assertEquals(
            CardError.NetworkLength("Diners Club", "14 to 19"),
            errors(draft(number = "3622720627166"))[Field.NUMBER]
        )
    }

    @Test
    fun unknownNumbersAllowTwelveToNineteenDigits() {
        assertEquals(
            CardError.NumberLength,
            errors(draft(number = "12345678901"))[Field.NUMBER]
        )
        assertEquals(
            CardError.NumberLength,
            errors(draft(number = "12345678901234567890"))[Field.NUMBER]
        )
        assertFalse(errors(draft(number = unknownNumber(12))).containsKey(Field.NUMBER))
        assertFalse(errors(draft(number = unknownNumber(19))).containsKey(Field.NUMBER))
        assertEquals(emptyMap<Field, CardError>(), errors(draft(number = unknownNumber(12))))
    }

    @Test
    fun luhnFailureIsReported() {
        assertEquals(
            CardError.Luhn,
            errors(draft(number = "4111111111111112"))[Field.NUMBER]
        )
    }

    @Test
    fun unionPayDoesNotRequireLuhn() {
        val unionPay = "6212345678901233"
        assertFalse(CardNumberRules.isLuhnValid(unionPay))
        assertFalse(errors(draft(number = unionPay)).containsKey(Field.NUMBER))
    }

    @Test
    fun duplicateNumberNamesTheCardAndHolder() {
        val duplicate: (String) -> ExistingCardRef? = { number ->
            if (number == "4111111111111111") {
                ExistingCardRef("HDFC Bank", "Aarav Mehta")
            } else {
                null
            }
        }
        assertEquals(
            CardError.Duplicate("HDFC Bank", "Aarav Mehta"),
            errors(draft(), duplicate)[Field.NUMBER]
        )
        assertEquals(
            CardError.Duplicate("HDFC Bank", null),
            errors(draft(), { ExistingCardRef("HDFC Bank") })[Field.NUMBER]
        )
        assertEquals(
            CardError.Duplicate("HDFC Bank", null),
            errors(draft(), { ExistingCardRef("HDFC Bank", "") })[Field.NUMBER]
        )
        assertNull(errors(draft(number = "378282246310005", cvv = "4821"), duplicate)[Field.NUMBER])
    }

    @Test
    fun expiredAndFarFutureExpiryUseTheRulesCopy() {
        for (expiry in listOf("", "1/29", "13/29", "00/29", "12-29", "1229", "12/2029")) {
            assertEquals(CardError.ExpiryFormat, errors(draft(expiry = expiry))[Field.EXPIRY])
        }
        assertNull(errors(draft(expiry = "10/26"))[Field.EXPIRY])
        assertNull(errors(draft(expiry = "11/26"))[Field.EXPIRY])
        assertEquals(CardError.Expired, errors(draft(expiry = "09/26"))[Field.EXPIRY])
        assertEquals(CardError.Expired, errors(draft(expiry = "12/25"))[Field.EXPIRY])
        assertNull(errors(draft(expiry = "01/46"))[Field.EXPIRY])
        assertNull(errors(draft(expiry = "12/46"))[Field.EXPIRY])
        assertEquals(CardError.ExpiryYear, errors(draft(expiry = "01/47"))[Field.EXPIRY])
        assertEquals(CardError.ExpiryYear, errors(draft(expiry = "12/47"))[Field.EXPIRY])
    }

    @Test
    fun cvvLengthFollowsTheNetwork() {
        assertEquals(
            CardError.CvvLength(4),
            errors(draft(number = "378282246310005", cvv = "123"))[Field.CVV]
        )
        assertNull(errors(draft(number = "378282246310005", cvv = "4821"))[Field.CVV])
        assertEquals(CardError.CvvLength(3), errors(draft(cvv = "1234"))[Field.CVV])
        assertNull(errors(draft(cvv = "123"))[Field.CVV])
    }

    @Test
    fun maestroCvvMayBeBlankButNotShort() {
        assertNull(errors(draft(number = "6759649826438453", cvv = ""))[Field.CVV])
        assertNull(errors(draft(number = "6759649826438453", cvv = "123"))[Field.CVV])
        assertEquals(
            CardError.CvvLength(3),
            errors(draft(number = "6759649826438453", cvv = "12"))[Field.CVV]
        )
        assertEquals(CardError.CvvLength(3), errors(draft(cvv = ""))[Field.CVV])
    }

    @Test
    fun unknownNetworkAcceptsThreeOrFourCvvDigits() {
        val unknown = unknownNumber(12)
        assertEquals(CardError.CvvUnknown, errors(draft(number = unknown, cvv = "12"))[Field.CVV])
        assertEquals(CardError.CvvUnknown, errors(draft(number = unknown, cvv = "12345"))[Field.CVV])
        assertNull(errors(draft(number = unknown, cvv = "123"))[Field.CVV])
        assertNull(errors(draft(number = unknown, cvv = "1234"))[Field.CVV])
    }

    @Test
    fun normalizeExpiryBuildsMonthSlashYear() {
        assertEquals("", CardValidator.normalizeExpiry(""))
        assertEquals("1", CardValidator.normalizeExpiry("1"))
        assertEquals("0", CardValidator.normalizeExpiry("0"))
        assertEquals("02", CardValidator.normalizeExpiry("2"))
        assertEquals("09", CardValidator.normalizeExpiry("9"))
        assertEquals("12", CardValidator.normalizeExpiry("12"))
        assertEquals("12/3", CardValidator.normalizeExpiry("123"))
        assertEquals("12/29", CardValidator.normalizeExpiry("1229"))
        assertEquals("12/29", CardValidator.normalizeExpiry("12/29"))
        assertEquals("12/29", CardValidator.normalizeExpiry("122029"))
        assertEquals("12/29", CardValidator.normalizeExpiry("12/2029"))
        assertEquals("13/29", CardValidator.normalizeExpiry("13/29"))
    }

    @Test
    fun documentedFieldLimitsAreExposed() {
        assertEquals(80, CardValidator.NOTE_MAX_LENGTH)
    }
}
