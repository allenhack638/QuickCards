package com.quickcards.domain.sheet

import com.quickcards.domain.BankCatalog
import com.quickcards.domain.CardNetwork
import java.time.YearMonth
import java.util.Locale

/** A column of the import sheet. [key] is stable; [header] is what the sheet shows. */
enum class ImportColumn(val key: String, val header: String, val required: Boolean) {
    HOLDER("holder", "Card holder", true),
    NUMBER("number", "Card number", true),
    MONTH("month", "Expiry month", true),
    YEAR("year", "Expiry year", true),
    CVV("cvv", "CVV", true),
    NETWORK("network", "Network", true),
    BANK("bank", "Bank", true),
    TYPE("type", "Card type", true),
    DUE_DAY("dueDay", "Payment due day", false),
    NOTE("note", "Note", false);

    /** The heading as written in the sheet: required columns carry a trailing `*`. */
    val sheetHeader: String get() = if (required) "$header*" else header
}

/**
 * The one description of the import sheet: columns, headings, list values and how typed
 * values are read. The sheet generator, the Rules tab, the importer and the guard tests all
 * read this object, so none of them can drift apart.
 */
object ImportSchema {

    /** Bumped whenever a column is added, removed or renamed; stamped into generated sheets. */
    const val VERSION = 1

    const val MAX_ROWS = 2_000
    const val MAX_FILE_BYTES = 5L * 1024 * 1024
    const val YEARS_AHEAD = 20

    val columns: List<ImportColumn> = ImportColumn.entries
    val headers: List<String> = columns.map { it.sheetHeader }

    val monthNames: List<String> = listOf(
        "January", "February", "March", "April", "May", "June",
        "July", "August", "September", "October", "November", "December"
    )

    /** "08 - August": the number first so the list sorts naturally and the name helps. */
    val monthLabels: List<String> = monthNames.mapIndexed { i, name -> "%02d - %s".format(Locale.ROOT, i + 1, name) }

    fun yearLabels(today: YearMonth): List<String> = (today.year..today.year + YEARS_AHEAD).map { it.toString() }

    val dayLabels: List<String> = (1..31).map { it.toString() }

    /** List value for a network: the name, plus the code as a hint when it reads differently. */
    fun networkLabel(network: CardNetwork): String {
        val code = network.code.trim()
        val hinted = code.length >= 4 && !key(network.name).contains(key(code))
        return if (hinted) "${network.name} ($code)" else network.name
    }

    /** Bank list values: the catalog labels, then the user's own banks that are not in it. */
    fun bankLabels(userBanks: List<String>): List<String> {
        val extra = userBanks.filter { BankCatalog.find(it) == null }.sortedBy { key(it) }
        return BankCatalog.all.map { it.label } + extra
    }

    /** Heading match: case, spaces, punctuation and the `*` do not matter. */
    fun headerKey(raw: String): String = key(raw.replace("*", ""))

    fun columnFor(header: String): ImportColumn? {
        val k = headerKey(header)
        return columns.firstOrNull { headerKey(it.header) == k }
    }

    /** Case-insensitive, bracketed hints dropped, punctuation ignored, spaces collapsed. */
    fun key(raw: String): String = BankCatalog.key(raw)

    /** 1..12 from "08 - August", "8", "08", "Aug", "August"; null otherwise. */
    fun parseMonth(raw: String): Int? {
        val text = raw.trim()
        if (text.isEmpty()) return null
        val leading = Regex("""^(\d{1,2})\b""").find(text)?.groupValues?.get(1)?.toIntOrNull()
        if (leading != null) return leading.takeIf { it in 1..12 }
        val k = key(text)
        val index = monthNames.indexOfFirst { name -> key(name) == k || key(name.take(3)) == k }
        return if (index >= 0) index + 1 else null
    }

    /** A four-digit year from "2029" or "29"; null otherwise. */
    fun parseYear(raw: String): Int? {
        val digits = raw.trim()
        if (!digits.all { it.isDigit() }) return null
        return when (digits.length) {
            2 -> 2000 + digits.toInt()
            4 -> digits.toInt()
            else -> null
        }
    }

    /** 1..31 from "5" or "05"; null for blank or anything else. */
    fun parseDueDay(raw: String): Int? = raw.trim().takeIf { it.isNotEmpty() && it.all(Char::isDigit) }?.toIntOrNull()?.takeIf { it in 1..31 }
}
