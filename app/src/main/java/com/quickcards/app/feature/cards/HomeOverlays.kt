package com.quickcards.app.feature.cards

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import com.quickcards.app.R
import com.quickcards.app.core.ui.theme.QcSize
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.HomeCard
import com.quickcards.app.core.ui.components.QcModalBottomSheet
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcType
import com.quickcards.app.core.ui.theme.qcPrimaryButtonColors
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

    QcModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val maxSheetHeight = maxHeight * QcDimens.SheetMaxHeightFraction
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxSheetHeight)
            ) {
                Text(
                    text = stringResource(R.string.cards_filter_title),
                    style = QcType.Headline,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(
                        start = QcDimens.Section,
                        end = QcDimens.Section,
                        top = QcDimens.SheetTitleTop,
                        bottom = QcDimens.Base
                    )
                )
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(rememberScrollState())
                ) {
                    groups.forEach { group ->
                        Text(
                            text = filterGroupLabel(group.group),
                            style = QcType.Body.copy(fontWeight = FontWeight(600)),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                start = QcDimens.Section,
                                end = QcDimens.Section,
                                top = QcDimens.Gutter,
                                bottom = QcDimens.Half
                            )
                        )
                        group.options.forEach { option ->
                            val checked = option.id in temp.ids(group.group)
                            FilterCheckRow(
                                label = option.label,
                                count = option.count,
                                checked = checked,
                                onToggle = {
                                    temp = temp.toggle(group.group, option.id)
                                }
                            )
                        }
                    }
                    if (statusOptions.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.cards_filter_group_status),
                            style = QcType.Body.copy(fontWeight = FontWeight(600)),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                start = QcDimens.Section,
                                end = QcDimens.Section,
                                top = QcDimens.Gutter,
                                bottom = QcDimens.Half
                            )
                        )
                        statusOptions.forEach { option ->
                            FilterCheckRow(
                                label = option.label,
                                count = option.count,
                                checked = option.id in tempStatus,
                                onToggle = {
                                    tempStatus = if (option.id in tempStatus) {
                                        tempStatus - option.id
                                    } else {
                                        tempStatus + option.id
                                    }
                                }
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = QcDimens.Section,
                            end = QcDimens.Section,
                            top = QcDimens.PassGap,
                            bottom = QcDimens.SheetBottom
                        ),
                    horizontalArrangement = Arrangement.spacedBy(QcDimens.Base, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            temp = CardFilters()
                            tempStatus = emptySet()
                            tempReview = false
                        },
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(stringResource(R.string.cards_filter_clear_all))
                    }
                    Button(
                        onClick = { onApply(temp, tempStatus, tempReview) },
                        enabled = count > 0,
                        shape = CircleShape,
                        colors = qcPrimaryButtonColors()
                    ) {
                        Text(
                            text = pluralStringResource(R.plurals.cards_filter_show, count, count),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FilterCheckRow(
    label: String,
    count: Int,
    checked: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = QcDimens.Touch)
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = { onToggle() }
            )
            .padding(horizontal = QcDimens.Section),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(QcSize.icon),
            contentAlignment = Alignment.Center
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                modifier = Modifier.size(QcSize.iconSm),
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.secondary,
                    checkmarkColor = MaterialTheme.colorScheme.onSecondary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        Spacer(modifier = Modifier.size(QcDimens.Gutter))
        Text(
            text = label,
            style = QcText.F3,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = count.toString(),
            style = QcText.F2,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

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
