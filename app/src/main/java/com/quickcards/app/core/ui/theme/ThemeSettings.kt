package com.quickcards.app.core.ui.theme

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import com.quickcards.app.core.data.settings.AppTheme

/**
 * Android 12+ draws the splash before any app code runs, from the app's own night mode; setting
 * it from the in-app theme keeps a Dark choice from opening on a light splash. Older versions
 * draw the system theme's splash and cannot follow it.
 */
fun applyAppNightMode(context: Context, mode: AppTheme) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val night = when (mode) {
        AppTheme.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
        AppTheme.LIGHT -> UiModeManager.MODE_NIGHT_NO
        AppTheme.DARK -> UiModeManager.MODE_NIGHT_YES
    }
    context.getSystemService(UiModeManager::class.java)?.setApplicationNightMode(night)
}
