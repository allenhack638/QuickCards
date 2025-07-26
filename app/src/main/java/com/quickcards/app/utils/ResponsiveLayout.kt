package com.quickcards.app.utils

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration

/**
 * Utility for responsive layout handling that adapts to different orientations and screen sizes
 */
object ResponsiveLayout {
    
    /**
     * Determines if the current layout should use a horizontal (landscape) or vertical (portrait) arrangement
     * based on screen size and orientation
     */
    @Composable
    fun shouldUseHorizontalLayout(): Boolean {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        val screenHeight = configuration.screenHeightDp
        val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
        
        // Use horizontal layout for:
        // 1. Landscape orientation on any device
        // 2. Large tablets (width >= 840dp) in portrait
        return isLandscape || screenWidth >= 840
    }
    
    /**
     * Creates a responsive container that adapts its arrangement based on screen size and orientation
     */
    @Composable
    fun ResponsiveContainer(
        modifier: Modifier = Modifier,
        horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(ResponsiveDimensions.getResponsiveSpacing().medium),
        verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(ResponsiveDimensions.getResponsiveSpacing().medium),
        content: @Composable () -> Unit
    ) {
        if (shouldUseHorizontalLayout()) {
            Row(
                modifier = modifier.fillMaxSize(),
                horizontalArrangement = horizontalArrangement
            ) {
                content()
            }
        } else {
            Column(
                modifier = modifier.fillMaxSize(),
                verticalArrangement = verticalArrangement
            ) {
                content()
            }
        }
    }
    
    /**
     * Creates a responsive form layout that adapts to screen size
     */
    @Composable
    fun ResponsiveFormLayout(
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    ) {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        
        // For larger screens, use a centered form with max width
        val formModifier = if (screenWidth >= 600) {
            modifier.fillMaxWidth(0.8f)
        } else {
            modifier.fillMaxWidth()
        }
        
        Column(
            modifier = formModifier,
            verticalArrangement = Arrangement.spacedBy(ResponsiveDimensions.getResponsiveSpacing().medium)
        ) {
            content()
        }
    }
    
    /**
     * Creates a responsive grid layout for cards
     */
    @Composable
    fun ResponsiveGridLayout(
        modifier: Modifier = Modifier,
        content: @Composable () -> Unit
    ) {
        val configuration = LocalConfiguration.current
        val screenWidth = configuration.screenWidthDp
        
        // Determine grid columns based on screen width
        val columns = when {
            screenWidth >= 840 -> 3 // Large tablets and desktops
            screenWidth >= 600 -> 2 // Medium tablets
            else -> 1 // Phones
        }
        
        // For now, we'll use a simple column layout
        // In a real implementation, you might want to use LazyVerticalGrid
        Column(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(ResponsiveDimensions.getResponsiveSpacing().large)
        ) {
            content()
        }
    }
} 