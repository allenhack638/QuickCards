package com.quickcards.domain.property

import com.quickcards.domain.CardDraft
import com.quickcards.domain.CardError
import com.quickcards.domain.CardNetwork
import com.quickcards.domain.CardNumberRules
import com.quickcards.domain.CardValidator
import com.quickcards.domain.DefaultNetworks
import com.quickcards.domain.Field
import com.quickcards.domain.NetworkRule
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import java.time.YearMonth
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Generated-input checks for number detection, validation, lengths and grouping; seeds are fixed. */
class CardNumberAndNetworkPropertyTest {

    private val config = PropTestConfig(seed = 5_100L)
    private val networks = DefaultNetworks.all

    private val noise = listOf(
        '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
        'a', 'Z', ' ', '-', ',', '•', 'é', '\uD83D', '\uDE00'
    )

    private val lengthTokens = listOf(
        "12", "16", "19", "0", "11", "20", "999", "13,16", "14-19",
        "1-2147483647", "19-12", " 16 ", "16 - 18", "-", ""
    )

    private fun randomString(random: Random, length: Int): String = buildString {
        repeat(length) { append(noise[random.nextInt(noise.size)]) }
    }

    private fun randomLengthSpec(random: Random): String {
        val count = random.nextInt(5)
        return (0 until count).joinToString(",") { lengthTokens[random.nextInt(lengthTokens.size)] }
    }

    private fun prefixOf(network: CardNetwork, random: Random): String {
        val tokens = network.prefixes.split(",").map { it.trim() }
        val token = tokens[random.nextInt(tokens.size)]
        if ("-" !in token) return token
        val (low, high) = token.split("-").map { it.toInt() }
        return (low + random.nextInt(high - low + 1)).toString()
    }

    private fun validNumber(network: CardNetwork, length: Int, random: Random): String {
        val prefix = prefixOf(network, random)
        val partial = prefix + buildString {
            repeat(length - 1 - prefix.length) { append('0' + random.nextInt(10)) }
        }
        return partial + CardNumberRules.luhnCheckDigit(partial)
    }

    private fun numberErrorFor(network: CardNetwork, number: String): CardError? =
        CardValidator.validate(
            draft = CardDraft(holderId = "h", number = number, expiry = "12/40", cvv = "", bankId = "b"),
            networks = listOf(network),
            today = YearMonth.of(2030, 1)
        )[Field.NUMBER]

    // Detection runs on scanned text, so no input may crash it (docs/MAINTAINING.md: auto-detect).
    @Test
    fun detectNeverThrowsOnAnyInput() = runBlocking<Unit> {
        checkAll(1_000, config, Arb.int(0..40), Arb.int(0..Int.MAX_VALUE)) { length, seed ->
            val input = randomString(Random(seed), length)
            val detected = CardNumberRules.detect(input, networks)
            assertTrue(input, detected == null || networks.any { it.id == detected.id })
        }
    }

    // "Detection picks the longest matching prefix; equal scores keep the first list entry."
    @Test
    fun detectPicksTheLongestMatchingPrefix() = runBlocking<Unit> {
        checkAll(1_000, config, Arb.int(0..40), Arb.int(0..Int.MAX_VALUE)) { length, seed ->
            val random = Random(seed)
            val input = buildString {
                repeat(length) {
                    append(if (random.nextInt(4) == 0) noise[random.nextInt(noise.size)] else '0' + random.nextInt(10))
                }
            }
            val scores = networks.map { CardNumberRules.prefixScore(input, it.prefixes) }
            val best = scores.maxOrNull()!!
            val expected = if (best == 0) null else networks[scores.indexOf(best)].id
            assertEquals(input, expected, CardNumberRules.detect(input, networks)?.id)
        }
    }

    // A Luhn-valid number of an allowed length and prefix has no number error (docs/MAINTAINING.md §4).
    @Test
    fun luhnValidAllowedLengthHasNoNumberError() = runBlocking<Unit> {
        checkAll(1_000, config, Arb.int(0 until networks.size), Arb.int(0..Int.MAX_VALUE)) { index, seed ->
            val network = networks[index]
            val random = Random(seed)
            val lengths = CardNumberRules.parseLengths(network.lengths)
            val length = lengths[random.nextInt(lengths.size)]
            assertNull("${network.id} $length", numberErrorFor(network, validNumber(network, length, random)))
        }
    }

    // Length must match the detected network, so one digit past the longest length is a length error.
    @Test
    fun oneDigitPastLongestLengthIsALengthError() = runBlocking<Unit> {
        checkAll(1_000, config, Arb.int(0 until networks.size), Arb.int(0..Int.MAX_VALUE)) { index, seed ->
            val network = networks[index]
            val random = Random(seed)
            val length = CardNumberRules.parseLengths(network.lengths).last()
            val longer = validNumber(network, length, random) + ('0' + random.nextInt(10))
            val error = numberErrorFor(network, longer)
            assertTrue("${network.id} $length", error is CardError.NetworkLength)
        }
    }

    // Length must match the detected network, so one digit short of the shortest length is a length error.
    @Test
    fun oneDigitShortOfShortestLengthIsALengthError() = runBlocking<Unit> {
        checkAll(1_000, config, Arb.int(0 until networks.size), Arb.int(0..Int.MAX_VALUE)) { index, seed ->
            val network = networks[index]
            val random = Random(seed)
            val length = CardNumberRules.parseLengths(network.lengths).first()
            val shorter = validNumber(network, length, random).dropLast(1)
            val error = numberErrorFor(network, shorter)
            assertTrue("${network.id} $length", error is CardError.NetworkLength)
        }
    }

    // Built-in networks are their own standard, so no rule is reported as changed.
    @Test
    fun standardNetworksReportNoChangedRules() = runBlocking<Unit> {
        checkAll(1_000, config, Arb.int(0 until networks.size)) { index ->
            val network = networks[index]
            assertTrue(network.id, DefaultNetworks.changedRules(network).isEmpty())
        }
    }

    // "Every rule is compared": changing exactly one field reports exactly that rule.
    @Test
    fun changingOneRuleReportsIt() = runBlocking<Unit> {
        checkAll(1_000, config, Arb.int(0 until networks.size), Arb.int(0 until 4)) { index, field ->
            val network = networks[index]
            val changed: CardNetwork
            val expected: NetworkRule
            when (field) {
                0 -> {
                    changed = network.copy(prefixes = network.prefixes + ",999999")
                    expected = NetworkRule.PREFIXES
                }
                1 -> {
                    val compact = network.lengths.filterNot(Char::isWhitespace)
                    changed = network.copy(lengths = if (compact == "12") "13" else "12")
                    expected = NetworkRule.LENGTHS
                }
                2 -> {
                    changed = network.copy(cvvLength = if (network.cvvLength == 3) 4 else 3)
                    expected = NetworkRule.CVV
                }
                else -> {
                    changed = network.copy(luhn = !network.luhn)
                    expected = NetworkRule.LUHN
                }
            }
            assertEquals(network.id, setOf(expected), DefaultNetworks.changedRules(changed))
        }
    }

    // A grouping either does not exist or its parts add up to the full number length.
    @Test
    fun groupsForSumsToTheRequestedLength() = runBlocking<Unit> {
        checkAll(1_000, config, Arb.int(0..networks.size), Arb.int(0..30)) { index, length ->
            val network = if (index == networks.size) null else networks[index]
            val groups = CardNumberRules.groupsFor(network, length)
            if (groups != null) {
                assertEquals("${network?.id} $length", length, groups.sum())
                assertTrue("${network?.id} $length", groups.isNotEmpty())
            }
        }
    }

    // "Only card lengths are kept"; an imported 1-2147483647 must not allocate the whole range.
    @Test
    fun parsedLengthsStayInsideCardLengthsAndAreSortedUnique() = runBlocking<Unit> {
        checkAll(1_000, config, Arb.int(0..Int.MAX_VALUE)) { seed ->
            val spec = randomLengthSpec(Random(seed))
            val lengths = CardNumberRules.parseLengths(spec)
            assertTrue(spec, lengths.all { it in CardNumberRules.MIN_LENGTH..CardNumberRules.MAX_LENGTH })
            assertEquals(spec, lengths.sorted(), lengths)
            assertEquals(spec, lengths.distinct(), lengths)
            assertTrue(spec, lengths.size <= CardNumberRules.MAX_LENGTH - CardNumberRules.MIN_LENGTH + 1)
        }
    }

    // The typing limit is the largest allowed length, and can never leave the 12-19 card range.
    @Test
    fun maxLengthStaysInsideCardLengths() = runBlocking<Unit> {
        assertEquals(CardNumberRules.MAX_LENGTH, CardNumberRules.maxLength(null))
        checkAll(1_000, config, Arb.int(0 until networks.size), Arb.int(0..Int.MAX_VALUE)) { index, seed ->
            val network = networks[index].copy(lengths = randomLengthSpec(Random(seed)))
            val max = CardNumberRules.maxLength(network)
            assertTrue("${network.id} $max", max in CardNumberRules.MIN_LENGTH..CardNumberRules.MAX_LENGTH)
            val parsed = CardNumberRules.parseLengths(network.lengths)
            assertEquals(
                network.id,
                maxOf(parsed.maxOrNull() ?: CardNumberRules.MIN_LENGTH, CardNumberRules.MIN_LENGTH),
                max
            )
        }
    }
}
