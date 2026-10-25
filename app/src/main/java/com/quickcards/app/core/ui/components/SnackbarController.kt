package com.quickcards.app.core.ui.components

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.staticCompositionLocalOf
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class SnackbarController(
    private val host: SnackbarHostState,
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
