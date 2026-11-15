package com.quickcards.domain.sheet

import com.quickcards.domain.BankCatalog
import com.quickcards.domain.DefaultNetworks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

class ImportSchemaTest {

    @Test
    fun `the sheet headings are exactly these, in this order`() {
        assertEquals(
            listOf(
                "Card holder*", "Card number*", "Expiry month*", "Expiry year*", "CVV*",
                "Network*", "Bank*", "Card type*", "Payment due day", "Note"
            ),
            ImportSchema.headers
        )
        assertEquals(1, ImportSchema.VERSION)
    }

    @Test
    fun `headings match ignoring case, spaces and the star`() {
        assertEquals(ImportColumn.HOLDER, ImportSchema.columnFor("card holder*"))
        assertEquals(ImportColumn.HOLDER, ImportSchema.columnFor("  CARD  HOLDER "))
        assertEquals(ImportColumn.DUE_DAY, ImportSchema.columnFor("Payment due day"))
        assertNull(ImportSchema.columnFor("Name on card"))
        assertNull(ImportSchema.columnFor(""))
    }

    @Test
    fun `months read from the list label, a number or a name`() {
        assertEquals("08 - August", ImportSchema.monthLabels[7])
        listOf("08 - August", "8", "08", "Aug", "august", " AUGUST ").forEach { assertEquals(it, 8, ImportSchema.parseMonth(it)) }
        assertEquals(12, ImportSchema.parseMonth("12 - December"))
        listOf("0", "13", "Augu", "", "x").forEach { assertNull(it, ImportSchema.parseMonth(it)) }
    }

    @Test
    fun `years read as four or two digits`() {
        assertEquals(2029, ImportSchema.parseYear("2029"))
        assertEquals(2029, ImportSchema.parseYear("29"))
        listOf("", "029", "20299", "2O29", "2029.0").forEach { assertNull(it, ImportSchema.parseYear(it)) }
        assertEquals(21, ImportSchema.yearLabels(YearMonth.of(2026, 11)).size)
        assertEquals("2026", ImportSchema.yearLabels(YearMonth.of(2026, 11)).first())
    }

    @Test
    fun `due days are 1 to 31`() {
        assertEquals(5, ImportSchema.parseDueDay("5"))
        assertEquals(5, ImportSchema.parseDueDay("05"))
        listOf("", "0", "32", "5th").forEach { assertNull(it, ImportSchema.parseDueDay(it)) }
        assertEquals(31, ImportSchema.dayLabels.size)
    }

    @Test
    fun `list labels carry hints the importer ignores`() {
        val amex = DefaultNetworks.all.first { it.name == "American Express" }
        val visa = DefaultNetworks.all.first { it.name == "Visa" }
        assertEquals("American Express (AMEX)", ImportSchema.networkLabel(amex))
        assertEquals("Visa", ImportSchema.networkLabel(visa))
        val banks = ImportSchema.bankLabels(listOf("My Co-op Bank", "hdfc bank"))
        assertEquals(BankCatalog.all.size + 1, banks.size)
        assertTrue(banks.contains("SBI Card (State Bank of India)"))
        assertEquals("My Co-op Bank", banks.last())
        banks.forEach { label -> assertEquals(label, label, label) }
    }
}
