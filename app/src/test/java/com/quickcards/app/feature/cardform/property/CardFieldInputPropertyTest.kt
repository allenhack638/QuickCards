package com.quickcards.app.feature.cardform.property

import com.quickcards.app.feature.cardform.CardFieldInput
import com.quickcards.app.feature.cardform.CardFormLists
import com.quickcards.app.feature.cardform.CardFormOptions
import com.quickcards.app.feature.cardform.CardFormSession
import com.quickcards.domain.CardNumberRules
import com.quickcards.domain.CardValidator
import com.quickcards.domain.DefaultNetworks
import com.quickcards.domain.NamedRef
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
import org.junit.Assert.assertTrue
import org.junit.Test

/** Generated typing checks for the form field filters; seeds are fixed so a failure reproduces. */
class CardFieldInputPropertyTest {

    private val config = PropTestConfig(seed = 4_200L)
    private val networks = DefaultNetworks.all
    private val expiryShape = Regex("""^\d{0,2}(/\d{0,2})?$""")

    /** Half the samples are arbitrary unicode, half are what a keypad, a paste and OCR produce. */
    private val text = Arb.choice(
        Arb.string(0..200),
        Arb.list(Arb.element("0123456789/.- \tOIlSsBb|aZ".toList()), 0..200)
            .map { it.joinToString("") }
    )

    private fun digitsOf(raw: String): String = raw.filter { it in '0'..'9' }

    private fun session() = CardFormSession.forAdd(
        CardFormLists(
            holders = listOf(NamedRef("h1", "Holder")),
            networks = networks
        ),
        CardFormOptions()
    )

    /** CardFieldInput.number: the field keeps digits only, grouped, up to the network maximum. */
    @Test
    fun numberFieldKeepsOnlyDigitsWithinTheNetworkMaximum() = runBlocking<Unit> {
        checkAll(1_000, config, text, Arb.int(0..1)) { raw, typing ->
            val typed = digitsOf(raw)
            val network = CardNumberRules.detect(typed, networks)
            val out = CardFieldInput.number(raw, networks, typing = typing == 1)

            assertEquals(typed.take(CardNumberRules.maxLength(network)), digitsOf(out))
            assertEquals(out, out.filter { it in '0'..'9' || it == ' ' })
            assertEquals(out, CardFieldInput.number(out, networks, typing = typing == 1))
        }
    }

    /** CardFieldInput.cvv: digits only, never longer than the network's CVV (4 when unknown). */
    @Test
    fun cvvFieldKeepsOnlyDigitsWithinTheNetworkLength() = runBlocking<Unit> {
        checkAll(1_000, config, text, Arb.int(-1..networks.lastIndex)) { raw, index ->
            val network = networks.getOrNull(index)
            val out = CardFieldInput.cvv(raw, network)

            assertEquals(digitsOf(raw).take(network?.cvvLength ?: 4), out)
            assertTrue(out, out.all { it in '0'..'9' })
        }
    }

    /** CardFieldInput.expiry delegates to the validator's normaliser, so the field never holds junk. */
    @Test
    fun expiryFieldOnlyShowsTheNormalisedDate() = runBlocking<Unit> {
        checkAll(1_000, config, text) { raw ->
            val out = CardFieldInput.expiry(raw)

            assertEquals(CardValidator.normalizeExpiry(raw), out)
            assertTrue(out, expiryShape.matches(out))
        }
    }

    /** CardFormContent sends each keystroke to updateNumber/updateCvv, so the session keeps the same limits. */
    @Test
    fun sessionInputKeepsTheSameFieldLimits() = runBlocking<Unit> {
        checkAll(1_000, config, text, text, text) { number, cvv, expiry ->
            val state = session().updateNumber(number).updateCvv(cvv).updateExpiry(expiry)
            val network = CardNumberRules.detect(state.form.number, networks)

            assertEquals(digitsOf(number).take(CardNumberRules.maxLength(network)), state.form.number)
            assertEquals(digitsOf(cvv).take(network?.cvvLength ?: 4), state.form.cvv)
            assertEquals(CardValidator.normalizeExpiry(expiry), state.form.expiry)
        }
    }
}
