package com.quickcards.app.feature.backup

import android.app.NotificationManager
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.quickcards.app.core.reminders.Reminders
import android.content.Context
import android.content.ContextWrapper
import android.net.Uri
import android.provider.OpenableColumns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
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
import com.quickcards.app.core.data.repository.RepairSummary
import com.quickcards.app.core.data.repository.VaultRepository
import com.quickcards.app.core.data.settings.AppSettings
import com.quickcards.app.core.notifications.CopyCompanionNotifier
import com.quickcards.app.core.security.AppLockController
import com.quickcards.app.core.security.ClipboardHelper
import com.quickcards.app.core.security.ExportCrypto
import com.quickcards.app.core.security.UnlockSession
import com.quickcards.app.core.ui.components.LocalSnackbar
import com.quickcards.app.core.ui.components.SnackbarController
import com.quickcards.domain.BackupContent
import com.quickcards.domain.BackupFormatException
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

internal enum class ImportStep {
    PICK,
    REVIEW
}

internal data class ImportFile(
    val name: String,
    val text: String,
    val encrypted: Boolean,
    val cardCount: Int?
)

internal data class ImportReview(
    val fileName: String,
    val content: BackupContent,
    val sourceContent: BackupContent,
    val plan: BackupPlan,
    val exportedAt: String?,
    val sections: Set<BackupSection>,
    val formatsFixed: Int,
    val needCheck: Int
)

/** Export, Import and Erase flows from Settings and the Home empty state. */
@Stable
class BackupHostState internal constructor(
    private val appContext: Context,
    private val activity: FragmentActivity?,
    private val repository: VaultRepository,
    private val database: QuickCardsDatabase,
    private val settings: AppSettings,
    private val snackbar: SnackbarController,
    private val scope: CoroutineScope,
    private val launchCreateDocument: (String) -> Unit,
    private val launchOpenDocument: () -> Unit
) {

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

    internal var eraseOpen by mutableStateOf(false)
    internal var eraseAcknowledged by mutableStateOf(false)
    internal var eraseBusy by mutableStateOf(false)

    private var pendingExport: String? = null
    private var pendingExportWithSettings = false

    internal val exportPasswordError: String?
        get() {
            val confirm = exportConfirm.text
            return if (confirm.isNotEmpty() && exportPassphrase.text != confirm) {
                text(R.string.backup_password_mismatch)
            } else {
                null
            }
        }

    internal val exportReady: Boolean
        get() = exportPassphrase.text.length >= 8 && exportPassphrase.text == exportConfirm.text

    fun requestExport() {
        requireUnlocked(text(R.string.backup_reason_export)) {
            exportCardCount = repository.cards.first().size
            resetExport()
            exportOpen = true
        }
    }

    fun requestImport() {
        requireUnlocked(text(R.string.backup_reason_import)) {
            resetImport()
            importOpen = true
        }
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
        pendingExportWithSettings = false
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
                pendingExportWithSettings = includeSettings
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
        val withSettings = pendingExportWithSettings
        pendingExport = null
        pendingExportWithSettings = false
        if (uri == null || payload == null) return
        scope.launch {
            try {
                withContext(Dispatchers.IO) { writeText(uri, payload) }
                snackbar.show(
                    text(
                        if (withSettings) {
                            R.string.backup_snack_saved_settings
                        } else {
                            R.string.backup_snack_saved
                        }
                    )
                )
            } catch (_: Exception) {
                snackbar.show(text(R.string.backup_error_save))
            }
        }
    }

    internal fun chooseImportFile() {
        if (importBusy) return
        launchOpenDocument()
    }

    internal fun onImportFilePicked(uri: Uri?) {
        if (uri == null) return
        scope.launch {
            try {
                val file = withContext(Dispatchers.IO) { readImportFile(uri) }
                importFile = file
                importPassphrase = TextFieldValue("")
                importError = null
            } catch (_: Exception) {
                snackbar.show(text(R.string.backup_error_read))
            }
        }
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
                    fileName = file.name,
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
            } catch (_: BackupFormatException) {
                importError = null
                snackbar.show(text(R.string.backup_error_format))
            } catch (_: Exception) {
                importError = null
                snackbar.show(text(R.string.backup_error_format))
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
                importOpen = false
                resetImport()
                snackbar.show(importMessage(result))
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
                cancelCopyNotification()
                Reminders.clearAll(appContext)
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
        eraseOpen = false
        pendingExport = null
        pendingExportWithSettings = false
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
    }

    private fun resetErase() {
        eraseAcknowledged = false
        eraseBusy = false
    }

    private fun importMessage(result: BackupImportResult): String {
        val parts = buildList {
            if (result.added > 0) add(quantity(R.plurals.backup_result_added, result.added))
            if (result.updated > 0) add(quantity(R.plurals.backup_result_updated, result.updated))
            if (result.kept > 0) add(quantity(R.plurals.backup_result_kept, result.kept))
            if (result.same > 0) add(quantity(R.plurals.backup_result_unchanged, result.same))
        }
        val separator = text(R.string.backup_value_separator)
        val base = if (parts.isEmpty()) {
            text(R.string.backup_result_nothing)
        } else {
            parts.joinToString(separator)
        }
        return if (result.settingsRestored) {
            text(R.string.backup_result_with_settings, base)
        } else {
            base
        }
    }

    private fun readImportFile(uri: Uri): ImportFile {
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
            runCatching { BackupCodec.decodePayload(text).content.cards.size }.getOrNull()
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

    private fun writeText(uri: Uri, text: String) {
        appContext.contentResolver.openOutputStream(uri, "wt")?.use { stream ->
            stream.write(text.toByteArray(Charsets.UTF_8))
        } ?: throw IOException("Could not open the selected file")
    }

    private fun cancelCopyNotification() {
        runCatching {
            val manager = appContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(CopyCompanionNotifier.NOTIFICATION_ID)
        }
    }

    private fun text(resourceId: Int, vararg args: Any): String =
        appContext.getString(resourceId, *args)

    private fun quantity(resourceId: Int, count: Int): String =
        appContext.resources.getQuantityString(resourceId, count, count)
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
            }
        )
    }

    SideEffect { hostState = state }

    return state
}

@Composable
fun BackupHost(state: BackupHostState) {
    val locked by AppLockController.isLocked.collectAsState()
    LaunchedEffect(locked) {
        if (locked) state.dismissForLock()
    }

    if (!locked && state.exportOpen) {
        ExportBackupDialog(state)
    }
    if (!locked && state.importOpen) {
        ImportBackupDialog(state)
    }
    if (!locked && state.eraseOpen) {
        EraseAllDataDialog(state)
    }
}

private fun com.quickcards.app.core.data.model.CardNetwork.toDomain() =
    com.quickcards.domain.CardNetwork(
        id = id,
        name = name,
        code = code,
        prefixes = prefixes,
        lengths = lengths,
        cvvLength = cvvLength,
        luhn = luhn,
        groups = groups
    )

private fun Context.findFragmentActivity(): FragmentActivity? {
    var current: Context? = this
    while (current != null) {
        if (current is FragmentActivity) return current
        current = (current as? ContextWrapper)?.baseContext
    }
    return null
}
