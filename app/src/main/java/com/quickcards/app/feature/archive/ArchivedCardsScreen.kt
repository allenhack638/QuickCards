package com.quickcards.app.feature.archive

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quickcards.app.R
import com.quickcards.app.core.notifications.CopyCompanionNotifier
import com.quickcards.app.core.security.AppLockController
import com.quickcards.app.core.security.ClipboardHelper
import com.quickcards.app.core.security.UnlockSession
import com.quickcards.app.core.ui.HomeCard
import com.quickcards.app.core.ui.LocalAppLocked
import com.quickcards.app.core.ui.LockGate
import com.quickcards.app.core.ui.components.LocalSnackbar
import com.quickcards.app.core.ui.components.PassCard
import com.quickcards.app.core.ui.components.PassField
import com.quickcards.app.core.ui.components.QcDeleteCardDialog
import com.quickcards.app.core.ui.components.QcModalBottomSheet
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcType
import com.quickcards.domain.CardNumberRules
import com.quickcards.domain.TimeText
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val SheetSubtitle = TextStyle(
    fontSize = 13.sp,
    lineHeight = 18.sp,
    fontWeight = FontWeight(400)
)

@Composable
fun ArchivedCardsScreen(
    onBack: () -> Unit,
    onEdit: (String) -> Unit,
    bottomInset: Dp,
    viewModel: ArchivedCardsViewModel = viewModel()
) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val locked = LocalAppLocked.current

    var actionsCard by remember { mutableStateOf<ArchivedCardItem?>(null) }
    var deleteCard by remember { mutableStateOf<ArchivedCardItem?>(null) }
    var permissionAsked by rememberSaveable { mutableStateOf(false) }
    var pendingNotificationCard by remember { mutableStateOf<HomeCard?>(null) }

    LaunchedEffect(locked) {
        if (locked) {
            actionsCard = null
            deleteCard = null
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

    suspend fun copyNote(item: ArchivedCardItem): Boolean {
        val host = activity ?: return false
        val reason = context.getString(R.string.cards_unlock_note)
        if (!UnlockSession.requireUnlock(host, reason)) return false
        val note = item.card.card.note
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

    fun copyDetails(item: ArchivedCardItem) {
        actionsCard = null
        scope.launch {
            val host = activity ?: return@launch
            if (!UnlockSession.requireUnlock(host)) return@launch
            val card = item.card
            val network = card.network
            val number = CardNumberRules.format(
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

    fun editCard(item: ArchivedCardItem) {
        actionsCard = null
        scope.launch {
            val host = activity ?: return@launch
            if (!UnlockSession.requireUnlock(host, context.getString(R.string.cards_unlock_edit))) {
                return@launch
            }
            onEdit(item.card.id)
        }
    }

    fun restore(item: ArchivedCardItem) {
        actionsCard = null
        viewModel.restore(item.card.id)
        snackbar.show(context.getString(R.string.cards_restore_snackbar))
    }

    fun requestDelete(item: ArchivedCardItem) {
        actionsCard = null
        scope.launch {
            val host = activity ?: return@launch
            if (!UnlockSession.requireUnlock(host, context.getString(R.string.cards_unlock_delete))) {
                return@launch
            }
            deleteCard = item
        }
    }

    fun delete(item: ArchivedCardItem) {
        deleteCard = null
        viewModel.deleteCard(item)
        scope.launch {
            val result = snackbar.showAndAwait(
                message = context.getString(R.string.cards_deleted),
                actionLabel = context.getString(R.string.cards_undo),
                duration = SnackbarDuration.Short
            )
            if (result == SnackbarResult.ActionPerformed) {
                viewModel.restoreDeleted(item.card.card)
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        if (state.searchOn) {
            ArchivedSearchTopBar(
                query = state.query,
                onQueryChange = viewModel::setQuery,
                onBack = { viewModel.setSearchActive(false) }
            )
        } else {
            ArchivedTopBar(
                onBack = onBack,
                onSearch = if (state.cards.isEmpty()) {
                    null
                } else {
                    { viewModel.setSearchActive(true) }
                }
            )
        }
        val contentModifier = Modifier
            .fillMaxWidth()
            .weight(1f)
        when {
            state.loading -> Spacer(modifier = contentModifier)
            state.cards.isEmpty() -> ArchivedEmptyState(modifier = contentModifier)
            state.visibleCards.isEmpty() -> ArchivedNoResult(
                modifier = contentModifier,
                query = state.query
            )
            else -> ArchivedList(
                modifier = contentModifier,
                state = state,
                bottomInset = bottomInset,
                onCopy = { card, field -> performCopy(card, field) },
                onReveal = { field -> performReveal(field) },
                onCopyNote = { item -> copyNote(item) },
                onActions = { actionsCard = it }
            )
        }
    }

    val sheetItem = actionsCard
    if (sheetItem != null && LockGate.visible(true, locked)) {
        ArchivedActionsSheet(
            item = sheetItem,
            onDismiss = { actionsCard = null },
            onRestore = { restore(sheetItem) },
            onEdit = { editCard(sheetItem) },
            onCopyDetails = { copyDetails(sheetItem) },
            onDelete = { requestDelete(sheetItem) }
        )
    }

    val pendingDelete = deleteCard
    if (pendingDelete != null && LockGate.visible(true, locked)) {
        QcDeleteCardDialog(
            card = pendingDelete.card,
            onDismiss = { deleteCard = null },
            onConfirm = { delete(pendingDelete) }
        )
    }
}

@Composable
private fun ArchivedTopBar(
    onBack: () -> Unit,
    onSearch: (() -> Unit)?
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(QcDimens.AppBar)
            .padding(start = QcDimens.Half, end = QcDimens.Half),
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(QcDimens.Touch)) {
            Icon(
                imageVector = QcIcons.back,
                contentDescription = stringResource(R.string.manage_a11y_back),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        Text(
            text = stringResource(R.string.archive_title),
            style = QcType.NestedTitle,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .padding(start = QcDimens.Half)
        )
        if (onSearch != null) {
            IconButton(onClick = onSearch, modifier = Modifier.size(QcDimens.Touch)) {
                Icon(
                    imageVector = QcIcons.search,
                    contentDescription = stringResource(R.string.archive_search),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun ArchivedSearchTopBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    var value by remember { mutableStateOf(TextFieldValue(query, TextRange(query.length))) }
    val searchLabel = stringResource(R.string.archive_search)

    LaunchedEffect(Unit) {
        delay(120)
        focusRequester.requestFocus()
    }

    LaunchedEffect(query) {
        if (value.text != query) {
            value = TextFieldValue(query, TextRange(query.length))
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(QcDimens.AppBar)
            .padding(horizontal = QcDimens.Gutter),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(QcDimens.SearchBox)
                .background(MaterialTheme.colorScheme.surfaceContainerHigh, CircleShape)
                .border(
                    width = if (focused) 2.dp else 0.dp,
                    color = if (focused) MaterialTheme.colorScheme.onSurface else Color.Transparent,
                    shape = CircleShape
                )
                .padding(end = QcDimens.Half),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBack, modifier = Modifier.size(QcDimens.Touch)) {
                Icon(
                    imageVector = QcIcons.back,
                    contentDescription = stringResource(R.string.archive_search_close),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                if (value.text.isEmpty()) {
                    Text(
                        text = searchLabel,
                        style = QcType.BodyLarge.copy(fontWeight = FontWeight(400)),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = {
                        AppLockController.onUserInteraction()
                        value = it
                        onQueryChange(it.text)
                    },
                    singleLine = true,
                    textStyle = QcType.BodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight(400)
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.onSurface),
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                        .onFocusChanged { focused = it.isFocused }
                        .semantics { contentDescription = searchLabel }
                )
            }
            if (value.text.isNotEmpty()) {
                IconButton(
                    onClick = {
                        value = TextFieldValue("")
                        onQueryChange("")
                    },
                    modifier = Modifier.size(QcDimens.Touch)
                ) {
                    Icon(
                        imageVector = QcIcons.close,
                        contentDescription = stringResource(R.string.archive_search_clear),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
private fun ArchivedEmptyState(modifier: Modifier) {
    Column(
        modifier = modifier.padding(
            horizontal = QcDimens.Xl,
            vertical = QcDimens.Xl * 2f
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.archive_empty_title),
            style = QcType.Title,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(QcDimens.Half))
        Text(
            text = stringResource(R.string.archive_empty_body),
            style = QcType.Body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ArchivedNoResult(modifier: Modifier, query: String) {
    Column(
        modifier = modifier.padding(
            horizontal = QcDimens.Xl,
            vertical = QcDimens.Xl * 2f
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.archive_no_result_query, query),
            style = QcType.Title,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun ArchivedList(
    modifier: Modifier,
    state: ArchivedCardsUiState,
    bottomInset: Dp,
    onCopy: suspend (HomeCard, PassField) -> Boolean,
    onReveal: suspend (PassField) -> Boolean,
    onCopyNote: suspend (ArchivedCardItem) -> Boolean,
    onActions: (ArchivedCardItem) -> Unit
) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(
            start = QcDimens.Gutter,
            end = QcDimens.Gutter,
            bottom = QcDimens.Section + bottomInset
        ),
        verticalArrangement = Arrangement.spacedBy(QcDimens.ListPassGap)
    ) {
        items(state.visibleCards, key = { it.id }) { item ->
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.TopCenter
            ) {
                PassCard(
                    card = item.card,
                    maskNumbers = state.options.maskNumbers,
                    tagsShow = state.options.tagsShow,
                    modifier = Modifier.fillMaxWidth(),
                    expiryState = item.card.expiryState,
                    needsReview = item.card.card.needsReview.isNotEmpty(),
                    archivedLabel = archivedLabel(item),
                    note = item.card.card.note,
                    showNote = state.options.showNotes,
                    onCopy = { field -> onCopy(item.card, field) },
                    onReveal = onReveal,
                    onCopyNote = { onCopyNote(item) },
                    onActions = { onActions(item) }
                )
            }
        }
    }
}

@Composable
private fun archivedLabel(item: ArchivedCardItem): String {
    val reason = item.reason
    return if (reason.isNullOrBlank()) {
        stringResource(R.string.cards_status_archived)
    } else {
        stringResource(R.string.cards_status_archived_reason, reason)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ArchivedActionsSheet(
    item: ArchivedCardItem,
    onDismiss: () -> Unit,
    onRestore: () -> Unit,
    onEdit: () -> Unit,
    onCopyDetails: () -> Unit,
    onDelete: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val subtitleParts = buildList {
        item.card.network?.name?.takeIf { it.isNotBlank() }?.let(::add)
        item.card.holderName.takeIf { it.isNotBlank() }?.let(::add)
    }

    QcModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = Modifier.padding(bottom = QcDimens.SheetBottom)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = QcDimens.Section,
                        end = QcDimens.Section,
                        top = QcDimens.SheetTitleTop,
                        bottom = QcDimens.PassGap
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(QcDimens.Gutter)
            ) {
                Column {
                    Text(
                        text = item.card.title,
                        style = QcType.Title,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = buildString {
                            append(subtitleParts.joinToString(" · "))
                            if (subtitleParts.isNotEmpty()) append(" · ")
                            append(item.card.card.number.takeLast(4))
                            if (item.card.card.note.isNotBlank()) {
                                append(" · ").append(item.card.card.note)
                            }
                        },
                        style = SheetSubtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            ArchivedActionRow(
                icon = QcIcons.unarchive,
                label = stringResource(R.string.cards_action_restore),
                onClick = onRestore
            )
            ArchivedActionRow(
                icon = QcIcons.edit,
                label = stringResource(R.string.cards_action_edit),
                onClick = onEdit
            )
            ArchivedActionRow(
                icon = QcIcons.copy,
                label = stringResource(R.string.cards_action_copy_details),
                onClick = onCopyDetails
            )
            ArchivedActionRow(
                icon = QcIcons.delete,
                label = stringResource(R.string.cards_action_delete),
                danger = true,
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun ArchivedActionRow(
    icon: ImageVector,
    label: String,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = QcDimens.ListRowOneLine)
            .clickable(onClick = onClick)
            .padding(horizontal = QcDimens.Section),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(QcDimens.Gutter)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (danger) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            modifier = Modifier.size(QcDimens.Icon)
        )
        Text(
            text = label,
            style = QcType.BodyLarge,
            color = if (danger) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
