package com.quickcards.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.quickcards.app.core.ui.theme.LocalQcRaised
import com.quickcards.app.core.ui.theme.PassColors
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcShapes
import com.quickcards.app.core.ui.theme.QcSize
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTheme

/**
 * The one status chip on card art (Check, Expires soon, Expired, Last used, Archived): text
 * only, white `f1` on `on-art-chip`, 1dp ring ([danger] rings it in `on-art-danger`).
 * [quiet] drops the ring for Last used and the archive reason.
 */
@Composable
fun QcStatusChip(
    text: String,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
    quiet: Boolean = false,
    spokenText: String? = null
) {
    Box(
        modifier = modifier
            .heightIn(min = QcSize.statusChip)
            .background(PassColors.OnArtChip, QcShapes.xs)
            .then(
                if (quiet) {
                    Modifier
                } else {
                    Modifier.border(
                        QcSize.hairline,
                        if (danger) PassColors.OnArtDanger else PassColors.OnArtChipRing,
                        QcShapes.xs
                    )
                }
            )
            .padding(horizontal = QcSpacing.s2)
            .then(if (spokenText != null) Modifier.semantics { contentDescription = spokenText } else Modifier),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = if (quiet) QcText.F1 else QcText.F1Semibold,
            color = PassColors.OnArt,
            maxLines = 1
        )
    }
}

/**
 * The one inline banner for a fixable screen state: icon 20, one `f3`/600 line, then trailing
 * text actions or a close button. [error] tints only the icon and uses the error container.
 * At large text the actions drop below the line, right-aligned; the text stays by its icon.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QcBanner(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    error: Boolean = false,
    onClose: (() -> Unit)? = null,
    closeLabel: String? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    val colors = QcTheme.colors
    val container = when {
        error -> colors.dangerBg
        LocalQcRaised.current -> colors.tonalHi
        else -> colors.tonal
    }
    FlowRow(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = QcSize.row)
            .background(container, QcShapes.lg)
            .padding(
                start = QcSpacing.s4,
                end = if (actions != null || onClose != null) QcSpacing.s1 else QcSpacing.s3,
                top = QcSpacing.s1,
                bottom = QcSpacing.s1
            ),
        horizontalArrangement = Arrangement.End,
        verticalArrangement = Arrangement.Center
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .heightIn(min = QcSize.hit)
                .align(Alignment.CenterVertically),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(QcSpacing.s3)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (error) colors.danger else colors.ink2,
                modifier = Modifier.size(QcSize.icon)
            )
            Text(
                text = text,
                style = QcText.F3Semibold,
                color = colors.ink,
                modifier = Modifier.padding(vertical = QcSpacing.s2)
            )
        }
        if (actions != null || onClose != null) {
            Row(
                modifier = Modifier.align(Alignment.CenterVertically),
                verticalAlignment = Alignment.CenterVertically
            ) {
                actions?.invoke(this)
                if (onClose != null) {
                    IconButton(onClick = onClose, modifier = Modifier.size(QcSize.hit)) {
                        Icon(
                            imageVector = QcIcons.close,
                            contentDescription = closeLabel,
                            tint = colors.ink2,
                            modifier = Modifier.size(QcSize.iconLg)
                        )
                    }
                }
            }
        }
    }
}
