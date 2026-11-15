package com.quickcards.domain.sheet

import io.kotest.property.Arb
import io.kotest.property.PropTestConfig
import io.kotest.property.arbitrary.int
import io.kotest.property.arbitrary.list
import io.kotest.property.arbitrary.map
import io.kotest.property.arbitrary.string
import io.kotest.property.checkAll
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CsvReaderTest {

    @Test
    fun `comma, semicolon and tab files read the same`() {
        val expected = listOf(listOf("a", "b", "c"), listOf("1", "2", "3"))
        assertEquals(expected, CsvReader.read("a,b,c\n1,2,3"))
        assertEquals(expected, CsvReader.read("a;b;c\n1;2;3"))
        assertEquals(expected, CsvReader.read("a\tb\tc\n1\t2\t3"))
    }

    @Test
    fun `quotes hold separators, doubled quotes and line breaks`() {
        val rows = CsvReader.read("\"x, y\",\"he said \"\"hi\"\"\",\"two\nlines\"\r\nplain,,end")
        assertEquals(listOf(listOf("x, y", "he said \"hi\"", "two\nlines"), listOf("plain", "", "end")), rows)
    }

    @Test
    fun `a BOM, CRLF endings and a trailing newline are ignored`() {
        assertEquals(listOf(listOf("a", "b"), listOf("1", "2")), CsvReader.read("﻿a,b\r\n1,2\r\n"))
    }

    @Test
    fun `cells are trimmed and blank text gives no rows`() {
        assertEquals(listOf(listOf("a", "b")), CsvReader.read(" a , b "))
        assertTrue(CsvReader.read("").isEmpty())
        assertTrue(CsvReader.read("\n\n").isEmpty() || CsvReader.read("\n\n").all { r -> r.all { it.isEmpty() } })
    }

    @Test
    fun `any quoted grid round-trips`() = runBlocking<Unit> {
        val cell = Arb.string(0..12).map { it.replace('\r', ' ').trim() }
        val row = Arb.list(cell, 1..6)
        checkAll(300, PropTestConfig(seed = 7), Arb.list(row, 1..8), Arb.int(0..2)) { grid, which ->
            val delimiter = listOf(',', ';', '\t')[which]
            val width = grid.maxOf { it.size }
            val padded = grid.map { r -> r + List(width - r.size) { "" } }
            val text = padded.joinToString("\r\n") { r ->
                r.joinToString(delimiter.toString()) { c -> "\"" + c.replace("\"", "\"\"") + "\"" }
            }
            val back = CsvReader.read(text)
            val nonBlank = padded.filter { r -> r.any { it.isNotEmpty() } }
            assertEquals(text, nonBlank.map { r -> r.map { it.trim() } }, back.filter { r -> r.any { it.isNotEmpty() } })
        }
    }
}
