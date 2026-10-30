package com.quickcards.app.core.update

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarResult
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.quickcards.app.R
import com.quickcards.app.core.ui.components.SnackbarController
import kotlinx.coroutines.delay

private const val CHECK_AFTER_UNLOCK_MS = 1_500L

/** Checks on unlock and on every resume; offers Restart once Play has downloaded an update. */
@Composable
fun AppUpdatePrompts(
    locked: Boolean,
    launcher: ActivityResultLauncher<IntentSenderRequest>,
    snackbar: SnackbarController
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() } ?: return
    val lifecycleOwner = LocalLifecycleOwner.current
    val unlocked by rememberUpdatedState(!locked)

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME && unlocked) AppUpdates.check(activity, launcher)
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(locked) {
        if (locked) return@LaunchedEffect
        // Let the vault settle after the fingerprint before Play's sheet can appear.
        delay(CHECK_AFTER_UNLOCK_MS)
        AppUpdates.check(activity, launcher)
    }

    val state by AppUpdates.state.collectAsState()
    val message = stringResource(R.string.settings_update_snackbar)
    val action = stringResource(R.string.settings_update_restart)
    LaunchedEffect(state, locked) {
        if (state != AppUpdates.State.Downloaded || locked) return@LaunchedEffect
        val result = snackbar.showAndAwait(
            message = message,
            actionLabel = action,
            withDismissAction = true,
            duration = SnackbarDuration.Indefinite
        )
        if (result == SnackbarResult.ActionPerformed) AppUpdates.completeUpdate()
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
