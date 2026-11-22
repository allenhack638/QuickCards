package com.quickcards.app.core.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarData
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalAccessibilityManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import com.quickcards.app.R
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcMotion
import com.quickcards.app.core.ui.theme.QcShapes
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class SnackbarController(
    internal val host: SnackbarHostState,
    private val scope: CoroutineScope
) {
    fun show(message: String, long: Boolean = false) {
        scope.launch {
            host.showSnackbar(
                message = message,
                duration = if (long) SnackbarDuration.Long else SnackbarDuration.Short
            )
        }
    }

    suspend fun showAndAwait(
        message: String,
        actionLabel: String? = null,
        withDismissAction: Boolean = false,
        duration: SnackbarDuration = SnackbarDuration.Short
    ): SnackbarResult = host.showSnackbar(
        message = message,
        actionLabel = actionLabel,
        withDismissAction = withDismissAction,
        duration = duration
    )
}

val LocalSnackbar = staticCompositionLocalOf<SnackbarController> {
    error("SnackbarController is not provided")
}

/**
 * The app's snackbar host: M3 snackbar on `inverse`, one line, no icon, an optional
 * `inverse-accent` action; full width − 16 each side, 8 above whatever [modifier] places it on.
 * Short snackbars stay 3 s (4.5 s with TalkBack); one with an action (Undo, Clear) always 4.5 s.
 */
@Composable
fun QcSnackbarHost(hostState: SnackbarHostState, modifier: Modifier = Modifier) {
    val accessibility = LocalAccessibilityManager.current
    val current = hostState.currentSnackbarData
    LaunchedEffect(current) {
        if (current == null || current.visuals.duration != SnackbarDuration.Short) return@LaunchedEffect
        // An Undo or Clear must be reachable: action snackbars always get the TalkBack-length 4.5 s.
        val hasAction = current.visuals.actionLabel != null
        val base = if (hasAction) QcMotion.TOAST_A11Y_MS else QcMotion.TOAST_MS
        delay(
            accessibility?.calculateRecommendedTimeoutMillis(
                base,
                containsIcons = false,
                containsText = true,
                containsControls = hasAction
            )?.coerceIn(base, QcMotion.TOAST_A11Y_MS) ?: base
        )
        current.dismiss()
    }
    SnackbarHost(hostState = hostState, modifier = modifier) { data -> QcSnackbar(data) }
}

@Composable
private fun QcSnackbar(data: SnackbarData) {
    val colors = QcTheme.colors
    val action = data.visuals.actionLabel
    Snackbar(
        modifier = Modifier
            .padding(start = QcSpacing.s4, end = QcSpacing.s4, bottom = QcSpacing.s2)
            .semantics { liveRegion = LiveRegionMode.Polite },
        shape = QcShapes.md,
        containerColor = colors.inverse,
        contentColor = colors.onInverse,
        actionContentColor = colors.inverseAccent,
        action = action?.let {
            {
                TextButton(onClick = { data.performAction() }) {
                    Text(text = it, style = QcText.F2Semibold, color = colors.inverseAccent)
                }
            }
        },
        dismissAction = if (data.visuals.withDismissAction) {
            {
                IconButton(onClick = { data.dismiss() }) {
                    Icon(
                        imageVector = QcIcons.close,
                        contentDescription = stringResource(R.string.common_dismiss),
                        tint = colors.onInverse
                    )
                }
            }
        } else {
            null
        }
    ) {
        Text(text = data.visuals.message, style = QcText.F2Medium, maxLines = 2)
    }
}
