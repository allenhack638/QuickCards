package com.quickcards.app.core.ui

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker

@Composable
fun rememberSeparatingFold(): FoldingFeature? {
    val context = LocalContext.current
    val activity = context as? Activity
    val fold = remember(activity) { mutableStateOf<FoldingFeature?>(null) }

    LaunchedEffect(activity) {
        if (activity != null) {
            WindowInfoTracker.getOrCreate(context)
                .windowLayoutInfo(activity)
                .collect { layoutInfo ->
                    fold.value = layoutInfo.displayFeatures
                        .filterIsInstance<FoldingFeature>()
                        .firstOrNull { it.isSeparating }
                }
        }
    }

    return fold.value
}
