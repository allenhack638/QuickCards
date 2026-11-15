package com.quickcards.domain.sheet

/**
 * Reads the text of a .csv into rows of cells. Comma, semicolon and tab separators are
 * detected from the first non-empty line; quoted cells may hold separators, quotes ("") and
 * line breaks; CR, LF and CRLF all end a row; a leading UTF-8 BOM is ignored.
 */
object CsvReader {

    private const val BOM = '﻿'
    private val CANDIDATES = charArrayOf(',', ';', '\t')

    fun read(text: String): List<List<String>> {
        val body = text.removePrefix(BOM.toString())
        if (body.isBlank()) return emptyList()
        val delimiter = detectDelimiter(body)
        val rows = mutableListOf<List<String>>()
        var row = mutableListOf<String>()
        val cell = StringBuilder()
        var quoted = false
        var i = 0
        while (i < body.length) {
            val c = body[i]
            when {
                quoted -> when {
                    c == '"' && i + 1 < body.length && body[i + 1] == '"' -> { cell.append('"'); i++ }
                    c == '"' -> quoted = false
                    else -> cell.append(c)
                }
                c == '"' -> quoted = true
                c == delimiter -> { row.add(cell.toString()); cell.setLength(0) }
                c == '\r' || c == '\n' -> {
                    if (c == '\r' && i + 1 < body.length && body[i + 1] == '\n') i++
                    row.add(cell.toString()); cell.setLength(0)
                    rows.add(row); row = mutableListOf()
                }
                else -> cell.append(c)
            }
            i++
        }
        if (cell.isNotEmpty() || row.isNotEmpty()) {
            row.add(cell.toString())
            rows.add(row)
        }
        return rows.map { cells -> cells.map { it.trim() } }
    }

    /** The candidate that appears most often outside quotes on the first non-empty line. */
    fun detectDelimiter(text: String): Char {
        val line = text.lineSequence().firstOrNull { it.isNotBlank() } ?: return ','
        var quoted = false
        val counts = IntArray(CANDIDATES.size)
        for (c in line) {
            if (c == '"') quoted = !quoted
            if (!quoted) CANDIDATES.indexOf(c).takeIf { it >= 0 }?.let { counts[it]++ }
        }
        val best = counts.indices.maxByOrNull { counts[it] } ?: 0
        return if (counts[best] == 0) ',' else CANDIDATES[best]
    }
}
