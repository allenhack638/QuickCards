package com.quickcards.app.core.data.sheet

import com.quickcards.app.core.data.backup.BackupImportResult
import com.quickcards.app.core.data.backup.BackupImporter
import com.quickcards.app.core.data.database.QuickCardsDatabase
import com.quickcards.app.core.data.model.toDomain
import com.quickcards.app.core.security.EncryptionHelper
import com.quickcards.app.core.security.PanHasher
import com.quickcards.domain.BackupBank
import com.quickcards.domain.BackupCard
import com.quickcards.domain.BackupContent
import com.quickcards.domain.BackupNamed
import com.quickcards.domain.MergeMode
import com.quickcards.domain.sheet.CsvReader
import com.quickcards.domain.sheet.ExistingCardDetails
import com.quickcards.domain.sheet.ImportSchema
import com.quickcards.domain.sheet.SheetChecker
import com.quickcards.domain.sheet.SheetContext
import com.quickcards.domain.sheet.SheetReport
import com.quickcards.domain.sheet.XlsxReader
import java.time.YearMonth

/** What kind of file the user picked, decided from its bytes, never its name. */
enum class SheetFileKind { XLSX, CSV }

/** Why a picked file is not even opened. */
sealed interface SheetOpenProblem {
    data object TooLarge : SheetOpenProblem
    data object NotASheet : SheetOpenProblem
    data object Unreadable : SheetOpenProblem
}

sealed interface SheetOpenResult {
    data class Opened(val kind: SheetFileKind, val grid: List<List<String>>) : SheetOpenResult
    data class Refused(val problem: SheetOpenProblem) : SheetOpenResult
}

/**
 * Imports cards from a spreadsheet file: opens .xlsx or .csv by content, checks every row with
 * [SheetChecker] against the vault, and saves only the READY rows through [BackupImporter], so
 * encryption, name matching and the PAN-hash uniqueness are the same code path as a backup.
 */
object SheetImporter {

    /** Reads the grid out of the file bytes; "Cards" tab for a workbook, every row for a CSV. */
    fun open(bytes: ByteArray): SheetOpenResult {
        if (bytes.size > ImportSchema.MAX_FILE_BYTES) return SheetOpenResult.Refused(SheetOpenProblem.TooLarge)
        if (XlsxReader.looksLikeXlsx(bytes)) {
            val sheet = runCatching { XlsxReader.cardsSheet(XlsxReader.read(bytes)) }.getOrNull()
                ?: return SheetOpenResult.Refused(SheetOpenProblem.Unreadable)
            return SheetOpenResult.Opened(SheetFileKind.XLSX, sheet.rows)
        }
        if (!looksLikeText(bytes)) return SheetOpenResult.Refused(SheetOpenProblem.NotASheet)
        val text = runCatching { String(bytes, Charsets.UTF_8) }.getOrNull()
            ?: return SheetOpenResult.Refused(SheetOpenProblem.Unreadable)
        return SheetOpenResult.Opened(SheetFileKind.CSV, CsvReader.read(text))
    }

    /** Checks a grid against what is on the phone. Reads the vault, writes nothing. */
    suspend fun check(
        database: QuickCardsDatabase,
        grid: List<List<String>>,
        encryption: EncryptionHelper = EncryptionHelper.getInstance(),
        today: YearMonth = YearMonth.now()
    ): SheetReport {
        val context = SheetContext(
            networks = database.networkDao().getAllNetworksSync().map { it.toDomain() },
            banks = database.bankDao().getAllBanksSync().map { it.name },
            types = database.cardTypeDao().getAllCardTypesSync().map { it.name },
            holders = database.holderDao().getAllHoldersSync().mapNotNull { holder ->
                runCatching { encryption.decryptMetadata(holder.name) }.getOrNull()
            },
            existing = { number ->
                val card = kotlinx.coroutines.runBlocking { database.cardDao().getCardByPanHash(PanHasher.hash(number)) }
                card?.let {
                    val expiry = runCatching { encryption.decrypt(it.expiryDate) }.getOrNull() ?: return@let null
                    val cvv = runCatching { encryption.decrypt(it.cvv) }.getOrNull() ?: return@let null
                    ExistingCardDetails(expiry, cvv)
                }
            },
            today = today
        )
        return SheetChecker.check(grid, context)
    }

    /** Saves the READY rows. Rows that need fixing or are already in the app are never touched. */
    suspend fun import(
        database: QuickCardsDatabase,
        report: SheetReport,
        encryption: EncryptionHelper = EncryptionHelper.getInstance(),
        now: Long = System.currentTimeMillis()
    ): BackupImportResult {
        val content = toBackupContent(report)
        return BackupImporter.import(
            database = database,
            content = content,
            mode = MergeMode.KEEP,
            encryption = encryption,
            now = now
        )
    }

    /** READY rows as a backup: names become list items the importer matches by name or creates. */
    fun toBackupContent(report: SheetReport): BackupContent {
        class Names(private val prefix: String) {
            private val byKey = linkedMapOf<String, BackupNamed>()
            fun id(name: String): String =
                byKey.getOrPut(ImportSchema.key(name)) { BackupNamed("$prefix-${byKey.size + 1}", name) }.id
            val items: List<BackupNamed> get() = byKey.values.toList()
        }
        val holders = Names("h")
        val banks = Names("b")
        val types = Names("t")
        val cards = report.ready.mapNotNull { it.card }.mapIndexed { index, card ->
            BackupCard(
                id = "sheet-${index + 1}",
                number = card.number,
                exp = card.expiry,
                cvv = card.cvv,
                holderId = holders.id(card.holderName),
                bankId = banks.id(card.bankName),
                typeId = types.id(card.typeName),
                networkAuto = true,
                networkId = null,
                note = card.note,
                dueDay = card.dueDay,
                sortOrder = index
            )
        }
        return BackupContent(
            holders = holders.items,
            banks = banks.items.map { BackupBank(it.id, it.name) },
            types = types.items,
            cards = cards
        )
    }

    /** Text files have no NUL bytes and decode as UTF-8 without replacement characters. */
    private fun looksLikeText(bytes: ByteArray): Boolean {
        val sample = bytes.copyOf(minOf(bytes.size, 4096))
        if (sample.any { it == 0.toByte() }) return false
        return !String(sample, Charsets.UTF_8).contains('�')
    }
}
