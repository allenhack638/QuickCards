package com.quickcards.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.translate
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.quickcards.app.core.ui.theme.QcColors
import com.quickcards.app.core.ui.theme.QcElevation
import com.quickcards.app.core.ui.theme.QcRadius
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.selection.toggleable
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcShapes
import com.quickcards.app.core.ui.theme.QcSize
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcSwitch
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTheme
import com.quickcards.app.core.ui.theme.qcElevation1

/** Section label: its left edge is the rounded group's outer left edge. */
@Composable
fun QcGroupLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = QcText.F2Semibold,
        color = QcTheme.colors.ink2,
        modifier = modifier
            .fillMaxWidth()
            .padding(top = QcSpacing.s4, bottom = QcSpacing.s2)
            .semantics { heading() }
    )
}

/**
 * One row of a [QcGroup] laid out by a lazy list (keyed `items`): each row draws its own slice,
 * rounded on the first and last, with the hairline above every row but the first, so the list
 * looks like one group. The shadow stops short of each seam so it never shades a neighbour.
 */
fun Modifier.qcGroupItem(index: Int, count: Int, colors: QcColors): Modifier {
    val first = index == 0
    val last = index == count - 1
    val shape = RoundedCornerShape(
        topStart = if (first) QcRadius.lg else 0.dp,
        topEnd = if (first) QcRadius.lg else 0.dp,
        bottomEnd = if (last) QcRadius.lg else 0.dp,
        bottomStart = if (last) QcRadius.lg else 0.dp
    )
    val shadow = if (colors.dark) {
        Modifier
    } else {
        Modifier.shadow(QcElevation.e1, SeamInsetShape(shape, top = !first, bottom = !last), clip = false, ambientColor = colors.shadow, spotColor = colors.shadow)
    }
    return this
        .fillMaxWidth()
        .then(shadow)
        .clip(shape)
        .background(colors.surface)
        .drawWithContent {
            drawContent()
            val stroke = QcSize.hairline.toPx()
            // Below a seam the ring runs one stroke past the row and is clipped, so only the line above shows.
            val reach = if (last) 0f else stroke
            val outline = shape.createOutline(Size(size.width - stroke, size.height - stroke + reach), layoutDirection, this)
            translate(stroke / 2, stroke / 2) { drawOutline(outline, colors.line, style = Stroke(stroke)) }
        }
}

private class SeamInsetShape(private val shape: Shape, private val top: Boolean, private val bottom: Boolean) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val inset = with(density) { QcSpacing.s1.toPx() }
        val t = if (top) inset else 0f
        val b = if (bottom) inset else 0f
        val inner = shape.createOutline(Size(size.width, (size.height - t - b).coerceAtLeast(0f)), layoutDirection, density)
        return when (inner) {
            is Outline.Rectangle -> Outline.Rectangle(inner.rect.translate(0f, t))
            is Outline.Rounded -> Outline.Rounded(inner.roundRect.translate(Offset(0f, t)))
            is Outline.Generic -> Outline.Generic(Path().apply { addPath(inner.path, Offset(0f, t)) })
        }
    }
}

/** Rounded `surface` group (`r-lg`, e1) with a hairline between its rows. */
@Composable
fun QcGroup(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    val colors = QcTheme.colors
    val dividers = remember { mutableListOf<Int>() }
    Layout(
        content = content,
        modifier = modifier
            .fillMaxWidth()
            .qcElevation1(colors, QcShapes.lg)
            .clip(QcShapes.lg)
            .background(colors.surface)
            .drawWithContent {
                drawContent()
                val stroke = QcSize.hairline.toPx()
                dividers.forEach { y ->
                    drawLine(colors.line, Offset(0f, y.toFloat()), Offset(size.width, y.toFloat()), stroke)
                }
            }
    ) { measurables, constraints ->
        val placeables = measurables.map { it.measure(constraints.copy(minHeight = 0)) }
        layout(constraints.maxWidth, placeables.sumOf { it.height }) {
            dividers.clear()
            var y = 0
            placeables.forEachIndexed { index, placeable ->
                if (index > 0) dividers += y
                placeable.placeRelative(0, y)
                y += placeable.height
            }
        }
    }
}

/**
 * A list row: filled leading icon 24 in `ink-2`, title and optional supporting line, then a
 * trailing slot. Min 56, or 64 with a supporting line; 16 side padding (24 in sheets via [inset]).
 * A disabled row dims as a whole (icon, text and trailing) to 38%; say why with [QcDisabledReason].
 */
@Composable
fun QcListRow(
    title: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subtitle: String? = null,
    subtitleColor: Color? = null,
    enabled: Boolean = true,
    danger: Boolean = false,
    inset: Dp = QcSpacing.s4,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    val colors = QcTheme.colors
    val ink = if (danger) colors.danger else colors.ink
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (subtitle == null) QcSize.row else QcSize.row2)
            .then(
                if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier
            )
            .alpha(if (enabled) 1f else DISABLED_ALPHA)
            .padding(horizontal = inset, vertical = QcSpacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(QcSpacing.s4)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (danger) colors.danger else colors.ink2,
                modifier = Modifier.size(QcSize.iconLg)
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = QcText.F3, color = ink)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = QcText.F2,
                    color = subtitleColor ?: colors.ink2
                )
            }
        }
        if (trailing != null) {
            Row(
                modifier = Modifier.widthIn(max = QcSize.row * 3),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(QcSpacing.s2),
                content = trailing
            )
        }
    }
}

/** Trailing chevron for rows that open a sheet or page. */
@Composable
fun QcChevron() {
    Icon(
        imageVector = QcIcons.chevronRight,
        contentDescription = null,
        tint = QcTheme.colors.ink2,
        modifier = Modifier.size(QcSize.iconLg)
    )
}

/** The whole row is the switch: one target with switch role and checked state. */
@Composable
fun QcSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    subtitle: String? = null,
    enabled: Boolean = true,
    inset: Dp = QcSpacing.s4
) {
    QcListRow(
        title = title,
        icon = icon,
        subtitle = subtitle,
        enabled = enabled,
        inset = inset,
        modifier = modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange
        ),
        trailing = { QcSwitch(checked = checked) }
    )
}

/** The one line under a group of disabled rows that says why they are off (DESIGN › Accessibility). */
@Composable
fun QcDisabledReason(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = QcText.F2,
        color = QcTheme.colors.ink2,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = QcSpacing.s4, end = QcSpacing.s4, top = QcSpacing.s2)
    )
}

private const val DISABLED_ALPHA = 0.38f
