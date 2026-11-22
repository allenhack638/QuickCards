package com.quickcards.app.feature.manage

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.quickcards.app.R
import com.quickcards.app.core.ui.components.QcDialog
import com.quickcards.app.core.ui.entity.DialogBody
import com.quickcards.app.core.ui.theme.QcTextButton
import com.quickcards.app.core.ui.theme.QcTextTone
import com.quickcards.domain.EntityKind

/** "Can’t delete Aarav Mehta": the item is in use; OK, or Show cards to change them. */
@Composable
internal fun DeleteBlockedDialog(
    kind: EntityKind,
    entity: ManagedEntity,
    onDismiss: () -> Unit,
    onShowCards: () -> Unit
) {
    val count = pluralStringResource(R.plurals.manage_usage_cards, entity.usageCount, entity.usageCount)
    QcDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.manage_delete_blocked_title, entity.name),
        actions = {
            QcTextButton(stringResource(R.string.manage_action_ok), onDismiss, tone = QcTextTone.Neutral)
            QcTextButton(stringResource(R.string.manage_action_show_cards), onShowCards)
        }
    ) {
        DialogBody(
            stringResource(
                if (kind == EntityKind.TAG) R.string.manage_delete_blocked_tag_body else R.string.manage_delete_blocked_body,
                count
            )
        )
    }
}
