package com.quickcards.app.flow

import android.os.SystemClock
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isSelectable
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quickcards.app.core.security.AppLockController
import com.quickcards.app.core.security.BiometricAuthHelper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

/** 10 s interval: use keeps the app unlocked, idling locks it (needs an enrolled credential). */
@RunWith(AndroidJUnit4::class)
class IdleLockFlowTest : FlowTestBase() {

    @Test
    fun interactionKeepsTheAppUnlockedPastTheIdleInterval() {
        app.settings.setAppLockAfterSec(10)
        app.settings.setAppLockEnabled(true)
        AppLockController.unlock()

        val startedAt = SystemClock.elapsedRealtime()
        var taps = 0
        while (SystemClock.elapsedRealtime() - startedAt < 12_000) {
            // Compose test input is injected into the view tree, so the window
            // callback does not see it; make the same call it makes for a touch.
            AppLockController.onUserInteraction()
            compose.onNode(hasText("Cards") and isSelectable()).performClick()
            taps++
            Thread.sleep(1_000)
        }

        assertTrue("Expected a tap per second, got $taps", taps >= 10)
        assertFalse(
            "Continuous interaction must not hit the 10 s inactivity lock",
            AppLockController.isLocked.value
        )
    }

    @Test
    fun idleLocksAfterTheIntervalWhenAuthenticationIsAvailable() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        assumeTrue(
            "The test emulator has no enrolled device credential",
            BiometricAuthHelper(context).isAuthenticationAvailable()
        )

        app.settings.setAppLockAfterSec(10)
        app.settings.setAppLockEnabled(true)
        AppLockController.unlock()

        compose.waitUntil(15_000) { AppLockController.isLocked.value }
        assertTrue(AppLockController.isLocked.value)
        AppLockController.unlock()
    }
}
