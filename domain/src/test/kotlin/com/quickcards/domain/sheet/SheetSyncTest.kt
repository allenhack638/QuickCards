package com.quickcards.domain.sheet

import com.quickcards.domain.BankCatalog
import com.quickcards.domain.DefaultNetworks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.YearMonth

/**
 * The sync guard: the published sheet (its exports are the fixtures), the schema, the bank
 * catalog, the built-in networks and card types must all say the same thing. A change to any
 * one of them without the others fails here.
 */
class SheetSyncTest {

    private fun fixture(name: String): ByteArray =
        checkNotNull(javaClass.getResourceAsStream("/sheet/$name")) { "missing fixture $name" }.readBytes()

    private fun lists(name: String): Map<String, List<String>> {
        val sheet = XlsxReader.read(fixture(name)).first { it.name == "Lists" }
        val header = sheet.rows.first()
        return header.indices.filter { header[it].isNotEmpty() }.associate { col ->
            header[col] to sheet.rows.drop(1).mapNotNull { r -> r.getOrNull(col)?.takeIf { it.isNotEmpty() } }
        }
    }

    private val builtInTypes = listOf("Credit", "Debit", "Prepaid", "Forex")

    @Test
    fun `the public sheet's headings are the schema's headings`() {
        listOf("generated-empty.xlsx", "google-sheets-export-empty.xlsx").forEach { name ->
            val cards = XlsxReader.cardsSheet(XlsxReader.read(fixture(name)))!!
            assertEquals(name, ImportSchema.headers, cards.rows[1].take(ImportSchema.headers.size))
        }
        val csv = CsvReader.read(String(fixture("google-sheets-export-empty.csv"), Charsets.UTF_8))
        assertEquals(ImportSchema.headers, csv[1])
    }

    @Test
    fun `the sheet's lists are the app's lists`() {
        listOf("generated-empty.xlsx", "google-sheets-export-empty.xlsx").forEach { name ->
            val lists = lists(name)
            // Every network and bank in the sheet resolves to exactly the app's entry, and nothing is missing.
            val networks = lists.getValue("Networks")
            assertEquals(name, DefaultNetworks.all.map { it.name }, networks.map { SheetChecker.findNetwork(it, DefaultNetworks.all)?.name })
            val banks = lists.getValue("Banks")
            assertEquals(name, BankCatalog.all.map { it.name }, banks.map { BankCatalog.find(it)?.name })
            assertEquals(name, builtInTypes, lists.getValue("CardTypes"))
            assertEquals(name, (1..12).toList(), lists.getValue("Months").map { ImportSchema.parseMonth(it) })
            assertEquals(name, (1..31).toList(), lists.getValue("Days").map { ImportSchema.parseDueDay(it) })
            val years = lists.getValue("Years").map { ImportSchema.parseYear(it) }
            assertEquals(name, ImportSchema.YEARS_AHEAD + 1, years.size)
            assertEquals(name, lists.getValue("qc_schema"), listOf(ImportSchema.VERSION.toString()))
            assertEquals(name, lists.getValue("qc_catalog"), listOf(BankCatalog.VERSION))
        }
    }

    @Test
    fun `the schema's own list labels round-trip through the matchers`() {
        ImportSchema.monthLabels.forEachIndexed { i, label -> assertEquals(label, i + 1, ImportSchema.parseMonth(label)) }
        ImportSchema.yearLabels(YearMonth.of(2026, 11)).forEach { assertTrue(it, ImportSchema.parseYear(it) != null) }
        DefaultNetworks.all.forEach { n -> assertEquals(n.name, n.id, SheetChecker.findNetwork(ImportSchema.networkLabel(n), DefaultNetworks.all)?.id) }
        ImportSchema.bankLabels(emptyList()).forEach { label -> assertTrue(label, BankCatalog.find(label) != null) }
    }

    @Test
    fun `the filled fixtures check as expected end to end`() {
        val context = SheetContext(
            networks = DefaultNetworks.all,
            banks = BankCatalog.all.map { it.name },
            types = builtInTypes,
            holders = listOf("Priya S"),
            existing = { null },
            today = YearMonth.of(2026, 11)
        )
        val xlsx = SheetChecker.check(XlsxReader.cardsSheet(XlsxReader.read(fixture("generated-rows.xlsx")))!!.rows, context)
        assertEquals(null, xlsx.fileProblem)
        assertEquals(listOf(RowStatus.READY, RowStatus.READY, RowStatus.READY, RowStatus.NEEDS_FIXING, RowStatus.NEEDS_FIXING, RowStatus.NEEDS_FIXING), xlsx.rows.map { it.status })
        assertEquals(listOf(3, 4, 5, 6, 7, 8), xlsx.rows.map { it.row })
        assertEquals(listOf(RowProblem.NumberLuhn), xlsx.rows[3].problems)
        assertEquals(listOf(RowProblem.NetworkMismatch("Visa", "RuPay")), xlsx.rows[4].problems)
        assertTrue(xlsx.rows[5].problems.containsAll(listOf(
            RowProblem.Missing(ImportColumn.HOLDER), RowProblem.NumberChangedBySpreadsheet,
            RowProblem.NetworkUnknown("Foo"), RowProblem.BankUnknown("Foo Bank"),
            RowProblem.TypeUnknown("Gold"), RowProblem.DueDayInvalid
        )))
        assertEquals(listOf("Arjun"), xlsx.newHolders)
        assertEquals("American Express", xlsx.rows[1].card?.bankName)
        assertEquals("SBI Card", xlsx.rows[2].card?.bankName)

        listOf("filled-semicolon.csv", "filled-banner.csv").forEach { name ->
            val report = SheetChecker.check(CsvReader.read(String(fixture(name), Charsets.UTF_8)), context)
            assertEquals(name, null, report.fileProblem)
            assertEquals(name, 3, report.ready.size)
            assertEquals(name, "01/30", report.rows[1].card?.expiry)
            assertEquals(name, "12/28", report.rows[2].card?.expiry)
            assertEquals(name, "Salary, account", report.rows[2].card?.note)
        }
    }
}
