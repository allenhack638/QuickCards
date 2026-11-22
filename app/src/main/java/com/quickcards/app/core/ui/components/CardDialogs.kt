package com.quickcards.app.core.ui.components

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.quickcards.app.R
import com.quickcards.app.core.ui.HomeCard
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTextButton
import com.quickcards.app.core.ui.theme.QcTextTone
import com.quickcards.app.core.ui.theme.QcTheme

/** The delete confirmation shared by Cards, Card details and Archived; Undo follows in a snackbar. */
@Composable
fun QcDeleteCardDialog(
    card: HomeCard,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    QcDialog(
        onDismissRequest = onDismiss,
        title = maskedNumberText(stringResource(R.string.cards_delete_title, card.bankName, card.card.number.takeLast(4))),
        actions = {
            QcTextButton(stringResource(R.string.cards_cancel), onDismiss, tone = QcTextTone.Neutral)
            QcTextButton(stringResource(R.string.cards_delete), onConfirm, tone = QcTextTone.Danger)
        }
    ) {
        Text(
            text = stringResource(R.string.cards_delete_body),
            style = QcText.F2,
            color = QcTheme.colors.ink2
        )
    }
}
