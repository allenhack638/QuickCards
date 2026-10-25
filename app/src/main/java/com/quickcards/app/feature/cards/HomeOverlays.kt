package com.quickcards.app.feature.cards

import androidx.compose.ui.draw.alpha
import com.quickcards.app.core.ui.theme.qcSwitchColors
import com.quickcards.app.core.ui.components.QcDialog
import androidx.compose.material3.Switch
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickcards.app.R
import com.quickcards.app.core.data.settings.AppSettingsDefaults
import com.quickcards.app.core.ui.HomeCard
import com.quickcards.app.core.ui.components.QcModalBottomSheet
import com.quickcards.app.core.ui.theme.PassColors
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcType
import com.quickcards.app.core.ui.theme.qcPrimaryButtonColors
import com.quickcards.domain.CardFilters
import com.quickcards.domain.CardQuery
import com.quickcards.domain.ExpiryState
import com.quickcards.domain.FilterGroup
import com.quickcards.domain.FilterGroupOptions
import com.quickcards.domain.FilterOption

private val FilterRowText = TextStyle(
    fontSize = 16.sp,
    lineHeight = 24.sp,
    fontWeight = FontWeight(400)
)
private val CountText = TextStyle(
    fontSize = 13.sp,
    lineHeight = 18.sp,
    fontWeight = FontWeight(400)
)
private val ActionsSubtitle = TextStyle(
    fontSize = 13.sp,
    lineHeight = 18.sp,
    fontWeight = FontWeight(400)
)

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
            modifier = Modifier.size(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                modifier = Modifier.size(18.dp),
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
            style = FilterRowText,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = count.toString(),
            style = CountText,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ActionsSheet(
    card: HomeCard,
    onDismiss: () -> Unit,
    onCopyAll: () -> Unit,
    onShare: () -> Unit,
    onEdit: () -> Unit,
    onTogglePin: () -> Unit,
    onArchive: () -> Unit,
    onRestore: () -> Unit,
    onDelete: () -> Unit,
    pinLimitReached: Boolean
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val subtitleParts = buildList {
        card.network?.name?.takeIf { it.isNotBlank() }?.let(::add)
        card.holderName.takeIf { it.isNotBlank() }?.let(::add)
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
                        text = card.title,
                        style = QcType.Title,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = buildString {
                            append(subtitleParts.joinToString(" · "))
                            if (subtitleParts.isNotEmpty()) append(" · ")
                            append(card.card.number.takeLast(4))
                            if (card.card.note.isNotBlank()) append(" · ").append(card.card.note)
                        },
                        style = ActionsSubtitle,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (card.card.archived) {
                ActionRow(
                    icon = QcIcons.unarchive,
                    label = stringResource(R.string.cards_action_restore),
                    onClick = onRestore
                )
                ActionRow(
                    icon = QcIcons.edit,
                    label = stringResource(R.string.cards_action_edit),
                    onClick = onEdit
                )
                ActionRow(
                    icon = QcIcons.copy,
                    label = stringResource(R.string.cards_action_copy_details),
                    onClick = onCopyAll
                )
                ActionRow(
                    icon = QcIcons.share,
                    label = stringResource(R.string.cards_action_share),
                    onClick = onShare
                )
            } else {
                ActionRow(
                    icon = QcIcons.copy,
                    label = stringResource(R.string.cards_action_copy_details),
                    onClick = onCopyAll
                )
                ActionRow(
                    icon = QcIcons.share,
                    label = stringResource(R.string.cards_action_share),
                    onClick = onShare
                )
                ActionRow(
                    icon = QcIcons.edit,
                    label = stringResource(R.string.cards_action_edit),
                    onClick = onEdit
                )
                val pinBlocked = !card.card.pinned && pinLimitReached
                ActionRow(
                    icon = if (card.card.pinned) QcIcons.pinFilled else QcIcons.pin,
                    label = stringResource(
                        if (card.card.pinned) R.string.cards_action_unpin else R.string.cards_action_pin
                    ),
                    supporting = if (pinBlocked) {
                        pluralStringResource(
                            R.plurals.cards_pin_limit_reached,
                            AppSettingsDefaults.PINNED_MAX,
                            AppSettingsDefaults.PINNED_MAX
                        )
                    } else {
                        null
                    },
                    enabled = !pinBlocked,
                    onClick = onTogglePin
                )
                ActionRow(
                    icon = QcIcons.inventory2,
                    label = stringResource(R.string.cards_action_archive),
                    onClick = onArchive
                )
            }
            ActionRow(
                icon = QcIcons.delete,
                label = stringResource(R.string.cards_action_delete),
                danger = true,
                onClick = onDelete
            )
        }
    }
}

@Composable
private fun ActionRow(
    icon: ImageVector,
    label: String,
    danger: Boolean = false,
    supporting: String? = null,
    enabled: Boolean = true,
    onClick: () -> Unit
) {
    val tint = if (danger) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(
                minHeight = if (supporting == null) QcDimens.ListRowOneLine else QcDimens.ListRowTwoLine
            )
            .clickable(enabled = enabled, onClick = onClick)
            .alpha(if (enabled) 1f else DisabledAlpha)
            .padding(horizontal = QcDimens.Section),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(QcDimens.Gutter)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(QcDimens.Icon)
        )
        Column {
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
            if (supporting != null) {
                Text(
                    text = supporting,
                    style = QcType.Body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/** Material 3 disabled content opacity. */
private const val DisabledAlpha = 0.38f

/** Asks whether the CVV goes along before the system share sheet opens; the CVV is off by default. */
@Composable
internal fun ShareCardDialog(onDismiss: () -> Unit, onShare: (includeCvv: Boolean) -> Unit) {
    var includeCvv by remember { mutableStateOf(false) }
    QcDialog(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.cards_share_title),
            style = QcType.DialogTitle,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(QcDimens.Gutter))
        Text(
            text = stringResource(R.string.cards_share_body),
            style = QcType.Body,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(QcDimens.Gutter))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = QcDimens.Touch)
                .toggleable(value = includeCvv, role = Role.Switch) { includeCvv = it },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.cards_share_include_cvv),
                style = QcType.BodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.weight(1f)
            )
            Switch(checked = includeCvv, onCheckedChange = null, colors = qcSwitchColors())
        }
        Spacer(modifier = Modifier.height(QcDimens.DialogActionGap))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(QcDimens.Base, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Text(stringResource(R.string.cards_share_cancel))
            }
            TextButton(
                onClick = { onShare(includeCvv) },
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
            ) {
                Text(stringResource(R.string.cards_share_confirm))
            }
        }
    }
}
