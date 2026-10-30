package com.quickcards.domain.property

import com.quickcards.domain.CardNumberRules
import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Generated-input checks for the Luhn helpers; the seed is fixed so a failure reproduces. */
class LuhnPropertyTest {

    private val config = PropTestConfig(seed = 2_500L)
    private val lengths = Arb.int(1..30)

    @Test
    fun anyPartialNumberWithItsCheckDigitPassesLuhn() = runBlocking<Unit> {
        checkAll(1_000, config, lengths, Arb.int(0..Int.MAX_VALUE)) { length, seed ->
            val random = kotlin.random.Random(seed)
            val partial = (1..length).map { '0' + random.nextInt(10) }.joinToString("")
            val number = partial + CardNumberRules.luhnCheckDigit(partial)
            assertTrue(number, CardNumberRules.isLuhnValid(number))
        }
    }

    @Test
    fun changingOneDigitBreaksLuhn() = runBlocking<Unit> {
        checkAll(1_000, config, lengths, Arb.int(0..Int.MAX_VALUE)) { length, seed ->
            val random = kotlin.random.Random(seed)
            val partial = (1..length).map { '0' + random.nextInt(10) }.joinToString("")
            val number = partial + CardNumberRules.luhnCheckDigit(partial)
            val at = random.nextInt(number.length)
            val other = ('0' + (number[at] - '0' + 1 + random.nextInt(9)) % 10)
            val broken = number.substring(0, at) + other + number.substring(at + 1)
            assertFalse(broken, CardNumberRules.isLuhnValid(broken))
        }
    }
}
