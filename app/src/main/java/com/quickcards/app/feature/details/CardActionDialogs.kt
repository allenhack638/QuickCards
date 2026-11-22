package com.quickcards.app.feature.details

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.quickcards.app.R
import com.quickcards.app.core.ui.components.FormTextField
import com.quickcards.app.core.ui.components.QcDialog
import com.quickcards.app.core.ui.theme.QcSize
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcSwitch
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTextButton
import com.quickcards.app.core.ui.theme.QcTextTone
import com.quickcards.app.core.ui.theme.QcTheme
import kotlinx.coroutines.delay

/** Asked after the unlock, before the system share sheet; the CVV is off by default. */
@Composable
internal fun ShareCardDialog(
    hasCvv: Boolean,
    onDismiss: () -> Unit,
    onShare: (includeCvv: Boolean) -> Unit
) {
    var includeCvv by rememberSaveable { mutableStateOf(false) }
    QcDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.cards_share_title),
        actions = {
            QcTextButton(stringResource(R.string.cards_share_cancel), onDismiss, tone = QcTextTone.Neutral)
            QcTextButton(stringResource(R.string.cards_share_confirm), { onShare(hasCvv && includeCvv) })
        }
    ) {
        DialogBody(stringResource(R.string.cards_share_body))
        if (hasCvv) {
            val offState = stringResource(R.string.cards_share_cvv_state)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = QcSpacing.s3)
                    .heightIn(min = QcSize.hit)
                    .toggleable(value = includeCvv, role = Role.Switch) { includeCvv = it }
                    .semantics { if (!includeCvv) stateDescription = offState },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.cards_share_include_cvv),
                    style = QcText.F3,
                    color = QcTheme.colors.ink,
                    modifier = Modifier.weight(1f)
                )
                QcSwitch(checked = includeCvv)
            }
        }
    }
}

/** Archive confirmation with an optional 40-character reason. */
@Composable
internal fun ArchiveCardDialog(
    onArchive: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var reason by remember { mutableStateOf(TextFieldValue("")) }
    val focusRequester = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(FOCUS_DELAY_MS)
        runCatching { focusRequester.requestFocus() }
    }
    QcDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.cards_archive_title),
        actions = {
            QcTextButton(stringResource(R.string.cards_cancel), onDismiss, tone = QcTextTone.Neutral)
            QcTextButton(
                stringResource(R.string.cards_action_archive),
                { onArchive(reason.text.trim().takeIf { it.isNotEmpty() }) }
            )
        }
    ) {
        DialogBody(stringResource(R.string.cards_archive_body))
        FormTextField(
            value = reason,
            onValueChange = { value ->
                val text = value.text.take(REASON_MAX)
                reason = value.copy(
                    text = text,
                    selection = TextRange(value.selection.start.coerceIn(0, text.length), value.selection.end.coerceIn(0, text.length))
                )
            },
            label = stringResource(R.string.cards_archive_reason),
            placeholder = stringResource(R.string.cards_archive_reason_helper),
            counterMax = REASON_MAX,
            focusRequester = focusRequester,
            modifier = Modifier.padding(top = QcSpacing.s4)
        )
    }
}

@Composable
private fun DialogBody(text: String) {
    Text(text = text, style = QcText.F2, color = QcTheme.colors.ink2)
}

private const val REASON_MAX = 40

// The dialog window must be attached before the field can take focus and raise the keyboard.
private const val FOCUS_DELAY_MS = 60L
