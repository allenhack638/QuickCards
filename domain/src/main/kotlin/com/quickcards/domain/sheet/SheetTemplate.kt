package com.quickcards.domain.sheet

import com.quickcards.domain.BankCatalog
import com.quickcards.domain.CardNetwork
import java.io.ByteArrayOutputStream
import java.time.YearMonth
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * Generates the import sheet on the phone from [ImportSchema] and the phone's own lists, so a
 * downloaded sheet always carries the current headings, networks, banks and card types.
 * Plain .xlsx: no cell formulas, no macros; list validations on a hidden "Lists" tab; a
 * "Rules" tab that spells out every rule. The public Google Sheet is this same output.
 *
 * The sheet is a generated English document (its list values are English names), so its
 * wording lives here with the schema rather than in Android string resources.
 */
object SheetTemplate {

    const val ROWS = 1_000
    const val FIRST_DATA_ROW = 3
    const val FILE_NAME = "QuickCards - Add your cards.xlsx"
    const val CSV_FILE_NAME = "QuickCards - Add your cards.csv"
    const val VALUES_FILE_NAME = "QuickCards - Allowed values.csv"

    const val BANNER = "First make your own copy: File > Make a copy.   Then add your cards in your copy, one card per row."

    /** Shown when a cell is selected and, in Google Sheets, when a value is refused. */
    val help: Map<ImportColumn, String> = mapOf(
        ImportColumn.HOLDER to "The person this card belongs to, as printed on the card. Up to 40 characters.",
        ImportColumn.NUMBER to "Digits only, no spaces. 12 to 19 digits. Example: 4111111111111111",
        ImportColumn.MONTH to "Pick the month from the list.",
        ImportColumn.YEAR to "Pick the year from the list.",
        ImportColumn.CVV to "3 digits (4 for American Express). Digits only.",
        ImportColumn.NETWORK to "Pick the card's network from the list. It must match the card number.",
        ImportColumn.BANK to "Pick your bank from the list. Not there? Add that card in the app instead.",
        ImportColumn.TYPE to "Pick from the list: Credit, Debit, Prepaid or Forex.",
        ImportColumn.DUE_DAY to "Optional. Pick the day of the month your bill is due.",
        ImportColumn.NOTE to "Optional. Up to 80 characters."
    )

    /** Column, required, what is allowed, example: the table on the Rules tab. */
    val columnRules: List<List<String>> = listOf(
        listOf("Card holder", "Yes", "The person this card belongs to, as printed on the card. 1 to 40 characters. This name is shown on the card in QuickCards. New names are added to QuickCards.", "Priya S"),
        listOf("Card number", "Yes", "Digits only, no spaces. 12 to 19 digits. QuickCards also checks that it is a real card number.", "4111111111111111"),
        listOf("Expiry month", "Yes", "Pick from the list.", "08 - August"),
        listOf("Expiry year", "Yes", "Pick from the list. Expired cards are not imported.", "2029"),
        listOf("CVV", "Yes", "3 digits (4 for American Express). Digits only. If it starts with 0, type the 0 too.", "123"),
        listOf("Network", "Yes", "Pick from the list. It must match the card number: QuickCards checks this when you import and refuses a row where they disagree (for example a Visa number with Network set to RuPay).", "Visa"),
        listOf("Bank", "Yes", "Pick from the list.", "HDFC Bank"),
        listOf("Card type", "Yes", "Pick from the list: Credit, Debit, Prepaid or Forex.", "Credit"),
        listOf("Payment due day", "No", "Pick the day of the month your bill is due (1 to 31).", "5"),
        listOf("Note", "No", "Anything you want to remember. Up to 80 characters.", "Fuel card")
    )

    val beforeYouStart: List<String> = listOf(
        "1. Make your own copy of this sheet: File > Make a copy. The original is view only.",
        "2. Fill in the Cards tab, one card per row. Start on row 3.",
        "3. Do not change, move or rename the headings in row 2, and do not add tabs or formulas.",
        "4. Columns marked with * must be filled in. A card with an empty * column is not imported."
    )

    val goodToKnow: List<String> = listOf(
        "- Network, Bank and Card type accept only the values in their lists. If your bank, network or card type is not there, leave that card out of the sheet and add it in QuickCards by hand (tap + on the home screen).",
        "- The list columns refuse typed or pasted values that are not in the list. Pasting a whole row can remove a cell's list; QuickCards checks every row again when you import, so nothing wrong gets in.",
        "- The sheet checks every cell as you type. If a value is refused, the message tells you what to fix.",
        "- QuickCards does not save anything until you see the check screen. It lists every row as Ready, Needs fixing (with the reason) or Already in QuickCards.",
        "- A card already in QuickCards (same number, expiry and CVV) is skipped, so importing the same sheet twice is safe.",
        "- The words in brackets in the lists, like (Amex), are only there to help you find the right entry."
    )

    val whenDone: List<String> = listOf(
        "1. Google Sheets: File > Download > Microsoft Excel (.xlsx). Comma-separated values (.csv) also works.",
        "2. Excel or another app: save the file as .xlsx or .csv. Other formats are not accepted.",
        "3. In QuickCards: Settings > Backup > Import cards > Cards from a spreadsheet > Proceed, then choose the file.",
        "4. After the import, delete the file and this copy. They hold your card numbers."
    )

    /** The lists behind the dropdowns, in the order the Lists tab writes them. */
    data class Lists(
        val networks: List<String>,
        val banks: List<String>,
        val types: List<String>,
        val months: List<String>,
        val years: List<String>,
        val days: List<String>
    ) {
        fun named(): List<Pair<String, List<String>>> = listOf(
            "Networks" to networks, "Banks" to banks, "CardTypes" to types,
            "Months" to months, "Years" to years, "Days" to days
        )
    }

    fun lists(
        networks: List<CardNetwork>,
        userBanks: List<String>,
        types: List<String>,
        today: YearMonth
    ): Lists = Lists(
        networks = networks.map { ImportSchema.networkLabel(it) },
        banks = ImportSchema.bankLabels(userBanks),
        types = types,
        months = ImportSchema.monthLabels,
        years = ImportSchema.yearLabels(today),
        days = ImportSchema.dayLabels
    )

    /** The .xlsx bytes. [rows] pre-fills the Cards tab (tests and samples only). */
    fun xlsx(lists: Lists, rows: List<List<String>> = emptyList()): ByteArray {
        val strings = SharedStrings()
        val sheets = mapOf(
            "sheet1" to cardsSheet(lists, rows, strings),
            "sheet2" to rulesSheet(strings),
            "sheet3" to listsSheet(lists, strings)
        )
        val defined = lists.named().mapIndexed { i, (name, values) ->
            """<definedName name="$name">Lists!$${col(i + 1)}$2:$${col(i + 1)}$${values.size + 1}</definedName>"""
        }.joinToString("")
        val parts = linkedMapOf(
            "[Content_Types].xml" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types"><Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/><Default Extension="xml" ContentType="application/xml"/><Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>${sheets.keys.joinToString("") { """<Override PartName="/xl/worksheets/$it.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>""" }}<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/><Override PartName="/xl/sharedStrings.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sharedStrings+xml"/></Types>""",
            "_rels/.rels" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="$R/officeDocument" Target="xl/workbook.xml"/></Relationships>""",
            "xl/workbook.xml" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><workbook $NS xmlns:r="$R"><sheets><sheet name="Cards" sheetId="1" r:id="rId1"/><sheet name="Rules" sheetId="2" r:id="rId2"/><sheet name="Lists" sheetId="3" state="hidden" r:id="rId3"/></sheets><definedNames>$defined</definedNames></workbook>""",
            "xl/_rels/workbook.xml.rels" to """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships"><Relationship Id="rId1" Type="$R/worksheet" Target="worksheets/sheet1.xml"/><Relationship Id="rId2" Type="$R/worksheet" Target="worksheets/sheet2.xml"/><Relationship Id="rId3" Type="$R/worksheet" Target="worksheets/sheet3.xml"/><Relationship Id="rId4" Type="$R/styles" Target="styles.xml"/><Relationship Id="rId5" Type="$R/sharedStrings" Target="sharedStrings.xml"/></Relationships>""",
            "xl/styles.xml" to STYLES
        )
        sheets.forEach { (name, xml) -> parts["xl/worksheets/$name.xml"] = xml }
        parts["xl/sharedStrings.xml"] = strings.xml()
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            parts.forEach { (name, text) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(text.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    /** The simple CSV: headings only. */
    fun csv(): String = CsvWriter.write(listOf(ImportSchema.headers))

    /** The allowed values as a CSV, one list per column, for people who use the simple CSV. */
    fun valuesCsv(lists: Lists): String {
        val columns = lists.named()
        val height = columns.maxOf { it.second.size }
        val grid = listOf(columns.map { it.first }) + (0 until height).map { r ->
            columns.map { (_, values) -> values.getOrNull(r).orEmpty() }
        }
        return CsvWriter.write(grid)
    }

    // ---- workbook parts ----

    private const val NS = """xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main""""
    private const val R = "http://schemas.openxmlformats.org/officeDocument/2006/relationships"

    private class SharedStrings {
        private val index = linkedMapOf<String, Int>()
        fun id(text: String): Int = index.getOrPut(text) { index.size }
        fun xml(): String = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><sst $NS count="${index.size}" uniqueCount="${index.size}">""" +
            index.keys.joinToString("") { """<si><t xml:space="preserve">${esc(it)}</t></si>""" } + "</sst>"
    }

    private fun esc(text: String): String = text
        .replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")

    private fun col(n: Int): String {
        var value = n
        val out = StringBuilder()
        while (value > 0) {
            val rem = (value - 1) % 26
            out.insert(0, ('A' + rem))
            value = (value - 1) / 26
        }
        return out.toString()
    }

    private fun cell(ref: String, text: String, style: Int, strings: SharedStrings): String =
        """<c r="$ref" s="$style" t="s"><v>${strings.id(text)}</v></c>"""

    private val widths = listOf(20, 24, 14, 13, 8, 18, 30, 12, 16, 30)

    private fun cardsSheet(lists: Lists, rows: List<List<String>>, strings: SharedStrings): String {
        val columns = ImportSchema.columns
        val n = columns.size
        val lastRow = FIRST_DATA_ROW + ROWS - 1
        val cols = widths.mapIndexed { i, w -> """<col min="${i + 1}" max="${i + 1}" width="$w" customWidth="1" style="2"/>""" }.joinToString("")
        val banner = """<row r="1" ht="42" customHeight="1">""" + cell("A1", BANNER, 4, strings) +
            (2..n).joinToString("") { """<c r="${col(it)}1" s="4"/>""" } + "</row>"
        val head = """<row r="2" ht="28" customHeight="1">""" +
            columns.mapIndexed { i, c -> cell("${col(i + 1)}2", c.sheetHeader, 1, strings) }.joinToString("") + "</row>"
        val body = (FIRST_DATA_ROW..lastRow).joinToString("") { r ->
            val values = rows.getOrNull(r - FIRST_DATA_ROW)
            """<row r="$r">""" + (1..n).joinToString("") { i ->
                val v = values?.getOrNull(i - 1).orEmpty()
                if (v.isEmpty()) """<c r="${col(i)}$r" s="2"/>""" else cell("${col(i)}$r", v, 2, strings)
            } + "</row>"
        }
        val listNames = mapOf(
            ImportColumn.MONTH to "Months", ImportColumn.YEAR to "Years", ImportColumn.NETWORK to "Networks",
            ImportColumn.BANK to "Banks", ImportColumn.TYPE to "CardTypes", ImportColumn.DUE_DAY to "Days"
        )
        val validations = columns.mapIndexed { i, c ->
            val ref = "${col(i + 1)}$FIRST_DATA_ROW:${col(i + 1)}$lastRow"
            val title = esc(c.header)
            val prompt = esc(help.getValue(c))
            val first = "${col(i + 1)}$FIRST_DATA_ROW"
            when {
                listNames[c] != null -> """<dataValidation type="list" errorStyle="stop" allowBlank="1" showInputMessage="1" showErrorMessage="1" promptTitle="$title" prompt="$prompt" errorTitle="Pick from the list" error="$title: pick one of the values in the list. Typing or pasting something else is not accepted." sqref="$ref"><formula1>${listNames[c]}</formula1></dataValidation>"""
                c == ImportColumn.HOLDER -> custom(ref, title, prompt, "textLength", """ operator="between"""", "<formula1>1</formula1><formula2>40</formula2>", "Type the name of the person this card belongs to (up to 40 characters).")
                c == ImportColumn.NUMBER -> custom(ref, title, prompt, "custom", "", """<formula1>AND(LEN($first)&gt;=12,LEN($first)&lt;=19,ISNUMBER(VALUE($first)),ISERROR(FIND(" ",$first)),ISERROR(FIND(".",$first)),ISERROR(FIND("-",$first)),ISERROR(FIND("E",UPPER($first))))</formula1>""", "Digits only, no spaces. 12 to 19 digits. Example: 4111111111111111")
                c == ImportColumn.CVV -> custom(ref, title, prompt, "custom", "", """<formula1>AND(LEN($first)&gt;=3,LEN($first)&lt;=4,ISNUMBER(VALUE($first)),ISERROR(FIND(".",$first)),ISERROR(FIND("-",$first)))</formula1>""", "The CVV is 3 digits (4 for American Express). Digits only.")
                c == ImportColumn.NOTE -> custom(ref, title, prompt, "textLength", """ operator="lessThanOrEqual"""", "<formula1>80</formula1>", "A note can be up to 80 characters.")
                else -> ""
            }
        }.filter { it.isNotEmpty() }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet $NS><dimension ref="A1:${col(n)}$lastRow"/><sheetViews><sheetView workbookViewId="0" tabSelected="1"><pane ySplit="2" topLeftCell="A3" activePane="bottomLeft" state="frozen"/><selection pane="bottomLeft" activeCell="A3" sqref="A3"/></sheetView></sheetViews><sheetFormatPr defaultRowHeight="20" customHeight="1"/><cols>$cols</cols><sheetData>$banner$head$body</sheetData><mergeCells count="1"><mergeCell ref="A1:${col(n)}1"/></mergeCells><dataValidations count="${validations.size}">${validations.joinToString("")}</dataValidations></worksheet>"""
    }

    private fun custom(ref: String, title: String, prompt: String, type: String, operator: String, formulas: String, error: String): String =
        """<dataValidation type="$type"$operator errorStyle="stop" allowBlank="1" showInputMessage="1" showErrorMessage="1" promptTitle="$title" prompt="$prompt" errorTitle="Please check this" error="${esc(error)}" sqref="$ref">$formulas</dataValidation>"""

    private fun rulesSheet(strings: SharedStrings): String {
        val lines = mutableListOf<Pair<List<String>, Int>>() // cells to style
        lines += listOf("How to add your cards to QuickCards") to 7
        lines += listOf("") to 0
        lines += listOf("Before you start") to 3
        beforeYouStart.forEach { lines += listOf(it) to 0 }
        lines += listOf("") to 0
        lines += listOf("Column rules") to 3
        lines += listOf("Column", "Required", "What is allowed", "Example") to 5
        columnRules.forEach { lines += it to 6 }
        lines += listOf("") to 0
        lines += listOf("Good to know") to 3
        goodToKnow.forEach { lines += listOf(it) to 0 }
        lines += listOf("") to 0
        lines += listOf("When you are done") to 3
        whenDone.forEach { lines += listOf(it) to 0 }
        val rows = lines.mapIndexed { i, (cells, style) ->
            val r = i + 1
            val height = if (style == 7 || style == 3) """ ht="30" customHeight="1"""" else ""
            """<row r="$r"$height>""" + cells.mapIndexedNotNull { c, v ->
                if (v.isEmpty() && style != 5) null else cell("${col(c + 1)}$r", v, style, strings)
            }.joinToString("") + "</row>"
        }.joinToString("")
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet $NS><sheetViews><sheetView workbookViewId="0" showGridLines="0"/></sheetViews><sheetFormatPr defaultRowHeight="20" customHeight="1"/><cols><col min="1" max="1" width="16" customWidth="1"/><col min="2" max="2" width="10" customWidth="1"/><col min="3" max="3" width="78" customWidth="1"/><col min="4" max="4" width="20" customWidth="1"/></cols><sheetData>$rows</sheetData></worksheet>"""
    }

    private fun listsSheet(lists: Lists, strings: SharedStrings): String {
        val named = lists.named()
        val height = named.maxOf { it.second.size } + 1
        val stampCol = named.size + 2
        val rows = (1..height).joinToString("") { r ->
            val cells = named.mapIndexedNotNull { i, (name, values) ->
                val all = listOf(name) + values
                all.getOrNull(r - 1)?.let { cell("${col(i + 1)}$r", it, 2, strings) }
            }.toMutableList()
            if (r == 1) {
                cells += cell("${col(stampCol)}1", "qc_schema", 2, strings)
                cells += cell("${col(stampCol + 1)}1", "qc_catalog", 2, strings)
            } else if (r == 2) {
                cells += cell("${col(stampCol)}2", ImportSchema.VERSION.toString(), 2, strings)
                cells += cell("${col(stampCol + 1)}2", BankCatalog.VERSION, 2, strings)
            }
            """<row r="$r">${cells.joinToString("")}</row>"""
        }
        return """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><worksheet $NS><sheetData>$rows</sheetData></worksheet>"""
    }

    private val STYLES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?><styleSheet $NS>
<fonts count="5">
<font><sz val="11"/><color rgb="FF1F1F1F"/><name val="Arial"/></font>
<font><b/><sz val="11"/><color rgb="FF1F1F1F"/><name val="Arial"/></font>
<font><b/><sz val="11"/><color rgb="FF1F1F1F"/><name val="Arial"/></font>
<font><b/><sz val="11"/><color rgb="FF3C4043"/><name val="Arial"/></font>
<font><b/><sz val="15"/><color rgb="FF1F1F1F"/><name val="Arial"/></font>
</fonts>
<fills count="4"><fill><patternFill patternType="none"/></fill><fill><patternFill patternType="gray125"/></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FFF1F3F4"/><bgColor indexed="64"/></patternFill></fill>
<fill><patternFill patternType="solid"><fgColor rgb="FFFEF7E0"/><bgColor indexed="64"/></patternFill></fill></fills>
<borders count="2"><border><left/><right/><top/><bottom/><diagonal/></border>
<border><left style="thin"><color rgb="FFDEDBD4"/></left><right style="thin"><color rgb="FFDEDBD4"/></right><top style="thin"><color rgb="FFDEDBD4"/></top><bottom style="thin"><color rgb="FFDEDBD4"/></bottom><diagonal/></border></borders>
<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
<cellXfs count="8"><xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
<xf numFmtId="49" fontId="1" fillId="2" borderId="1" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
<xf numFmtId="49" fontId="0" fillId="0" borderId="1" xfId="0" applyNumberFormat="1" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>
<xf numFmtId="0" fontId="2" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1"><alignment vertical="center"/></xf>
<xf numFmtId="49" fontId="3" fillId="3" borderId="0" xfId="0" applyNumberFormat="1" applyFont="1" applyFill="1" applyAlignment="1"><alignment horizontal="center" vertical="center" wrapText="1"/></xf>
<xf numFmtId="0" fontId="1" fillId="2" borderId="1" xfId="0" applyFont="1" applyFill="1" applyBorder="1" applyAlignment="1"><alignment vertical="center"/></xf>
<xf numFmtId="0" fontId="0" fillId="0" borderId="1" xfId="0" applyBorder="1" applyAlignment="1"><alignment vertical="center" wrapText="1"/></xf>
<xf numFmtId="0" fontId="4" fillId="0" borderId="0" xfId="0" applyFont="1" applyAlignment="1"><alignment vertical="center"/></xf>
</cellXfs>
<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles></styleSheet>"""
}
