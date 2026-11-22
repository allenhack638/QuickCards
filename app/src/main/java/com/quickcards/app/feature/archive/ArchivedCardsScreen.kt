package com.quickcards.app.feature.archive

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quickcards.app.R
import com.quickcards.app.core.notifications.rememberCardNotifications
import com.quickcards.app.core.security.ClipboardHelper
import com.quickcards.app.core.security.UnlockSession
import com.quickcards.app.core.ui.HomeCard
import com.quickcards.app.core.ui.components.LocalSnackbar
import com.quickcards.app.core.ui.components.PassCard
import com.quickcards.app.core.ui.components.QcEmptyMessage
import com.quickcards.app.core.ui.components.QcIconButton
import com.quickcards.app.core.ui.components.QcNestedTopBar
import com.quickcards.app.core.ui.components.SearchTopBar
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.domain.CardStatus
import com.quickcards.domain.TimeText
import kotlinx.coroutines.launch

/**
 * Manage › Archived: the Home tiles with the "Archived · reason" chip, a search over every field
 * (reason included), and hold for the shared card actions sheet ([onCardActions]).
 */
@Composable
fun ArchivedCardsScreen(
    onBack: () -> Unit,
    onCardActions: (cardId: String) -> Unit,
    bottomInset: Dp,
    viewModel: ArchivedCardsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val notifications = rememberCardNotifications()

    BackHandler(enabled = state.searchOn) { viewModel.setSearchActive(false) }
    DisposableEffect(viewModel) { onDispose { viewModel.setSearchActive(false) } }

    fun copied(label: String, clipboardLabel: String, value: String) {
        val clearSec = state.options.clipClearSec
        ClipboardHelper.copySensitive(context, clipboardLabel, value, if (clearSec > 0) clearSec * 1000L else null)
        scope.launch {
            val result = snackbar.showAndAwait(
                message = if (clearSec > 0) {
                    context.getString(R.string.cards_copied_clears, label, TimeText.clipText(clearSec))
                } else {
                    context.getString(R.string.cards_copied, label)
                },
                actionLabel = if (clearSec > 0) context.getString(R.string.cards_clear) else null,
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) ClipboardHelper.clearNow()
        }
    }

    fun copyNumber(card: HomeCard) = scope.launch {
        val host = activity ?: return@launch
        if (!UnlockSession.requireUnlock(host)) return@launch
        val label = context.getString(R.string.cards_field_number)
        copied(label, context.getString(R.string.cards_clipboard_field, card.title, label), card.card.number)
        notifications.numberCopied(card)
    }

    fun copyNote(card: HomeCard) = scope.launch {
        val host = activity ?: return@launch
        if (!UnlockSession.requireUnlock(host, context.getString(R.string.cards_unlock_note))) return@launch
        val note = card.card.note
        if (note.isBlank()) return@launch
        val clearSec = state.options.clipClearSec
        ClipboardHelper.copySensitive(
            context,
            context.getString(R.string.cards_clipboard_note),
            note,
            if (clearSec > 0) clearSec * 1000L else null
        )
        snackbar.show(context.getString(R.string.cards_note_copied))
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (state.searchOn) {
            SearchTopBar(
                query = state.query,
                onQueryChange = viewModel::setQuery,
                onBack = { viewModel.setSearchActive(false) },
                placeholder = stringResource(R.string.archive_search_placeholder),
                label = stringResource(R.string.archive_search)
            )
        } else {
            QcNestedTopBar(
                onBack = onBack,
                backLabel = stringResource(R.string.manage_a11y_back),
                title = stringResource(R.string.archive_title)
            ) {
                if (state.cards.isNotEmpty()) {
                    QcIconButton(QcIcons.search, stringResource(R.string.archive_search), { viewModel.setSearchActive(true) })
                }
            }
        }
        val content = Modifier
            .fillMaxWidth()
            .weight(1f)
        when {
            state.loading -> Spacer(modifier = content)
            state.cards.isEmpty() -> QcEmptyMessage(
                icon = QcIcons.inventory2,
                title = stringResource(R.string.archive_empty_title),
                body = stringResource(R.string.archive_empty_body),
                modifier = content
            )
            state.visibleCards.isEmpty() -> QcEmptyMessage(
                icon = QcIcons.search,
                title = stringResource(R.string.cards_no_result_title),
                body = stringResource(R.string.cards_no_result_body),
                compact = true,
                actionLabel = stringResource(R.string.cards_no_result_clear),
                onAction = { viewModel.setQuery("") },
                modifier = content
            )
            else -> LazyColumn(
                modifier = content,
                contentPadding = PaddingValues(
                    start = QcSpacing.s4,
                    end = QcSpacing.s4,
                    top = QcSpacing.s2,
                    bottom = QcSpacing.s6 + bottomInset
                ),
                verticalArrangement = Arrangement.spacedBy(QcSpacing.s3)
            ) {
                items(state.visibleCards, key = { it.id }) { item ->
                    PassCard(
                        card = item.card,
                        status = CardStatus.ARCHIVED,
                        archiveReason = item.reason,
                        note = item.card.card.note.takeIf { state.options.showNotes && it.isNotBlank() },
                        tags = if (state.options.tagsShow) item.card.tagNames else emptyList(),
                        onOpen = { onCardActions(item.id) },
                        onActions = { onCardActions(item.id) },
                        onCopy = { copyNumber(item.card) },
                        onCopyNote = { copyNote(item.card) }
                    )
                }
            }
        }
    }
}
