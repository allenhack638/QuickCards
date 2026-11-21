package com.quickcards.app.feature.details

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quickcards.app.R
import com.quickcards.app.core.data.settings.AppSettings
import com.quickcards.app.core.data.settings.AppSettingsDefaults
import com.quickcards.app.core.security.AppLockController
import com.quickcards.app.core.security.ClipboardHelper
import com.quickcards.app.core.security.UnlockSession
import com.quickcards.app.core.ui.HomeCard
import com.quickcards.app.core.ui.LocalAppLocked
import com.quickcards.app.core.ui.components.LocalSnackbar
import com.quickcards.app.core.ui.components.QcDeleteCardDialog
import com.quickcards.app.core.ui.components.QcListRow
import com.quickcards.app.core.ui.components.QcModalBottomSheet
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcSize
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTheme
import com.quickcards.domain.CardNumberRules
import kotlinx.coroutines.launch

/** Which card's actions sheet is open, and which follow-up dialog; reset whenever the app locks. */
@Stable
class CardActionsState {
    internal var sheetId by mutableStateOf<String?>(null)
    internal var shareId by mutableStateOf<String?>(null)
    internal var archiveId by mutableStateOf<String?>(null)
    internal var deleteId by mutableStateOf<String?>(null)

    /** Opens the card actions sheet (hold a tile on Home, or ⋮ on Card details). */
    fun open(cardId: String) {
        sheetId = cardId
    }

    internal fun closeAll() {
        sheetId = null
        shareId = null
        archiveId = null
        deleteId = null
    }
}

@Composable
fun rememberCardActionsState(): CardActionsState = remember { CardActionsState() }

/**
 * The card actions sheet and its dialogs, drawn once above every screen. [cards] must hold any
 * card that can be opened (archived ones included); [onEdit] runs after the unlock.
 */
@Composable
fun CardActionsHost(
    state: CardActionsState,
    cards: List<HomeCard>,
    onEdit: (cardId: String) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val viewModel: CardActionsViewModel = viewModel()
    val locked = LocalAppLocked.current
    LaunchedEffect(locked) { if (locked) state.closeAll() }

    fun find(id: String?): HomeCard? = id?.let { wanted -> cards.firstOrNull { it.id == wanted } }

    suspend fun unlocked(reason: Int? = null, fresh: Boolean = false): Boolean {
        val host = activity ?: return false
        return UnlockSession.requireUnlock(host, reason?.let(context::getString), fresh) &&
            !AppLockController.isLocked.value
    }

    suspend fun undoable(message: Int): Boolean = snackbar.showAndAwait(
        message = context.getString(message),
        actionLabel = context.getString(R.string.cards_undo),
        duration = SnackbarDuration.Short
    ) == SnackbarResult.ActionPerformed

    fun act(block: suspend () -> Unit) {
        state.sheetId = null
        scope.launch { block() }
    }

    val pinLimitReached = cards.count { it.card.pinned && !it.card.archived } >= AppSettingsDefaults.PINNED_MAX

    find(state.sheetId)?.let { card ->
        CardActionsSheet(
            card = card,
            pinLimitReached = pinLimitReached,
            onDismiss = { state.sheetId = null },
            onCopyDetails = {
                act {
                    if (!unlocked()) return@act
                    copyDetails(context, card)
                    val result = snackbar.showAndAwait(
                        message = context.getString(R.string.cards_copied_details),
                        actionLabel = context.getString(R.string.cards_clear),
                        duration = SnackbarDuration.Short
                    )
                    if (result == SnackbarResult.ActionPerformed) ClipboardHelper.clearNow()
                }
            },
            onShare = { act { if (unlocked(R.string.cards_unlock_share, fresh = true)) state.shareId = card.id } },
            onEdit = { act { if (unlocked(R.string.cards_unlock_edit)) onEdit(card.id) } },
            onTogglePin = {
                act {
                    val outcome = viewModel.togglePin(card.id, pin = !card.card.pinned) ?: return@act
                    if (undoable(if (outcome.pinned) R.string.cards_pinned else R.string.cards_unpinned)) {
                        viewModel.undoPin(outcome)
                    }
                }
            },
            onArchive = { state.sheetId = null; state.archiveId = card.id },
            onRestore = {
                act {
                    viewModel.restore(card.id)
                    if (undoable(R.string.cards_restore_snackbar)) viewModel.rearchive(card.id, card.card.archiveReason)
                }
            },
            onDelete = { act { if (unlocked(R.string.cards_unlock_delete)) state.deleteId = card.id } }
        )
    }

    find(state.shareId)?.let { card ->
        ShareCardDialog(
            hasCvv = card.card.cvv.isNotEmpty(),
            onDismiss = { state.shareId = null },
            onShare = { includeCvv ->
                state.shareId = null
                share(context, card, includeCvv)
            }
        )
    }

    find(state.archiveId)?.let { card ->
        ArchiveCardDialog(
            onDismiss = { state.archiveId = null },
            onArchive = { reason ->
                state.archiveId = null
                scope.launch {
                    val outcome = viewModel.archive(card.id, reason)
                    if (undoable(R.string.cards_archive_snackbar)) viewModel.undoArchive(outcome)
                }
            }
        )
    }

    find(state.deleteId)?.let { card ->
        QcDeleteCardDialog(
            card = card,
            onDismiss = { state.deleteId = null },
            onConfirm = {
                state.deleteId = null
                scope.launch {
                    viewModel.delete(card.id)
                    if (undoable(R.string.cards_deleted)) viewModel.undoDelete(card.card)
                }
            }
        )
    }
}

/** Text-only header (bank ••last 4, type · holder, note), then the actions; Delete sits apart. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CardActionsSheet(
    card: HomeCard,
    pinLimitReached: Boolean,
    onDismiss: () -> Unit,
    onCopyDetails: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onTogglePin: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = QcTheme.colors
    val inset = QcSpacing.s6
    QcModalBottomSheet(onDismissRequest = onDismiss, containerColor = colors.surfaceHi) {
        Column(modifier = Modifier.padding(bottom = QcSpacing.s6)) {
            SheetHeader(card)
            if (card.card.archived) {
                QcListRow(stringResource(R.string.cards_action_restore), icon = QcIcons.unarchive, inset = inset, onClick = onRestore)
                QcListRow(stringResource(R.string.cards_action_copy_details), icon = QcIcons.copy, inset = inset, onClick = onCopyDetails)
                QcListRow(stringResource(R.string.cards_action_edit), icon = QcIcons.edit, inset = inset, onClick = onEdit)
            } else {
                QcListRow(stringResource(R.string.cards_action_copy_details), icon = QcIcons.copy, inset = inset, onClick = onCopyDetails)
                QcListRow(stringResource(R.string.cards_action_share), icon = QcIcons.share, inset = inset, onClick = onShare)
                QcListRow(stringResource(R.string.cards_action_edit), icon = QcIcons.edit, inset = inset, onClick = onEdit)
                val pinned = card.card.pinned
                val blocked = !pinned && pinLimitReached
                QcListRow(
                    title = stringResource(if (pinned) R.string.cards_action_unpin else R.string.cards_action_pin),
                    icon = QcIcons.pinFilled,
                    subtitle = if (blocked) stringResource(R.string.cards_pin_limit_reached) else null,
                    enabled = !blocked,
                    inset = inset,
                    onClick = onTogglePin
                )
                QcListRow(stringResource(R.string.cards_action_archive), icon = QcIcons.inventory2, inset = inset, onClick = onArchive)
            }
            Box(
                modifier = Modifier
                    .padding(horizontal = inset, vertical = QcSpacing.s2)
                    .fillMaxWidth()
                    .height(QcSize.hairline)
                    .background(colors.line)
            )
            QcListRow(stringResource(R.string.cards_action_delete), icon = QcIcons.delete, danger = true, inset = inset, onClick = onDelete)
        }
    }
}

@Composable
private fun SheetHeader(card: HomeCard) {
    val colors = QcTheme.colors
    val meta = listOfNotNull(card.typeName?.takeIf { it.isNotBlank() }, card.holderName.takeIf { it.isNotBlank() })
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = QcSpacing.s6, end = QcSpacing.s6, top = QcSpacing.s2, bottom = QcSpacing.s3)
    ) {
        Text(
            text = buildAnnotatedString {
                append(card.bankName)
                append(" ")
                withStyle(SpanStyle(fontFamily = QcText.MonoF3.fontFamily, letterSpacing = QcText.MonoDotsF3.letterSpacing)) {
                    append(COMPACT_MASK)
                }
                withStyle(SpanStyle(fontFamily = QcText.MonoF3.fontFamily, fontFeatureSettings = QcText.MonoF3.fontFeatureSettings)) {
                    append(card.card.number.takeLast(4))
                }
            },
            style = QcText.F3Semibold,
            color = colors.ink,
            modifier = Modifier.semantics { heading() }
        )
        if (meta.isNotEmpty()) {
            Text(
                text = if (meta.size == 2) stringResource(R.string.details_actions_meta, meta[0], meta[1]) else meta[0],
                style = QcText.F2,
                color = colors.ink2
            )
        }
        if (card.card.note.isNotBlank()) {
            Text(
                text = card.card.note,
                style = QcText.F2,
                color = colors.ink2,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

private fun copyDetails(context: Context, card: HomeCard) {
    val clearSec = AppSettings.get(context).clipClearSec.value
    val number = CardNumberRules.format(card.card.number, card.network, masked = false)
    ClipboardHelper.copySensitive(
        context,
        context.getString(R.string.cards_clipboard_details, card.title),
        "$number\n${card.card.expiry}\n${card.holderName}",
        if (clearSec > 0) clearSec * 1000L else null
    )
}

private fun share(context: Context, card: HomeCard, includeCvv: Boolean) {
    val send = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        putExtra(Intent.EXTRA_TEXT, CardShareText.build(context, card, includeCvv))
    }
    AppLockController.beginShareSheet()
    context.startActivity(Intent.createChooser(send, context.getString(R.string.cards_share_chooser)))
}

private const val COMPACT_MASK = "••"
