package com.quickcards.domain.sheet

import com.quickcards.domain.BankCatalog
import com.quickcards.domain.DefaultNetworks
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.YearMonth
import java.util.zip.ZipInputStream

/** The on-device generator must produce exactly what the importer and the sync guard expect. */
class SheetTemplateTest {

    private val today = YearMonth.of(2026, 11)
    private val types = listOf("Credit", "Debit", "Prepaid", "Forex")
    private val lists = SheetTemplate.lists(DefaultNetworks.all, listOf("My Co-op Bank"), types, today)

    private fun parts(bytes: ByteArray): Map<String, String> {
        val map = mutableMapOf<String, String>()
        ZipInputStream(bytes.inputStream()).use { zip ->
            var e = zip.nextEntry
            while (e != null) { map[e.name] = String(zip.readBytes(), Charsets.UTF_8); e = zip.nextEntry }
        }
        return map
    }

    @Test
    fun `the generated workbook reads back as the schema says`() {
        val bytes = SheetTemplate.xlsx(lists)
        val sheets = XlsxReader.read(bytes)
        assertEquals(listOf("Cards", "Rules", "Lists"), sheets.map { it.name })
        val cards = XlsxReader.cardsSheet(sheets)!!
        assertEquals(SheetTemplate.BANNER, cards.rows[0][0])
        assertEquals(ImportSchema.headers, cards.rows[1])
        val listsTab = sheets[2]
        assertEquals(listOf("Networks", "Banks", "CardTypes", "Months", "Years", "Days", "", "qc_schema", "qc_catalog"), listsTab.rows[0])
        assertEquals(ImportSchema.VERSION.toString(), listsTab.rows[1][7])
        assertEquals(BankCatalog.VERSION, listsTab.rows[1][8])
        val banks = listsTab.rows.drop(1).mapNotNull { it.getOrNull(1)?.takeIf(String::isNotEmpty) }
        assertEquals(BankCatalog.all.map { it.name } + "My Co-op Bank", banks.map { BankCatalog.find(it)?.name ?: it })
    }

    @Test
    fun `every column has a reject-input rule and the workbook has no formulas`() {
        val xml = parts(SheetTemplate.xlsx(lists)).getValue("xl/worksheets/sheet1.xml")
        assertEquals(ImportSchema.columns.size, Regex("<dataValidation ").findAll(xml).count())
        assertEquals(ImportSchema.columns.size, Regex("""errorStyle="stop"""").findAll(xml).count())
        assertFalse(Regex("<c [^>]*><f[ >]").containsMatchIn(xml))
        assertTrue(xml.contains("""<mergeCell ref="A1:J1"/>"""))
    }

    @Test
    fun `the rules tab names every column and the sheet checks itself through the checker`() {
        val rules = XlsxReader.read(SheetTemplate.xlsx(lists))[1].rows.flatten()
        SheetTemplate.columnRules.forEach { assertTrue(it[0], rules.contains(it[0])) }
        assertTrue(rules.any { it.startsWith("4. After the import, delete the file") })

        val filled = SheetTemplate.xlsx(lists, rows = listOf(listOf("Priya", "4111111111111111", "08 - August", "2029", "123", "Visa", "HDFC Bank", "Credit", "", "")))
        val context = SheetContext(DefaultNetworks.all, BankCatalog.all.map { it.name }, types, emptyList(), { null }, today)
        val report = SheetChecker.check(XlsxReader.cardsSheet(XlsxReader.read(filled))!!.rows, context)
        assertEquals(1, report.ready.size)
    }

    @Test
    fun `the CSV files carry the headings and every allowed value`() {
        assertEquals(listOf(ImportSchema.headers), CsvReader.read(SheetTemplate.csv()))
        val values = CsvReader.read(SheetTemplate.valuesCsv(lists))
        assertEquals(listOf("Networks", "Banks", "CardTypes", "Months", "Years", "Days"), values[0])
        assertEquals(lists.days.size + 1, values.size)
        assertEquals("08 - August", values[8][3])
    }

    /** Writes the sheet to build/ so it can be uploaded as the public Google Sheet. */
    @Test
    fun `write the publishable sheet`() {
        val dir = File("build/sheet").apply { mkdirs() }
        File(dir, SheetTemplate.FILE_NAME).writeBytes(SheetTemplate.xlsx(SheetTemplate.lists(DefaultNetworks.all, emptyList(), types, today)))
        File(dir, SheetTemplate.CSV_FILE_NAME).writeText(SheetTemplate.csv())
        File(dir, SheetTemplate.VALUES_FILE_NAME).writeText(SheetTemplate.valuesCsv(SheetTemplate.lists(DefaultNetworks.all, emptyList(), types, today)))
        assertTrue(File(dir, SheetTemplate.FILE_NAME).length() > 10_000)
    }
}
