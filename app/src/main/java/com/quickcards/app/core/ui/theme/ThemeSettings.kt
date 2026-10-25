package com.quickcards.app.core.ui.theme

import android.content.SharedPreferences
import com.quickcards.app.core.data.settings.AppSettings
import com.quickcards.app.core.data.settings.AppTheme
import kotlinx.coroutines.flow.StateFlow

typealias ThemeMode = AppTheme

/**
 * Read-only theme view over [AppSettings]. The preference keys are unchanged
 * ("theme_mode" and "dynamic_color"), so an existing choice is kept.
 */
class ThemeSettings(preferences: SharedPreferences) {

    private val settings = AppSettings.from(preferences)

    val dynamicColor: StateFlow<Boolean> = settings.dynamicColor

    val themeMode: StateFlow<ThemeMode> = settings.theme
}
