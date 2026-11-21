package com.quickcards.app.core.ui.components

import androidx.compose.foundation.background
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
    val ink = when {
        !enabled -> colors.ink3
        danger -> colors.danger
        else -> colors.ink
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = if (subtitle == null) QcSize.row else QcSize.row2)
            .then(
                if (onClick != null) Modifier.clickable(enabled = enabled, onClick = onClick) else Modifier
            )
            .padding(horizontal = inset, vertical = QcSpacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(QcSpacing.s4)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = when {
                    !enabled -> colors.ink3
                    danger -> colors.danger
                    else -> colors.ink2
                },
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
        trailing = { QcSwitch(checked = checked, enabled = enabled) }
    )
}
