package com.quickcards.app.feature.backup

import com.quickcards.app.core.data.model.toDomain
import com.quickcards.app.core.autofill.CardUsage
import com.quickcards.app.core.reminders.Reminders
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.fragment.app.FragmentActivity
import com.quickcards.app.BuildConfig
import com.quickcards.app.QuickCardsApplication
import com.quickcards.app.R
import com.quickcards.app.core.data.backup.BackupCodec
import com.quickcards.app.core.data.backup.BackupImportResult
import com.quickcards.app.core.data.backup.BackupImporter
import com.quickcards.app.core.data.backup.BackupNormalizer
import com.quickcards.app.core.data.backup.BackupSnapshot
import com.quickcards.app.core.data.database.QuickCardsDatabase
import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.app.core.data.repository.RepairSummary
import com.quickcards.app.core.data.repository.VaultRepository
import com.quickcards.app.core.data.sheet.SheetImporter
import com.quickcards.app.core.data.sheet.SheetFileMessage
import com.quickcards.app.core.data.sheet.SheetMessages
import com.quickcards.app.core.data.sheet.SheetOpenResult
import com.quickcards.domain.sheet.CsvWriter
import com.quickcards.domain.sheet.ImportSchema
import com.quickcards.domain.sheet.SheetReport
import com.quickcards.domain.sheet.SheetTemplate
import java.time.YearMonth
import com.quickcards.app.core.data.settings.AppSettings
import com.quickcards.app.core.notifications.CopyCompanionNotifier
import com.quickcards.app.core.security.AppLockController
import com.quickcards.app.core.security.ClipboardHelper
import com.quickcards.app.core.security.ExportCrypto
import com.quickcards.app.core.security.UnlockSession
import com.quickcards.app.core.ui.components.LocalSnackbar
import com.quickcards.app.core.ui.components.SnackbarController
import com.quickcards.app.core.ui.components.HelpTopic
import com.quickcards.domain.BackupContent
import com.quickcards.domain.CardNumberRules
import com.quickcards.domain.BackupPlan
import com.quickcards.domain.BackupPlanner
import com.quickcards.domain.BackupSection
import com.quickcards.domain.DecodedBackup
import com.quickcards.domain.MergeMode
import com.quickcards.domain.WrongPassphraseException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStreamReader

private const val MAX_IMPORT_BYTES = 10_000_000
private const val FAN_SIZE = 3
private const val MIN_PASSWORD = 8

internal enum class ImportStep {
    PICK,
    REVIEW,
    RESULT
}

internal enum class SheetStep {
    GET,
    CHECK,
    DONE
}

/** What a CreateDocument result should receive. */
internal enum class SheetDownload {
    XLSX,
    CSV,
    VALUES,
    FIX
}

internal data class ImportFile(
    val name: String,
    val text: String,
    val encrypted: Boolean,
    val cardCount: Int?
)

internal data class ImportReview(
    val content: BackupContent,
    val sourceContent: BackupContent,
    val plan: BackupPlan,
    val exportedAt: String?,
    val sections: Set<BackupSection>,
    val formatsFixed: Int,
    val needCheck: Int
)

/** One imported card in the result fan: its colour and network, nothing secret. */
internal data class FanCard(val band: String, val networkId: String?)

internal data class ImportOutcome(val result: BackupImportResult, val fan: List<FanCard>)

/** Export, Import and Erase flows from Settings and the Home empty state. */
@Stable
class BackupHostState internal constructor(
    private val appContext: Context,
    private val activity: FragmentActivity?,
    private val repository: VaultRepository,
    private val database: QuickCardsDatabase,
    private val settings: AppSettings,
    internal val snackbar: SnackbarController,
    private val scope: CoroutineScope,
    private val launchCreateDocument: (String) -> Unit,
    private val launchOpenDocument: () -> Unit,
    private val launchCreateXlsx: (String) -> Unit = {},
    private val launchCreateCsv: (String) -> Unit = {},
    private val launchOpenSheet: () -> Unit = {}
) {

    /** Opens a Help row's target picked from the spreadsheet's help sheet; set by [BackupHost]. */
    internal var onHelpTopic: (HelpTopic) -> Unit = {}

    private val version: String = BuildConfig.VERSION_NAME

    internal var exportOpen by mutableStateOf(false)
    internal var exportCardCount by mutableIntStateOf(0)
    internal var exportWithSettings by mutableStateOf(true)
    internal var exportPassphrase by mutableStateOf(TextFieldValue(""))
    internal var exportConfirm by mutableStateOf(TextFieldValue(""))
    internal var exportShowPassphrase by mutableStateOf(false)
    internal var exportShowConfirm by mutableStateOf(false)
    internal var exportBusy by mutableStateOf(false)

    internal var importOpen by mutableStateOf(false)
    internal var importStep by mutableStateOf(ImportStep.PICK)
    internal var importFile by mutableStateOf<ImportFile?>(null)
    internal var importPassphrase by mutableStateOf(TextFieldValue(""))
    internal var importShowPassphrase by mutableStateOf(false)
    internal var importError by mutableStateOf<String?>(null)
    internal var importBusy by mutableStateOf(false)
    internal var importMode by mutableStateOf(MergeMode.UPDATE)
    internal var importRestore by mutableStateOf(true)
    internal var importReview by mutableStateOf<ImportReview?>(null)
    internal var importOutcome by mutableStateOf<ImportOutcome?>(null)
    internal var importBadFile by mutableStateOf(false)

    internal var importChoiceOpen by mutableStateOf(false)
    internal var sheetOpen by mutableStateOf(false)
    internal var sheetStep by mutableStateOf(SheetStep.GET)
    internal var sheetBusy by mutableStateOf(false)
    internal var sheetProblem by mutableStateOf<SheetFileMessage?>(null)
    internal var sheetHelpOpen by mutableStateOf(false)
    internal var sheetReport by mutableStateOf<SheetReport?>(null)
    internal var sheetAdded by mutableIntStateOf(0)
    internal var sheetFan by mutableStateOf<List<FanCard>>(emptyList())
    private var sheetUri: Uri? = null
    internal val sheetHasFile: Boolean get() = sheetUri != null
    private var pendingSheetDownload: SheetDownload? = null

    internal var eraseOpen by mutableStateOf(false)
    internal var eraseAcknowledged by mutableStateOf(false)
    internal var eraseBusy by mutableStateOf(false)

    private var pendingExport: String? = null

    internal val exportLongEnough: Boolean
        get() = exportPassphrase.text.length >= MIN_PASSWORD

    internal val exportReady: Boolean
        get() = exportLongEnough && exportPassphrase.text == exportConfirm.text

    fun requestExport() {
        requireUnlocked(text(R.string.backup_reason_export)) {
            exportCardCount = repository.cards.first().size
            resetExport()
            exportOpen = true
        }
    }

    /** Import cards: one sheet, two sources. Both re-ask for the fingerprint first. */
    fun requestImport() {
        requireUnlocked(text(R.string.backup_reason_import)) {
            resetImport()
            resetSheet()
            importChoiceOpen = true
        }
    }

    internal fun closeImportChoice() {
        importChoiceOpen = false
    }

    /** The backup source goes straight to the file picker; the dialog opens on the picked file. */
    internal fun chooseBackupImport() {
        importChoiceOpen = false
        resetImport()
        launchOpenDocument()
    }

    internal fun chooseSheetImport() {
        importChoiceOpen = false
        resetSheet()
        sheetOpen = true
    }

    // ---- Cards from a spreadsheet ----

    internal fun downloadSheet(kind: SheetDownload) {
        if (sheetBusy) return
        pendingSheetDownload = kind
        when (kind) {
            SheetDownload.XLSX -> launchCreateXlsx(SheetTemplate.FILE_NAME)
            SheetDownload.CSV -> launchCreateCsv(SheetTemplate.CSV_FILE_NAME)
            SheetDownload.VALUES -> launchCreateCsv(SheetTemplate.VALUES_FILE_NAME)
            SheetDownload.FIX -> launchCreateCsv(text(R.string.import_fix_file_name))
        }
    }

    internal fun onSheetFileCreated(uri: Uri?) {
        val kind = pendingSheetDownload ?: return
        pendingSheetDownload = null
        if (uri == null) return
        scope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) {
                    when (kind) {
                        SheetDownload.XLSX -> SheetTemplate.xlsx(currentLists())
                        SheetDownload.CSV -> SheetTemplate.csv().toByteArray(Charsets.UTF_8)
                        SheetDownload.VALUES -> SheetTemplate.valuesCsv(currentLists()).toByteArray(Charsets.UTF_8)
                        SheetDownload.FIX -> {
                            val report = sheetReport ?: return@withContext ByteArray(0)
                            CsvWriter.write(report.fixRows { SheetMessages.rowSentence(appContext.resources, it) })
                                .toByteArray(Charsets.UTF_8)
                        }
                    }
                }
                withContext(Dispatchers.IO) { writeBytes(uri, bytes) }
                when (kind) {
                    SheetDownload.XLSX -> snackbar.show(text(R.string.import_snack_sheet_saved))
                    SheetDownload.CSV -> {
                        snackbar.show(text(R.string.import_snack_sheet_saved))
                        downloadSheet(SheetDownload.VALUES)
                    }
                    SheetDownload.VALUES -> snackbar.show(text(R.string.import_snack_values_saved))
                    SheetDownload.FIX -> snackbar.show(text(R.string.import_snack_fix_saved))
                }
            } catch (_: Exception) {
                snackbar.show(text(R.string.import_snack_save_failed))
            }
        }
    }

    /** The sheet's dropdown lists come from this phone's lists, so a new bank or type shows up. */
    private suspend fun currentLists(): SheetTemplate.Lists = SheetTemplate.lists(
        networks = repository.networks.first().map { it.toDomain() },
        userBanks = repository.banks.first().map { it.name },
        types = repository.cardTypes.first().map { it.name },
        today = YearMonth.now()
    )

    internal fun openGoogleSheet() {
        val host = activity ?: return
        runCatching {
            host.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(text(R.string.import_google_sheet_url))))
        }
    }

    internal fun chooseSheetFile() {
        if (sheetBusy) return
        sheetProblem = null
        launchOpenSheet()
    }

    /** "Save blank sheet" on a refused file: the bundled template, generated on the phone. */
    internal fun saveBlankSheet() {
        sheetProblem = null
        downloadSheet(SheetDownload.XLSX)
    }

    internal fun closeSheetProblem() {
        sheetProblem = null
    }

    internal fun onSheetPicked(uri: Uri?) {
        if (uri == null) return
        sheetBusy = true
        sheetProblem = null
        scope.launch {
            try {
                val bytes = withContext(Dispatchers.IO) { readBytes(uri, ImportSchema.MAX_FILE_BYTES.toInt() + 1) }
                when (val opened = SheetImporter.open(bytes)) {
                    is SheetOpenResult.Refused -> sheetProblem = SheetMessages.open(appContext.resources, opened.problem)
                    is SheetOpenResult.Opened -> {
                        val report = withContext(Dispatchers.IO) { SheetImporter.check(database, opened.grid) }
                        val problem = report.fileProblem
                        if (problem != null) {
                            sheetProblem = SheetMessages.file(appContext.resources, problem)
                        } else {
                            sheetReport = report
                            sheetUri = uri
                            sheetStep = SheetStep.CHECK
                        }
                    }
                }
            } catch (_: Exception) {
                sheetProblem = SheetMessages.unreadable(appContext.resources)
            } finally {
                sheetBusy = false
            }
        }
    }

    internal fun importSheet() {
        val report = sheetReport ?: return
        if (sheetBusy || report.ready.isEmpty()) return
        sheetBusy = true
        scope.launch {
            try {
                val added = withContext(Dispatchers.IO) { SheetImporter.import(database, report).added }
                sheetFan = withContext(Dispatchers.IO) {
                    fanOf(repository.cards.first().sortedByDescending { it.createdAt }.take(minOf(added, FAN_SIZE)))
                }
                sheetAdded = added
                sheetStep = SheetStep.DONE
            } catch (_: Exception) {
                snackbar.show(text(R.string.import_snack_import_failed))
            } finally {
                sheetBusy = false
            }
        }
    }

    internal fun saveRowsToFix() {
        if (sheetReport?.needsFixing.isNullOrEmpty()) return
        downloadSheet(SheetDownload.FIX)
    }

    internal fun deleteSheetFile() {
        val uri = sheetUri ?: return
        scope.launch {
            val deleted = withContext(Dispatchers.IO) {
                runCatching { DocumentsContract.deleteDocument(appContext.contentResolver, uri) }.getOrDefault(false)
            }
            snackbar.show(text(if (deleted) R.string.import_snack_deleted else R.string.import_snack_delete_failed))
            if (deleted) closeSheet()
        }
    }

    /** Back from the check step returns to step 1 to pick another file; elsewhere it closes. */
    internal fun backFromSheet() {
        if (sheetBusy) return
        if (sheetStep == SheetStep.CHECK) {
            sheetStep = SheetStep.GET
            sheetReport = null
            sheetUri = null
        } else {
            closeSheet()
        }
    }

    internal fun closeSheet() {
        if (sheetBusy) return
        sheetOpen = false
        resetSheet()
    }

    internal fun openHelpTopic(topic: HelpTopic) {
        sheetHelpOpen = false
        if (topic == HelpTopic.SPREADSHEET) return
        closeSheet()
        onHelpTopic(topic)
    }

    private fun resetSheet() {
        sheetStep = SheetStep.GET
        sheetBusy = false
        sheetProblem = null
        sheetHelpOpen = false
        sheetReport = null
        sheetAdded = 0
        sheetFan = emptyList()
        sheetUri = null
        pendingSheetDownload = null
    }

    fun requestErase() {
        requireUnlocked(text(R.string.backup_reason_erase)) {
            resetErase()
            eraseOpen = true
        }
    }

    internal fun closeExport() {
        if (exportBusy) return
        exportOpen = false
        pendingExport = null
        resetExport()
    }

    internal fun confirmExport() {
        if (!exportReady || exportBusy) return
        exportBusy = true
        val includeSettings = exportWithSettings
        val passphrase = exportPassphrase.text.toCharArray()
        scope.launch {
            try {
                val encrypted = withContext(Dispatchers.IO) {
                    val content = BackupSnapshot.capture(
                        repository = repository,
                        settings = settings,
                        includeSettings = includeSettings,
                        version = version
                    )
                    ExportCrypto.encrypt(BackupCodec.encode(content), passphrase)
                }
                pendingExport = encrypted
                exportOpen = false
                resetExport()
                launchCreateDocument(BackupSnapshot.fileName())
            } catch (_: Exception) {
                snackbar.show(text(R.string.backup_error_create))
            } finally {
                passphrase.fill('\u0000')
                exportBusy = false
            }
        }
    }

    internal fun onExportFileCreated(uri: Uri?) {
        val payload = pendingExport
        pendingExport = null
        if (uri == null || payload == null) return
        scope.launch {
            try {
                withContext(Dispatchers.IO) { writeText(uri, payload) }
                snackbar.show(text(R.string.backup_snack_saved))
            } catch (_: Exception) {
                snackbar.show(text(R.string.backup_error_save))
            }
        }
    }

    /** Change, or Choose another file after a refused one. */
    internal fun chooseImportFile() {
        if (importBusy) return
        importBadFile = false
        launchOpenDocument()
    }

    internal fun onImportFilePicked(uri: Uri?) {
        if (uri == null) return
        scope.launch {
            try {
                val file = withContext(Dispatchers.IO) { readImportFile(uri) }
                if (file == null) {
                    importOpen = false
                    importBadFile = true
                    return@launch
                }
                importFile = file
                importPassphrase = TextFieldValue("")
                importError = null
                importOpen = true
            } catch (_: Exception) {
                snackbar.show(text(R.string.backup_error_read))
            }
        }
    }

    internal fun closeBadFile() {
        importBadFile = false
        resetImport()
    }

    internal fun openImport() {
        val file = importFile ?: return
        if (importBusy) return
        importBusy = true
        scope.launch {
            try {
                val decoded: DecodedBackup = withContext(Dispatchers.IO) {
                    if (file.encrypted) {
                        val passphrase = importPassphrase.text.toCharArray()
                        try {
                            BackupCodec.decode(file.text, passphrase)
                        } finally {
                            passphrase.fill('\u0000')
                        }
                    } else {
                        BackupCodec.decode(file.text, null)
                    }
                }
                val snapshot = withContext(Dispatchers.IO) {
                    BackupSnapshot.phoneSnapshot(repository)
                }
                val normalized = withContext(Dispatchers.IO) {
                    val networks = repository.networks.first().map { it.toDomain() }
                    BackupNormalizer.normalize(decoded.content, networks)
                }
                val plan = BackupPlanner.plan(normalized.content, snapshot)
                importReview = ImportReview(
                    content = normalized.content,
                    sourceContent = decoded.content,
                    plan = plan,
                    exportedAt = decoded.content.meta?.exportedAt,
                    sections = decoded.sections,
                    formatsFixed = normalized.formatsFixed,
                    needCheck = normalized.needCheck
                )
                importMode = MergeMode.UPDATE
                importRestore = true
                importError = null
                importStep = ImportStep.REVIEW
            } catch (_: WrongPassphraseException) {
                importError = text(R.string.backup_wrong_password)
            } catch (_: Exception) {
                importOpen = false
                importBadFile = true
            } finally {
                importBusy = false
            }
        }
    }

    internal fun confirmImport() {
        val review = importReview ?: return
        if (importBusy) return
        importBusy = true
        val restore = importRestore && review.content.settings != null
        scope.launch {
            try {
                val result: BackupImportResult = withContext(Dispatchers.IO) {
                    BackupImporter.import(
                        database = database,
                        content = review.sourceContent,
                        mode = importMode,
                        settings = settings,
                        restoreSettings = restore
                    )
                }
                if (result.formatsFixed > 0 || result.needCheck > 0) {
                    withContext(Dispatchers.IO) {
                        repository.recordRepairSummary(
                            RepairSummary(
                                fixedCards = result.formatsFixed,
                                needsCheck = result.needCheck
                            )
                        )
                    }
                }
                val numbers = review.content.cards.map { it.number }.toSet()
                val fan = withContext(Dispatchers.IO) {
                    fanOf(repository.cards.first().filter { it.number in numbers }.take(FAN_SIZE))
                }
                importOutcome = ImportOutcome(result, fan)
                importStep = ImportStep.RESULT
            } catch (_: Exception) {
                snackbar.show(text(R.string.backup_error_import))
            } finally {
                importBusy = false
            }
        }
    }

    internal fun closeImport() {
        if (importBusy) return
        importOpen = false
        resetImport()
    }

    internal fun confirmErase() {
        if (!eraseAcknowledged || eraseBusy) return
        eraseBusy = true
        scope.launch {
            try {
                withContext(Dispatchers.IO) {
                    BackupImporter.eraseAll(database)
                    repository.dismissRepairSummary()
                }
                UnlockSession.lockNow()
                ClipboardHelper.clearNow()
                CopyCompanionNotifier.cancel(appContext)
                Reminders.clearAll(appContext)
                CardUsage.clear(appContext)
                eraseOpen = false
                eraseAcknowledged = false
                snackbar.show(text(R.string.backup_snack_erased))
            } catch (_: Exception) {
                snackbar.show(text(R.string.backup_error_erase))
            } finally {
                eraseBusy = false
            }
        }
    }

    internal fun closeErase() {
        if (eraseBusy) return
        eraseOpen = false
        eraseAcknowledged = false
    }

    /** Locking the app closes every overlay. */
    internal fun dismissForLock() {
        exportOpen = false
        importOpen = false
        importChoiceOpen = false
        sheetOpen = false
        eraseOpen = false
        resetSheet()
        pendingExport = null
        resetExport()
        resetImport()
        resetErase()
    }

    private fun requireUnlocked(reason: String, block: suspend () -> Unit) {
        scope.launch {
            // Whole-vault actions always re-ask, even inside the copy window.
            val host = activity ?: return@launch
            if (!UnlockSession.requireUnlock(host, reason, fresh = true)) return@launch
            block()
        }
    }

    private fun resetExport() {
        exportPassphrase = TextFieldValue("")
        exportConfirm = TextFieldValue("")
        exportShowPassphrase = false
        exportShowConfirm = false
        exportWithSettings = true
        exportBusy = false
    }

    private fun resetImport() {
        importStep = ImportStep.PICK
        importFile = null
        importPassphrase = TextFieldValue("")
        importShowPassphrase = false
        importError = null
        importBusy = false
        importMode = MergeMode.UPDATE
        importRestore = true
        importReview = null
        importOutcome = null
        importBadFile = false
    }

    /** Colour and network only; an auto network is detected from the number, as on Home. */
    private suspend fun fanOf(cards: List<VaultCard>): List<FanCard> {
        val networks = repository.networks.first().map { it.toDomain() }
        return cards.map { card ->
            val networkId = if (card.networkAuto) CardNumberRules.detect(card.number, networks)?.id else card.networkId
            FanCard(card.bandColor, networkId)
        }
    }

    private fun resetErase() {
        eraseAcknowledged = false
        eraseBusy = false
    }

    /** The picked file, or null when it is not a QuickCards backup at all. */
    private fun readImportFile(uri: Uri): ImportFile? {
        val text = appContext.contentResolver.openInputStream(uri)?.use { stream ->
            val reader = BufferedReader(InputStreamReader(stream, Charsets.UTF_8))
            val builder = StringBuilder()
            val buffer = CharArray(8192)
            var total = 0
            while (true) {
                val count = reader.read(buffer)
                if (count < 0) break
                total += count
                if (total > MAX_IMPORT_BYTES) throw IOException("Import file is too large")
                builder.append(buffer, 0, count)
            }
            builder.toString()
        } ?: throw IOException("Could not read the selected file")

        val name = displayName(uri)
        val encrypted = BackupCodec.isEncrypted(text)
        val cardCount = if (encrypted) {
            null
        } else {
            runCatching { BackupCodec.decodePayload(text).content.cards.size }.getOrNull() ?: return null
        }
        return ImportFile(
            name = name,
            text = text,
            encrypted = encrypted,
            cardCount = cardCount
        )
    }

    private fun displayName(uri: Uri): String {
        var name: String? = null
        runCatching {
            appContext.contentResolver.query(
                uri,
                arrayOf(OpenableColumns.DISPLAY_NAME),
                null,
                null,
                null
            )?.use { cursor ->
                val index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                if (index >= 0 && cursor.moveToFirst()) {
                    name = cursor.getString(index)
                }
            }
        }
        return name ?: uri.lastPathSegment ?: "quickcards-backup.qcb"
    }

    private fun readBytes(uri: Uri, limit: Int): ByteArray {
        val stream = appContext.contentResolver.openInputStream(uri) ?: throw IOException("Could not read the selected file")
        return stream.use { input ->
            val out = java.io.ByteArrayOutputStream()
            val buffer = ByteArray(8192)
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                out.write(buffer, 0, count)
                if (out.size() > limit) break
            }
            out.toByteArray()
        }
    }

    private fun writeBytes(uri: Uri, bytes: ByteArray) {
        appContext.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
            stream.write(bytes)
        } ?: throw IOException("Could not open the selected file")
    }

    private fun writeText(uri: Uri, text: String) {
        appContext.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
            stream.write(text.toByteArray(Charsets.UTF_8))
        } ?: throw IOException("Could not open the selected file")
    }

    private fun text(resourceId: Int, vararg args: Any): String =
        appContext.getString(resourceId, *args)
}

@Composable
fun rememberBackupHostState(): BackupHostState {
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val app = context.applicationContext as QuickCardsApplication

    var hostState by remember { mutableStateOf<BackupHostState?>(null) }

    val createDocument = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri -> hostState?.onExportFileCreated(uri) }

    val openDocument = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> hostState?.onImportFilePicked(uri) }

    val createXlsx = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")
    ) { uri -> hostState?.onSheetFileCreated(uri) }

    val createCsv = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> hostState?.onSheetFileCreated(uri) }

    val openSheet = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri -> hostState?.onSheetPicked(uri) }

    val state = remember(app, snackbar) {
        BackupHostState(
            appContext = context.applicationContext,
            activity = context.findFragmentActivity(),
            repository = app.repository,
            database = app.database,
            settings = app.settings,
            snackbar = snackbar,
            scope = scope,
            launchCreateDocument = { name ->
                AppLockController.beginSystemPicker()
                createDocument.launch(name)
            },
            launchOpenDocument = {
                AppLockController.beginSystemPicker()
                openDocument.launch(arrayOf("*/*"))
            },
            launchCreateXlsx = { name ->
                AppLockController.beginSystemPicker()
                createXlsx.launch(name)
            },
            launchCreateCsv = { name ->
                AppLockController.beginSystemPicker()
                createCsv.launch(name)
            },
            launchOpenSheet = {
                AppLockController.beginSystemPicker()
                openSheet.launch(arrayOf("*/*"))
            }
        )
    }

    SideEffect { hostState = state }

    return state
}

@Composable
fun BackupHost(state: BackupHostState, onHelpTopic: (HelpTopic) -> Unit) {
    SideEffect { state.onHelpTopic = onHelpTopic }
    val locked by AppLockController.isLocked.collectAsState()
    LaunchedEffect(locked) {
        if (locked) state.dismissForLock()
    }
    if (locked) return

    if (state.sheetOpen) SpreadsheetScreen(state)
    if (state.importOpen) {
        when (state.importStep) {
            ImportStep.PICK -> ImportBackupDialog(state)
            ImportStep.REVIEW -> state.importReview?.let { ImportReviewScreen(state, it) }
            ImportStep.RESULT -> state.importOutcome?.let { ImportResultScreen(state, it) }
        }
    }
    if (state.importBadFile) NotABackupDialog(state)
    if (state.importChoiceOpen) ImportChoiceSheet(state)
    if (state.exportOpen) ExportBackupDialog(state)
    if (state.eraseOpen) EraseAllDataDialog(state)
}

private fun Context.findFragmentActivity(): FragmentActivity? {
    var current: Context? = this
    while (current != null) {
        if (current is FragmentActivity) return current
        current = (current as? ContextWrapper)?.baseContext
    }
    return null
}
