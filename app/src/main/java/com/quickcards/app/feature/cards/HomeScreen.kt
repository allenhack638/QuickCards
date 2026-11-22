package com.quickcards.app.feature.cards

import android.app.KeyguardManager
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
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
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.navigation.NavController
import com.quickcards.app.R
import com.quickcards.app.core.reminders.Reminders
import com.quickcards.app.core.security.ClipboardHelper
import com.quickcards.app.core.security.UnlockSession
import com.quickcards.app.core.ui.CardCopyField
import com.quickcards.app.core.ui.HomeCard
import com.quickcards.app.core.ui.LocalAppLocked
import com.quickcards.app.core.ui.LockGate
import com.quickcards.app.core.ui.components.LocalSnackbar
import com.quickcards.app.core.ui.components.SearchTopBar
import com.quickcards.app.core.ui.rememberCardCopier
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.domain.ExpiredPlacement
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    navController: NavController,
    viewModel: HomeViewModel,
    bottomInset: Dp,
    onImport: () -> Unit,
    onOpenArchived: () -> Unit = {},
    onOpenCard: (String) -> Unit,
    onCardActions: (String) -> Unit,
    modifier: Modifier = Modifier,
    selectedId: String? = null,
    chipsInBar: Boolean = false
) {
    val state by viewModel.uiState.collectAsState()
    val keyUnavailable by viewModel.keyUnavailable.collectAsState()
    var keyDialogDismissed by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val view = LocalView.current
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val activity = context as? androidx.fragment.app.FragmentActivity

    var filterSheetOpen by remember { mutableStateOf(false) }
    var copiedId by remember { mutableStateOf<String?>(null) }
    val locked = LocalAppLocked.current
    val screenLockSet = rememberScreenLockSet()

    LaunchedEffect(locked) {
        if (locked) filterSheetOpen = false
    }

    LaunchedEffect(copiedId) {
        if (copiedId == null) return@LaunchedEffect
        delay(QcDimens.CopiedMillis)
        copiedId = null
    }

    BackHandler(enabled = state.searchOn) {
        viewModel.setSearchActive(false)
    }

    val copier = rememberCardCopier()

    if (state.showTips) {
        FirstRunTipsScreen(
            bottomInset = bottomInset,
            onSkip = viewModel::finishTips,
            onAdd = {
                viewModel.finishTips()
                navController.navigate("card/add")
            },
            modifier = modifier
        )
        return
    }

    fun copyNumber(card: HomeCard) {
        scope.launch {
            val host = activity ?: return@launch
            if (!UnlockSession.requireUnlock(host)) return@launch
            val announcement = copier.copy(card, CardCopyField.NUMBER) ?: return@launch
            copiedId = card.id
            view.announceForAccessibility(announcement)
        }
    }

    fun copyNote(card: HomeCard) {
        scope.launch {
            val host = activity ?: return@launch
            if (card.card.note.isBlank()) return@launch
            if (!UnlockSession.requireUnlock(host, context.getString(R.string.cards_unlock_note))) return@launch
            copier.copy(card, CardCopyField.NOTE) ?: return@launch
            val result = snackbar.showAndAwait(
                message = context.getString(R.string.cards_note_copied),
                actionLabel = context.getString(R.string.cards_clear),
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) ClipboardHelper.clearNow()
        }
    }

    fun markPaid(card: HomeCard) {
        val key = card.dueKey ?: return
        Reminders.markPaid(context, key)
        scope.launch {
            val result = snackbar.showAndAwait(
                message = context.getString(R.string.cards_due_marked_paid),
                actionLabel = context.getString(R.string.cards_undo),
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) Reminders.undoPaid(context, key)
        }
    }

    val tileActions = TileActions(
        open = { card -> onOpenCard(card.id) },
        actions = { card -> onCardActions(card.id) },
        copy = ::copyNumber,
        copyNote = ::copyNote,
        markPaid = ::markPaid
    )
    val tileState = TileState(
        options = state.options,
        lastUsedId = state.lastUsedId,
        copiedId = copiedId,
        copyEnabled = screenLockSet,
        selectedId = selectedId
    )

    Column(modifier = modifier.fillMaxSize()) {
        if (state.searchOn) {
            SearchTopBar(
                query = state.query,
                onQueryChange = viewModel::setQuery,
                onBack = { viewModel.setSearchActive(false) }
            )
        } else {
            val chips: @Composable (Modifier) -> Unit = { chipModifier ->
                ChipRow(
                    state = state,
                    onHolder = viewModel::selectHolder,
                    onRemoveFilter = viewModel::removeFilter,
                    onRemoveStatus = viewModel::removeStatusFilter,
                    onRemoveReview = viewModel::removeReviewFilter,
                    onFilter = { filterSheetOpen = true },
                    modifier = chipModifier
                )
            }
            val chipsUp = chipsInBar && state.hasAnyCards
            HomeTopBar(
                showSearch = state.hasAnyCards || state.loading,
                onSearch = { viewModel.setSearchActive(true) },
                onAdd = { navController.navigate("card/add") },
                chips = if (chipsUp) ({ chips(Modifier.weight(1f).padding(start = QcSpacing.s2)) }) else null
            )
            if (state.hasAnyCards && !chipsUp) chips(Modifier.fillMaxWidth())
        }

        val contentModifier = Modifier
            .fillMaxWidth()
            .weight(1f)

        when {
            state.loading -> CardListSkeleton(modifier = contentModifier)
            !state.hasAnyCards -> EmptyState(
                modifier = contentModifier,
                bottomInset = bottomInset,
                onAdd = { navController.navigate("card/add") },
                onImport = onImport,
                onTapCard = { navController.navigate("card/add") }
            )
            state.visibleCards.isEmpty() -> if (state.emptyNote == CardsEmptyNote.NONE) {
                NoResult(
                    modifier = contentModifier,
                    onClearSearch = if (state.searchOn) ({ viewModel.setQuery("") }) else null
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
                tile = tileState,
                header = ListHeader(
                    noLock = !screenLockSet && !state.searchOn,
                    repair = if (state.searchOn) null else state.repairSummary,
                    recent = if (state.searchOn && state.query.isBlank()) state.recentCards else emptyList(),
                    onScreenLock = { context.startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS)) },
                    onReviewRepair = viewModel::showNeedsCheck,
                    onDismissRepair = viewModel::dismissRepairSummary
                ),
                actions = tileActions,
                bottomInset = bottomInset,
                modifier = contentModifier
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

    if (keyUnavailable && LockGate.visible(!keyDialogDismissed, locked)) {
        KeyUnavailableDialog(
            onRetry = viewModel::retryKeyCheck,
            onDismiss = { keyDialogDismissed = true }
        )
    }
}

/** Whether the phone has a screen lock, checked again each time the app comes back. */
@Composable
private fun rememberScreenLockSet(): Boolean {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val keyguard = remember { context.getSystemService(KeyguardManager::class.java) }
    var secure by remember { mutableStateOf(keyguard?.isDeviceSecure != false) }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) secure = keyguard?.isDeviceSecure != false
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    return secure
}

