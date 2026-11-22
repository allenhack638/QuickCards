package com.quickcards.app.feature.cards

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.quickcards.app.R
import com.quickcards.app.core.data.repository.RepairSummary
import com.quickcards.app.core.ui.components.QcBanner
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcTextButton

/** The one-time legacy repair summary: "3 fixed · 1 to check", Review and a close button. */
@Composable
internal fun RepairBanner(
    summary: RepairSummary,
    onReview: () -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    QcBanner(
        text = stringResource(R.string.cards_repair_summary, summary.fixedCards, summary.needsCheck),
        icon = QcIcons.autoFixHigh,
        modifier = modifier,
        onClose = onDismiss,
        closeLabel = stringResource(R.string.cards_repair_dismiss),
        actions = if (summary.needsCheck > 0) {
            { QcTextButton(text = stringResource(R.string.cards_repair_review), onClick = onReview) }
        } else {
            null
        }
    )
}

/** Without a device screen lock nothing can be copied; Settings opens the screen lock settings. */
@Composable
internal fun NoLockBanner(onSettings: () -> Unit, modifier: Modifier = Modifier) {
    val actionLabel = stringResource(R.string.cards_a11y_nolock_action)
    QcBanner(
        text = stringResource(R.string.cards_nolock_banner),
        icon = QcIcons.phonelinkLock,
        modifier = modifier,
        error = true,
        actions = {
            QcTextButton(
                text = stringResource(R.string.cards_nolock_action),
                onClick = onSettings,
                modifier = Modifier.semantics { contentDescription = actionLabel }
            )
        }
    )
}
