package com.quickcards.app.feature.lock

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quickcards.app.core.ui.theme.QuickCardsTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LockScreenTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun touchingUnlockPromptsButTouchesNeverReachTheAppBelow() {
        var unlocks = 0
        var appTaps = 0
        compose.setContent {
            QuickCardsTheme(darkTheme = true) {
                // Same layering as QcApp: the lock is drawn over the app, not inside it.
                Box(modifier = Modifier.fillMaxSize()) {
                    Box(modifier = Modifier.fillMaxSize().clickable { appTaps++ })
                    LockScreen(onUnlock = { unlocks++ })
                }
            }
        }
        compose.onNodeWithContentDescription("Unlock QuickCards").performTouchInput { click() }
        compose.onRoot().performTouchInput { click(center.copy(y = height * 0.1f)) }
        compose.waitForIdle()
        assertEquals(1, unlocks)
        assertEquals(0, appTaps)
    }
}
