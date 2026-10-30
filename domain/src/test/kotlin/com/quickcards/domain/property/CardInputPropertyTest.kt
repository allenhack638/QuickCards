package com.quickcards.domain.property

import com.quickcards.domain.CardDraft
import com.quickcards.domain.CardError
import com.quickcards.domain.CardNumberRules
import com.quickcards.domain.CardValidator
import com.quickcards.domain.DefaultNetworks
import com.quickcards.domain.Field
import com.quickcards.domain.LegacyCardNormalizer
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.choice
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth
import java.util.Locale
import kotlin.random.Random

/** Generated typing checks for expiry, CVV and legacy repair; seeds are fixed so a failure reproduces. */
class CardInputPropertyTest {

    private val config = PropTestConfig(seed = 4_100L)
    private val today = YearMonth.of(2026, 10)
    private val networks = DefaultNetworks.all
    private val expiryShape = Regex("""^\d{0,2}(/\d{0,2})?$""")

    /** Half the samples are arbitrary unicode, half are what a keypad, a paste and OCR produce. */
    private val text = Arb.choice(
        Arb.string(0..200),
        Arb.list(Arb.element("0123456789/.- \tOIlSsBb|aZ".toList()), 0..200)
            .map { it.joinToString("") }
    )

    /** One prefix per built-in network, so the CVV properties hit every expected length. */
    private val prefixes = listOf(
        "4", "51", "2221", "34", "37", "508500", "300", "6011", "3528", "5018", "62", "2200"
    )

    private fun draft(
        number: String = "4111111111111111",
        expiry: String = "12/29",
        cvv: String = "123"
    ) = CardDraft(holderId = "h1", number = number, expiry = expiry, cvv = cvv, bankId = "b1")

    private fun errors(draft: CardDraft): Map<Field, CardError> = CardValidator.validate(
        draft = draft,
        networks = networks,
        today = today
    )

    private fun number(prefix: String, extra: Int, seed: Int): String {
        val random = Random(seed)
        return prefix + (1..extra).map { '0' + random.nextInt(10) }.joinToString("")
    }

    /** CardFormContent feeds the field value back each frame; the handler keeps four digits at most. */
    @Test
    fun anyTypedExpiryNormalisesToMonthAndYearDigitsOnly() = runBlocking<Unit> {
        checkAll(1_000, config, text) { raw ->
            val normalized = CardValidator.normalizeExpiry(raw)

            assertTrue(normalized, expiryShape.matches(normalized))
            assertTrue(normalized, normalized.count { it in '0'..'9' } <= 4)
            assertEquals(normalized, CardValidator.normalizeExpiry(normalized))
        }
    }

    /** MAINTAINING "Card rules": expiry is MM/YY, and CardValidatorTest rejects 00 and 13. */
    @Test
    fun expiryMonthIsAcceptedOnlyFromOneToTwelve() = runBlocking<Unit> {
        checkAll(1_000, config, Arb.int(0..99), Arb.int(27..46)) { month, year ->
            val expiry = String.format(Locale.ROOT, "%02d/%02d", month, year)
            val error = errors(draft(expiry = expiry))[Field.EXPIRY]

            if (month in 1..12) {
                assertNull(expiry, error)
            } else {
                assertEquals(expiry, CardError.ExpiryFormat, error)
            }
        }
    }

    /** MAINTAINING "Card rules": a CVV is exactly the network's length, 3 or 4 when unknown. */
    @Test
    fun cvvLengthMustMatchTheDetectedNetwork() = runBlocking<Unit> {
        checkAll(
            1_000,
            config,
            Arb.int(0..prefixes.lastIndex),
            Arb.int(0..12),
            Arb.int(0..Int.MAX_VALUE),
            Arb.int(1..6)
        ) { prefixIndex, extra, seed, cvvLength ->
            val number = number(prefixes[prefixIndex], extra, seed)
            val expected = CardNumberRules.detect(number, networks)?.cvvLength
            val error = errors(draft(number = number, cvv = "7".repeat(cvvLength)))[Field.CVV]

            if (expected == null) {
                assertEquals(
                    number,
                    if (cvvLength in 3..4) null else CardError.CvvUnknown,
                    error
                )
            } else {
                assertEquals(
                    number,
                    if (cvvLength == expected) null else CardError.CvvLength(expected),
                    error
                )
            }
        }
    }

    /** CardNetwork.cvvOptional: cards on this network may have no printed CVV, so it may stay blank. */
    @Test
    fun blankCvvIsAllowedExactlyOnAnOptionalCvvNetwork() = runBlocking<Unit> {
        checkAll(
            1_000,
            config,
            Arb.int(0..prefixes.lastIndex),
            Arb.int(0..12),
            Arb.int(0..Int.MAX_VALUE)
        ) { prefixIndex, extra, seed ->
            val number = number(prefixes[prefixIndex], extra, seed)
            val network = CardNumberRules.detect(number, networks)
            val error = errors(draft(number = number, cvv = ""))[Field.CVV]

            assertEquals(number, network?.cvvOptional == true, error == null)
            if (error != null) {
                assertEquals(
                    number,
                    network?.let { CardError.CvvLength(it.cvvLength) } ?: CardError.CvvUnknown,
                    error
                )
            }
        }
    }

    /** LegacyCardNormalizer: "Repairs only pre-v2 values it can prove valid ... Idempotent." */
    @Test
    fun anyLegacyValueNormalisesIdempotently() = runBlocking<Unit> {
        checkAll(1_000, config, text, text, text) {
                number,
                expiry,
                cvv ->
            val first = LegacyCardNormalizer.normalize(number, expiry, cvv, networks)
            val second = LegacyCardNormalizer.normalize(first.number, first.expiry, first.cvv, networks)

            assertEquals(number, first.number, second.number)
            assertEquals(expiry, first.expiry, second.expiry)
            assertEquals(cvv, first.cvv, second.cvv)
            assertTrue(second.fixed.toString(), second.fixed.isEmpty())
            assertEquals(first.needsReview, second.needsReview)
        }
    }

    /** An undone repair puts raw legacy text back into the form, so validation must accept any string. */
    @Test
    fun anyTypedTextValidatesWithoutThrowing() = runBlocking<Unit> {
        checkAll(1_000, config, text, text, text) {
                number,
                expiry,
                cvv ->
            val result = errors(draft(number = number, expiry = expiry, cvv = cvv))

            assertTrue(result.keys.all { it in Field.values() })
        }
    }
}
