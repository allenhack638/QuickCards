package com.quickcards.app.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/** DESIGN.md › Compose mapping: Daylight roles onto Material 3 roles. */
private fun QcColors.toColorScheme(): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    return base.copy(
        primary = accent,
        onPrimary = surface,
        primaryContainer = tonal,
        onPrimaryContainer = ink,
        inversePrimary = inverseAccent,
        secondary = select,
        onSecondary = onSelect,
        secondaryContainer = tonal2,
        onSecondaryContainer = ink,
        tertiary = select,
        onTertiary = onSelect,
        tertiaryContainer = tonal2,
        onTertiaryContainer = ink,
        background = bg,
        onBackground = ink,
        surface = surface,
        onSurface = ink,
        surfaceVariant = tonal,
        onSurfaceVariant = ink2,
        surfaceTint = Color.Transparent,
        surfaceBright = surfaceHi,
        surfaceDim = bg,
        surfaceContainerLowest = surface,
        surfaceContainerLow = bg,
        surfaceContainer = tonal,
        surfaceContainerHigh = surfaceHi,
        surfaceContainerHighest = tonal2,
        inverseSurface = inverse,
        inverseOnSurface = onInverse,
        error = danger,
        onError = onDanger,
        errorContainer = dangerBg,
        onErrorContainer = ink,
        outline = outline,
        outlineVariant = line,
        scrim = Color.Black
    )
}

private val LightScheme = LightQcColors.toColorScheme()
private val DarkScheme = DarkQcColors.toColorScheme()

object QcTheme {
    val colors: QcColors
        @Composable
        @ReadOnlyComposable
        get() = LocalQcColors.current
}

@Composable
fun QuickCardsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val qcColors = if (darkTheme) DarkQcColors else LightQcColors
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            WindowCompat.setDecorFitsSystemWindows(window, false)
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                window.isNavigationBarContrastEnforced = false
            }
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalQcColors provides qcColors) {
        MaterialTheme(
            colorScheme = if (darkTheme) DarkScheme else LightScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
