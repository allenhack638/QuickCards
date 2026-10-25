package com.quickcards.app.core.security

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.quickcards.app.core.data.settings.AppSettings
import com.quickcards.app.core.notifications.CopyCompanionNotifier
import com.quickcards.app.core.security.BiometricAuthHelper
import com.quickcards.app.core.security.UnlockSession
import com.quickcards.domain.AppLockPolicy
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * App lock from prototype/v2.html: shown on launch, after the configured
 * inactivity time, when the app leaves the foreground, and from Lock now.
 * Elapsed time comes from SystemClock so the timer is unaffected by the
 * animation scale.
 */
object AppLockController {

    private const val POLL_INTERVAL_MS = 500L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val _isLocked = MutableStateFlow(false)
    val isLocked: StateFlow<Boolean> = _isLocked.asStateFlow()

    private var appContext: Context? = null
    private var settings: AppSettings? = null
    private var lastActiveMillis = SystemClock.elapsedRealtime()
    private var started = false

    fun attach(context: Context) {
        if (started) return
        started = true
        val app = context.applicationContext
        appContext = app
        val prefs = AppSettings.get(app)
        settings = prefs
        lastActiveMillis = SystemClock.elapsedRealtime()

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = evaluateIdle()

            override fun onStop(owner: LifecycleOwner) = onAppStopped()
        })

        if (prefs.appLockEnabled.value) {
            // Show the lock on the first frame; verify in the background that
            // this device can actually authenticate.
            showLock()
            scope.launch(Dispatchers.IO) {
                if (!canAuthenticate(app)) unlock()
            }
        }

        scope.launch {
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                evaluateIdle()
            }
        }
    }

    fun onUserInteraction() {
        lastActiveMillis = SystemClock.elapsedRealtime()
        UnlockSession.onUserInteraction()
    }

    fun lock() {
        val prefs = settings ?: return
        if (!prefs.appLockEnabled.value) return
        val context = appContext ?: return
        if (!canAuthenticate(context)) return
        showLock()
    }

    fun unlock() {
        _isLocked.value = false
        lastActiveMillis = SystemClock.elapsedRealtime()
    }

    private fun evaluateIdle() {
        val prefs = settings ?: return
        if (_isLocked.value) return
        if (!prefs.appLockEnabled.value) return
        val now = SystemClock.elapsedRealtime()
        if (AppLockPolicy.shouldLock(now, lastActiveMillis, prefs.appLockAfterSec.value)) {
            val context = appContext ?: return
            if (canAuthenticate(context)) {
                showLock()
            } else {
                lastActiveMillis = now
            }
        }
    }

    private fun onAppStopped() {
        UnlockSession.lockNow()
        appContext?.let(CopyCompanionNotifier::cancel)
        val prefs = settings ?: return
        if (prefs.appLockEnabled.value && prefs.lockOnLeave.value) {
            lock()
        }
    }

    private fun showLock() {
        UnlockSession.lockNow()
        appContext?.let(CopyCompanionNotifier::cancel)
        _isLocked.value = true
    }

    private fun canAuthenticate(context: Context): Boolean =
        BiometricAuthHelper(context).isAuthenticationAvailable()
}
