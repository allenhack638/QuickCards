package com.quickcards.app.core.autofill

import android.app.UiModeManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AutofillRowsToneTest {

    @Test
    fun aFixedSystemModeWinsOverTheInAppTheme() {
        assertFalse(AutofillRows.dropdownDark(sdk = 34, systemNightMode = UiModeManager.MODE_NIGHT_NO, appNight = true))
        assertTrue(AutofillRows.dropdownDark(sdk = 34, systemNightMode = UiModeManager.MODE_NIGHT_YES, appNight = false))
    }

    @Test
    fun anAutomaticSystemModeFollowsTheAppConfiguration() {
        assertTrue(AutofillRows.dropdownDark(sdk = 34, systemNightMode = UiModeManager.MODE_NIGHT_AUTO, appNight = true))
        assertFalse(AutofillRows.dropdownDark(sdk = 34, systemNightMode = UiModeManager.MODE_NIGHT_CUSTOM, appNight = false))
    }

    @Test
    fun beforeAndroid12TheAppConfigurationIsTheSystemOne() {
        assertFalse(AutofillRows.dropdownDark(sdk = 30, systemNightMode = UiModeManager.MODE_NIGHT_YES, appNight = false))
    }
}
