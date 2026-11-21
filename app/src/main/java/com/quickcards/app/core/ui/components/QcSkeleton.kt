package com.quickcards.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import com.quickcards.app.core.ui.theme.QcShapes
import com.quickcards.app.core.ui.theme.QcTheme

/** A static tonal block standing in for content that is still loading; it never animates. */
@Composable
fun QcSkeleton(modifier: Modifier = Modifier, shape: Shape = QcShapes.md) {
    Box(modifier = modifier.background(QcTheme.colors.tonal, shape))
}
