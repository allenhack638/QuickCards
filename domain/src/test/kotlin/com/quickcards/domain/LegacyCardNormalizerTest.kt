package com.quickcards.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LegacyCardNormalizerTest {

    private val networks = DefaultNetworks.all

    private val visa = "4111111111111111"
    private val amex = "378282246310005"
    private val unionPay = "6212345678901231"

    private val customNetwork = CardNetwork(
        id = "custom-1",
        name = "Custom",
        code = "CUSTOM",
        prefixes = "88",
        lengths = "16",
        cvvLength = 3,
        luhn = true,
        cvvOptional = true
    )
    private val customNumber = ("88" + "1".repeat(13))
        .let { partial -> partial + CardNumberRules.luhnCheckDigit(partial) }

    private fun normalize(
        number: String = visa,
        expiry: String = "12/29",
        cvv: String = "381"
    ) = LegacyCardNormalizer.normalize(number, expiry, cvv, networks)

    @Test
    fun `a clean card is left untouched`() {
        val result = normalize()

        assertEquals(visa, result.number)
        assertEquals("12/29", result.expiry)
        assertEquals("381", result.cvv)
        assertTrue(result.fixed.isEmpty())
        assertTrue(result.needsReview.isEmpty())
    }

    @Test
    fun `number separators are removed`() {
        listOf(
            "4111 1111 1111 1111",
            "4111-1111-1111-1111",
            "4111.1111.1111.1111",
            " 4111 1111 1111 1111 "
        ).forEach { raw ->
            val result = normalize(number = raw)

            assertEquals(visa, result.number)
            assertEquals(raw to visa, result.fixed[CardField.NUMBER])
            assertFalse(CardField.NUMBER in result.needsReview)
        }
    }

    @Test
    fun `OCR letters are repaired only inside a digit group`() {
        val result = normalize(number = "4111 1111 1111 11I1")

        assertEquals(visa, result.number)
        assertEquals("4111 1111 1111 11I1" to visa, result.fixed[CardField.NUMBER])
        assertTrue(result.fixed.containsKey(CardField.NUMBER))
    }

    @Test
    fun `a number that is too short needs review and keeps its value`() {
        val result = normalize(number = "4111 1111 111")

        assertEquals("4111 1111 111", result.number)
        assertFalse(result.fixed.containsKey(CardField.NUMBER))
        assertTrue(CardField.NUMBER in result.needsReview)
    }

    @Test
    fun `a number that fails Luhn needs review and keeps its value`() {
        val raw = "4111 1111 1111 1112"
        val result = normalize(number = raw)

        assertEquals(raw, result.number)
        assertFalse(result.fixed.containsKey(CardField.NUMBER))
        assertTrue(CardField.NUMBER in result.needsReview)
    }

    @Test
    fun `a number outside the detected network length needs review`() {
        val fourteen = "4532778103492" + CardNumberRules.luhnCheckDigit("4532778103492")
        assertEquals(14, fourteen.length)

        val result = normalize(number = fourteen)

        assertEquals(fourteen, result.number)
        assertTrue(CardField.NUMBER in result.needsReview)
    }

    @Test
    fun `UnionPay numbers skip Luhn`() {
        val raw = "6212 3456 7890 1231"
        assertEquals(unionPay, raw.filter { it in '0'..'9' })

        val result = normalize(number = raw)

        assertEquals(unionPay, result.number)
        assertEquals(raw to unionPay, result.fixed[CardField.NUMBER])
        assertTrue(result.needsReview.isEmpty())
    }

    @Test
    fun `every documented expiry form becomes month slash year`() {
        val forms = listOf(
            "11//22" to "11/22",
            "11-22" to "11/22",
            "11.22" to "11/22",
            "11 22" to "11/22",
            "1122" to "11/22",
            "11/2022" to "11/22",
            "112022" to "11/22",
            "202211" to "11/22",
            "11 / 22" to "11/22",
            "11-2022" to "11/22",
            "2022-11" to "11/22",
            "25/11" to "11/25",
            "1/23" to "01/23",
            " 11 // 22 " to "11/22"
        )

        forms.forEach { (raw, expected) ->
            val result = normalize(expiry = raw)

            assertEquals("failed for $raw", expected, result.expiry)
            assertEquals("failed for $raw", raw to expected, result.fixed[CardField.EXPIRY])
            assertFalse("failed for $raw", CardField.EXPIRY in result.needsReview)
        }
    }

    @Test
    fun `unparseable expiry needs review and keeps its value`() {
        listOf("13/22", "00/22", "", "11/2", "11-22-33", "expired").forEach { raw ->
            val result = normalize(expiry = raw)

            assertEquals(raw, result.expiry)
            assertFalse("failed for $raw", result.fixed.containsKey(CardField.EXPIRY))
            assertTrue("failed for $raw", CardField.EXPIRY in result.needsReview)
        }
    }

    @Test
    fun `a past expiry is valid`() {
        val result = normalize(expiry = "05/20")

        assertEquals("05/20", result.expiry)
        assertTrue(result.fixed.isEmpty())
        assertTrue(result.needsReview.isEmpty())
    }

    @Test
    fun `CVV spaces are removed`() {
        val result = normalize(cvv = " 12 3")

        assertEquals("123", result.cvv)
        assertEquals(" 12 3" to "123", result.fixed[CardField.CVV])
        assertFalse(CardField.CVV in result.needsReview)
    }

    @Test
    fun `a CVV with a letter needs review`() {
        val result = normalize(cvv = "12a3")

        assertEquals("12a3", result.cvv)
        assertFalse(result.fixed.containsKey(CardField.CVV))
        assertTrue(CardField.CVV in result.needsReview)
    }

    @Test
    fun `CVV length follows the detected network`() {
        val visaFour = normalize(cvv = "1234")
        assertEquals("1234", visaFour.cvv)
        assertTrue(CardField.CVV in visaFour.needsReview)

        val amexFour = normalize(number = amex, cvv = " 4821 ")
        assertEquals("4821", amexFour.cvv)
        assertEquals(" 4821 " to "4821", amexFour.fixed[CardField.CVV])

        val amexThree = normalize(number = amex, cvv = "482")
        assertEquals("482", amexThree.cvv)
        assertTrue(CardField.CVV in amexThree.needsReview)
    }

    @Test
    fun `an empty CVV needs review but is not an error`() {
        val result = normalize(cvv = "")

        assertEquals("", result.cvv)
        assertTrue(CardField.CVV in result.needsReview)
        assertFalse(result.fixed.containsKey(CardField.CVV))
    }

    @Test
    fun `normalizing twice changes nothing`() {
        val first = normalize(
            number = "4111 1111 1111 1111",
            expiry = "11//22",
            cvv = " 12 3"
        )

        assertEquals(3, first.fixed.size)
        assertTrue(first.needsReview.isEmpty())

        val second = LegacyCardNormalizer.normalize(
            first.number,
            first.expiry,
            first.cvv,
            networks
        )

        assertEquals(visa, second.number)
        assertEquals("11/22", second.expiry)
        assertEquals("123", second.cvv)
        assertTrue(second.fixed.isEmpty())
        assertTrue(second.needsReview.isEmpty())
    }

    @Test
    fun `field mask round trips`() {
        val fields = setOf(CardField.NUMBER, CardField.CVV)
        val mask = CardFieldMask.encode(fields)

        assertEquals(5, mask)
        assertEquals(fields, CardFieldMask.decode(mask))
        assertEquals(emptySet<CardField>(), CardFieldMask.decode(0))
    }

    @Test
    fun `a blank Maestro CVV is valid but a blank Visa CVV needs review`() {
        val maestro = normalize(number = "6759649826438453", cvv = "")
        assertTrue(maestro.needsReview.isEmpty())
        assertEquals("", maestro.cvv)

        val visa = normalize(cvv = "")
        assertEquals(setOf(CardField.CVV), visa.needsReview)
    }

    @Test
    fun `a custom network with an optional CVV keeps a blank CVV`() {
        val result = LegacyCardNormalizer.normalize(
            customNumber,
            "12/29",
            "",
            networks + customNetwork
        )

        assertEquals(customNumber, result.number)
        assertEquals("", result.cvv)
        assertFalse(result.fixed.containsKey(CardField.CVV))
        assertTrue(result.needsReview.isEmpty())
    }

    @Test
    fun `a custom network without an optional CVV flags a blank CVV`() {
        val required = networks + customNetwork.copy(cvvOptional = false)
        val result = LegacyCardNormalizer.normalize(customNumber, "12/29", "", required)

        assertEquals(customNumber, result.number)
        assertEquals("", result.cvv)
        assertFalse(result.fixed.containsKey(CardField.CVV))
        assertEquals(setOf(CardField.CVV), result.needsReview)
    }
}
