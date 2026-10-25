package com.quickcards.domain

import java.time.YearMonth
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CardTextParserTest {

    private val today = YearMonth.of(2026, 10)

    private fun parse(vararg lines: String): ParsedCardText =
        CardTextParser.parse(lines.toList(), DefaultNetworks.all, today)

    @Test
    fun readsAVisaNumberPrintedInGroups() {
        val result = parse("4532 7781 0349 2211")
        assertEquals("4532778103492211", result.number)
        assertNull(result.expiry)
    }

    @Test
    fun readsAVisaNumberSeparatedByDashes() {
        assertEquals("4532778103492211", parse("4532-7781-0349-2211").number)
    }

    @Test
    fun readsAnAmexFourSixFiveNumber() {
        assertEquals("378282246310005", parse("3782 822463 10005").number)
    }

    @Test
    fun readsAFourteenDigitDinersNumber() {
        assertEquals("36227206271667", parse("3622 720627 1667").number)
    }

    @Test
    fun readsARuPayNumber() {
        assertEquals("6521500000000006", parse("6521 5000 0000 0006").number)
    }

    @Test
    fun readsANineteenDigitMaestroNumber() {
        assertEquals("5018000000000000007", parse("5018 0000 0000 0000 007").number)
    }

    @Test
    fun joinsEmbossedFragmentsSplitAcrossTwoLines() {
        val result = parse("4532 7781", "0349 2211")
        assertEquals("4532778103492211", result.number)
    }

    @Test
    fun acceptsUnionPayWithoutLuhn() {
        assertEquals("6212345678901234", parse("6212 3456 7890 1234").number)
    }

    @Test
    fun ignoresPhoneNumbersNextToTheirLabel() {
        val result = parse(
            "CUSTOMER CARE 1800 123 4567",
            "4532 7781 0349 2211"
        )
        assertEquals("4532778103492211", result.number)
    }

    @Test
    fun returnsNoNumberWhenTheCardOnlyShowsAPhoneNumber() {
        assertNull(parse("TEL +91 98765 43210").number)
    }

    @Test
    fun ignoresIFSCAndHelplineLines() {
        val result = parse(
            "IFSC HDFC0001234",
            "HELPLINE 022 1234 5678",
            "3782 822463 10005"
        )
        assertEquals("378282246310005", result.number)
    }

    @Test
    fun picksTheDateAfterValidThruWhenACardPrintsBothDates() {
        val result = parse("VALID FROM 03/24  VALID THRU 03/29")
        assertEquals("03/29", result.expiry)
    }

    @Test
    fun prefersTheDateAfterExpOverAnIssuedDate() {
        val result = parse("ISSUED 03/24  EXP 03/27  PRINTED 12/29")
        assertEquals("03/27", result.expiry)
    }

    @Test
    fun normalisesAFourDigitExpiryYear() {
        assertEquals("12/29", parse("EXP 12/2029").expiry)
    }

    @Test
    fun readsAnExpiryWithADash() {
        assertEquals("12/29", parse("EXP 12-29").expiry)
    }

    @Test
    fun rejectsAnExpiredDate() {
        assertNull(parse("EXP 03/24").expiry)
    }

    @Test
    fun rejectsADateMoreThanTwentyYearsAhead() {
        assertNull(parse("EXP 12/47").expiry)
    }

    @Test
    fun rejectsAnImpossibleMonth() {
        assertNull(parse("EXP 13/29").expiry)
    }

    @Test
    fun repairsOcrConfusionsInsideDigitGroups() {
        val result = parse("4S32 77B1 O349 22I1")
        assertEquals("4532778103492211", result.number)
    }

    @Test
    fun ignoresANumberThatFailsLuhn() {
        assertNull(parse("4532 7781 0349 2212").number)
    }

    @Test
    fun ignoresAnElevenDigitNumber() {
        assertNull(parse("4532 7781 034").number)
    }

    @Test
    fun neverExtractsACvvOrAHolderName() {
        val result = parse("AARAV MEHTA", "CVV 123", "4532 7781 0349 2211")
        assertEquals("4532778103492211", result.number)
        assertNull(result.expiry)
    }

    @Test
    fun prefersANumberThatMatchesAKnownNetwork() {
        val result = parse(
            "ACCOUNT 1 9999 9999 9999 9995",
            "ACCOUNT 2 4532 7781 0349 2211"
        )
        assertEquals("4532778103492211", result.number)
    }

    @Test
    fun readsANumberAndExpiryFromTheSameLine() {
        val result = parse("4532 7781 0349 2211 EXP 12/29")
        assertEquals("4532778103492211", result.number)
        assertEquals("12/29", result.expiry)
    }

    @Test
    fun readsANumberAndExpiryFromAFullCardBack() {
        val result = parse(
            "STATE BANK OF INDIA",
            "4532 7781 0349 2211",
            "VALID THRU 12/29",
            "AARAV MEHTA"
        )
        assertEquals("4532778103492211", result.number)
        assertEquals("12/29", result.expiry)
    }
}
