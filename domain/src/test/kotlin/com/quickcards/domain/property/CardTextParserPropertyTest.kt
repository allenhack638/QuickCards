package com.quickcards.domain.property

import com.quickcards.domain.CardNumberRules
import com.quickcards.domain.CardTextParser
import com.quickcards.domain.DefaultNetworks
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.element
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.checkAll
import java.time.YearMonth
import java.util.Locale
import kotlin.random.Random
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Generated-input checks for loose card text. Fixed seeds so a failure reproduces.
 * Contracts: CardTextParser's KDoc, MAINTAINING.md §5.12 and the network table.
 */
class CardTextParserPropertyTest {

    private val today = YearMonth.of(2026, 10)

    private val words = listOf(
        "alpha", "bravo", "charlie", "delta", "echo", "foxtrot", "golf", "hotel",
        "india", "juliet", "kilo", "lima", "mike", "november", "oscar", "papa",
        "quebec", "romeo", "sierra", "tango", "uniform", "victor", "whiskey",
        "xray", "yankee", "zulu"
    )

    private val publicNumbers = listOf(
        "4111111111111111",
        "4012888888881881",
        "5555555555554444",
        "378282246310005",
        "6759649826438453"
    )

    private val impossibleMonths = listOf("00") + (13..99).map {
        String.format(Locale.ROOT, "%02d", it)
    }

    private val pieces = listOf(
        "0", "1", "2", "3", "4", "5", "6", "7", "8", "9",
        "a", "Z", " ", "\n", "\r\n", "\t", "\u0000", "\u0007", "\u001B",
        "/", "-", ".",
        "😀", "🧑🏽‍🚀", "שלום", "مرحبا", "٤١١١", "４１１１", "\uD83D",
        "1".repeat(40), "9".repeat(200), "1234567890".repeat(500)
    )

    private val anyTextPieces: Arb<List<String>> = Arb.list(Arb.element(pieces), 0..400)

    private val shortTextPieces: Arb<List<String>> = Arb.list(Arb.element(pieces), 0..40)

    private fun List<String>.asText(limit: Int) = joinToString("").take(limit)

    private fun parse(text: String) =
        CardTextParser.parse(text.lines(), DefaultNetworks.all, today)

    private fun cardTextPropertyConfig(seed: Long) = PropTestConfig(seed = seed)

    // Contract: junk text keeps the digit filter and never crashes (§5.12 rule 5).
    @Test
    fun parseNeverThrowsOnAnyTextUpTo5000Chars() = runBlocking<Unit> {
        checkAll(1_000, cardTextPropertyConfig(3_010L), anyTextPieces) { text ->
            parse(text.asText(5_000))
        }
    }

    // Contract: a number printed in groups with spaces or dashes is read (CardTextParserTest).
    @Test
    fun aGroupedPublicNumberEmbeddedInWordsIsFoundExactly() = runBlocking<Unit> {
        checkAll(
            1_000,
            cardTextPropertyConfig(3_020L),
            Arb.element(publicNumbers),
            Arb.int(0..Int.MAX_VALUE)
        ) { number, seed ->
            val random = Random(seed)
            val groupCount = random.nextInt(2, 6)
            val parts = IntArray(groupCount) { 3 }
            repeat(number.length - 3 * groupCount) {
                parts[random.nextInt(groupCount)]++
            }
            val separator = if (random.nextBoolean()) " " else "-"
            val printed = buildString {
                var at = 0
                parts.forEachIndexed { index, part ->
                    if (index > 0) append(separator)
                    append(number, at, at + part)
                    at += part
                }
            }
            val before = List(random.nextInt(0, 4)) { words.random(random) }
            val after = List(random.nextInt(0, 4)) { words.random(random) }
            val text = (before + printed + after).joinToString(" ")

            assertEquals(number, parse(text).number)
        }
    }

    // Contract: an expiry in range is normalised to MM/YY; 20 years ahead is the limit.
    @Test
    fun aValidExpiryInTheTextIsRead() = runBlocking<Unit> {
        checkAll(
            1_000,
            cardTextPropertyConfig(3_030L),
            Arb.int(0..239),
            Arb.int(0..Int.MAX_VALUE)
        ) { offset, seed ->
            val date = today.plusMonths(offset.toLong())
            val printed = if (Random(seed).nextBoolean()) {
                String.format(Locale.ROOT, "%02d/%04d", date.monthValue, date.year)
            } else {
                String.format(Locale.ROOT, "%02d/%02d", date.monthValue, date.year % 100)
            }
            val expected = String.format(Locale.ROOT, "%02d/%02d", date.monthValue, date.year % 100)

            assertEquals(expected, parse("VALID THRU $printed").expiry)
        }
    }

    // Contract: an impossible month (00, 13-99) is never returned as an expiry.
    @Test
    fun anImpossibleExpiryMonthIsNeverReturned() = runBlocking<Unit> {
        checkAll(
            1_000,
            cardTextPropertyConfig(3_040L),
            Arb.element(impossibleMonths),
            Arb.int(0..Int.MAX_VALUE)
        ) { month, seed ->
            val year = 2_000 + Random(seed).nextInt(100)
            assertNull(parse("EXP $month/$year").expiry)
        }
    }

    // Contract: numbers are 12-19 digits passing Luhn unless the network disables it.
    @Test
    fun aRunFailingLuhnOnAKnownNetworkNeverYieldsANumber() = runBlocking<Unit> {
        checkAll(
            1_000,
            cardTextPropertyConfig(3_050L),
            Arb.element(listOf(13, 16, 19)),
            Arb.int(0..Int.MAX_VALUE)
        ) { length, seed ->
            val random = Random(seed)
            var digits = "4" + (2..length).map { '0' + random.nextInt(10) }.joinToString("")
            if (CardNumberRules.isLuhnValid(digits)) {
                val last = '0' + (digits.last() - '0' + 1) % 10
                digits = digits.dropLast(1) + last
            }
            assertFalse(digits, CardNumberRules.isLuhnValid(digits))

            val text = "${words.random(random)} $digits ${words.random(random)}"
            assertNull(parse(text).number)
        }
    }

    // Contract: WITHOUT a Luhn-valid run, noise yields nothing, whatever the length/prefix.
    @Test
    fun anyRandomRunFailingLuhnNeverYieldsANumber() = runBlocking<Unit> {
        checkAll(
            1_000,
            cardTextPropertyConfig(3_055L),
            Arb.int(12..19),
            Arb.int(0..Int.MAX_VALUE)
        ) { length, seed ->
            val random = Random(seed)
            var digits = (1..length).map { '0' + random.nextInt(10) }.joinToString("")
            // UnionPay (62..., 16-19) skips Luhn, so keep the run off that prefix.
            if (digits.startsWith("62")) digits = "63" + digits.drop(2)
            if (CardNumberRules.isLuhnValid(digits)) {
                val last = '0' + (digits.last() - '0' + 1) % 10
                digits = digits.dropLast(1) + last
            }
            assertFalse(digits, CardNumberRules.isLuhnValid(digits))

            val text = if (random.nextBoolean()) {
                digits
            } else {
                "${words.random(random)} $digits ${words.random(random)}"
            }
            assertNull(parse(text).number)
        }
    }

    // Contract: the parsed number is plain digits (CardTextParser returns the digits only).
    @Test
    fun aParsedNumberIsAlwaysPlainDigits() = runBlocking<Unit> {
        checkAll(
            1_000,
            cardTextPropertyConfig(3_060L),
            shortTextPieces
        ) { text ->
            val number = parse(text.asText(300)).number
            assertTrue(number == null || number.all { it in '0'..'9' })
        }
    }
}
