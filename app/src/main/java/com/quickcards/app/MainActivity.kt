package com.quickcards.app

import android.content.Intent
import androidx.compose.runtime.getValue
import android.content.res.Configuration
import android.graphics.drawable.ColorDrawable
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
import com.quickcards.app.core.reminders.OpenCardRequest
import com.quickcards.app.navigation.QcApp
import com.quickcards.app.core.ui.theme.QuickCardsTheme
import com.quickcards.app.core.data.settings.AppSettings
import com.quickcards.app.core.data.settings.AppTheme
import com.quickcards.app.core.ui.theme.DarkQcColors
import com.quickcards.app.core.ui.theme.LightQcColors
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class MainActivity : BaseActivity() {

    override fun onSecureCreate(savedInstanceState: Bundle?) {
        AppLockController.attach(this)
        AppUpdates.attach(this)
        installInteractionTracking()
        seedDemoDataIfRequested(intent)
        if (savedInstanceState == null) OpenCardRequest.offer(intent)

        val themeSettings = AppSettings.get(this)
        paintWindowForTheme(themeSettings.theme.value)

        setContent {
            val themeMode by themeSettings.theme.collectAsState()
            val darkTheme = when (themeMode) {
                AppTheme.SYSTEM -> isSystemInDarkTheme()
                AppTheme.LIGHT -> false
                AppTheme.DARK -> true
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

    /**
     * Before Android 12 the window background comes from the system's night mode, not the in-app
     * theme; painting it in the saved theme keeps Dark from showing a light frame before Compose
     * draws. (The system's own starting window still follows the system theme there.)
     */
    private fun paintWindowForTheme(theme: AppTheme) {
        val systemDark = (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val dark = theme == AppTheme.DARK || (theme == AppTheme.SYSTEM && systemDark)
        window.setBackgroundDrawable(ColorDrawable((if (dark) DarkQcColors else LightQcColors).bg.toArgb()))
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
        OpenCardRequest.offer(intent)
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
