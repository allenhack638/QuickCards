package com.quickcards.app.core.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val QuickCardsLightColorScheme = lightColorScheme(
    primary = BrandRed,
    onPrimary = BrandOnRed,
    primaryContainer = LightSurfaceContainerHighest,
    onPrimaryContainer = LightOnSurface,
    inversePrimary = LightInversePrimary,

    secondary = LightSelected,
    onSecondary = LightOnSelected,
    secondaryContainer = LightIndicator,
    onSecondaryContainer = LightOnSurface,

    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightBackground,
    onSurface = LightOnSurface,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = LightBackground,
    surfaceContainerLow = LightSurfaceContainerLow,
    surfaceContainer = LightSurfaceContainer,
    surfaceContainerHigh = LightSurfaceContainerHigh,
    surfaceContainerHighest = LightSurfaceContainerHighest,
    surfaceBright = LightSurfaceContainer,
    surfaceDim = LightSurfaceContainerHigh,
    inverseSurface = LightInverseSurface,
    inverseOnSurface = LightInverseOnSurface,

    error = LightError,
    onError = Color.White,

    outline = LightOutline,
    outlineVariant = LightOutlineVariant,
    scrim = Color.Black
)

private val QuickCardsDarkColorScheme = darkColorScheme(
    primary = BrandRed,
    onPrimary = BrandOnRed,
    primaryContainer = DarkSurfaceContainerHighest,
    onPrimaryContainer = DarkOnSurface,
    inversePrimary = DarkInversePrimary,

    secondary = DarkSelected,
    onSecondary = DarkOnSelected,
    secondaryContainer = DarkIndicator,
    onSecondaryContainer = DarkOnSurface,

    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkBackground,
    onSurface = DarkOnSurface,
    surfaceTint = Color.Transparent,
    surfaceContainerLowest = DarkBackground,
    surfaceContainerLow = DarkSurfaceContainerLow,
    surfaceContainer = DarkSurfaceContainer,
    surfaceContainerHigh = DarkSurfaceContainerHigh,
    surfaceContainerHighest = DarkSurfaceContainerHighest,
    surfaceBright = DarkSurfaceContainerHighest,
    surfaceDim = DarkBackground,
    inverseSurface = DarkInverseSurface,
    inverseOnSurface = DarkInverseOnSurface,

    error = DarkError,
    onError = Color.White,

    outline = DarkOutline,
    outlineVariant = DarkOutlineVariant,
    scrim = Color.Black
)

@Composable
fun QuickCardsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val dynamic = if (darkTheme) {
                dynamicDarkColorScheme(context)
            } else {
                dynamicLightColorScheme(context)
            }
            val base = if (darkTheme) QuickCardsDarkColorScheme else QuickCardsLightColorScheme
            base.copy(
                surfaceContainerLow = dynamic.surfaceContainerLow,
                surfaceContainer = dynamic.surfaceContainer,
                surfaceContainerHigh = dynamic.surfaceContainerHigh,
                surfaceContainerHighest = dynamic.surfaceContainerHighest
            )
        }
        darkTheme -> QuickCardsDarkColorScheme
        else -> QuickCardsLightColorScheme
    }
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
            colorScheme = colorScheme,
            typography = Typography,
            shapes = Shapes,
            content = content
        )
    }
}
