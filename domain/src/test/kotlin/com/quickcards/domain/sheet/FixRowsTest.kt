package com.quickcards.domain.sheet

import com.quickcards.domain.BankCatalog
import com.quickcards.domain.DefaultNetworks
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.YearMonth

class FixRowsTest {

    private val context = SheetContext(
        networks = DefaultNetworks.all,
        banks = BankCatalog.all.map { it.name },
        types = listOf("Credit"),
        holders = emptyList(),
        existing = { null },
        today = YearMonth.of(2026, 11)
    )

    @Test
    fun `rows to fix carry the original cells, the row number and the reasons, and read back`() {
        val good = listOf("Priya", "4111111111111111", "8", "2029", "123", "Visa", "HDFC Bank", "Credit", "", "", "")
        val bad = listOf("Priya", "4111111111111112", "13", "2029", "12", "Visa", "HDFC Bank", "Credit", "", "", "x, y")
        val report = SheetChecker.check(listOf(ImportSchema.headers, good, bad), context)

        val grid = report.fixRows { it::class.simpleName.orEmpty() }
        assertEquals(ImportSchema.headers + listOf("Row", "Problem"), grid[0])
        assertEquals(bad + listOf("3", "MonthInvalid NumberLuhn CvvLength"), grid[1])
        assertEquals(2, grid.size)

        val text = CsvWriter.write(grid)
        assertEquals(grid, CsvReader.read(text))
    }
}
