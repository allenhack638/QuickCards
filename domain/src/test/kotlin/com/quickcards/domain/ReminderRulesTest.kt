package com.quickcards.domain

import java.time.LocalDate
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ReminderRulesTest {

    @Test
    fun expiryIsRemindedInsideTheWindowOnly() {
        assertEquals(30L, ReminderRules.expiryReminderDays("12/29", LocalDate.of(2029, 12, 1)))
        assertEquals(0L, ReminderRules.expiryReminderDays("12/29", LocalDate.of(2029, 12, 31)))
        assertNull(ReminderRules.expiryReminderDays("12/29", LocalDate.of(2029, 11, 30)))
        assertNull(ReminderRules.expiryReminderDays("12/29", LocalDate.of(2030, 1, 1)))
        assertNull(ReminderRules.expiryReminderDays("11//22", LocalDate.of(2029, 12, 1)))
        assertEquals(15L, ReminderRules.expiryReminderDays("02/2028", LocalDate.of(2028, 2, 14)))
    }

    @Test
    fun dueDayClampsToShortMonths() {
        assertEquals(LocalDate.of(2026, 2, 28), ReminderRules.nextDueDate(31, LocalDate.of(2026, 2, 10)))
        assertEquals(LocalDate.of(2026, 3, 5), ReminderRules.nextDueDate(5, LocalDate.of(2026, 2, 10)))
        assertEquals(LocalDate.of(2026, 2, 10), ReminderRules.nextDueDate(10, LocalDate.of(2026, 2, 10)))
        assertNull(ReminderRules.nextDueDate(0, LocalDate.of(2026, 2, 10)))
    }

    @Test
    fun dueReminderFiresThreeDaysBefore() {
        assertEquals(LocalDate.of(2026, 3, 5), ReminderRules.dueReminderDate(5, LocalDate.of(2026, 3, 2)))
        assertEquals(LocalDate.of(2026, 3, 5), ReminderRules.dueReminderDate(5, LocalDate.of(2026, 3, 5)))
        assertNull(ReminderRules.dueReminderDate(5, LocalDate.of(2026, 3, 1)))
        assertNull(ReminderRules.dueReminderDate(null, LocalDate.of(2026, 3, 1)))
    }

    @Test
    fun keysAreOncePerPeriod() {
        assertEquals("exp2:c1:tag", ReminderRules.expiryKey("c1", "tag"))
        assertEquals("due:c1:2026-3", ReminderRules.dueKey("c1", LocalDate.of(2026, 3, 5)))
        assertEquals("paid:due:c1:2026-3", ReminderRules.paidKey("due:c1:2026-3"))
        assertEquals("due:c1:2026-3@2026-03-03", ReminderRules.dueDayKey("due:c1:2026-3", LocalDate.of(2026, 3, 3)))
    }
}
