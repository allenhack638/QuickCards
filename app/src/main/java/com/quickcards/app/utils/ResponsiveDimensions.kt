package com.quickcards.app.utils

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp

/**
 * Utility class for responsive dimensions that adapt to different screen sizes and orientations
 */
object ResponsiveDimensions {
    
    /**
     * Get responsive padding based on screen size
     */
    @Composable
    fun getResponsivePadding(): ResponsivePadding {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        val screenHeight = configuration.screenHeightDp
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        
        return when {
            // Large tablets and desktops
            screenWidth >= 840 -> ResponsivePadding(
                horizontal = 32.dp,
                vertical = 24.dp,
                small = 16.dp,
                medium = 24.dp,
                large = 32.dp
            )
            // Medium tablets
            screenWidth >= 600 -> ResponsivePadding(
                horizontal = 24.dp,
                vertical = 20.dp,
                small = 12.dp,
                medium = 20.dp,
                large = 28.dp
            )
            // Small tablets and large phones
            screenWidth >= 480 -> ResponsivePadding(
                horizontal = 20.dp,
                vertical = 16.dp,
                small = 8.dp,
                medium = 16.dp,
                large = 24.dp
            )
            // Standard phones
            else -> ResponsivePadding(
                horizontal = 16.dp,
                vertical = 12.dp,
                small = 8.dp,
                medium = 12.dp,
                large = 20.dp
            )
        }
    }
    
    /**
     * Get responsive spacing based on screen size
     */
    @Composable
    fun getResponsiveSpacing(): ResponsiveSpacing {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        
        return when {
            screenWidth >= 840 -> ResponsiveSpacing(
                small = 8.dp,
                medium = 16.dp,
                large = 24.dp,
                extraLarge = 32.dp
            )
            screenWidth >= 600 -> ResponsiveSpacing(
                small = 6.dp,
                medium = 12.dp,
                large = 20.dp,
                extraLarge = 28.dp
            )
            screenWidth >= 480 -> ResponsiveSpacing(
                small = 4.dp,
                medium = 8.dp,
                large = 16.dp,
                extraLarge = 24.dp
            )
            else -> ResponsiveSpacing(
                small = 4.dp,
                medium = 8.dp,
                large = 12.dp,
                extraLarge = 20.dp
            )
        }
    }
    
    /**
     * Get responsive card dimensions
     */
    @Composable
    fun getResponsiveCardDimensions(): ResponsiveCardDimensions {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        
        return when {
            screenWidth >= 840 -> ResponsiveCardDimensions(
                cornerRadius = 20.dp,
                elevation = 16.dp,
                padding = 24.dp
            )
            screenWidth >= 600 -> ResponsiveCardDimensions(
                cornerRadius = 16.dp,
                elevation = 12.dp,
                padding = 20.dp
            )
            screenWidth >= 480 -> ResponsiveCardDimensions(
                cornerRadius = 12.dp,
                elevation = 8.dp,
                padding = 16.dp
            )
            else -> ResponsiveCardDimensions(
                cornerRadius = 12.dp,
                elevation = 6.dp,
                padding = 12.dp
            )
        }
    }
    
    /**
     * Get responsive input field dimensions
     */
    @Composable
    fun getResponsiveInputDimensions(): ResponsiveInputDimensions {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        
        return when {
            screenWidth >= 840 -> ResponsiveInputDimensions(
                minHeight = 64.dp,
                padding = 16.dp,
                cornerRadius = 12.dp
            )
            screenWidth >= 600 -> ResponsiveInputDimensions(
                minHeight = 56.dp,
                padding = 12.dp,
                cornerRadius = 8.dp
            )
            screenWidth >= 480 -> ResponsiveInputDimensions(
                minHeight = 52.dp,
                padding = 12.dp,
                cornerRadius = 8.dp
            )
            else -> ResponsiveInputDimensions(
                minHeight = 48.dp,
                padding = 12.dp,
                cornerRadius = 8.dp
            )
        }
    }
}

/**
 * Data classes for responsive dimensions
 */
data class ResponsivePadding(
    val horizontal: androidx.compose.ui.unit.Dp,
    val vertical: androidx.compose.ui.unit.Dp,
    val small: androidx.compose.ui.unit.Dp,
    val medium: androidx.compose.ui.unit.Dp,
    val large: androidx.compose.ui.unit.Dp
)

data class ResponsiveSpacing(
    val small: androidx.compose.ui.unit.Dp,
    val medium: androidx.compose.ui.unit.Dp,
    val large: androidx.compose.ui.unit.Dp,
    val extraLarge: androidx.compose.ui.unit.Dp
)

data class ResponsiveCardDimensions(
    val cornerRadius: androidx.compose.ui.unit.Dp,
    val elevation: androidx.compose.ui.unit.Dp,
    val padding: androidx.compose.ui.unit.Dp
)

data class ResponsiveInputDimensions(
    val minHeight: androidx.compose.ui.unit.Dp,
    val padding: androidx.compose.ui.unit.Dp,
    val cornerRadius: androidx.compose.ui.unit.Dp
) 