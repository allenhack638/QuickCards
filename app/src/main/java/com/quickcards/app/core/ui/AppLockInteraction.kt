package com.quickcards.app.core.ui

import com.quickcards.app.core.security.AppLockController
import android.annotation.SuppressLint
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/**
 * Compose Dialog windows do not route input through the activity's
 * Window.Callback, so overlay content reports its own touches to the
 * inactivity timer.
 */
@SuppressLint("ReturnFromAwaitPointerEventScope")
fun Modifier.trackAppLockInteraction(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Initial)
            AppLockController.onUserInteraction()
        }
    }
}
