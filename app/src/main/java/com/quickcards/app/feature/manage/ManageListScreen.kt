package com.quickcards.app.feature.manage

import androidx.compose.foundation.background
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.quickcards.app.core.ui.components.NetworkMarks
import com.quickcards.app.core.ui.components.NetworkMark
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.quickcards.app.R
import com.quickcards.app.core.ui.LocalAppLocked
import com.quickcards.app.core.ui.components.LocalSnackbar
import com.quickcards.app.core.ui.entity.EntityDialog
import com.quickcards.app.core.ui.entity.emptyTitleRes
import com.quickcards.app.core.ui.entity.oneRes
import com.quickcards.app.core.ui.entity.titleRes
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcType
import com.quickcards.domain.CardNumberRules
import com.quickcards.domain.EntityKind
import com.quickcards.domain.Monogram
import kotlinx.coroutines.launch

@Composable
fun ManageListScreen(
    kind: EntityKind,
    viewModel: ManageViewModel,
    onBack: () -> Unit,
    onShowCards: (EntityKind, String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val entities = state.list(kind)
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var editorOpen by remember { mutableStateOf(false) }
    var editingId by remember { mutableStateOf<String?>(null) }
    var deleteTarget by remember { mutableStateOf<ManagedEntity?>(null) }
    var blockedTarget by remember { mutableStateOf<ManagedEntity?>(null) }
    val locked = LocalAppLocked.current

    LaunchedEffect(locked) {
        if (locked) {
            editorOpen = false
            editingId = null
            deleteTarget = null
            blockedTarget = null
        }
    }

    fun openAdd() {
        editingId = null
        editorOpen = true
    }

    fun openEdit(id: String) {
        editingId = id
        editorOpen = true
    }

    fun delete(entity: ManagedEntity) {
        scope.launch {
            val deleted = viewModel.delete(entity)
            if (deleted != null) {
                val result = snackbar.showAndAwait(
                    message = context.getString(R.string.manage_deleted, deleted.name),
                    actionLabel = context.getString(R.string.manage_action_undo),
                    duration = SnackbarDuration.Long
                )
                if (result == SnackbarResult.ActionPerformed) {
                    viewModel.undo(deleted)
                }
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
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
                text = stringResource(kind.titleRes),
                style = QcType.NestedTitle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = QcDimens.Half)
            )
            IconButton(onClick = ::openAdd, modifier = Modifier.size(QcDimens.Touch)) {
                Icon(
                    imageVector = QcIcons.add,
                    contentDescription = stringResource(
                        R.string.manage_a11y_add,
                        stringResource(kind.oneRes)
                    ),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }

        if (state.loading) {
            Spacer(modifier = Modifier.weight(1f))
        } else if (entities.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = QcDimens.Xl, vertical = QcDimens.Xl * 2f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(kind.emptyTitleRes),
                    style = QcType.Title,
                    color = MaterialTheme.colorScheme.onSurface,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(QcDimens.Half))
                Text(
                    text = stringResource(R.string.manage_empty_body),
                    style = QcType.Body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            val navigationBar = WindowInsets.navigationBars
                .asPaddingValues()
                .calculateBottomPadding()
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                contentPadding = PaddingValues(
                    bottom = QcDimens.Section + QcDimens.Half + navigationBar
                )
            ) {
                items(entities, key = { it.id }) { entity ->
                    EntityRow(
                        kind = kind,
                        entity = entity,
                        onEdit = { openEdit(entity.id) },
                        onDelete = {
                            if (entity.usedCards.isNotEmpty()) {
                                blockedTarget = entity
                            } else {
                                deleteTarget = entity
                            }
                        }
                    )
                }
            }
        }
    }

    if (!locked && editorOpen) {
        EntityDialog(
            kind = kind,
            editingId = editingId,
            onDismiss = {
                editorOpen = false
                editingId = null
            },
            onSaved = {
                editorOpen = false
                editingId = null
            }
        )
    }

    val blocked = blockedTarget
    if (!locked && blocked != null) {
        DeleteBlockedDialog(
            kind = kind,
            entity = blocked,
            onDismiss = { blockedTarget = null },
            onShowCards = {
                blockedTarget = null
                onShowCards(kind, blocked.id)
            }
        )
    }

    val target = deleteTarget
    if (!locked && target != null) {
        DeleteConfirmDialog(
            kind = kind,
            name = target.name,
            onDismiss = { deleteTarget = null },
            onConfirm = {
                deleteTarget = null
                delete(target)
            }
        )
    }
}

@Composable
private fun EntityRow(
    kind: EntityKind,
    entity: ManagedEntity,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = QcDimens.ListRowTwoLine)
            .padding(
                start = QcDimens.Gutter,
                end = QcDimens.Base,
                top = QcDimens.Base,
                bottom = QcDimens.Base
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val leadSlot = if (kind == EntityKind.NETWORK) {
            Modifier.width(QcDimens.NetworkBadgeSlot)
        } else {
            Modifier
        }
        Box(
            modifier = leadSlot.heightIn(min = QcDimens.Monogram),
            contentAlignment = Alignment.CenterStart
        ) {
            EntityLead(kind = kind, entity = entity)
        }
        Spacer(modifier = Modifier.width(QcDimens.Gutter))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entity.name,
                style = QcType.BodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = usageSubtitle(kind, entity),
                style = QcType.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        IconButton(onClick = onEdit, modifier = Modifier.size(QcDimens.Touch)) {
            Icon(
                imageVector = QcIcons.edit,
                contentDescription = stringResource(R.string.manage_a11y_edit, entity.name),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        IconButton(onClick = onDelete, modifier = Modifier.size(QcDimens.Touch)) {
            Icon(
                imageVector = QcIcons.delete,
                contentDescription = stringResource(R.string.manage_a11y_delete, entity.name),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun usageSubtitle(kind: EntityKind, entity: ManagedEntity): String {
    val usage = if (entity.usageCount > 0) {
        pluralStringResource(
            R.plurals.manage_usage_cards,
            entity.usageCount,
            entity.usageCount
        )
    } else {
        stringResource(R.string.manage_usage_not_used)
    }
    if (kind != EntityKind.NETWORK) return usage
    val network = entity.network ?: return usage
    return stringResource(
        R.string.manage_usage_network,
        CardNumberRules.lengthsText(network.lengths),
        network.cvvLength,
        usage
    )
}

@Composable
private fun EntityLead(kind: EntityKind, entity: ManagedEntity) {
    when (kind) {
        EntityKind.HOLDER -> Box(
            modifier = Modifier
                .size(QcDimens.Monogram)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = Monogram.initials(entity.name),
                style = QcType.Label.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight(700)
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        EntityKind.BANK -> Box(
            modifier = Modifier
                .size(QcDimens.Monogram)
                .clip(RoundedCornerShape(QcDimens.CornerMd))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = Monogram.monoFor(entity.name),
                style = QcType.Label.copy(
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight(700)
                ),
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        EntityKind.NETWORK -> {
            val fontScale = LocalDensity.current.fontScale
            val mark = NetworkMarks.drawable(entity.network?.id)
            if (mark != null) {
                NetworkMark(resId = mark, contentDescription = null)
            } else Box(
                modifier = Modifier
                    .widthIn(
                        min = QcDimens.NetworkBadgeMinWidth,
                        max = QcDimens.NetworkBadgeSlot
                    )
                    .height(QcDimens.NetworkBadgeHeight)
                    .border(
                        width = QcDimens.Divider,
                        color = MaterialTheme.colorScheme.outline,
                        shape = RoundedCornerShape(QcDimens.CornerBadge)
                    )
                    .padding(horizontal = QcDimens.Base),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = entity.network?.code.orEmpty(),
                    style = QcType.BandNetwork.copy(
                        fontSize = 9.sp / fontScale,
                        lineHeight = 12.sp / fontScale,
                        letterSpacing = 0.04.em
                    ),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        EntityKind.TYPE, EntityKind.TAG -> Box(
            modifier = Modifier
                .size(QcDimens.Monogram)
                .clip(RoundedCornerShape(QcDimens.CornerMd))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
            contentAlignment = Alignment.Center
        ) {
            if (kind == EntityKind.TAG) {
                Icon(
                    imageVector = QcIcons.tag,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(QcDimens.Icon)
                )
            } else {
                Text(
                    text = entity.name.firstOrNull()?.toString()
                        ?: stringResource(R.string.manage_unknown_initial),
                    style = QcType.BodyLarge.copy(
                        fontSize = 16.sp,
                        lineHeight = 20.sp,
                        fontWeight = FontWeight(700)
                    ),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
