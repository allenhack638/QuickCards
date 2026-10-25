package com.quickcards.app.feature.cardform

import com.quickcards.domain.DefaultNetworks
import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SmartPasteTest {

    private val today = YearMonth.of(2026, 10)

    private fun paste(
        previous: String,
        incoming: String,
        field: SmartPasteField = SmartPasteField.NUMBER,
        expiryEmpty: Boolean = true
    ): SmartPasteResult = SmartPaste.apply(
        previous = previous,
        incoming = incoming,
        field = field,
        networks = DefaultNetworks.all,
        today = today,
        expiryEmpty = expiryEmpty
    )

    @Test
    fun multiLineScannedTextFillsNumberAndExpiry() {
        val result = paste(
            previous = "",
            incoming = "VISA\n4532 7781 0349 2211\nVALID THRU 12/29"
        )

        assertEquals("4532778103492211", result.number)
        assertEquals("12/29", result.expiry)
    }

    @Test
    fun numberWithSpacesOrDashesIsAccepted() {
        assertEquals("4532778103492211", paste("", "4532 7781 0349 2211").number)
        assertEquals("4532778103492211", paste("", "4532-7781-0349-2211").number)
    }

    @Test
    fun validThruTextPastedIntoExpiryFillsTheDate() {
        val result = paste("", "VALID THRU 12/29", field = SmartPasteField.EXPIRY)

        assertEquals("12/29", result.expiry)
        assertNull(result.number)
    }

    @Test
    fun junkTextFallsBackToTheDigitFilter() {
        assertEquals(SmartPasteResult(), paste("", "hello there"))
    }

    @Test
    fun typingOneCharacterAtATimeIsUnchanged() {
        assertEquals(SmartPasteResult(), paste("4532 7781 0349 221", "4532 7781 0349 2211"))
        assertEquals(SmartPasteResult(), paste("4532 7781 0349 221", "4532 7781 0349 22"))
        assertEquals(SmartPasteResult(), paste("4532 7781 0349 2211", "4532 77810 0349 2211"))
    }

    @Test
    fun pastingIntoAPartlyTypedNumberReplacesTheField() {
        val result = paste(
            previous = "4111",
            incoming = "VISA\n4532 7781 0349 2211"
        )

        assertEquals("4532778103492211", result.number)
    }

    @Test
    fun aFullReplacementOfTheSameLengthIsStillAPaste() {
        val result = paste(
            previous = "4111 1111 1111 1111",
            incoming = "4532 7781 0349 2211"
        )

        assertEquals("4532778103492211", result.number)
    }

    @Test
    fun amexFifteenDigitsAreRead() {
        val result = paste("", "3782 822463 10005")

        assertEquals("378282246310005", result.number)
    }

    @Test
    fun aPhoneNumberLineIsIgnored() {
        val result = paste(
            previous = "",
            incoming = "CUSTOMER CARE 1800 123 4567\n4532 7781 0349 2211"
        )

        assertEquals("4532778103492211", result.number)
        assertNull(result.expiry)
    }

    @Test
    fun aPhoneNumberOnlyFillsNothing() {
        assertEquals(SmartPasteResult(), paste("", "TEL +91 98765 43210"))
    }

    @Test
    fun aFilledExpiryIsNotOverwritten() {
        val result = paste(
            previous = "",
            incoming = "VISA 4532 7781 0349 2211 VALID THRU 12/29",
            expiryEmpty = false
        )

        assertEquals("4532778103492211", result.number)
        assertNull(result.expiry)
    }

    @Test
    fun aFourDigitExpiryYearIsNormalised() {
        val result = paste("", "EXP 12/2029", field = SmartPasteField.EXPIRY)

        assertEquals("12/29", result.expiry)
    }

    @Test
    fun neverReturnsACvv() {
        val result = paste("", "CVV 123\n4532 7781 0349 2211\nVALID THRU 12/29")

        assertEquals("4532778103492211", result.number)
        assertEquals("12/29", result.expiry)
    }
}
