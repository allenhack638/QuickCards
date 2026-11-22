package com.quickcards.app.core.ui.theme

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Selection is neutral (`select` / `on-select`), never brand or accent. */
@Composable
fun qcSwitchColors(): SwitchColors {
    val colors = QcTheme.colors
    return SwitchDefaults.colors(
        checkedThumbColor = colors.onSelect,
        checkedTrackColor = colors.select,
        checkedBorderColor = colors.select,
        checkedIconColor = colors.select,
        uncheckedThumbColor = colors.ink2,
        uncheckedTrackColor = colors.tonal2,
        uncheckedBorderColor = colors.outline,
        uncheckedIconColor = Color.Transparent
    )
}

/**
 * The Daylight switch (44 × 24 track, 16 thumb). Visual only: the row that hosts it is the
 * toggle target (`Modifier.toggleable(role = Role.Switch)`), which also carries the 48dp hit.
 */
@Composable
fun QcSwitch(checked: Boolean, modifier: Modifier = Modifier) {
    val colors = QcTheme.colors
    val track by animateColorAsState(
        if (checked) colors.select else colors.tonal2,
        tween(QcMotion.STATE_MS)
    )
    val thumb by animateColorAsState(
        if (checked) colors.onSelect else colors.ink2,
        tween(QcMotion.STATE_MS)
    )
    val travel by animateDpAsState(
        if (checked) QcSpacing.s5 else 0.dp,
        tween(QcMotion.STATE_MS, easing = QcMotion.ease)
    )
    Box(
        modifier = modifier
            .size(width = QcSize.switchWidth, height = QcSize.ctlSm)
            .background(track, QcShapes.full)
            .then(
                if (checked) Modifier else Modifier.border(QcSize.hairline, colors.outline, QcShapes.full)
            )
            .padding(QcSpacing.s1),
        contentAlignment = Alignment.CenterStart
    ) {
        Box(
            modifier = Modifier
                .offset(x = travel)
                .size(QcSize.iconXs)
                .background(thumb, QcShapes.full)
        )
    }
}

/** Checkbox box 20, `r-xs`; visual only, like [QcSwitch]. */
@Composable
fun QcCheckbox(checked: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = QcTheme.colors
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.38f)
            .size(QcSize.icon)
            .then(
                if (checked) {
                    Modifier.background(colors.select, QcShapes.xs)
                } else {
                    Modifier.border(QcSize.focus, colors.ink2, QcShapes.xs)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        if (checked) {
            Icon(
                imageVector = QcIcons.check,
                contentDescription = null,
                tint = colors.onSelect,
                modifier = Modifier.size(QcSize.iconXs)
            )
        }
    }
}

/** Radio 20 with a 10 dot; visual only, like [QcSwitch]. */
@Composable
fun QcRadio(selected: Boolean, modifier: Modifier = Modifier, enabled: Boolean = true) {
    val colors = QcTheme.colors
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.38f)
            .size(QcSize.icon)
            .border(QcSize.focus, if (selected) colors.select else colors.ink2, QcShapes.full),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Box(modifier = Modifier.size(QcSize.icon / 2).background(colors.select, QcShapes.full))
        }
    }
}
