package com.quickcards.app.core.ui.entity

import androidx.compose.foundation.layout.RowScope
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quickcards.app.R
import com.quickcards.app.core.ui.components.QcDialog
import com.quickcards.app.core.ui.theme.PassColors
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcType
import com.quickcards.app.core.ui.theme.qcPrimaryButtonColors
import com.quickcards.domain.EntityKind
import com.quickcards.domain.UsageCard

/** One affected card, rendered by the rename-confirm and delete-blocked dialogs. */
data class ManageCardItem(
    val id: String,
    val title: String,
    val lastFour: String,
    val holderFirstName: String,
    val bandColor: String
)

fun UsageCard.toManageCardItem(): ManageCardItem = ManageCardItem(
    id = id,
    title = title,
    lastFour = lastFour,
    holderFirstName = holderFirstName,
    bandColor = bandColor
)

/** List title on the Manage root and the list screen. */
@get:StringRes
val EntityKind.titleRes: Int
    get() = when (this) {
        EntityKind.HOLDER -> R.string.manage_list_holders
        EntityKind.BANK -> R.string.manage_list_banks
        EntityKind.NETWORK -> R.string.manage_list_networks
        EntityKind.TYPE -> R.string.manage_list_types
        EntityKind.TAG -> R.string.manage_list_tags
    }

/** The Manage root subtitle for each list. */
@get:StringRes
val EntityKind.subtitleRes: Int
    get() = when (this) {
        EntityKind.HOLDER -> R.string.manage_subtitle_holders
        EntityKind.BANK -> R.string.manage_subtitle_banks
        EntityKind.NETWORK -> R.string.manage_subtitle_networks
        EntityKind.TYPE -> R.string.manage_subtitle_types
        EntityKind.TAG -> R.string.manage_subtitle_tags
    }

/** Lowercase name used inside sentences: "Add card holder". */
@get:StringRes
val EntityKind.oneRes: Int
    get() = when (this) {
        EntityKind.HOLDER -> R.string.manage_kind_holder
        EntityKind.BANK -> R.string.manage_kind_bank
        EntityKind.NETWORK -> R.string.manage_kind_network
        EntityKind.TYPE -> R.string.manage_kind_type
        EntityKind.TAG -> R.string.manage_kind_tag
    }

/** Capitalised name used as a sentence subject: "Card holder added". */
@get:StringRes
val EntityKind.oneTitleRes: Int
    get() = when (this) {
        EntityKind.HOLDER -> R.string.manage_kind_holder_title
        EntityKind.BANK -> R.string.manage_kind_bank_title
        EntityKind.NETWORK -> R.string.manage_kind_network_title
        EntityKind.TYPE -> R.string.manage_kind_type_title
        EntityKind.TAG -> R.string.manage_kind_tag_title
    }

/** Empty-state title for the list editor. */
@get:StringRes
val EntityKind.emptyTitleRes: Int
    get() = when (this) {
        EntityKind.HOLDER -> R.string.manage_empty_holders
        EntityKind.BANK -> R.string.manage_empty_banks
        EntityKind.NETWORK -> R.string.manage_empty_networks
        EntityKind.TYPE -> R.string.manage_empty_types
        EntityKind.TAG -> R.string.manage_empty_tags
    }

@Composable
internal fun ManageDialogFrame(
    onDismissRequest: () -> Unit,
    scrollState: ScrollState = rememberScrollState(),
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    QcDialog(
        onDismissRequest = onDismissRequest,
        scrollState = scrollState,
        actions = actions,
        content = content
    )
}

@Composable
internal fun DialogActions(
    onCancel: (() -> Unit)? = null,
    cancelLabel: String? = null,
    confirmLabel: String,
    onConfirm: () -> Unit,
    confirmDanger: Boolean = false,
    confirmEnabled: Boolean = true,
    confirmPrimary: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = QcDimens.DialogActionGap),
        horizontalArrangement = Arrangement.spacedBy(QcDimens.Base, Alignment.End),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onCancel != null) {
            TextButton(
                onClick = onCancel,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(cancelLabel ?: stringResource(R.string.manage_action_cancel))
            }
        }
        if (confirmPrimary) {
            Button(
                onClick = onConfirm,
                enabled = confirmEnabled,
                shape = CircleShape,
                contentPadding = PaddingValues(horizontal = QcDimens.Section),
                colors = qcPrimaryButtonColors()
            ) {
                Text(
                    text = confirmLabel,
                    style = QcType.Body.copy(fontWeight = FontWeight(600))
                )
            }
        } else {
            TextButton(
                onClick = onConfirm,
                enabled = confirmEnabled,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = if (confirmDanger) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    }
                )
            ) {
                Text(confirmLabel)
            }
        }
    }
}

/** The framed affected-card list shared by both confirmation dialogs. */
@Composable
internal fun ManageCardList(cards: List<ManageCardItem>) {
    if (cards.isEmpty()) return
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = QcDimens.Divider,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(QcDimens.CornerMd)
            )
    ) {
        cards.forEachIndexed { index, card ->
            if (index > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = QcDimens.CornerMd, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 22.dp, height = 14.dp)
                        .background(
                            PassColors.band(card.bandColor),
                            RoundedCornerShape(3.dp)
                        )
                )
                Text(
                    text = stringResource(
                        R.string.manage_card_list_item,
                        card.title,
                        card.lastFour
                    ),
                    style = QcType.Body,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                if (card.holderFirstName.isNotEmpty()) {
                    Text(
                        text = card.holderFirstName,
                        style = QcType.Body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
internal fun WarningPanel(text: AnnotatedString) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerHighest,
                RoundedCornerShape(QcDimens.CornerMd)
            )
            .padding(QcDimens.CornerMd),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = QcIcons.warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(QcDimens.Icon)
        )
        Text(
            text = text,
            style = QcType.Body,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
internal fun updateWarningText(
    beforeName: String,
    nextName: String,
    usedCount: Int
): AnnotatedString {
    val renamed = beforeName != nextName
    val text = if (renamed) {
        pluralStringResource(
            R.plurals.manage_warning_renaming,
            usedCount,
            beforeName,
            nextName,
            usedCount
        )
    } else {
        pluralStringResource(
            R.plurals.manage_warning_changing,
            usedCount,
            beforeName,
            usedCount
        )
    }
    return buildAnnotatedString {
        append(text)
        val bold = SpanStyle(fontWeight = FontWeight(700))
        val prefixEnd = text.indexOf(' ').let { if (it >= 0) it + 1 else 0 }
        val first = text.indexOf(beforeName, prefixEnd)
        if (first >= 0 && beforeName.isNotEmpty()) {
            addStyle(bold, first, first + beforeName.length)
        }
        if (renamed) {
            val from = if (first >= 0) first + beforeName.length else prefixEnd
            val second = text.indexOf(nextName, from)
            if (second >= 0 && nextName.isNotEmpty()) {
                addStyle(bold, second, second + nextName.length)
            }
        }
    }
}
