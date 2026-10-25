package com.quickcards.app.feature.manage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import com.quickcards.app.R
import com.quickcards.app.core.ui.entity.DialogActions
import com.quickcards.app.core.ui.entity.ManageCardList
import com.quickcards.app.core.ui.entity.ManageDialogFrame
import com.quickcards.app.core.ui.entity.oneRes
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcType
import com.quickcards.domain.EntityKind

@Composable
internal fun DeleteBlockedDialog(
    kind: EntityKind,
    entity: ManagedEntity,
    onDismiss: () -> Unit,
    onShowCards: () -> Unit
) {
    val usedCount = entity.usedCards.size
    val cardCount = pluralStringResource(
        R.plurals.manage_usage_cards,
        usedCount,
        usedCount
    )
    val thatCard = pluralStringResource(
        R.plurals.manage_delete_blocked_that_card,
        usedCount,
        usedCount
    )
    val tagClause = if (kind == EntityKind.TAG) {
        stringResource(R.string.manage_delete_blocked_tag_clause)
    } else {
        ""
    }
    val body = stringResource(
        R.string.manage_delete_blocked_body,
        entity.name,
        cardCount,
        thatCard,
        tagClause,
        stringResource(kind.oneRes)
    )
    val onSurface = MaterialTheme.colorScheme.onSurface

    ManageDialogFrame(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.manage_delete_blocked_title),
            style = QcType.DialogTitle,
            color = onSurface
        )
        Spacer(modifier = Modifier.height(QcDimens.Gutter))
        Text(
            text = buildAnnotatedString {
                append(body)
                val start = body.indexOf(entity.name)
                if (start >= 0) {
                    addStyle(
                        SpanStyle(fontWeight = FontWeight(700), color = onSurface),
                        start,
                        start + entity.name.length
                    )
                }
            },
            style = QcType.Body,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(QcDimens.Gutter))
        ManageCardList(entity.usedCards)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = QcDimens.DialogActionGap),
            horizontalArrangement = Arrangement.spacedBy(QcDimens.Base, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onShowCards,
                colors = ButtonDefaults.textButtonColors(contentColor = onSurface)
            ) {
                Text(stringResource(R.string.manage_action_show_cards))
            }
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = onSurface)
            ) {
                Text(stringResource(R.string.manage_action_ok))
            }
        }
    }
}

@Composable
internal fun DeleteConfirmDialog(
    kind: EntityKind,
    name: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    ManageDialogFrame(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.manage_delete_confirm_title, name),
            style = QcType.DialogTitle,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(QcDimens.Gutter))
        Text(
            text = stringResource(
                R.string.manage_delete_confirm_body,
                stringResource(kind.oneRes)
            ),
            style = QcType.Body,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        DialogActions(
            onCancel = onDismiss,
            confirmLabel = stringResource(R.string.manage_action_delete),
            onConfirm = onConfirm,
            confirmDanger = true
        )
    }
}
