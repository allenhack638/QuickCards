package com.quickcards.app.feature.backup

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The import review only adds the repair line when the file changed or
 * flagged at least one card.
 */
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
