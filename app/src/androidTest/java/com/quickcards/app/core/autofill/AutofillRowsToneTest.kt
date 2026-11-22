package com.quickcards.app.core.autofill

import android.content.Context
import android.content.res.Configuration
import android.graphics.Canvas
import android.graphics.Color
import android.os.Build
import android.view.View
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.graphics.createBitmap
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.quickcards.app.R
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The dropdown row is applied by the system in the system theme, so on Android 12+ a forced in-app
 * theme must not decide the mark's tone. Visa is a wordmark: colour on light, white on dark.
 */
@RunWith(AndroidJUnit4::class)
class AutofillRowsToneTest {

    private val base = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun aForcedDarkAppOnALightSystemGetsTheColourMark() {
        assertEquals(expectedWhite(appNight = true, systemNight = false), markIsWhite(appNight = true, systemNight = false))
    }

    @Test
    fun aForcedLightAppOnADarkSystemGetsTheWhiteMark() {
        assertEquals(expectedWhite(appNight = false, systemNight = true), markIsWhite(appNight = false, systemNight = true))
    }

    @Test
    fun theSystemThemeAloneDecidesTheMark() {
        assertEquals(false, markIsWhite(appNight = false, systemNight = false))
        assertEquals(true, markIsWhite(appNight = true, systemNight = true))
    }

    /** Before Android 12 the in-app theme never changes the configuration, so the app's is the system's. */
    private fun expectedWhite(appNight: Boolean, systemNight: Boolean) =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) systemNight else appNight

    private fun markIsWhite(appNight: Boolean, systemNight: Boolean): Boolean {
        val row = AutofillRows.card(night(appNight), "HDFC Bank", "0366", "visa")
        val host = night(systemNight)
        lateinit var view: View
        InstrumentationRegistry.getInstrumentation().runOnMainSync { view = row.apply(host, FrameLayout(host)) }
        val icon = view.findViewById<ImageView>(R.id.autofill_icon).drawable
        val bitmap = createBitmap(icon.intrinsicWidth, icon.intrinsicHeight)
        icon.setBounds(0, 0, bitmap.width, bitmap.height)
        icon.draw(Canvas(bitmap))
        val opaque = IntArray(bitmap.width * bitmap.height).also { bitmap.getPixels(it, 0, bitmap.width, 0, 0, bitmap.width, bitmap.height) }
            .filter { Color.alpha(it) > 200 }
        return opaque.count { Color.red(it) > 240 && Color.green(it) > 240 && Color.blue(it) > 240 } > opaque.size / 2
    }

    private fun night(on: Boolean): Context = base.createConfigurationContext(Configuration(base.resources.configuration).apply {
        uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
            if (on) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
    })
}
