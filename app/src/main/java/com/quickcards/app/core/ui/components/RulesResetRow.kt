package com.quickcards.app.core.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.quickcards.app.R
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTextButton
import com.quickcards.app.core.ui.theme.QcTheme

/** One help line saying a built-in network's rules were changed, with Reset to standard. */
@Composable
fun RulesResetRow(text: String, networkName: String, onReset: () -> Unit) {
    val resetLabel = stringResource(R.string.manage_a11y_reset_rules, networkName)
    Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(text = text, style = QcText.F1Regular, color = QcTheme.colors.danger, modifier = Modifier.weight(1f))
        QcTextButton(
            text = stringResource(R.string.rules_reset_to_standard),
            onClick = onReset,
            modifier = Modifier.semantics { contentDescription = resetLabel }
        )
    }
}
