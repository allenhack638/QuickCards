package com.quickcards.app.core.ui.theme

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import com.quickcards.app.core.data.settings.AppSettings
import com.quickcards.app.core.data.settings.AppTheme
import kotlinx.coroutines.flow.StateFlow

typealias ThemeMode = AppTheme

/** Read-only theme view over [AppSettings]; the "theme_mode" key stays for upgrades. */
class ThemeSettings(context: Context) {

    private val settings = AppSettings.get(context)

    val themeMode: StateFlow<ThemeMode> = settings.theme
}

/**
 * Android 12+ draws the splash before any app code runs, from the app's own night mode; setting
 * it from the in-app theme keeps a Dark choice from opening on a light splash. Older versions
 * draw the system theme's splash and cannot follow it.
 */
fun applyAppNightMode(context: Context, mode: ThemeMode) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val night = when (mode) {
        ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
        ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
        ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
    }
    context.getSystemService(UiModeManager::class.java)?.setApplicationNightMode(night)
}
