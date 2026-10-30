package com.quickcards.app

import android.content.Intent
import androidx.compose.runtime.getValue
import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.fillMaxSize
import androidx.lifecycle.lifecycleScope
import com.quickcards.app.core.data.repository.DemoSeeder
import com.quickcards.app.core.ui.base.BaseActivity
import com.quickcards.app.core.security.AppLockController
import com.quickcards.app.core.update.AppUpdates
import com.quickcards.app.core.security.ClipboardHelper
import com.quickcards.app.core.security.InteractionTrackingWindowCallback
import com.quickcards.app.navigation.QcApp
import com.quickcards.app.core.ui.theme.QuickCardsTheme
import com.quickcards.app.core.ui.theme.ThemeMode
import com.quickcards.app.core.ui.theme.ThemeSettings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {

    override fun onSecureCreate(savedInstanceState: Bundle?) {
        AppLockController.attach(this)
        AppUpdates.attach(this)
        installInteractionTracking()
        seedDemoDataIfRequested(intent)

        val themeSettings = ThemeSettings(this)

        setContent {
            val themeMode by themeSettings.themeMode.collectAsState()
            val darkTheme = when (themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            QuickCardsTheme(darkTheme = darkTheme) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    QcApp()
                }
            }
        }
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // A copy timer dies with the process; clear an overdue value once this window can read
        // the clipboard. onResume is too early on Android 10+ (the window has no focus yet).
        if (hasFocus) ClipboardHelper.expireIfNeeded(this)
    }

    private fun installInteractionTracking() {
        val delegate = window.callback
        if (delegate is InteractionTrackingWindowCallback) return
        window.callback = InteractionTrackingWindowCallback(delegate)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        seedDemoDataIfRequested(intent)
    }

    private fun seedDemoDataIfRequested(intent: Intent?) {
        if (!BuildConfig.DEBUG) return
        if (intent?.getBooleanExtra(EXTRA_SEED_DEMO, false) != true) return
        val cardCount = intent.getIntExtra(EXTRA_SEED_CARDS, 0)
        val app = application as QuickCardsApplication
        lifecycleScope.launch(Dispatchers.IO) {
            DemoSeeder.seed(app.database, app.repository, cardCount)
        }
    }

    companion object {
        const val EXTRA_SEED_DEMO = "seed_demo"
        const val EXTRA_SEED_CARDS = "seed_cards"
    }
}
