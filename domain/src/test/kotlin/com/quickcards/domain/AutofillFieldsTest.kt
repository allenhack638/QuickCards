package com.quickcards.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AutofillFieldsTest {

    private fun kind(hints: List<String> = emptyList(), html: String? = null, id: String? = null, hint: String? = null) =
        AutofillFields.classify(hints, html, id, hint)

    @Test
    fun explicitHintsWin() {
        assertEquals(CardFieldKind.NUMBER, kind(hints = listOf("creditCardNumber"), id = "cvv"))
        assertEquals(CardFieldKind.EXPIRY_MONTH, kind(hints = listOf("creditCardExpirationMonth")))
        assertEquals(CardFieldKind.EXPIRY_YEAR, kind(hints = listOf("creditCardExpirationYear")))
        assertEquals(CardFieldKind.EXPIRY_DATE, kind(hints = listOf("creditCardExpirationDate")))
        assertEquals(CardFieldKind.CVV, kind(hints = listOf("creditCardSecurityCode")))
        assertEquals(CardFieldKind.NAME, kind(hints = listOf("name")))
    }

    @Test
    fun htmlAutocompleteTokensAreRecognised() {
        assertEquals(CardFieldKind.NUMBER, kind(html = "cc-number"))
        assertEquals(CardFieldKind.EXPIRY_DATE, kind(html = "cc-exp"))
        assertEquals(CardFieldKind.CVV, kind(html = "cc-csc"))
        assertEquals(CardFieldKind.NAME, kind(html = "cc-name"))
    }

    @Test
    fun namesAreAFallback() {
        assertEquals(CardFieldKind.NUMBER, kind(id = "card_number"))
        assertEquals(CardFieldKind.NUMBER, kind(hint = "Card no."))
        assertEquals(CardFieldKind.EXPIRY_MONTH, kind(id = "expMonth"))
        assertEquals(CardFieldKind.EXPIRY_YEAR, kind(id = "exp_yy"))
        assertEquals(CardFieldKind.EXPIRY_DATE, kind(hint = "Expiry (MM/YY)"))
        assertEquals(CardFieldKind.CVV, kind(hint = "CVV"))
        assertEquals(CardFieldKind.NAME, kind(id = "cardholder_name"))
    }

    @Test
    fun unrelatedFieldsAreLeftAlone() {
        assertNull(kind(id = "email"))
        assertNull(kind(hint = "Password"))
        assertNull(kind(id = "address_line_1"))
        assertNull(kind(hints = listOf("username")))
    }

    @Test
    fun valuesFollowTheFieldShape() {
        assertEquals("4532778103492211", AutofillFields.valueFor(CardFieldKind.NUMBER, "4532 7781 0349 2211", "12/29", "381", "AARAV MEHTA"))
        assertEquals("12", AutofillFields.valueFor(CardFieldKind.EXPIRY_MONTH, "", "12/29", "", ""))
        assertEquals("2029", AutofillFields.valueFor(CardFieldKind.EXPIRY_YEAR, "", "12/29", "", ""))
        assertEquals("29", AutofillFields.valueFor(CardFieldKind.EXPIRY_YEAR, "", "12/29", "", "", twoDigitYear = true))
        assertEquals("12/29", AutofillFields.valueFor(CardFieldKind.EXPIRY_DATE, "", "12/2029", "", ""))
        assertNull(AutofillFields.valueFor(CardFieldKind.CVV, "", "", "", ""))
    }
}
