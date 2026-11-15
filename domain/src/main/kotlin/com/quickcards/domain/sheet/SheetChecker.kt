package com.quickcards.domain.sheet

import com.quickcards.domain.BankCatalog
import com.quickcards.domain.CardDraft
import com.quickcards.domain.CardError
import com.quickcards.domain.CardNetwork
import com.quickcards.domain.CardNumberRules
import com.quickcards.domain.CardValidator
import com.quickcards.domain.EntityValidator
import com.quickcards.domain.Field
import java.time.YearMonth
import java.util.Locale

/** Why a whole file is refused before any row is looked at. */
sealed interface SheetFileProblem {
    data class MissingColumns(val headers: List<String>) : SheetFileProblem
    data class UnknownColumns(val headers: List<String>) : SheetFileProblem
    data object NoHeaderRow : SheetFileProblem
    data object NoRows : SheetFileProblem
    data class TooManyRows(val rows: Int, val max: Int) : SheetFileProblem
}

/** Why one row is not imported. Every message the user sees maps from one of these. */
sealed interface RowProblem {
    data class Missing(val column: ImportColumn) : RowProblem
    data object NumberNotDigits : RowProblem
    data object NumberChangedBySpreadsheet : RowProblem
    data class NumberLength(val networkName: String, val lengths: String) : RowProblem
    data object NumberLuhn : RowProblem
    data object NumberUnknownNetwork : RowProblem
    data class NetworkUnknown(val value: String) : RowProblem
    data class NetworkMismatch(val looksLike: String, val picked: String) : RowProblem
    data object MonthInvalid : RowProblem
    data object YearInvalid : RowProblem
    data object Expired : RowProblem
    data object ExpiryTooFar : RowProblem
    data class CvvLength(val digits: Int) : RowProblem
    data class BankUnknown(val value: String) : RowProblem
    data class TypeUnknown(val value: String) : RowProblem
    data class HolderTooLong(val max: Int) : RowProblem
    data object DueDayInvalid : RowProblem
    data class NoteTooLong(val max: Int) : RowProblem
    data class RepeatedInFile(val firstRow: Int) : RowProblem
    data object AlreadyInAppWithDifferentDetails : RowProblem
}

enum class RowStatus { READY, NEEDS_FIXING, ALREADY_IN_APP }

/** A row that passed every rule, ready to become a card. Names are display names. */
data class ImportedCard(
    val holderName: String,
    val number: String,
    val expiry: String,
    val cvv: String,
    val networkId: String,
    val bankName: String,
    val typeName: String,
    val dueDay: Int?,
    val note: String
)

data class RowResult(
    /** 1-based row number as the user sees it in the sheet. */
    val row: Int,
    val status: RowStatus,
    val problems: List<RowProblem> = emptyList(),
    val card: ImportedCard? = null,
    /** The row's cells in schema column order, so a "rows to fix" sheet can be written back. */
    val cells: List<String> = emptyList()
)

data class SheetReport(
    val fileProblem: SheetFileProblem? = null,
    val rows: List<RowResult> = emptyList(),
    /** Holder names the import would create, in first-seen order. */
    val newHolders: List<String> = emptyList()
) {
    val ready: List<RowResult> get() = rows.filter { it.status == RowStatus.READY }
    val needsFixing: List<RowResult> get() = rows.filter { it.status == RowStatus.NEEDS_FIXING }
    val alreadyInApp: List<RowResult> get() = rows.filter { it.status == RowStatus.ALREADY_IN_APP }

    /**
     * The rows that need fixing as a grid: our headings plus "Row" and "Problem" columns, one
     * line per row, so the user fixes only those and imports the file again.
     */
    fun fixRows(describe: (RowProblem) -> String): List<List<String>> {
        val header = ImportSchema.headers + listOf("Row", "Problem")
        return listOf(header) + needsFixing.map { result ->
            result.cells + listOf(result.row.toString(), result.problems.joinToString(" ") { describe(it) })
        }
    }
}

/** A card already on the phone, for the duplicate rule. */
data class ExistingCardDetails(val expiry: String, val cvv: String)

/** What the phone knows; the checker is pure and never touches storage. */
data class SheetContext(
    val networks: List<CardNetwork>,
    val banks: List<String>,
    val types: List<String>,
    val holders: List<String>,
    /** The phone's card with this number (digits only), if any. */
    val existing: (String) -> ExistingCardDetails?,
    val today: YearMonth = YearMonth.now()
)

/**
 * Checks every row of a sheet against the same rules as the card form. Nothing is saved: the
 * caller shows the report and imports the READY cards. The grid is raw cells (CSV or xlsx);
 * the header row is found by its headings, so a banner above them is fine.
 */
object SheetChecker {

    private val SCIENTIFIC = Regex("""^-?\d+(\.\d+)?[eE][+-]?\d+$""")
    private val NUMBER_SEPARATORS = Regex("""[\s-]+""")

    fun check(grid: List<List<String>>, context: SheetContext): SheetReport {
        val headerIndex = headerRowIndex(grid)
        if (headerIndex < 0) return SheetReport(fileProblem = SheetFileProblem.NoHeaderRow)
        val headerRow = grid[headerIndex]
        val columnAt = headerRow.map { ImportSchema.columnFor(it) }
        val unknown = headerRow.filterIndexed { i, h -> h.isNotBlank() && columnAt[i] == null }
        if (unknown.isNotEmpty()) return SheetReport(fileProblem = SheetFileProblem.UnknownColumns(unknown))
        val missing = ImportSchema.columns.filter { it.required && it !in columnAt }
        if (missing.isNotEmpty()) return SheetReport(fileProblem = SheetFileProblem.MissingColumns(missing.map { it.sheetHeader }))

        val dataRows = grid.drop(headerIndex + 1)
            .mapIndexed { i, cells -> (headerIndex + i + 2) to cells }
            .filter { (_, cells) -> cells.any { it.isNotBlank() } }
        if (dataRows.isEmpty()) return SheetReport(fileProblem = SheetFileProblem.NoRows)
        if (dataRows.size > ImportSchema.MAX_ROWS) {
            return SheetReport(fileProblem = SheetFileProblem.TooManyRows(dataRows.size, ImportSchema.MAX_ROWS))
        }

        fun cell(cells: List<String>, column: ImportColumn): String {
            val index = columnAt.indexOf(column)
            return if (index in cells.indices) cells[index].trim() else ""
        }

        val seenNumbers = linkedMapOf<String, Int>()
        val newHolders = linkedMapOf<String, String>()
        val holderKeys = context.holders.associateBy { ImportSchema.key(it) }
        val results = dataRows.map { (rowNumber, cells) ->
            val ordered = ImportSchema.columns.map { column -> cell(cells, column) }
            val checked = checkRow(rowNumber, { column -> cell(cells, column) }, context, seenNumbers).copy(cells = ordered)
            checked.card?.let { card ->
                val key = ImportSchema.key(card.holderName)
                if (holderKeys[key] == null) newHolders.putIfAbsent(key, card.holderName)
            }
            checked
        }
        return SheetReport(rows = results, newHolders = newHolders.values.toList())
    }

    /** The row with the most of our headings (at least three), so a banner above is skipped. */
    private fun headerRowIndex(grid: List<List<String>>): Int {
        var best = -1
        var bestCount = 2
        grid.forEachIndexed { index, row ->
            val count = row.mapNotNull { ImportSchema.columnFor(it) }.toSet().size
            if (count > bestCount) { best = index; bestCount = count }
        }
        return best
    }

    private fun checkRow(
        rowNumber: Int,
        cell: (ImportColumn) -> String,
        context: SheetContext,
        seenNumbers: MutableMap<String, Int>
    ): RowResult {
        val problems = mutableListOf<RowProblem>()

        // Required cells first: a blank required cell is one clear message.
        ImportSchema.columns.filter { it.required && cell(it).isEmpty() }
            .forEach { problems += RowProblem.Missing(it) }

        // Holder: 1..40 characters, matched to an existing holder ignoring case.
        val holderRaw = cell(ImportColumn.HOLDER)
        val holderName = EntityValidator.normalizeName(com.quickcards.domain.EntityKind.HOLDER, holderRaw)
        if (holderRaw.isNotEmpty() && holderName.length > EntityValidator.HOLDER_NAME_MAX) {
            problems += RowProblem.HolderTooLong(EntityValidator.HOLDER_NAME_MAX)
        }
        val holderDisplay = context.holders.firstOrNull { ImportSchema.key(it) == ImportSchema.key(holderName) } ?: holderName

        // Number: digits only in the sheet; spaces and dashes are forgiven here, nothing else.
        val numberRaw = cell(ImportColumn.NUMBER)
        val number = numberRaw.replace(NUMBER_SEPARATORS, "")
        var numberOk = numberRaw.isNotEmpty()
        if (numberRaw.isNotEmpty() && !number.all(Char::isDigit)) {
            numberOk = false
            problems += if (SCIENTIFIC.matches(numberRaw.trim())) RowProblem.NumberChangedBySpreadsheet else RowProblem.NumberNotDigits
        }
        val detected = if (numberOk) CardNumberRules.detect(number, context.networks) else null

        // Network: must be in the list and must be the network the number belongs to.
        val networkRaw = cell(ImportColumn.NETWORK)
        val picked = if (networkRaw.isEmpty()) null else findNetwork(networkRaw, context.networks)
        if (networkRaw.isNotEmpty() && picked == null) problems += RowProblem.NetworkUnknown(networkRaw)
        if (numberOk && picked != null) {
            when {
                detected == null -> problems += RowProblem.NumberUnknownNetwork
                detected.id != picked.id -> problems += RowProblem.NetworkMismatch(detected.name, picked.name)
            }
        }

        // Expiry from month + year.
        val monthRaw = cell(ImportColumn.MONTH)
        val yearRaw = cell(ImportColumn.YEAR)
        val month = ImportSchema.parseMonth(monthRaw)
        val year = ImportSchema.parseYear(yearRaw)
        if (monthRaw.isNotEmpty() && month == null) problems += RowProblem.MonthInvalid
        if (yearRaw.isNotEmpty() && year == null) problems += RowProblem.YearInvalid
        val expiry = if (month != null && year != null) "%02d/%02d".format(Locale.ROOT, month, year % 100) else ""

        // CVV, bank, type, due day, note.
        val cvv = cell(ImportColumn.CVV)
        val cvvDigits = cvv.all(Char::isDigit)
        val bankRaw = cell(ImportColumn.BANK)
        val bankName = if (bankRaw.isEmpty()) null else findBank(bankRaw, context.banks)
        if (bankRaw.isNotEmpty() && bankName == null) problems += RowProblem.BankUnknown(bankRaw)
        val typeRaw = cell(ImportColumn.TYPE)
        val typeName = if (typeRaw.isEmpty()) null else context.types.firstOrNull { ImportSchema.key(it) == ImportSchema.key(typeRaw) }
        if (typeRaw.isNotEmpty() && typeName == null) problems += RowProblem.TypeUnknown(typeRaw)
        val dueRaw = cell(ImportColumn.DUE_DAY)
        val dueDay = ImportSchema.parseDueDay(dueRaw)
        if (dueRaw.isNotEmpty() && dueDay == null) problems += RowProblem.DueDayInvalid
        val note = cell(ImportColumn.NOTE)
        if (note.length > CardValidator.NOTE_MAX_LENGTH) problems += RowProblem.NoteTooLong(CardValidator.NOTE_MAX_LENGTH)

        // The card form's own rules for number, expiry and CVV, against the picked network.
        if (numberOk && picked != null && detected?.id == picked.id) {
            val errors = CardValidator.validate(
                draft = CardDraft(holderId = "h", number = number, expiry = expiry, cvv = cvv, bankId = "b", typeId = "t"),
                networks = context.networks,
                today = context.today,
                assigned = picked
            )
            errors[Field.NUMBER]?.let { problems += numberProblem(it) }
            if (month != null && year != null) errors[Field.EXPIRY]?.let { problems += expiryProblem(it) }
            // The sheet requires a CVV even where the form allows none (Maestro): those cards go in by hand.
            if (!cvvDigits) {
                problems += RowProblem.CvvLength(picked.cvvLength)
            } else if (cvv.isNotEmpty()) {
                errors[Field.CVV]?.let { problems += cvvProblem(it, picked) }
            }
        }

        // Duplicates: in this file, then on the phone.
        if (numberOk && number.isNotEmpty()) {
            val first = seenNumbers[number]
            if (first != null) {
                problems += RowProblem.RepeatedInFile(first)
            } else {
                seenNumbers[number] = rowNumber
                val existing = context.existing(number)
                if (existing != null && problems.isEmpty()) {
                    return if (existing.expiry == expiry && existing.cvv == cvv) {
                        RowResult(rowNumber, RowStatus.ALREADY_IN_APP)
                    } else {
                        RowResult(rowNumber, RowStatus.NEEDS_FIXING, listOf(RowProblem.AlreadyInAppWithDifferentDetails))
                    }
                }
            }
        }

        if (problems.isNotEmpty()) return RowResult(rowNumber, RowStatus.NEEDS_FIXING, problems.distinct())
        return RowResult(
            row = rowNumber,
            status = RowStatus.READY,
            card = ImportedCard(
                holderName = holderDisplay,
                number = number,
                expiry = expiry,
                cvv = cvv,
                networkId = picked!!.id,
                bankName = bankName!!,
                typeName = typeName!!,
                dueDay = dueDay,
                note = note
            )
        )
    }

    /** By name or code, ignoring case and a bracketed hint ("American Express (Amex)"). */
    fun findNetwork(raw: String, networks: List<CardNetwork>): CardNetwork? {
        val k = ImportSchema.key(raw)
        if (k.isEmpty()) return null
        return networks.firstOrNull { ImportSchema.key(it.name) == k }
            ?: networks.firstOrNull { ImportSchema.key(it.code) == k }
            ?: Regex("""\(([^)]*)\)""").find(raw)?.groupValues?.get(1)?.let { hint ->
                val hk = ImportSchema.key(hint)
                networks.firstOrNull { ImportSchema.key(it.name) == hk || ImportSchema.key(it.code) == hk }
            }
    }

    /** The phone's bank with that name, else the catalog name the value refers to. */
    fun findBank(raw: String, banks: List<String>): String? {
        val k = ImportSchema.key(raw)
        if (k.isEmpty()) return null
        banks.firstOrNull { ImportSchema.key(it) == k }?.let { return it }
        val catalog = BankCatalog.find(raw) ?: return null
        return banks.firstOrNull { BankCatalog.find(it)?.name == catalog.name } ?: catalog.name
    }

    private fun numberProblem(error: CardError): RowProblem = when (error) {
        is CardError.NetworkLength -> RowProblem.NumberLength(error.networkName, error.lengths)
        CardError.Luhn -> RowProblem.NumberLuhn
        CardError.NumberLength -> RowProblem.NumberUnknownNetwork
        else -> RowProblem.NumberNotDigits
    }

    private fun expiryProblem(error: CardError): RowProblem = when (error) {
        CardError.Expired -> RowProblem.Expired
        CardError.ExpiryYear -> RowProblem.ExpiryTooFar
        else -> RowProblem.MonthInvalid
    }

    private fun cvvProblem(error: CardError, network: CardNetwork): RowProblem = when (error) {
        is CardError.CvvLength -> RowProblem.CvvLength(error.digits)
        else -> RowProblem.CvvLength(network.cvvLength)
    }
}
