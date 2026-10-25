package com.quickcards.app.core.ui.theme

import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable

/** Filled actions stay brand red even with system colours; disabled follows Material 3. */
@Composable
fun qcPrimaryButtonColors(): ButtonColors = ButtonDefaults.buttonColors(
    containerColor = BrandRed,
    contentColor = BrandOnRed,
    disabledContainerColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.12f),
    disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
)
