package com.quickcards.app.feature.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.quickcards.app.R
import com.quickcards.app.core.ui.HomeCard
import com.quickcards.app.core.ui.components.QcModalBottomSheet
import com.quickcards.app.core.ui.components.QcSheetActions
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcShapes
import com.quickcards.app.core.ui.theme.QcSize
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTextButton
import com.quickcards.app.core.ui.theme.QcTheme
import com.quickcards.domain.CardFilters
import com.quickcards.domain.CardQuery
import com.quickcards.domain.ExpiryState
import com.quickcards.domain.FilterGroup
import com.quickcards.domain.FilterGroupOptions
import com.quickcards.domain.FilterOption

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun FilterSheet(
    groups: List<FilterGroupOptions>,
    cards: List<HomeCard>,
    statusOptions: List<FilterOption>,
    initial: CardFilters,
    initialStatus: Set<String>,
    reviewOnly: Boolean,
    hideExpired: Boolean,
    onDismiss: () -> Unit,
    onApply: (CardFilters, Set<String>, Boolean) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var temp by remember { mutableStateOf(initial) }
    var tempStatus by remember { mutableStateOf(initialStatus) }
    var tempReview by remember { mutableStateOf(reviewOnly) }
    val count by remember {
        derivedStateOf {
            cards.count { card ->
                val statusOk = CardsStatusFilter.matches(card.expiryState, tempStatus)
                val hidden = card.expiryState == ExpiryState.EXPIRED &&
                    hideExpired &&
                    CardsStatusFilter.EXPIRED !in tempStatus
                val reviewOk = !tempReview || card.card.needsReview.isNotEmpty()
                CardQuery.passes(card.entry, temp) && statusOk && !hidden && reviewOk
            }
        }
    }

    QcModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight * QcDimens.SheetMaxHeightFraction)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = QcSpacing.s6, end = QcSpacing.s3, top = QcSpacing.s2, bottom = QcSpacing.s3),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.cards_filter_title),
                        style = QcText.F4,
                        color = QcTheme.colors.ink,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { heading() }
                    )
                    QcTextButton(
                        text = stringResource(R.string.cards_filter_clear_all),
                        onClick = {
                            temp = CardFilters()
                            tempStatus = emptySet()
                            tempReview = false
                        }
                    )
                }
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    groups.forEach { group ->
                        FilterGroupChips(
                            label = filterGroupLabel(group.group),
                            options = group.options,
                            isOn = { it.id in temp.ids(group.group) },
                            showCount = false,
                            onToggle = { temp = temp.toggle(group.group, it.id) }
                        )
                    }
                    if (statusOptions.isNotEmpty()) {
                        FilterGroupChips(
                            label = stringResource(R.string.cards_filter_group_status),
                            options = statusOptions,
                            isOn = { it.id in tempStatus },
                            showCount = true,
                            onToggle = { option ->
                                tempStatus = if (option.id in tempStatus) tempStatus - option.id else tempStatus + option.id
                            }
                        )
                    }
                }
                QcSheetActions(
                    primary = pluralStringResource(R.plurals.cards_filter_show, count, count),
                    onPrimary = { onApply(temp, tempStatus, tempReview) },
                    primaryEnabled = count > 0
                )
            }
        }
    }
}

/** One filter group: its label, then wrapping chips. Status chips also show how many cards match. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun FilterGroupChips(
    label: String,
    options: List<FilterOption>,
    isOn: (FilterOption) -> Boolean,
    showCount: Boolean,
    onToggle: (FilterOption) -> Unit
) {
    Text(
        text = label,
        style = QcText.F2Semibold,
        color = QcTheme.colors.ink2,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = QcSpacing.s6, end = QcSpacing.s6, top = QcSpacing.s3, bottom = QcSpacing.s2)
            .semantics { heading() }
    )
    FlowRow(
        modifier = Modifier.padding(horizontal = QcSpacing.s6),
        horizontalArrangement = Arrangement.spacedBy(QcSpacing.s2),
        verticalArrangement = Arrangement.spacedBy(QcSpacing.s2)
    ) {
        options.forEach { option ->
            FilterChoiceChip(
                label = option.label,
                count = option.count.takeIf { showCount },
                checked = isOn(option),
                onToggle = { onToggle(option) }
            )
        }
    }
}

/**
 * A filter chip: 32 visible (Compose widens its touch target to 48), outlined on the surface, or
 * ink-filled with a leading check 18 when selected.
 */
@Composable
private fun FilterChoiceChip(label: String, count: Int?, checked: Boolean, onToggle: () -> Unit) {
    val colors = QcTheme.colors
    val ink = if (checked) colors.onSelect else colors.ink
    Row(
        modifier = Modifier
            .heightIn(min = QcSize.ctl)
            .widthIn(min = QcSize.hit)
            .background(if (checked) colors.select else colors.surface, QcShapes.full)
            .border(QcSize.hairline, if (checked) Color.Transparent else colors.line, QcShapes.full)
            .toggleable(value = checked, role = Role.Checkbox, onValueChange = { onToggle() })
            .padding(start = if (checked) QcSpacing.s2 else QcSpacing.s3, end = QcSpacing.s3, top = QcSpacing.s1, bottom = QcSpacing.s1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(QcSpacing.s1, Alignment.CenterHorizontally)
    ) {
        if (checked) Icon(QcIcons.check, contentDescription = null, tint = ink, modifier = Modifier.size(QcSize.iconSm))
        Text(text = label, style = QcText.F2Medium, color = ink, maxLines = 1)
        if (count != null) Text(text = count.toString(), style = QcText.F2Medium, color = ink.copy(alpha = COUNT_ALPHA))
    }
}

private const val COUNT_ALPHA = 0.72f

@Composable
private fun filterGroupLabel(group: FilterGroup): String = stringResource(
    when (group) {
        FilterGroup.HOLDER -> R.string.cards_filter_group_holder
        FilterGroup.BANK -> R.string.cards_filter_group_bank
        FilterGroup.NETWORK -> R.string.cards_filter_group_network
        FilterGroup.TYPE -> R.string.cards_filter_group_type
        FilterGroup.TAG -> R.string.cards_filter_group_tag
    }
)
