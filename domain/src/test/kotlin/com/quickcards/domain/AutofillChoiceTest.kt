package com.quickcards.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillChoiceTest {

    private fun card(id: String, pinnedAt: Long? = null, sort: Int = 0, used: Long? = null) =
        AutofillCandidate(id = id, pinnedAt = pinnedAt, sortOrder = sort, lastUsed = used)

    @Test
    fun pinnedFirstThenRecentlyUsedThenListOrder() {
        val ranked = AutofillChoice.rank(
            listOf(
                card("a", sort = 0),
                card("b", sort = 1, used = 50),
                card("c", sort = 2, pinnedAt = 2),
                card("d", sort = 3, used = 90),
                card("e", sort = 4, pinnedAt = 1),
                card("f", sort = 5)
            )
        ).map { it.id }
        assertEquals(listOf("e", "c", "d", "b", "a", "f"), ranked)
    }

    @Test
    fun topIsCappedAtFive() {
        val cards = (1..30).map { card("c$it", sort = it) }
        assertEquals(5, AutofillChoice.top(cards).size)
    }

    private fun matches(number: String, typed: String) =
        Regex(AutofillChoice.typeFilter(number)).matches(typed)

    @Test
    fun typingTheFirstDigitsOrTheLastFourNarrows() {
        val n = "4532778103492211"
        assertTrue(matches(n, ""))
        assertTrue(matches(n, "4"))
        assertTrue(matches(n, "4532"))
        assertTrue(matches(n, "4532 77"))
        assertTrue(matches(n, "453277"))
        assertTrue(matches(n, "22"))
        assertTrue(matches(n, "2211"))
        assertFalse(matches(n, "5"))
        assertFalse(matches(n, "4533"))
        assertFalse(matches(n, "2212"))
    }

    @Test
    fun theFilterNeverCarriesMoreThanTheFirstSixAndLastFourDigits() {
        val pattern = AutofillChoice.typeFilter("4532778103492211")
        assertFalse(pattern.contains("8103"))
        assertFalse(pattern.contains("0349"))
        assertFalse(Regex(pattern).matches("4532778"))
    }
}
