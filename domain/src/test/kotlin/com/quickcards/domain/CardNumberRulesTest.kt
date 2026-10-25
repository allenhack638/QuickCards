package com.quickcards.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CardNumberRulesTest {

    private val networks = DefaultNetworks.all

    private fun network(id: String): CardNetwork = networks.first { it.id == id }

    private fun withCheckDigit(partial: String): String =
        partial + CardNumberRules.luhnCheckDigit(partial)

    private data class FormatRow(
        val id: String,
        val partial: String,
        val appendsCheckDigit: Boolean,
        val number: String,
        val shown: String,
        val masked: String
    )

    private val formatRows = listOf(
        FormatRow("visa", "411111111111111", true, "4111111111111111", "4111 1111 1111 1111", "•••• •••• •••• 1111"),
        FormatRow("mc", "555555555555444", true, "5555555555554444", "5555 5555 5555 4444", "•••• •••• •••• 4444"),
        FormatRow("amex", "37828224631000", true, "378282246310005", "3782 822463 10005", "•••• •••••• •0005"),
        FormatRow("rupay", "652188047712993", true, "6521880477129938", "6521 8804 7712 9938", "•••• •••• •••• 9938"),
        FormatRow("diners", "3622720627166", true, "36227206271667", "3622 720627 1667", "•••• •••••• 1667"),
        FormatRow("discover", "601111111111111", true, "6011111111111117", "6011 1111 1111 1117", "•••• •••• •••• 1117"),
        FormatRow("jcb", "353011133330000", true, "3530111333300000", "3530 1113 3330 0000", "•••• •••• •••• 0000"),
        FormatRow("maestro", "675964982643845", true, "6759649826438453", "6759 6498 2643 8453", "•••• •••• •••• 8453"),
        FormatRow("unionpay", "6212345678901232", false, "6212345678901232", "6212 3456 7890 1232", "•••• •••• •••• 1232"),
        FormatRow("mir", "220000000000000", true, "2200000000000004", "2200 0000 0000 0004", "•••• •••• •••• 0004")
    )

    @Test
    fun timelineFormatRowsMatchDetectionGroupingAndMasking() {
        for (row in formatRows) {
            val number = if (row.appendsCheckDigit) withCheckDigit(row.partial) else row.partial
            val detected = CardNumberRules.detect(number, networks)
            assertEquals(row.id, detected?.id)
            assertEquals(row.number, number)
            assertEquals(row.shown, CardNumberRules.format(number, detected, masked = false, typing = false))
            assertEquals(row.masked, CardNumberRules.format(number, detected, masked = true, typing = false))
        }
    }

    @Test
    fun routingOverlapsPickTheLongestPrefix() {
        assertEquals("rupay", CardNumberRules.detect("6521500000000000", networks)?.id)
        assertEquals("rupay", CardNumberRules.detect("6531490000000000", networks)?.id)
        assertEquals("discover", CardNumberRules.detect("6531500000000000", networks)?.id)
        assertEquals("discover", CardNumberRules.detect("6500000000000000", networks)?.id)
        assertEquals("jcb", CardNumberRules.detect("3530000000000000", networks)?.id)
        assertEquals("discover", CardNumberRules.detect("6011000000000000", networks)?.id)
        assertEquals("mc", CardNumberRules.detect("2221000000000000", networks)?.id)
        assertEquals("mir", CardNumberRules.detect("2200000000000000", networks)?.id)
        assertNull(CardNumberRules.detect("", networks))
        assertNull(CardNumberRules.detect("999999999999", networks))
    }

    @Test
    fun prefixScoresUseDigitLengthAndNumericRanges() {
        assertEquals(6, CardNumberRules.prefixScore("6521880000000000", "508500-508999,652150-653149"))
        assertEquals(2, CardNumberRules.prefixScore("6500000000000000", "6011,644-649,65"))
        assertEquals(4, CardNumberRules.prefixScore("2221000000000000", "51-55,2221-2720"))
        assertEquals(4, CardNumberRules.prefixScore("2200000000000000", "2200-2204"))
        assertEquals(4, CardNumberRules.prefixScore("6011000000000000", "6011,644-649,65"))
        assertEquals(0, CardNumberRules.prefixScore("", "4"))
        assertEquals(0, CardNumberRules.prefixScore("1234", "22"))
    }

    @Test
    fun typingUsesFourDigitGroupsUntilTheNumberSettles() {
        val visa = network("visa")
        assertEquals("4111 1111 1111 1", CardNumberRules.format("4111111111111", visa, masked = false, typing = true))
        assertEquals("4111 1111 11111", CardNumberRules.format("4111111111111", visa, masked = false, typing = false))
        assertEquals("•••• •••• •1111", CardNumberRules.format("4111111111111", visa, masked = true, typing = false))
        assertEquals("5555 5555 5555 4", CardNumberRules.format("5555555555554", network("mc"), masked = false, typing = true))
        assertEquals(
            "3622 7206 2716 67",
            CardNumberRules.format("36227206271667", network("diners"), masked = false, typing = true)
        )
        assertEquals(
            "6212 3456 7890 1234 567",
            CardNumberRules.format("6212345678901234567", network("unionpay"), masked = false, typing = true)
        )
    }

    @Test
    fun amexUsesFourSixFiveAndDinersFourSixFour() {
        val amex = network("amex")
        assertEquals("3782 822463 10005", CardNumberRules.format("378282246310005", amex, masked = false, typing = false))
        assertEquals("3782 8", CardNumberRules.format("37828", amex, masked = false, typing = true))
        assertEquals("•••• •••••• •0005", CardNumberRules.format("378282246310005", amex, masked = true, typing = false))

        val diners = network("diners")
        assertEquals("3622 720627 1667", CardNumberRules.format("36227206271667", diners, masked = false, typing = false))
        assertEquals("•••• •••••• 1667", CardNumberRules.format("36227206271667", diners, masked = true, typing = false))
    }

    @Test
    fun visaNineteenDigitsUseFourFourFourFourThree() {
        val number = withCheckDigit("411111111111111111")
        assertEquals(19, number.length)
        assertEquals("4111 1111 1111 1111 110", CardNumberRules.format(number, network("visa"), masked = false, typing = false))
        assertEquals("•••• •••• •••• •••1 110", CardNumberRules.format(number, network("visa"), masked = true, typing = false))
    }

    @Test
    fun unknownThirteenDigitsUseFourFourFive() {
        assertEquals("1234 5678 90123", CardNumberRules.format("1234567890123", null, masked = false, typing = false))
        assertEquals("•••• •••• •0123", CardNumberRules.format("1234567890123", null, masked = true, typing = false))
        assertEquals("", CardNumberRules.format("", null, masked = false, typing = false))
    }

    @Test
    fun lengthSpecsExpandAndReadAsSentences() {
        assertEquals(listOf(13, 16, 19), CardNumberRules.parseLengths("13,16,19"))
        assertEquals(listOf(14, 15, 16, 17, 18, 19), CardNumberRules.parseLengths("14-19"))
        assertEquals(listOf(16), CardNumberRules.parseLengths(" 16 "))
        assertEquals(emptyList<Int>(), CardNumberRules.parseLengths(""))
        assertEquals(emptyList<Int>(), CardNumberRules.parseLengths("19-12"))
        assertEquals("13, 16 or 19", CardNumberRules.lengthsText("13,16,19"))
        assertEquals("16 to 19", CardNumberRules.lengthsText("16-19"))
        assertEquals("16", CardNumberRules.lengthsText("16"))
        assertEquals("12 to 19", CardNumberRules.lengthsText("12-19"))
        assertEquals("", CardNumberRules.lengthsText("nonsense"))
    }

    @Test
    fun maxLengthAndCvvLengthFollowTheNetwork() {
        assertEquals(15, CardNumberRules.maxLength(network("amex")))
        assertEquals(16, CardNumberRules.maxLength(network("mc")))
        assertEquals(19, CardNumberRules.maxLength(network("visa")))
        assertEquals(19, CardNumberRules.maxLength(null))
        assertEquals(4..4, CardNumberRules.cvvLength(network("amex")))
        assertEquals(3..3, CardNumberRules.cvvLength(network("visa")))
        assertEquals(3..4, CardNumberRules.cvvLength(null))
    }

    @Test
    fun groupsFallBackFromCustomGroupingToLength() {
        assertEquals(listOf(4, 6, 5), CardNumberRules.groupsFor(network("amex"), 15))
        assertEquals(listOf(4, 4, 4, 4), CardNumberRules.groupsFor(network("amex"), 16))
        assertEquals(listOf(4, 6, 4), CardNumberRules.groupsFor(network("diners"), 14))
        assertEquals(listOf(4, 4, 5), CardNumberRules.groupsFor(null, 13))
        assertNull(CardNumberRules.groupsFor(null, 11))
        assertNull(CardNumberRules.groupsFor(network("visa"), 20))
    }

    @Test
    fun luhnCheckDigitAndValidationMatchThePrototype() {
        assertEquals('1', CardNumberRules.luhnCheckDigit("411111111111111"))
        assertEquals("4111111111111111", withCheckDigit("411111111111111"))
        assertTrue(CardNumberRules.isLuhnValid("4111111111111111"))
        assertFalse(CardNumberRules.isLuhnValid("4111111111111112"))
        assertFalse(CardNumberRules.isLuhnValid("411111111111111"))
        assertFalse(CardNumberRules.isLuhnValid("0"))
        assertFalse(CardNumberRules.isLuhnValid(""))
        assertTrue(CardNumberRules.isLuhnValid("6212345678901232"))
        assertFalse(CardNumberRules.isLuhnValid("6212345678901233"))
        assertFalse(CardNumberRules.isLuhnValid("621234567890123X"))
    }
}
