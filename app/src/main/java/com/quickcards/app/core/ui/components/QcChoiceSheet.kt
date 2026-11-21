package com.quickcards.app.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.quickcards.app.core.ui.theme.QcRadio
import com.quickcards.app.core.ui.theme.QcSize
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTheme

/** One option of a [QcChoiceSheet]. */
data class QcChoice<T>(val value: T, val label: String)

/**
 * Every single choice: a sheet with a title (and an optional supporting line), then radio rows.
 * Picking applies and closes; Back, the scrim and the handle cancel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> QcChoiceSheet(
    title: String,
    options: List<QcChoice<T>>,
    selected: T,
    onPick: (T) -> Unit,
    onDismiss: () -> Unit,
    subtitle: String? = null
) {
    val colors = QcTheme.colors
    QcModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = QcSpacing.s6, end = QcSpacing.s6, top = QcSpacing.s2, bottom = QcSpacing.s3)
        ) {
            Text(
                text = title,
                style = QcText.F4,
                color = colors.ink,
                modifier = Modifier.semantics { heading() }
            )
            if (subtitle != null) {
                Text(text = subtitle, style = QcText.F2, color = colors.ink2)
            }
        }
        Column(modifier = Modifier.selectableGroup().padding(bottom = QcSpacing.s4)) {
            options.forEach { option ->
                val on = option.value == selected
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = QcSize.row)
                        .selectable(
                            selected = on,
                            role = Role.RadioButton,
                            onClick = {
                                onPick(option.value)
                                onDismiss()
                            }
                        )
                        .padding(horizontal = QcSpacing.s6, vertical = QcSpacing.s2),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(QcSpacing.s3)
                ) {
                    QcRadio(selected = on)
                    Text(text = option.label, style = QcText.F3, color = colors.ink)
                }
            }
        }
    }
}
