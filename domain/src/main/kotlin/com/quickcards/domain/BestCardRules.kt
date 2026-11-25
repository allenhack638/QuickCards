package com.quickcards.domain

import java.time.LocalDate
import java.time.YearMonth
import java.time.temporal.ChronoUnit

/** Which credit card gives the longest wait before a purchase made today must be paid. */
object BestCardRules {

    data class Candidate(val id: String, val statementDay: Int?, val dueDay: Int?)

    data class Pick(val id: String, val days: Long)

    /** The statement a purchase made today lands on, and the day that bill is due. */
    data class Bill(val statement: LocalDate, val due: LocalDate)

    /** The card that clearly beats every other comparable card today; null on a tie or with fewer than two. */
    fun best(cards: List<Candidate>, today: LocalDate): Pick? {
        val ranked = cards.mapNotNull { card ->
            val days = interestFreeDays(card.statementDay ?: return@mapNotNull null, card.dueDay ?: return@mapNotNull null, today)
            days?.let { Pick(card.id, it) }
        }.sortedByDescending { it.days }
        if (ranked.size < 2 || ranked[0].days == ranked[1].days) return null
        return ranked[0]
    }

    /** Days from [today] to the due date of the statement a purchase made today lands on. */
    fun interestFreeDays(statementDay: Int, dueDay: Int, today: LocalDate): Long? =
        bill(statementDay, dueDay, today)?.let { ChronoUnit.DAYS.between(today, it.due) }

    fun bill(statementDay: Int, dueDay: Int, today: LocalDate): Bill? {
        if (statementDay !in 1..31 || dueDay !in 1..31) return null
        val statement = onOrAfter(statementDay, today)
        return Bill(statement, onOrAfter(dueDay, statement.plusDays(1)))
    }

    private fun onOrAfter(day: Int, from: LocalDate): LocalDate {
        val month = YearMonth.from(from)
        val candidate = month.atDay(minOf(day, month.lengthOfMonth()))
        if (!candidate.isBefore(from)) return candidate
        val next = month.plusMonths(1)
        return next.atDay(minOf(day, next.lengthOfMonth()))
    }
}
