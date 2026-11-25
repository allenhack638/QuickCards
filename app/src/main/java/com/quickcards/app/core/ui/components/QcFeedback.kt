package com.quickcards.app.core.ui.components

import androidx.compose.foundation.background
import com.quickcards.app.R
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.layout.Layout
import androidx.compose.runtime.remember
import androidx.compose.runtime.getValue
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
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
 * The one status chip on card art: a slim, icon-free Copy pill (its fill and hairline ring), white
 * `f1`, 20 tall so it adds nothing to the meta line. Without [onClick] it is decoration (the card's
 * own label says the status). With it, it is a button whose tap area is at least 48 tall and
 * [QcSpacing.s1] wider each side, laid over the art without moving anything; [onLongClick] keeps
 * the card's hold working on that area.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun QcStatusChip(
    text: String,
    modifier: Modifier = Modifier,
    spokenText: String? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onLongClickLabel: String? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val pill: @Composable () -> Unit = {
        Box(
            modifier = Modifier
                .heightIn(min = QcSize.statusChip)
                .background(if (pressed) PassColors.OnArtFillPressed else PassColors.OnArtFill, QcShapes.full)
                .border(QcSize.hairline, PassColors.OnArtFillRing, QcShapes.full)
                .padding(horizontal = QcSpacing.s2)
                .clearAndSetSemantics { },
            contentAlignment = Alignment.Center
        ) {
            Text(text = text, style = QcText.F1, color = PassColors.OnArt, maxLines = 1)
        }
    }
    if (onClick == null) {
        Box(modifier) { pill() }
        return
    }
    val whyLabel = stringResource(R.string.cards_status_why)
    Layout(
        modifier = modifier,
        content = {
            pill()
            Box(
                Modifier
                    .combinedClickable(
                        interactionSource = interaction,
                        indication = null,
                        role = Role.Button,
                        onClickLabel = whyLabel,
                        onLongClickLabel = onLongClickLabel,
                        onLongClick = onLongClick,
                        onClick = onClick
                    )
                    .semantics { contentDescription = spokenText ?: text }
            )
        }
    ) { measurables, constraints ->
        val chip = measurables[0].measure(constraints.copy(minWidth = 0, minHeight = 0))
        val dx = QcSpacing.s1.roundToPx()
        val hitHeight = maxOf(chip.height, QcSize.hit.roundToPx())
        val target = measurables[1].measure(Constraints.fixed(chip.width + 2 * dx, hitHeight))
        layout(chip.width, chip.height) {
            chip.place(0, 0)
            target.place(-dx, (chip.height - hitHeight) / 2)
        }
    }
}

/**
 * The one inline banner for a fixable screen state: icon 20, one `f3`/600 line (an `f2` regular
 * line when [quiet], for a plain note), then trailing text actions or a close button. [error]
 * tints only the icon; the container stays tonal.
 * At large text the actions drop below the line, right-aligned; the text stays by its icon.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun QcBanner(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    error: Boolean = false,
    quiet: Boolean = false,
    onClose: (() -> Unit)? = null,
    closeLabel: String? = null,
    actions: (@Composable RowScope.() -> Unit)? = null
) {
    val colors = QcTheme.colors
    val container = if (LocalQcRaised.current) colors.tonalHi else colors.tonal
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
                style = if (quiet) QcText.F2 else QcText.F3Semibold,
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
