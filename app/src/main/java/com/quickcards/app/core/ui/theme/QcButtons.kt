package com.quickcards.app.core.ui.theme

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/**
 * The one filled action colour. It stays brand red whatever "Use system
 * colours" does to the rest of the theme; disabled follows Material 3 exactly
 * (onSurface at 12% for the container and 38% for the label).
 */
@Composable
fun qcPrimaryButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = BrandRed,
    contentColor = BrandOnRed,
    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
)
