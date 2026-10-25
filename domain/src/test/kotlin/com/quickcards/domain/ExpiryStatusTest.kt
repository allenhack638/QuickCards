package com.quickcards.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ExpiryStatusTest {

    @Test
    fun `a card expires only after the last day of its month`() {
        val lastDay = LocalDate.of(2026, 10, 31)

        assertEquals(
            ExpiryState.EXPIRES_SOON,
            ExpiryStatus.expiryState("10/26", lastDay, soonMonths = 1)
        )
        assertEquals(
            ExpiryState.EXPIRED,
            ExpiryStatus.expiryState("10/26", lastDay.plusDays(1), soonMonths = 1)
        )
    }

    @Test
    fun `the soon window includes the current month and every month up to the window`() {
        val today = LocalDate.of(2026, 10, 24)

        assertEquals(ExpiryState.EXPIRES_SOON, ExpiryStatus.expiryState("10/26", today, 1))
        assertEquals(ExpiryState.EXPIRES_SOON, ExpiryStatus.expiryState("11/26", today, 1))
        assertEquals(ExpiryState.VALID, ExpiryStatus.expiryState("12/26", today, 1))
        assertEquals(ExpiryState.EXPIRES_SOON, ExpiryStatus.expiryState("12/26", today, 2))
        assertEquals(ExpiryState.EXPIRES_SOON, ExpiryStatus.expiryState("01/27", today, 3))
        assertEquals(ExpiryState.VALID, ExpiryStatus.expiryState("02/27", today, 3))
    }

    @Test
    fun `a further expiry is labelled with a three month window`() {
        val today = LocalDate.of(2026, 10, 1)

        assertEquals(ExpiryState.VALID, ExpiryStatus.expiryState("01/27", today, 2))
        assertEquals(ExpiryState.EXPIRES_SOON, ExpiryStatus.expiryState("01/27", today, 3))
    }

    @Test
    fun `the window rolls over December to January`() {
        val december = LocalDate.of(2026, 12, 15)

        assertEquals(ExpiryState.EXPIRES_SOON, ExpiryStatus.expiryState("12/26", december, 1))
        assertEquals(ExpiryState.EXPIRES_SOON, ExpiryStatus.expiryState("01/27", december, 1))
        assertEquals(ExpiryState.VALID, ExpiryStatus.expiryState("02/27", december, 1))
    }

    @Test
    fun `expired is never also expires soon`() {
        val today = LocalDate.of(2026, 10, 24)

        assertEquals(ExpiryState.EXPIRED, ExpiryStatus.expiryState("09/26", today, 1))
        assertEquals(ExpiryState.EXPIRED, ExpiryStatus.expiryState("09/26", today, 3))
        assertEquals(ExpiryState.EXPIRED, ExpiryStatus.expiryState("05/20", today, 3))
    }

    @Test
    fun `an unparseable expiry has no state`() {
        val today = LocalDate.of(2026, 10, 24)

        assertNull(ExpiryStatus.expiryState("", today, 1))
        assertNull(ExpiryStatus.expiryState("13/26", today, 1))
        assertNull(ExpiryStatus.expiryState("1/26", today, 1))
        assertNull(ExpiryStatus.expiryState("11-26", today, 1))
        assertNull(ExpiryStatus.expiryState("11/2026", today, 1))
    }

    @Test
    fun `a window of zero only labels the current month`() {
        val today = LocalDate.of(2026, 10, 24)

        assertEquals(ExpiryState.EXPIRES_SOON, ExpiryStatus.expiryState("10/26", today, 0))
        assertEquals(ExpiryState.VALID, ExpiryStatus.expiryState("11/26", today, 0))
    }
}
