package com.quickcards.domain

import java.time.LocalDate
import java.time.YearMonth

/** When a card deserves a reminder. Pure date rules; the app decides how to notify. */
object ReminderRules {

    const val EXPIRY_WINDOW_DAYS = 30L
    const val DUE_LEAD_DAYS = 3L

    /** The card's last valid day, or null when the expiry is not MM/YY or MM/YYYY. */
    fun lastValidDay(expiry: String): LocalDate? {
        val parts = expiry.split("/")
        if (parts.size != 2) return null
        val month = parts[0].trim().toIntOrNull() ?: return null
        val rawYear = parts[1].trim().toIntOrNull() ?: return null
        val year = if (parts[1].trim().length == 2) 2000 + rawYear else rawYear
        if (month !in 1..12 || year !in 2000..2199) return null
        return YearMonth.of(year, month).atEndOfMonth()
    }

    /** Days until the card expires, when that is within the reminder window and not past. */
    fun expiryReminderDays(expiry: String, today: LocalDate): Long? {
        val last = lastValidDay(expiry) ?: return null
        val days = java.time.temporal.ChronoUnit.DAYS.between(today, last)
        return days.takeIf { it in 0..EXPIRY_WINDOW_DAYS }
    }

    /** A stable key so each card is reminded once per expiry. */
    fun expiryKey(cardId: String, expiry: String): String = "exp:$cardId:${expiry.trim()}"

    /** The next payment date for a due day, clamped to months that are shorter. */
    fun nextDueDate(dueDay: Int, today: LocalDate): LocalDate? {
        if (dueDay !in 1..31) return null
        val thisMonth = YearMonth.from(today)
        val candidate = thisMonth.atDay(minOf(dueDay, thisMonth.lengthOfMonth()))
        if (!candidate.isBefore(today)) return candidate
        val next = thisMonth.plusMonths(1)
        return next.atDay(minOf(dueDay, next.lengthOfMonth()))
    }

    /** The due date when today is inside the lead window before it. */
    fun dueReminderDate(dueDay: Int?, today: LocalDate): LocalDate? {
        val due = nextDueDate(dueDay ?: return null, today) ?: return null
        val days = java.time.temporal.ChronoUnit.DAYS.between(today, due)
        return due.takeIf { days in 0..DUE_LEAD_DAYS }
    }

    fun dueKey(cardId: String, due: LocalDate): String = "due:$cardId:${due.year}-${due.monthValue}"
}
