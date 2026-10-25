package com.quickcards.app.core.ui.theme

import android.content.SharedPreferences
import com.quickcards.app.core.data.settings.AppSettings
import com.quickcards.app.core.data.settings.AppTheme
import kotlinx.coroutines.flow.StateFlow

typealias ThemeMode = AppTheme

/** Read-only theme view over [AppSettings]; the "theme_mode" key stays for upgrades. */
class ThemeSettings(preferences: SharedPreferences) {

    private val settings = AppSettings.from(preferences)

    val themeMode: StateFlow<ThemeMode> = settings.theme
}
