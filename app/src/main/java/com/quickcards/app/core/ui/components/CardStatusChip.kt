package com.quickcards.app.core.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.quickcards.app.R
import com.quickcards.domain.CardStatus

/** The card's one status chip (tile, details hero, Archived): same slot, text and tone everywhere. */
@Composable
fun CardStatusChip(
    status: CardStatus,
    modifier: Modifier = Modifier,
    archiveReason: String? = null
) {
    when (status) {
        CardStatus.ARCHIVED -> QcStatusChip(
            text = archiveReason?.takeIf { it.isNotBlank() }
                ?.let { stringResource(R.string.cards_status_archived_reason, it) }
                ?: stringResource(R.string.cards_status_archived),
            modifier = modifier,
            quiet = !archiveReason.isNullOrBlank()
        )
        CardStatus.EXPIRED -> QcStatusChip(stringResource(R.string.cards_status_expired), modifier, danger = true)
        CardStatus.CHECK -> QcStatusChip(
            text = stringResource(R.string.cards_status_check_details),
            modifier = modifier,
            spokenText = stringResource(R.string.cards_a11y_status_check)
        )
        CardStatus.EXPIRES_SOON -> QcStatusChip(stringResource(R.string.cards_status_expires_soon), modifier)
        CardStatus.LAST_USED -> QcStatusChip(stringResource(R.string.cards_last_used), modifier)
    }
}
