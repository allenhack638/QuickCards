package com.quickcards.app.feature.cards

import android.Manifest
import com.quickcards.app.core.reminders.Reminders
import com.quickcards.app.core.reminders.PaidStore
import com.quickcards.app.core.security.AppLockController
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavController
import com.quickcards.app.R
import com.quickcards.app.core.data.settings.AppSettingsDefaults
import com.quickcards.app.core.notifications.CopyCompanionNotifier
import com.quickcards.app.core.security.ClipboardHelper
import com.quickcards.app.core.security.UnlockSession
import com.quickcards.app.core.ui.HomeCard
import com.quickcards.app.core.ui.LocalAppLocked
import com.quickcards.app.core.ui.LockGate
import com.quickcards.app.core.ui.components.LocalSnackbar
import com.quickcards.app.core.ui.components.PassField
import com.quickcards.app.core.ui.components.QcDeleteCardDialog
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcType
import com.quickcards.domain.ExpiredPlacement
import com.quickcards.domain.TimeText
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel,
    bottomInset: Dp,
    onImport: () -> Unit,
    onOpenArchived: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val state by viewModel.uiState.collectAsState()
    val keyUnavailable by viewModel.keyUnavailable.collectAsState()
    var keyDialogDismissed by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val activity = context as? androidx.fragment.app.FragmentActivity

    var filterSheetOpen by remember { mutableStateOf(false) }
    var actionsCard by remember { mutableStateOf<HomeCard?>(null) }
    var shareCard by remember { mutableStateOf<HomeCard?>(null) }
    var deleteCard by remember { mutableStateOf<HomeCard?>(null) }
    var archiveCard by remember { mutableStateOf<HomeCard?>(null) }
    var permissionAsked by rememberSaveable { mutableStateOf(false) }
    var pendingNotificationCard by remember { mutableStateOf<HomeCard?>(null) }
    val locked = LocalAppLocked.current

    LaunchedEffect(locked) {
        if (locked) {
            filterSheetOpen = false
            actionsCard = null
            shareCard = null
            deleteCard = null
            archiveCard = null
        }
    }

    BackHandler(enabled = state.searchOn) {
        viewModel.setSearchActive(false)
    }

    fun notificationsAllowed(): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED

    fun postNotification(card: HomeCard) {
        CopyCompanionNotifier.notifyCardCopied(
            context = context,
            title = context.getString(
                R.string.cards_notification_title,
                card.title,
                card.card.number.takeLast(4)
            ),
            expiry = card.card.expiry,
            cvv = card.card.cvv,
            showExpiry = state.options.notifEnabled,
            showCvv = state.options.notifEnabled,
            seconds = state.options.notifSec
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        val card = pendingNotificationCard
        pendingNotificationCard = null
        if (granted && card != null) postNotification(card)
    }

    fun maybeNotify(card: HomeCard) {
        if (!state.options.notifEnabled) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !notificationsAllowed()) {
            if (!permissionAsked) {
                permissionAsked = true
                pendingNotificationCard = card
                permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
            return
        }
        postNotification(card)
    }

    fun fieldLabel(field: PassField): String = context.getString(
        when (field) {
            PassField.NUMBER -> R.string.cards_field_number
            PassField.EXPIRY -> R.string.cards_field_expiry
            PassField.CVV -> R.string.cards_field_cvv
            PassField.NAME -> R.string.cards_field_name
        }
    )

    suspend fun performCopy(card: HomeCard, field: PassField): Boolean {
        val host = activity ?: return false
        if (!UnlockSession.requireUnlock(host)) return false
        val label = fieldLabel(field)
        val value = when (field) {
            PassField.NUMBER -> card.card.number
            PassField.EXPIRY -> card.card.expiry
            PassField.CVV -> card.card.cvv
            PassField.NAME -> card.holderName
        }
        val clearSec = state.options.clipClearSec
        ClipboardHelper.copySensitive(
            context,
            context.getString(R.string.cards_clipboard_field, card.title, label),
            value,
            if (clearSec > 0) clearSec * 1000L else null
        )
        val message = if (clearSec > 0) {
            context.getString(R.string.cards_copied_clears, label, TimeText.clipText(clearSec))
        } else {
            context.getString(R.string.cards_copied, label)
        }
        scope.launch {
            val result = snackbar.showAndAwait(
                message = message,
                actionLabel = if (clearSec > 0) context.getString(R.string.cards_clear) else null,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) ClipboardHelper.clearNow()
        }
        if (field == PassField.NUMBER) maybeNotify(card)
        return true
    }

    suspend fun performReveal(field: PassField): Boolean {
        val host = activity ?: return false
        val reason = context.getString(
            if (field == PassField.CVV) R.string.cards_reveal_cvv else R.string.cards_reveal_number
        )
        return UnlockSession.requireUnlock(host, reason)
    }

    suspend fun copyNote(card: HomeCard): Boolean {
        val host = activity ?: return false
        val reason = context.getString(R.string.cards_unlock_note)
        if (!UnlockSession.requireUnlock(host, reason)) return false
        val note = card.card.note
        if (note.isBlank()) return false
        val clearSec = state.options.clipClearSec
        ClipboardHelper.copySensitive(
            context,
            context.getString(R.string.cards_clipboard_note),
            note,
            if (clearSec > 0) clearSec * 1000L else null
        )
        snackbar.show(context.getString(R.string.cards_note_copied))
        return true
    }

    fun copyAll(card: HomeCard) {
        actionsCard = null
        scope.launch {
            val host = activity ?: return@launch
            if (!UnlockSession.requireUnlock(host)) return@launch
            val network = card.network
            val number = com.quickcards.domain.CardNumberRules.format(
                card.card.number,
                network,
                masked = false
            )
            ClipboardHelper.copySensitive(
                context,
                context.getString(R.string.cards_clipboard_details, card.title),
                "$number\n${card.card.expiry}\n${card.holderName}",
                if (state.options.clipClearSec > 0) state.options.clipClearSec * 1000L else null
            )
            val result = snackbar.showAndAwait(
                message = context.getString(R.string.cards_copied_details),
                actionLabel = context.getString(R.string.cards_clear),
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) ClipboardHelper.clearNow()
        }
    }

    fun share(card: HomeCard) {
        actionsCard = null
        scope.launch {
            val host = activity ?: return@launch
            if (!UnlockSession.requireUnlock(host, context.getString(R.string.cards_unlock_share))) return@launch
            shareCard = card
        }
    }

    fun shareNow(card: HomeCard, includeCvv: Boolean) {
        shareCard = null
        val text = CardShareText.build(context, card, includeCvv)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        // The share sheet is another app's window; like the file picker, it must not lock us.
        AppLockController.beginSystemPicker()
        context.startActivity(
            Intent.createChooser(send, context.getString(R.string.cards_share_chooser))
        )
    }

    fun edit(card: HomeCard) {
        actionsCard = null
        scope.launch {
            val host = activity ?: return@launch
            val reason = context.getString(R.string.cards_unlock_edit)
            if (!UnlockSession.requireUnlock(host, reason)) return@launch
            navController.navigate("card/edit/${card.id}")
        }
    }

    suspend fun announcePin(outcome: HomeViewModel.PinOutcome?) {
        if (outcome == null) return
        val result = snackbar.showAndAwait(
            message = context.getString(
                if (outcome.pinned) R.string.cards_pinned else R.string.cards_unpinned
            ),
            actionLabel = context.getString(R.string.cards_undo),
            duration = SnackbarDuration.Short
        )
        if (result == SnackbarResult.ActionPerformed) {
            viewModel.undoPin(outcome.undo)
        }
    }

    val pinLimitReached =
        state.cards.count { it.card.pinned } >= AppSettingsDefaults.PINNED_MAX

    fun togglePin(card: HomeCard) {
        actionsCard = null
        if (card.card.pinned) {
            scope.launch { announcePin(viewModel.unpin(card.id)) }
            return
        }
        if (pinLimitReached) return
        scope.launch { announcePin(viewModel.pin(card.id)) }
    }

    fun requestDelete(card: HomeCard) {
        actionsCard = null
        scope.launch {
            val host = activity ?: return@launch
            val reason = context.getString(R.string.cards_unlock_delete)
            if (!UnlockSession.requireUnlock(host, reason)) return@launch
            deleteCard = card
        }
    }

    fun requestArchive(card: HomeCard) {
        actionsCard = null
        archiveCard = card
    }

    fun confirmArchive(card: HomeCard, reason: String?) {
        archiveCard = null
        scope.launch {
            val outcome = viewModel.archive(card, reason)
            val result = snackbar.showAndAwait(
                message = context.getString(R.string.cards_archive_snackbar),
                actionLabel = context.getString(R.string.cards_undo),
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.undoArchive(outcome)
            }
        }
    }

    fun restoreArchived(card: HomeCard) {
        actionsCard = null
        scope.launch {
            viewModel.restoreArchived(card)
            snackbar.show(context.getString(R.string.cards_restore_snackbar))
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        if (state.searchOn) {
            SearchTopBar(
                query = state.query,
                onQueryChange = viewModel::setQuery,
                onBack = { viewModel.setSearchActive(false) }
            )
        } else {
            HomeTopBar(
                showActions = state.hasAnyCards,
                filterCount = state.filters.activeCount +
                    state.statusFilter.size +
                    if (state.reviewOnly) 1 else 0,
                onSearch = { viewModel.setSearchActive(true) },
                onFilter = { filterSheetOpen = true },
                onAdd = { navController.navigate("card/add") }
            )
        }

        if (state.hasAnyCards) {
            ChipRow(
                state = state,
                onHolder = viewModel::selectHolder,
                onRemoveFilter = viewModel::removeFilter,
                onRemoveStatus = viewModel::removeStatusFilter,
                onRemoveReview = viewModel::removeReviewFilter
            )
            if (state.searchOn && state.query.isBlank()) {
                Text(
                    text = context.getString(R.string.cards_search_hint_tags),
                    style = QcType.Body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = 20.dp,
                        end = 20.dp,
                        bottom = QcDimens.PassGap
                    )
                )
            }
        }

        val contentModifier = Modifier
            .fillMaxWidth()
            .weight(1f)

        when {
            state.loading -> Spacer(modifier = contentModifier)
            !state.hasAnyCards -> EmptyState(
                modifier = contentModifier,
                bottomInset = bottomInset,
                onAdd = { navController.navigate("card/add") },
                onImport = onImport
            )
            state.visibleCards.isEmpty() -> if (state.emptyNote == CardsEmptyNote.NONE) {
                NoResult(
                    modifier = contentModifier,
                    searchOn = state.searchOn,
                    query = state.query
                )
            } else {
                EmptyNote(
                    modifier = contentModifier,
                    note = state.emptyNote,
                    onShowExpired = viewModel::showExpired,
                    onOpenArchived = onOpenArchived
                )
            }
            else -> CardList(
                cards = state.visibleCards,
                options = state.options,
                repairSummary = if (state.searchOn) null else state.repairSummary,
                bottomInset = bottomInset,
                modifier = contentModifier,
                onCopy = { card, field -> performCopy(card, field) },
                onReveal = { field -> performReveal(field) },
                onCopyNote = { card -> copyNote(card) },
                onMarkPaid = { card ->
                    val key = card.dueKey ?: return@CardList
                    Reminders.markPaid(context, key)
                    scope.launch {
                        val result = snackbar.showAndAwait(
                            message = context.getString(R.string.cards_due_marked_paid),
                            actionLabel = context.getString(R.string.cards_undo),
                            duration = SnackbarDuration.Short
                        )
                        if (result == SnackbarResult.ActionPerformed) PaidStore.unmarkPaid(context, key)
                    }
                },
                onReviewRepair = viewModel::showNeedsCheck,
                onDismissRepair = viewModel::dismissRepairSummary,
                onActions = { card -> actionsCard = card }
            )
        }
    }

    if (LockGate.visible(filterSheetOpen, locked)) {
        FilterSheet(
            groups = state.filterGroups,
            cards = state.cards,
            statusOptions = state.statusOptions,
            initial = state.filters,
            initialStatus = state.statusFilter,
            reviewOnly = state.reviewOnly,
            hideExpired = state.options.expiredCards == ExpiredPlacement.HIDE,
            onDismiss = { filterSheetOpen = false },
            onApply = { filters, status, review ->
                viewModel.applyFilters(filters, status, review)
                filterSheetOpen = false
            }
        )
    }

    val shareTarget = shareCard
    if (shareTarget != null && LockGate.visible(true, locked)) {
        ShareCardDialog(
            onDismiss = { shareCard = null },
            onShare = { includeCvv -> shareNow(shareTarget, includeCvv) }
        )
    }

    val sheetCard = actionsCard
    if (sheetCard != null && LockGate.visible(true, locked)) {
        ActionsSheet(
            card = sheetCard,
            onDismiss = { actionsCard = null },
            onCopyAll = { copyAll(sheetCard) },
            onShare = { share(sheetCard) },
            onEdit = { edit(sheetCard) },
            onTogglePin = { togglePin(sheetCard) },
            onArchive = { requestArchive(sheetCard) },
            onRestore = { restoreArchived(sheetCard) },
            onDelete = { requestDelete(sheetCard) },
            pinLimitReached = pinLimitReached
        )
    }

    val pendingArchive = archiveCard
    if (pendingArchive != null && LockGate.visible(true, locked)) {
        ArchiveCardDialog(
            onArchive = { reason -> confirmArchive(pendingArchive, reason) },
            onDismiss = { archiveCard = null }
        )
    }

    if (keyUnavailable && LockGate.visible(!keyDialogDismissed, locked)) {
        KeyUnavailableDialog(
            onRetry = viewModel::retryKeyCheck,
            onDismiss = { keyDialogDismissed = true }
        )
    }

    val pendingDelete = deleteCard
    if (pendingDelete != null && LockGate.visible(true, locked)) {
        QcDeleteCardDialog(
            card = pendingDelete,
            onDismiss = { deleteCard = null },
            onConfirm = {
                deleteCard = null
                viewModel.deleteCard(pendingDelete)
                scope.launch {
                    val result = snackbar.showAndAwait(
                        message = context.getString(R.string.cards_deleted),
                        actionLabel = context.getString(R.string.cards_undo),
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        viewModel.restoreCard(pendingDelete)
                    }
                }
            }
        )
    }
}
