package com.quickcards.app.core.ui

import com.quickcards.app.core.security.AppLockController
import android.annotation.SuppressLint
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput

/** Dialog windows bypass the activity's Window.Callback, so overlays report touches themselves. */
@SuppressLint("ReturnFromAwaitPointerEventScope")
fun Modifier.trackAppLockInteraction(): Modifier = pointerInput(Unit) {
    awaitPointerEventScope {
        while (true) {
            awaitPointerEvent(PointerEventPass.Initial)
            AppLockController.onUserInteraction()
        }
    }
}
