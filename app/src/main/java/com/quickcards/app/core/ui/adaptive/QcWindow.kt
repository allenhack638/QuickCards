package com.quickcards.app.core.ui.adaptive

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker
import com.quickcards.app.core.ui.theme.QcDimens

enum class WidthClass { COMPACT, MEDIUM, EXPANDED }

/** A vertical separating hinge (book posture), in window pixels. */
@Immutable
data class Hinge(val left: Int, val right: Int)

/** DESIGN.md › Adaptive: the window's size class, a short (landscape phone) height and the hinge. */
@Immutable
data class QcWindow(
    val width: WidthClass,
    val compactHeight: Boolean,
    val hinge: Hinge? = null
) {
    val rail: Boolean get() = width != WidthClass.COMPACT

    /** List and detail always side by side: expanded, or split by a hinge. */
    val twoPane: Boolean get() = !compactHeight && (width == WidthClass.EXPANDED || hinge != null)

    /** Opening a card puts it beside the list (medium) instead of on its own screen. */
    val detailBesideList: Boolean get() = twoPane || (width == WidthClass.MEDIUM && !compactHeight)

    /** Add and Edit as a 560 dialog over the current screen. */
    val formDialog: Boolean get() = width != WidthClass.COMPACT && !compactHeight

    /** Card details as card left, rows right on a full screen (phone landscape). */
    val sideBySideDetails: Boolean get() = compactHeight && width != WidthClass.COMPACT

    val settingsTwoPane: Boolean get() = width == WidthClass.EXPANDED && !compactHeight

    companion object {
        fun of(width: Dp, height: Dp, hinge: Hinge? = null): QcWindow = QcWindow(
            width = when {
                width >= QcDimens.ExpandedWidth -> WidthClass.EXPANDED
                width >= QcDimens.MediumWidth -> WidthClass.MEDIUM
                else -> WidthClass.COMPACT
            },
            compactHeight = height < QcDimens.CompactHeight,
            hinge = hinge
        )
    }
}

/** The vertical separating fold, read from the window (never the device model). */
@Composable
fun rememberHinge(): Hinge? {
    val context = LocalContext.current
    val activity = context as? Activity ?: return null
    var hinge by remember(activity) { mutableStateOf<Hinge?>(null) }
    LaunchedEffect(activity) {
        WindowInfoTracker.getOrCreate(context).windowLayoutInfo(activity).collect { info ->
            hinge = info.displayFeatures
                .filterIsInstance<FoldingFeature>()
                .firstOrNull { it.isSeparating && it.orientation == FoldingFeature.Orientation.VERTICAL }
                ?.let { Hinge(it.bounds.left, it.bounds.right) }
        }
    }
    return hinge
}
