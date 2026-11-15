package com.quickcards.domain.sheet

/** Writes a grid as UTF-8 CSV with a BOM (so Excel on Windows reads it), CRLF rows, RFC 4180 quoting. */
object CsvWriter {

    fun write(rows: List<List<String>>): String = buildString {
        append('\uFEFF')
        rows.forEach { row ->
            append(row.joinToString(",") { quote(it) })
            append("\r\n")
        }
    }

    private fun quote(cell: String): String =
        if (cell.any { it == ',' || it == '"' || it == '\n' || it == '\r' || it == ';' }) {
            "\"" + cell.replace("\"", "\"\"") + "\""
        } else {
            cell
        }
}
