package com.quickcards.app.feature.cards

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.quickcards.app.R
import com.quickcards.app.core.ui.components.QcDialog
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTextButton
import com.quickcards.app.core.ui.theme.QcTextTone
import com.quickcards.app.core.ui.theme.QcTheme

/** Shown when the Keystore key is gone; it offers no destructive action. */
@Composable
internal fun KeyUnavailableDialog(
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    QcDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.cards_key_unavailable_title),
        actions = {
            QcTextButton(stringResource(R.string.cards_key_unavailable_dismiss), onDismiss, tone = QcTextTone.Neutral)
            QcTextButton(stringResource(R.string.cards_key_unavailable_retry), onRetry)
        }
    ) {
        Text(
            text = stringResource(R.string.cards_key_unavailable_body),
            style = QcText.F2,
            color = QcTheme.colors.ink2
        )
    }
}
