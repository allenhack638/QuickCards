package com.quickcards.app.core.ui.theme

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign

/** True inside sheets, dialogs and menus, where tonal fills step up to the raised tones. */
val LocalQcRaised = compositionLocalOf { false }

enum class QcTextTone { Accent, Neutral, Danger }

/** Filled actions stay brand red; disabled is ink 12% / 38% like every variant. */
@Composable
fun qcPrimaryButtonColors(): ButtonColors = qcButtonColors(BrandRed, BrandOnRed)

@Composable
private fun qcButtonColors(container: Color, content: Color): ButtonColors {
    val ink = QcTheme.colors.ink
    return ButtonDefaults.buttonColors(
        containerColor = container,
        contentColor = content,
        disabledContainerColor = if (container == Color.Transparent) container else ink.copy(alpha = 0.12f),
        disabledContentColor = ink.copy(alpha = 0.38f)
    )
}

/** The one brand-red primary action per screen. */
@Composable
fun QcFilledButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    large: Boolean = false,
    icon: ImageVector? = null
) = QcButton(text, onClick, modifier, enabled, large, icon, qcPrimaryButtonColors())

/** A lone sheet footer action (Done); steps to tonal-2 on the app background. */
@Composable
fun QcTonalButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    large: Boolean = false,
    icon: ImageVector? = null
) {
    val colors = QcTheme.colors
    val fill = if (LocalQcRaised.current) colors.tonalHi else colors.tonal2
    QcButton(text, onClick, modifier, enabled, large, icon, qcButtonColors(fill, colors.ink))
}

/** Secondary beside a filled primary (Cancel): 1dp outline, ink text. */
@Composable
fun QcOutlinedButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    large: Boolean = false,
    icon: ImageVector? = null
) {
    val colors = QcTheme.colors
    QcButton(
        text = text,
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        large = large,
        icon = icon,
        colors = qcButtonColors(Color.Transparent, colors.ink),
        border = BorderStroke(
            QcSize.hairline,
            if (enabled) colors.outline else colors.ink.copy(alpha = 0.12f)
        )
    )
}

/** Dialog, inline and banner actions; [QcTextTone.Danger] is the destructive confirm. */
@Composable
fun QcTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    tone: QcTextTone = QcTextTone.Accent,
    icon: ImageVector? = null
) {
    val colors = QcTheme.colors
    val content = when (tone) {
        QcTextTone.Accent -> colors.accent
        QcTextTone.Neutral -> colors.ink2
        QcTextTone.Danger -> colors.danger
    }
    QcButton(
        text = text,
        onClick = onClick,
        modifier = modifier.widthIn(min = QcSize.hit),
        enabled = enabled,
        large = false,
        icon = icon,
        colors = qcButtonColors(Color.Transparent, content),
        padding = PaddingValues(horizontal = QcSpacing.s3, vertical = QcSpacing.s1)
    )
}

@Composable
private fun QcButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier,
    enabled: Boolean,
    large: Boolean,
    icon: ImageVector?,
    colors: ButtonColors,
    border: BorderStroke? = null,
    padding: PaddingValues = if (large) {
        PaddingValues(horizontal = QcSpacing.s6, vertical = QcSpacing.s2)
    } else {
        PaddingValues(horizontal = QcSpacing.s4, vertical = QcSpacing.s1)
    }
) {
    Button(
        onClick = onClick,
        modifier = modifier.heightIn(min = if (large) QcSize.ctlLg else QcSize.ctlMd),
        enabled = enabled,
        shape = QcShapes.full,
        colors = colors,
        elevation = null,
        border = border,
        contentPadding = padding
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(if (large) QcSize.icon else QcSize.iconSm)
            )
            Spacer(modifier = Modifier.width(QcSpacing.s2))
        }
        Text(
            text = text,
            style = if (large) QcText.F3Semibold else MaterialTheme.typography.labelLarge,
            textAlign = TextAlign.Center
        )
    }
}
