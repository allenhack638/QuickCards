package com.quickcards.domain

import java.time.LocalDate
import java.time.YearMonth

enum class ExpiryState {
    VALID,
    EXPIRES_SOON,
    EXPIRED
}

/**
 * Expired after the last day of the MM/YY month; soon within [soonMonths] (the current month
 * counts when the window is at least 0). Unparseable values return null.
 */
object ExpiryStatus {

    private val EXPIRY = Regex("""^(\d{2})/(\d{2})$""")

    fun expiryState(expiry: String, today: LocalDate, soonMonths: Int): ExpiryState? {
        val match = EXPIRY.matchEntire(expiry.trim()) ?: return null
        val month = match.groupValues[1].toIntOrNull() ?: return null
        val shortYear = match.groupValues[2].toIntOrNull() ?: return null
        if (month !in 1..12) return null

        val expiryMonth = YearMonth.of(2000 + shortYear, month)
        if (today.isAfter(expiryMonth.atEndOfMonth())) return ExpiryState.EXPIRED

        val windowEnd = YearMonth.from(today).plusMonths(soonMonths.coerceAtLeast(0).toLong())
        return if (expiryMonth <= windowEnd) ExpiryState.EXPIRES_SOON else ExpiryState.VALID
    }
}
