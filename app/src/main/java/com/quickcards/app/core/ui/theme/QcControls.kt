package com.quickcards.app.core.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwitchColors
import androidx.compose.material3.SwitchDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

/** Switches select in neutral (secondary), never in brand red. */
@Composable
fun qcSwitchColors(): SwitchColors = SwitchDefaults.colors(
    checkedThumbColor = MaterialTheme.colorScheme.onSecondary,
    checkedTrackColor = MaterialTheme.colorScheme.secondary,
    checkedBorderColor = MaterialTheme.colorScheme.secondary,
    checkedIconColor = MaterialTheme.colorScheme.onSecondary,
    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
    uncheckedIconColor = Color.Transparent
)
