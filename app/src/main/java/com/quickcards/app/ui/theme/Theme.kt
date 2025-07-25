package com.quickcards.app.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    // Primary colors - using brighter variants for better contrast on black
    primary = PrimaryTealDark,
    secondary = SecondaryGoldDark,
    tertiary = SuccessGreen,
    
    // Pure pitch black backgrounds
    background = BackgroundDark,           // Pure black #000000
    surface = SurfaceDark,                 // Pure black #000000
    surfaceVariant = SurfaceVariantDark,   // Slightly elevated black #0A0A0A
    
    // Text colors on backgrounds
    onPrimary = TextPrimaryDark,           // White text on primary
    onSecondary = BackgroundDark,          // Black text on gold/yellow
    onTertiary = TextPrimaryDark,          // White text on green
    onBackground = TextPrimaryDark,        // White text on black background
    onSurface = TextPrimaryDark,           // White text on black surface
    onSurfaceVariant = TextSecondaryDark,  // Light grey text on surface variant
    
    // Additional surface colors
    surfaceContainer = SurfaceDark,
    surfaceContainerHigh = SurfaceVariantDark,
    surfaceContainerHighest = BorderDark,
    
    // Outline and borders
    outline = DividerDark,
    outlineVariant = BorderDark,
    
    // Error colors
    error = ErrorRed,
    onError = TextPrimaryDark
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryTeal,
    secondary = SecondaryGold,
    tertiary = SuccessGreen,
    background = BackgroundLight,
    surface = CardBackground,
    onPrimary = BackgroundLight,
    onSecondary = TextPrimary,
    onTertiary = BackgroundLight,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = ErrorRed
)

@Composable
fun QuickCardsTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }

        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            // Set status bar to pure black in dark theme, primary color in light theme
            window.statusBarColor = if (darkTheme) BackgroundDark.toArgb() else colorScheme.primary.toArgb()
            // Set navigation bar to match background
            window.navigationBarColor = colorScheme.background.toArgb()
            WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}