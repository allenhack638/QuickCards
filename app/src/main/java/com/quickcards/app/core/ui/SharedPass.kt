package com.quickcards.app.core.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.SharedTransitionScope.OverlayClip
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import com.quickcards.app.core.ui.theme.QcDimens

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

val LocalNavAnimatedScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

private val passClip = RoundedCornerShape(QcDimens.PassCorner)

/**
 * The card on Home and its preview on Edit share bounds, so opening a card morphs it into
 * the form (and back). A no-op outside a shared-transition host, e.g. in tests and previews.
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun Modifier.sharedPass(cardId: String?): Modifier {
    val shared = LocalSharedTransitionScope.current ?: return this
    val animated = LocalNavAnimatedScope.current ?: return this
    if (cardId.isNullOrEmpty()) return this
    return with(shared) {
        this@sharedPass.sharedBounds(
            sharedContentState = rememberSharedContentState(key = "pass-$cardId"),
            animatedVisibilityScope = animated,
            boundsTransform = { _, _ ->
                spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = Spring.StiffnessMediumLow,
                    visibilityThreshold = Rect.VisibilityThreshold
                )
            },
            clipInOverlayDuringTransition = OverlayClip(passClip)
        )
    }
}
