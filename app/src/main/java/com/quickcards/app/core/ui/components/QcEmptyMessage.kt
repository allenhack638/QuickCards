package com.quickcards.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.quickcards.app.core.ui.theme.QcShapes
import com.quickcards.app.core.ui.theme.QcSize
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTextButton
import com.quickcards.app.core.ui.theme.QcTheme

/**
 * Empty or no-result state: a 64 tonal disc with the icon, the title (`f4`, or `f3` when
 * [compact]), an optional `f2` body and an optional text action.
 */
@Composable
fun QcEmptyMessage(
    icon: ImageVector,
    title: String,
    modifier: Modifier = Modifier,
    body: String? = null,
    compact: Boolean = false,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null
) {
    val colors = QcTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = QcSpacing.s6,
                end = QcSpacing.s6,
                top = if (compact) QcSpacing.s10 else QcSpacing.s12
            ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(QcSpacing.s16)
                .background(colors.tonal, QcShapes.full),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = colors.ink2, modifier = Modifier.size(QcSize.iconLg))
        }
        Text(
            text = title,
            style = if (compact) QcText.F3Semibold else QcText.F4,
            color = colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier
                .padding(top = if (compact) QcSpacing.s4 else QcSpacing.s6)
                .semantics { heading() }
        )
        if (body != null) {
            Text(
                text = body,
                style = QcText.F2,
                color = colors.ink2,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = QcSpacing.s2)
            )
        }
        if (actionLabel != null && onAction != null) {
            QcTextButton(text = actionLabel, onClick = onAction, modifier = Modifier.padding(top = QcSpacing.s6))
        }
    }
}
