package com.quickcards.app.feature.details

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.Constraints
import com.quickcards.app.core.ui.theme.PassColors
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcMotion
import com.quickcards.app.core.ui.theme.QcShapes
import com.quickcards.app.core.ui.theme.QcSize
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTheme

/** What one details row shows and does; [onCopy] null means nothing to copy (CVV None). */
internal class DetailsRowSpec(
    val label: String,
    val value: AnnotatedString,
    val mono: Boolean,
    val spokenValue: String,
    val copyLabel: String,
    val copiedLabel: String,
    val onCopy: (() -> Unit)?,
    val revealLabel: String? = null,
    val onReveal: (() -> Unit)? = null,
    val muted: Boolean = false
)

/**
 * Key, value and a right-aligned copy disc. The value wraps under the key when its longest word
 * no longer fits beside it (large text). Copied tints the value in the card hue and fills the
 * disc; [drain] (1 → 0) draws the reveal line under the row.
 */
@Composable
internal fun DetailsRow(
    spec: DetailsRowSpec,
    copied: Boolean,
    hueInk: Color,
    hueFill: Color,
    copiedWord: String,
    drain: Float?
) {
    val colors = QcTheme.colors
    val lineColor = colors.ink
    val latestReveal by rememberUpdatedState(spec.onReveal)
    val reveal = { latestReveal?.invoke() }
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = QcSize.row2)
            .then(
                if (spec.onReveal != null) {
                    Modifier
                        .pointerInput(Unit) { detectTapGestures(onLongPress = { reveal() }) }
                        .semantics { onLongClick(spec.revealLabel) { reveal(); true } }
                } else {
                    Modifier
                }
            )
            .drawBehind {
                if (drain != null && drain > 0f) {
                    val h = RevealLine.toPx()
                    val start = QcSpacing.s4.toPx()
                    drawRoundRect(
                        color = lineColor,
                        topLeft = Offset(start, size.height - h),
                        size = Size((size.width - start * 2) * drain, h),
                        cornerRadius = CornerRadius(h / 2)
                    )
                }
            }
            .padding(start = QcSpacing.s4, end = QcSpacing.s2, top = QcSpacing.s1, bottom = QcSpacing.s1),
        contentAlignment = Alignment.CenterStart
    ) {
        val valueColor = when {
            copied -> hueInk
            spec.muted -> colors.ink2
            else -> colors.ink
        }
        KeyValueAct(
            key = {
                Text(text = spec.label, style = QcText.F2, color = colors.ink2, modifier = Modifier.clearAndSetSemantics { })
            },
            value = {
                Text(
                    text = spec.value,
                    style = when {
                        spec.mono -> QcText.MonoF3
                        spec.muted -> QcText.F3
                        else -> QcText.F3Medium
                    },
                    color = valueColor,
                    modifier = Modifier.semantics { contentDescription = spec.spokenValue }
                )
            },
            act = {
                if (spec.onCopy != null) CopyDisc(spec, copied, hueInk, hueFill, copiedWord)
            }
        )
    }
}

@Composable
private fun CopyDisc(spec: DetailsRowSpec, copied: Boolean, hueInk: Color, hueFill: Color, copiedWord: String) {
    val colors = QcTheme.colors
    val wordAlpha by animateFloatAsState(if (copied) 1f else 0f, tween(QcMotion.STATE_MS))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = copiedWord,
            style = QcText.F1,
            color = hueInk,
            modifier = Modifier
                .padding(end = QcSpacing.s1)
                .alpha(wordAlpha)
                .clearAndSetSemantics { }
        )
        Box(
            modifier = Modifier
                .size(QcSize.hit)
                .clickable(role = Role.Button, onClick = { spec.onCopy?.invoke() })
                .semantics { contentDescription = if (copied) spec.copiedLabel else spec.copyLabel },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(QcSize.ctlMd)
                    .background(if (copied) hueFill else colors.tonal, QcShapes.full),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (copied) QcIcons.check else QcIcons.copy,
                    contentDescription = null,
                    tint = if (copied) PassColors.OnArt else colors.ink,
                    modifier = Modifier.size(QcSize.iconSm)
                )
            }
        }
    }
}

@Composable
private fun KeyValueAct(
    key: @Composable () -> Unit,
    value: @Composable () -> Unit,
    act: @Composable () -> Unit
) {
    Layout(contents = listOf(key, value, act)) { (keys, values, acts), constraints ->
        val gap = QcSpacing.s3.roundToPx()
        val width = constraints.maxWidth
        val actPlaceable = acts.firstOrNull()?.measure(Constraints())
        val actWidth = actPlaceable?.width ?: 0
        val textMax = (width - actWidth - if (actPlaceable != null) gap else 0).coerceAtLeast(0)
        val keyPlaceable = keys.first().measure(
            Constraints(minWidth = KeyMinWidth.roundToPx().coerceAtMost(textMax), maxWidth = textMax)
        )
        val value = values.first()
        val besideMax = textMax - keyPlaceable.width - gap
        val inline = besideMax > 0 && value.minIntrinsicWidth(Constraints.Infinity) <= besideMax
        val valuePlaceable = value.measure(Constraints(maxWidth = if (inline) besideMax else textMax))
        val textHeight = if (inline) {
            maxOf(keyPlaceable.height, valuePlaceable.height)
        } else {
            keyPlaceable.height + valuePlaceable.height
        }
        val height = maxOf(textHeight, actPlaceable?.height ?: 0, constraints.minHeight)
        layout(width, height) {
            val top = (height - textHeight) / 2
            if (inline) {
                keyPlaceable.placeRelative(0, top + (textHeight - keyPlaceable.height) / 2)
                valuePlaceable.placeRelative(keyPlaceable.width + gap, top + (textHeight - valuePlaceable.height) / 2)
            } else {
                keyPlaceable.placeRelative(0, top)
                valuePlaceable.placeRelative(0, top + keyPlaceable.height)
            }
            actPlaceable?.placeRelative(width - actWidth, (height - actPlaceable.height) / 2)
        }
    }
}

private val KeyMinWidth = QcSpacing.s16 + QcSpacing.s2
private val RevealLine = QcSize.focus
