package com.quickcards.app.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.quickcards.app.R
import com.quickcards.domain.CardStatus

/**
 * The card's one status chip (tile, details hero, Archived): same slot, text and look everywhere.
 * Inside Manage › Archived ([inArchive]) an archived card's chip is its reason alone. [onClick]
 * opens the status's explanation sheet; [onLongClick] is the card's own hold.
 */
@Composable
fun CardStatusChip(
    status: CardStatus,
    modifier: Modifier = Modifier,
    archiveReason: String? = null,
    inArchive: Boolean = false,
    bestDays: Int? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null
) {
    val text = when (status) {
        CardStatus.ARCHIVED -> archiveReason?.takeIf { it.isNotBlank() }
            ?.let { if (inArchive) it else stringResource(R.string.cards_status_archived_reason, it) }
            ?: stringResource(R.string.cards_status_archived)
        CardStatus.EXPIRED -> stringResource(R.string.cards_status_expired)
        CardStatus.CHECK -> stringResource(R.string.cards_status_check_details)
        CardStatus.EXPIRES_SOON -> stringResource(R.string.cards_status_expires_soon)
        CardStatus.BEST_TODAY -> stringResource(R.string.cards_status_best_today)
        CardStatus.LAST_USED -> stringResource(R.string.cards_last_used)
    }
    val spoken = when (status) {
        CardStatus.CHECK -> stringResource(R.string.cards_a11y_status_check)
        CardStatus.BEST_TODAY -> bestDays?.let { pluralStringResource(R.plurals.cards_a11y_best_today, it, it) }
        else -> null
    }
    QcStatusChip(
        text = text,
        modifier = modifier,
        spokenText = spoken,
        onClick = onClick,
        onLongClick = onLongClick,
        onLongClickLabel = onLongClickLabel
    )
}
