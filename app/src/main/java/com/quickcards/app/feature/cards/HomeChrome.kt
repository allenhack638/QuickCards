package com.quickcards.app.feature.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.indication
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quickcards.app.R
import com.quickcards.app.core.security.AppLockController
import com.quickcards.app.core.security.UnlockSession
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcType
import com.quickcards.domain.CardQuery
import com.quickcards.domain.FilterGroup

@Composable
internal fun HomeTopBar(
    showActions: Boolean,
    filterCount: Int,
    onSearch: () -> Unit,
    onFilter: () -> Unit,
    onAdd: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(QcDimens.AppBar)
            .padding(start = QcDimens.Gutter, end = QcDimens.Half),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.cards_title),
            style = QcType.ScreenTitle,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        if (showActions) {
            IconButton(onClick = onSearch, modifier = Modifier.size(QcDimens.Touch)) {
                Icon(
                    imageVector = QcIcons.search,
                    contentDescription = stringResource(R.string.cards_search),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onFilter, modifier = Modifier.size(QcDimens.Touch)) {
                Icon(
                    imageVector = QcIcons.tune,
                    contentDescription = if (filterCount > 0) {
                        pluralStringResource(
                            R.plurals.cards_filter_active,
                            filterCount,
                            filterCount
                        )
                    } else {
                        stringResource(R.string.cards_filter)
                    },
                    tint = if (filterCount > 0) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }
            IconButton(onClick = onAdd, modifier = Modifier.size(QcDimens.Touch)) {
                Icon(
                    imageVector = QcIcons.add,
                    contentDescription = stringResource(R.string.cards_add),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
internal fun SearchTopBar(
    query: String,
    tagsSearch: Boolean,
    onQueryChange: (String) -> Unit,
    onBack: () -> Unit
) {
    val focusRequester = remember { FocusRequester() }
    var focused by remember { mutableStateOf(false) }
    var value by remember { mutableStateOf(TextFieldValue(query, TextRange(query.length))) }
    val searchContentDescription = stringResource(R.string.cards_search_placeholder)

    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(120)
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
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    CircleShape
                )
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
                    contentDescription = stringResource(R.string.cards_search_close),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
            Box(modifier = Modifier.weight(1f)) {
                if (value.text.isEmpty()) {
                    Text(
                        text = stringResource(
                            if (tagsSearch) {
                                R.string.cards_search_placeholder_tags
                            } else {
                                R.string.cards_search_placeholder
                            }
                        ),
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
                        .semantics {
                            contentDescription = searchContentDescription
                        }
                )
            }
            if (value.text.isNotEmpty()) {
                IconButton(onClick = {
                    value = TextFieldValue("")
                    onQueryChange("")
                }, modifier = Modifier.size(QcDimens.Touch)) {
                    Icon(
                        imageVector = QcIcons.close,
                        contentDescription = stringResource(R.string.cards_search_clear),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
internal fun ChipRow(
    state: HomeUiState,
    onHolder: (String?) -> Unit,
    onRemoveFilter: (FilterGroup, String) -> Unit,
    onRemoveStatus: (String) -> Unit,
    onRemoveReview: () -> Unit
) {
    val unlocked by UnlockSession.isUnlocked.collectAsState()
    val view = LocalView.current
    var lastAnnouncedUnlocked by remember { mutableStateOf(unlocked) }
    val unlockedLabel = stringResource(R.string.cards_unlocked)
    val lockedLabel = stringResource(R.string.cards_locked)

    LaunchedEffect(unlocked) {
        if (lastAnnouncedUnlocked != unlocked) {
            view.announceForAccessibility(if (unlocked) unlockedLabel else lockedLabel)
            lastAnnouncedUnlocked = unlocked
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(start = QcDimens.Gutter, end = QcDimens.Gutter, bottom = QcDimens.PassGap),
        horizontalArrangement = Arrangement.spacedBy(QcDimens.Base),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (state.holderChips.size > 1) {
            HolderChip(
                label = stringResource(R.string.cards_holder_all),
                selected = state.filters.holders.isEmpty(),
                showCheck = false,
                onClick = { onHolder(null) }
            )
            state.holderChips.forEach { holder ->
                HolderChip(
                    label = CardQuery.holderFirstName(holder.name),
                    selected = holder.id in state.filters.holders,
                    showCheck = true,
                    onClick = { onHolder(holder.id) }
                )
            }
        }

        val groups = listOf(
            FilterGroup.BANK,
            FilterGroup.NETWORK,
            FilterGroup.TYPE,
            FilterGroup.TAG
        )
        groups.forEach { group ->
            state.filters.ids(group).forEach { id ->
                InputChip(
                    label = if (group == FilterGroup.TAG) {
                        stringResource(
                            R.string.cards_tag_chip,
                            state.filterLabel(group, id)
                        )
                    } else {
                        state.filterLabel(group, id)
                    },
                    onRemove = { onRemoveFilter(group, id) }
                )
            }
        }

        state.statusFilter.forEach { id ->
            InputChip(
                label = statusFilterLabel(state, id),
                onRemove = { onRemoveStatus(id) }
            )
        }
        if (state.reviewOnly) {
            InputChip(
                label = stringResource(
                    R.string.cards_filter_needs_check,
                    state.needsCheckCount
                ),
                onRemove = onRemoveReview
            )
        }
    }
}

@Composable
private fun statusFilterLabel(state: HomeUiState, id: String): String {
    state.statusOptions.firstOrNull { it.id == id }?.label?.let { return it }
    return when (id) {
        CardsStatusFilter.EXPIRED -> stringResource(R.string.cards_status_expired)
        CardsStatusFilter.SOON -> stringResource(R.string.cards_status_expires_soon)
        else -> id
    }
}

@Composable
private fun QcChip(
    onClick: (() -> Unit)? = null,
    background: Color,
    border: Color? = null,
    paddingStart: Dp = QcDimens.Base,
    paddingEnd: Dp = QcDimens.PassGap,
    contentDescription: String? = null,
    selected: Boolean? = null,
    content: @Composable RowScope.() -> Unit
) {
    // The 48dp box is the touch target; the ripple is drawn by the 32dp pill, so a press
    // never paints a tall rectangle above and below the chip.
    val interactionSource = remember { MutableInteractionSource() }
    Box(
        modifier = Modifier
            .height(QcDimens.Touch)
            .then(
                if (selected != null) {
                    Modifier.semantics {
                        this.selected = selected
                        role = Role.Checkbox
                    }
                } else {
                    Modifier
                }
            )
            .then(
                if (contentDescription != null) {
                    Modifier.semantics { this.contentDescription = contentDescription }
                } else {
                    Modifier
                }
            )
            .then(
                if (onClick != null) {
                    Modifier.clickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = onClick
                    )
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .height(QcDimens.ChipHeight)
                .clip(RoundedCornerShape(QcDimens.CornerSm))
                .background(background)
                .indication(interactionSource, LocalIndication.current)
                .then(
                    if (border != null) {
                        Modifier.border(
                            QcDimens.Divider,
                            border,
                            RoundedCornerShape(QcDimens.CornerSm)
                        )
                    } else {
                        Modifier
                    }
                )
                .padding(start = paddingStart, end = paddingEnd),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(QcDimens.Base),
            content = content
        )
    }
}

@Composable
private fun HolderChip(
    label: String,
    selected: Boolean,
    showCheck: Boolean,
    onClick: () -> Unit
) {
    QcChip(
        onClick = onClick,
        background = if (selected) {
            MaterialTheme.colorScheme.secondaryContainer
        } else {
            Color.Transparent
        },
        border = if (selected) null else MaterialTheme.colorScheme.outlineVariant,
        selected = selected
    ) {
        if (selected && showCheck) {
            Icon(
                imageVector = QcIcons.check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(QcDimens.IconSmall)
            )
        }
        Text(
            text = label,
            style = QcType.Chip,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Composable
private fun InputChip(label: String, onRemove: () -> Unit) {
    Box(
        modifier = Modifier.height(QcDimens.Touch),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .height(QcDimens.ChipHeight)
                .clip(RoundedCornerShape(QcDimens.CornerSm))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                .padding(start = QcDimens.PassGap, end = 36.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = QcType.Chip,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(QcDimens.Touch)
                .clip(CircleShape)
                .clickable(onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = QcIcons.close,
                contentDescription = stringResource(R.string.cards_chip_remove),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(QcDimens.IconSmall)
            )
        }
    }
}
