package com.quickcards.app.core.ui.components

import android.os.Build
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.DialogWindowProvider

/**
 * Dialog, sheet and popup windows are separate windows: the activity's
 * `filterTouchesWhenObscured` and `setHideOverlayWindows` do not reach them. Apply both to the
 * overlay's own window so a confirmation (delete, erase, lock) cannot be tapjacked.
 */
@Composable
internal fun SecureOverlayWindow() {
    val view = LocalView.current
    SideEffect {
        view.filterTouchesWhenObscured = true
        val window = (view.parent as? DialogWindowProvider)?.window ?: return@SideEffect
        window.decorView.filterTouchesWhenObscured = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.setHideOverlayWindows(true)
        }
    }
}
