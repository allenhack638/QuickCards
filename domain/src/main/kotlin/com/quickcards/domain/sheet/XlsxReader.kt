package com.quickcards.domain.sheet

import java.io.ByteArrayInputStream
import java.io.InputStream
import java.util.zip.ZipInputStream
import javax.xml.parsers.SAXParserFactory
import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler

/** The cells of one worksheet, row by row, as the text a person would see. */
data class XlsxSheet(val name: String, val rows: List<List<String>>)

/**
 * Reads a .xlsx with nothing but `java.util.zip` and SAX, so it runs the same on the JVM and on
 * Android and needs no spreadsheet library. Only what the import needs: shared, inline and
 * plain strings, numbers (as the sheet stored them, so `4.11111E+15` arrives as such and the
 * checker can name it), booleans and formulas' cached values. Styles and charts are ignored.
 */
object XlsxReader {

    private const val MAX_CELLS = 400_000

    /** True when the bytes start like a zip container (every .xlsx does). */
    fun looksLikeXlsx(head: ByteArray): Boolean =
        head.size >= 4 && head[0] == 'P'.code.toByte() && head[1] == 'K'.code.toByte()

    /** All worksheets in workbook order; the import picks "Cards" or the first one. */
    fun read(bytes: ByteArray): List<XlsxSheet> = read(ByteArrayInputStream(bytes))

    fun read(input: InputStream): List<XlsxSheet> {
        val parts = mutableMapOf<String, ByteArray>()
        ZipInputStream(input).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                val name = entry.name
                if (name == "xl/workbook.xml" || name == "xl/sharedStrings.xml" ||
                    name == "xl/_rels/workbook.xml.rels" || name.startsWith("xl/worksheets/sheet")
                ) {
                    parts[name] = zip.readBytes()
                }
                zip.closeEntry()
                entry = zip.nextEntry
            }
        }
        val workbook = parts["xl/workbook.xml"] ?: throw IllegalArgumentException("Not a spreadsheet: no workbook")
        val shared = parts["xl/sharedStrings.xml"]?.let(::readSharedStrings).orEmpty()
        val targets = parts["xl/_rels/workbook.xml.rels"]?.let(::readRelationships).orEmpty()
        return readSheetList(workbook).map { (name, rid) ->
            val target = targets[rid]?.removePrefix("/")?.let { if (it.startsWith("xl/")) it else "xl/$it" }
            val xml = target?.let { parts[it] } ?: throw IllegalArgumentException("Sheet part missing for $name")
            XlsxSheet(name, readSheet(xml, shared))
        }
    }

    /** The "Cards" sheet if there is one, else the first; null when the workbook is empty. */
    fun cardsSheet(sheets: List<XlsxSheet>): XlsxSheet? =
        sheets.firstOrNull { it.name.equals("Cards", ignoreCase = true) } ?: sheets.firstOrNull()

    private fun parse(xml: ByteArray, handler: DefaultHandler) {
        val factory = SAXParserFactory.newInstance()
        factory.isNamespaceAware = false
        runCatching { factory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) }
        runCatching { factory.setFeature("http://xml.org/sax/features/external-general-entities", false) }
        runCatching { factory.setFeature("http://xml.org/sax/features/external-parameter-entities", false) }
        factory.newSAXParser().parse(ByteArrayInputStream(xml), handler)
    }

    private fun readSharedStrings(xml: ByteArray): List<String> {
        val strings = mutableListOf<String>()
        parse(xml, object : DefaultHandler() {
            private val current = StringBuilder()
            private var inText = false
            private var inItem = false
            override fun startElement(uri: String?, local: String?, qName: String, attrs: Attributes?) {
                when (qName) {
                    "si" -> { inItem = true; current.setLength(0) }
                    "t" -> inText = true
                }
            }
            override fun characters(ch: CharArray, start: Int, length: Int) {
                if (inText) current.appendRange(ch, start, start + length)
            }
            override fun endElement(uri: String?, local: String?, qName: String) {
                when (qName) {
                    "t" -> inText = false
                    "si" -> { inItem = false; strings.add(current.toString()) }
                }
            }
        })
        return strings
    }

    private fun readRelationships(xml: ByteArray): Map<String, String> {
        val map = mutableMapOf<String, String>()
        parse(xml, object : DefaultHandler() {
            override fun startElement(uri: String?, local: String?, qName: String, attrs: Attributes) {
                if (qName == "Relationship") {
                    val id = attrs.getValue("Id") ?: return
                    val target = attrs.getValue("Target") ?: return
                    map[id] = target
                }
            }
        })
        return map
    }

    private fun readSheetList(xml: ByteArray): List<Pair<String, String>> {
        val sheets = mutableListOf<Pair<String, String>>()
        parse(xml, object : DefaultHandler() {
            override fun startElement(uri: String?, local: String?, qName: String, attrs: Attributes) {
                if (qName == "sheet") {
                    val name = attrs.getValue("name") ?: return
                    val rid = attrs.getValue("r:id") ?: attrs.getValue("id") ?: return
                    sheets.add(name to rid)
                }
            }
        })
        return sheets
    }

    private fun readSheet(xml: ByteArray, shared: List<String>): List<List<String>> {
        val rows = mutableListOf<MutableList<String>>()
        var cells = 0
        parse(xml, object : DefaultHandler() {
            private var row: MutableList<String>? = null
            private var column = -1
            private var type = ""
            private val value = StringBuilder()
            private var inValue = false
            private var inInline = false
            override fun startElement(uri: String?, local: String?, qName: String, attrs: Attributes) {
                when (qName) {
                    "row" -> row = mutableListOf()
                    "c" -> {
                        column = columnIndex(attrs.getValue("r"))
                        type = attrs.getValue("t").orEmpty()
                        value.setLength(0)
                    }
                    "v" -> { inValue = true; value.setLength(0) }
                    "is" -> { inInline = true; value.setLength(0) }
                    "t" -> if (inInline) inValue = true
                }
            }
            override fun characters(ch: CharArray, start: Int, length: Int) {
                if (inValue) value.appendRange(ch, start, start + length)
            }
            override fun endElement(uri: String?, local: String?, qName: String) {
                when (qName) {
                    "v" -> inValue = false
                    "t" -> if (inInline) inValue = false
                    "is" -> inInline = false
                    "c" -> {
                        val current = row ?: return
                        val text = when (type) {
                            "s" -> value.toString().toIntOrNull()?.let { shared.getOrNull(it) }.orEmpty()
                            "b" -> if (value.toString() == "1") "TRUE" else "FALSE"
                            else -> value.toString()
                        }
                        if (++cells > MAX_CELLS) throw IllegalArgumentException("Sheet too large")
                        val index = if (column >= 0) column else current.size
                        while (current.size < index) current.add("")
                        if (current.size == index) current.add(text) else current[index] = text
                        column = -1
                        type = ""
                    }
                    "row" -> { row?.let { rows.add(it) }; row = null }
                }
            }
        })
        return rows.map { r -> r.map { it.trim() } }
    }

    /** "C7" -> 2; null or odd references fall back to append order. */
    private fun columnIndex(ref: String?): Int {
        if (ref == null) return -1
        var index = 0
        var seen = false
        for (ch in ref) {
            if (ch in 'A'..'Z') { index = index * 26 + (ch - 'A' + 1); seen = true } else break
        }
        return if (seen) index - 1 else -1
    }
}
