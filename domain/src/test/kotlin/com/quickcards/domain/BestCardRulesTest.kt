package com.quickcards.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Test

class BestCardRulesTest {

    @Test
    fun aPurchaseTodayIsPaidOnTheDueDayAfterTheNextStatement() {
        // Statement closes 15 Dec, bill due 5 Jan: 41 days from 25 Nov.
        assertEquals(41L, BestCardRules.interestFreeDays(statementDay = 15, dueDay = 5, today = LocalDate.of(2026, 11, 25)))
    }

    @Test
    fun onTheStatementDayThePurchaseLandsOnTodaysBillSoTheWaitIsShortest() {
        // Statement 25 Nov (today), due 15 Dec: 20 days. A day later the next cycle gives 50.
        assertEquals(20L, BestCardRules.interestFreeDays(statementDay = 25, dueDay = 15, today = LocalDate.of(2026, 11, 25)))
        assertEquals(50L, BestCardRules.interestFreeDays(statementDay = 25, dueDay = 15, today = LocalDate.of(2026, 11, 26)))
    }

    @Test
    fun shortMonthsClampTheStatementAndDueDays() {
        // Statement day 31 closes on 28 Feb; due day 30 then falls on 30 Mar.
        assertEquals(46L, BestCardRules.interestFreeDays(statementDay = 31, dueDay = 30, today = LocalDate.of(2026, 2, 12)))
    }

    @Test
    fun theDueDateRollsIntoTheNextYear() {
        // Statement 20 Dec, due 8 Jan 2027: 34 days from 5 Dec.
        assertEquals(34L, BestCardRules.interestFreeDays(statementDay = 20, dueDay = 8, today = LocalDate.of(2026, 12, 5)))
    }

    @Test
    fun theCardWithTheLongestWaitIsTheBestToday() {
        val today = LocalDate.of(2026, 11, 26)
        val cards = listOf(
            BestCardRules.Candidate("hdfc", statementDay = 25, dueDay = 15), // 50 days
            BestCardRules.Candidate("icici", statementDay = 15, dueDay = 5), // 40 days
            BestCardRules.Candidate("axis", statementDay = 26, dueDay = 16) // closes today: 20 days
        )
        assertEquals(BestCardRules.Pick("hdfc", 50L), BestCardRules.best(cards, today))
    }

    @Test
    fun noPickWithoutTwoComparableCardsOrWithATie() {
        val today = LocalDate.of(2026, 11, 26)
        val hdfc = BestCardRules.Candidate("hdfc", statementDay = 25, dueDay = 15)
        assertEquals(null, BestCardRules.best(listOf(hdfc), today))
        assertEquals(null, BestCardRules.best(listOf(hdfc, BestCardRules.Candidate("sbi", statementDay = null, dueDay = 10)), today))
        assertEquals(null, BestCardRules.best(listOf(hdfc, BestCardRules.Candidate("kotak", statementDay = 25, dueDay = 15)), today))
    }

    @Test
    fun outOfRangeDaysGiveNoAnswer() {
        assertEquals(null, BestCardRules.interestFreeDays(statementDay = 0, dueDay = 5, today = LocalDate.of(2026, 11, 25)))
        assertEquals(null, BestCardRules.interestFreeDays(statementDay = 15, dueDay = 32, today = LocalDate.of(2026, 11, 25)))
    }

    @Test
    fun theBillGivesTheStatementAndDueDatesOfASpendToday() {
        val today = LocalDate.of(2026, 11, 26)
        assertEquals(
            BestCardRules.Bill(statement = LocalDate.of(2026, 12, 25), due = LocalDate.of(2027, 1, 15)),
            BestCardRules.bill(statementDay = 25, dueDay = 15, today = today)
        )
        assertEquals(
            BestCardRules.Bill(statement = LocalDate.of(2026, 11, 26), due = LocalDate.of(2026, 12, 16)),
            BestCardRules.bill(statementDay = 26, dueDay = 16, today = today)
        )
        assertEquals(null, BestCardRules.bill(statementDay = 0, dueDay = 16, today = today))
    }
}
