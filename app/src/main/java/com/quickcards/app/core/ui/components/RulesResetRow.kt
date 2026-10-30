package com.quickcards.app.core.ui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.quickcards.app.R
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcType

/**
 * One quiet line saying a built-in network's rules were changed, with Reset to standard.
 * Used in the network editor and on the card form.
 */
@Composable
fun RulesResetRow(text: String, onReset: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = QcDimens.Half),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = QcType.Body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        // Text sits flush with the field edge above; the touch target stays full size.
        TextButton(
            onClick = onReset,
            contentPadding = PaddingValues(horizontal = QcDimens.Base),
            modifier = Modifier.offset(x = QcDimens.Base),
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Text(stringResource(R.string.rules_reset_to_standard))
        }
    }
}
