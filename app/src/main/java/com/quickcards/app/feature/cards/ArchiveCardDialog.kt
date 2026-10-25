package com.quickcards.app.feature.cards

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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.quickcards.app.R
import com.quickcards.app.core.ui.components.FormTextField
import com.quickcards.app.core.ui.components.QcDialog
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcType
import kotlinx.coroutines.delay

/** Archive confirmation with an optional 40-character reason. */
@Composable
fun ArchiveCardDialog(
    onArchive: (String?) -> Unit,
    onDismiss: () -> Unit
) {
    var reason by remember {
        mutableStateOf(TextFieldValue(""))
    }
    val focusRequester = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(60)
        runCatching { focusRequester.requestFocus() }
    }

    QcDialog(onDismissRequest = onDismiss) {
        Text(
            text = stringResource(R.string.cards_archive_title),
            style = QcType.DialogTitle,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(QcDimens.DialogHeaderGap))
        Text(
            text = stringResource(R.string.cards_archive_body),
            style = QcType.Body,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        FormTextField(
            value = reason,
            onValueChange = { value ->
                val text = value.text.take(40)
                val start = value.selection.start.coerceIn(0, text.length)
                val end = value.selection.end.coerceIn(0, text.length)
                reason = value.copy(text = text, selection = TextRange(start, end))
            },
            label = stringResource(R.string.cards_archive_reason),
            modifier = Modifier.padding(top = QcDimens.Gutter),
            helper = stringResource(R.string.cards_archive_reason_helper),
            counterMax = 40,
            focusRequester = focusRequester
        )
        Spacer(modifier = Modifier.height(QcDimens.DialogActionGap))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(QcDimens.Base, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(stringResource(R.string.cards_cancel))
            }
            TextButton(
                onClick = {
                    onArchive(reason.text.trim().takeIf { it.isNotEmpty() })
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(stringResource(R.string.cards_action_archive))
            }
        }
    }
}
