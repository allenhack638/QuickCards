package com.quickcards.app.feature.backup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImportSummaryTest {

    @Test
    fun hiddenWhenNothingWasFixedOrFlagged() {
        assertFalse(showsFormatSummary(0, 0))
    }

    @Test
    fun shownWhenOnlyFormatsWereFixed() {
        assertTrue(showsFormatSummary(12, 0))
    }

    @Test
    fun shownWhenOnlyCardsNeedACheck() {
        assertTrue(showsFormatSummary(0, 3))
    }

    @Test
    fun shownWhenBothCountsArePresent() {
        assertTrue(showsFormatSummary(12, 3))
    }
}
