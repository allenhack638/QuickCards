package com.quickcards.app.feature.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.quickcards.app.R
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcShapes
import com.quickcards.app.core.ui.theme.QcSize
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTheme
import com.quickcards.domain.CardQuery
import com.quickcards.domain.FilterGroup

/** "Cards", then search and add; the empty vault keeps only add. */
@Composable
internal fun HomeTopBar(
    showSearch: Boolean,
    onSearch: () -> Unit,
    onAdd: () -> Unit
) {
    val colors = QcTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(QcSize.bar)
            .padding(start = QcSpacing.s4, end = QcSpacing.s1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(QcSpacing.s1)
    ) {
        Text(
            text = stringResource(R.string.cards_title),
            style = QcText.F5,
            color = colors.ink,
            maxLines = 1,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        if (showSearch) {
            BarIcon(QcIcons.search, stringResource(R.string.cards_search), onSearch)
        }
        BarIcon(QcIcons.add, stringResource(R.string.cards_add), onAdd)
    }
}

@Composable
private fun BarIcon(icon: ImageVector, label: String, onClick: () -> Unit, tint: Color = QcTheme.colors.ink) {
    IconButton(onClick = onClick, modifier = Modifier.size(QcSize.hit)) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(QcSize.iconLg))
    }
}

/** Holder chips (All, then each holder), the active filters, and the filter button at the end. */
@Composable
internal fun ChipRow(
    state: HomeUiState,
    onHolder: (String?) -> Unit,
    onRemoveFilter: (FilterGroup, String) -> Unit,
    onRemoveStatus: (String) -> Unit,
    onRemoveReview: () -> Unit,
    onFilter: () -> Unit
) {
    val filterCount = state.filters.activeCount + state.statusFilter.size + if (state.reviewOnly) 1 else 0
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = QcSize.hit)
            .padding(start = QcSpacing.s4, end = QcSpacing.s1),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(QcSpacing.s2),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (state.holderChips.size > 1) {
                HolderChip(
                    label = stringResource(R.string.cards_holder_all),
                    selected = state.filters.holders.isEmpty(),
                    onClick = { onHolder(null) }
                )
                state.holderChips.forEach { holder ->
                    HolderChip(
                        label = CardQuery.holderFirstName(holder.name),
                        selected = holder.id in state.filters.holders,
                        onClick = { onHolder(holder.id) }
                    )
                }
            }
            listOf(FilterGroup.BANK, FilterGroup.NETWORK, FilterGroup.TYPE, FilterGroup.TAG).forEach { group ->
                state.filters.ids(group).forEach { id ->
                    val label = state.filterLabel(group, id)
                    FilterChip(
                        label = if (group == FilterGroup.TAG) stringResource(R.string.cards_tag_chip, label) else label,
                        onRemove = { onRemoveFilter(group, id) }
                    )
                }
            }
            state.statusFilter.forEach { id ->
                FilterChip(label = statusFilterLabel(state, id), onRemove = { onRemoveStatus(id) })
            }
            if (state.reviewOnly) {
                FilterChip(
                    label = stringResource(R.string.cards_filter_needs_check, state.needsCheckCount),
                    onRemove = onRemoveReview
                )
            }
        }
        BarIcon(
            icon = QcIcons.tune,
            label = if (filterCount > 0) {
                pluralStringResource(R.plurals.cards_filter_active, filterCount, filterCount)
            } else {
                stringResource(R.string.cards_filter)
            },
            onClick = onFilter,
            tint = if (filterCount > 0) QcTheme.colors.ink else QcTheme.colors.ink2
        )
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

/** Single-select holder chip: 32 visible in a 48 hit, ink fill when selected, no check. */
@Composable
private fun HolderChip(label: String, selected: Boolean, onClick: () -> Unit) {
    val colors = QcTheme.colors
    Box(
        modifier = Modifier
            .heightIn(min = QcSize.hit)
            .clickable(role = Role.Button, onClick = onClick, interactionSource = null, indication = null)
            .semantics { this.selected = selected },
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            style = QcText.F2Medium,
            color = if (selected) colors.onSelect else colors.ink,
            maxLines = 1,
            modifier = Modifier
                .widthIn(min = QcSize.hit)
                .heightIn(min = QcSize.ctl)
                .background(if (selected) colors.select else colors.surface, QcShapes.full)
                .border(QcSize.hairline, if (selected) Color.Transparent else colors.line, QcShapes.full)
                .padding(horizontal = QcSpacing.s3, vertical = QcSpacing.s1 + QcSpacing.s1 / 2),
            textAlign = TextAlign.Center
        )
    }
}

/** An active filter: selected chip with a leading check and a trailing close; tapping it removes the filter. */
@Composable
private fun FilterChip(label: String, onRemove: () -> Unit) {
    val colors = QcTheme.colors
    val removeLabel = "$label. ${stringResource(R.string.cards_chip_remove)}"
    Box(
        modifier = Modifier
            .heightIn(min = QcSize.hit)
            .clickable(onClick = onRemove, interactionSource = null, indication = null)
            .clearAndSetSemantics {
                contentDescription = removeLabel
                role = Role.Button
                onClick { onRemove(); true }
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = QcSize.ctl)
                .background(colors.select, QcShapes.full)
                .padding(start = QcSpacing.s2, end = QcSpacing.s1),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(QcSpacing.s1)
        ) {
            Icon(QcIcons.check, null, tint = colors.onSelect, modifier = Modifier.size(QcSize.iconSm))
            Text(text = label, style = QcText.F2Medium, color = colors.onSelect, maxLines = 1)
            Icon(QcIcons.close, null, tint = colors.onSelect, modifier = Modifier.size(QcSize.iconSm))
        }
    }
}
