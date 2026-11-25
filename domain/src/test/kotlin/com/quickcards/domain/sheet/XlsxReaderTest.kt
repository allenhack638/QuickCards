package com.quickcards.domain.sheet

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XlsxReaderTest {

    private fun fixture(name: String): ByteArray =
        checkNotNull(javaClass.getResourceAsStream("/sheet/$name")) { "missing fixture $name" }.readBytes()

    @Test
    fun `the generated sheet reads back with its three tabs and headings in row 2`() {
        val sheets = XlsxReader.read(fixture("generated-empty.xlsx"))
        assertEquals(listOf("Cards", "Rules", "Lists"), sheets.map { it.name })
        val cards = XlsxReader.cardsSheet(sheets)!!
        assertTrue(cards.rows[0][0].startsWith("First make your own copy"))
        assertEquals(ImportSchema.headers, cards.rows[1])
    }

    @Test
    fun `a Google Sheets export reads the same way`() {
        val cards = XlsxReader.cardsSheet(XlsxReader.read(fixture("google-sheets-export-empty.xlsx")))!!
        assertEquals(PublishedSheet.headers, cards.rows[1].take(PublishedSheet.headers.size))
    }

    @Test
    fun `filled rows keep their text exactly, including a number the spreadsheet mangled`() {
        val cards = XlsxReader.cardsSheet(XlsxReader.read(fixture("generated-rows.xlsx")))!!
        assertEquals(listOf("Priya S", "4111111111111111", "08 - August", "2029", "123", "Visa", "HDFC Bank", "Credit", "18", "5", "Fuel card"), cards.rows[2])
        assertEquals("4.11111E+15", cards.rows[7][1])
        assertEquals("", cards.rows[7][0])
    }

    @Test
    fun `zip sniffing and non-spreadsheets`() {
        assertTrue(XlsxReader.looksLikeXlsx(fixture("generated-empty.xlsx").copyOf(8)))
        assertFalse(XlsxReader.looksLikeXlsx("Card holder*,Card number*".toByteArray()))
        assertFalse(XlsxReader.looksLikeXlsx(ByteArray(2)))
        val error = runCatching { XlsxReader.read("not a zip".toByteArray()) }.exceptionOrNull()
        assertTrue(error.toString(), error != null)
    }
}
