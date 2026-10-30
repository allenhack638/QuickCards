package com.quickcards.app.core.security

import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.quickcards.app.core.data.settings.AppSettings
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
 * Locks on launch, after inactivity, on leaving the foreground and on Lock now. Uses
 * SystemClock so the timer ignores the animation scale.
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
    private var systemPickerUntil = 0L

    /**
     * Instrumented tests drive the lock directly; the prompt would cover the app. The only
     * production read is guarded by BuildConfig.DEBUG, so release builds always auto-prompt.
     */
    @Volatile
    var autoPromptOnLock = true

    private const val SYSTEM_PICKER_GRACE_MS = 5 * 60_000L
    private const val PLAY_UPDATE_GRACE_MS = 60_000L

    fun attach(context: Context) {
        if (started) return
        started = true
        val app = context.applicationContext
        appContext = app
        val prefs = AppSettings.get(app)
        settings = prefs
        lastActiveMillis = SystemClock.elapsedRealtime()

        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) {
                if (systemPickerUntil != 0L) {
                    systemPickerUntil = 0L
                    lastActiveMillis = SystemClock.elapsedRealtime()
                }
                evaluateIdle()
            }

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

    /**
     * Call right before opening the system file picker so "Lock when you leave" does not
     * fire and drop the import/export. Leaving any other way still locks.
     */
    fun beginSystemPicker() {
        systemPickerUntil = SystemClock.elapsedRealtime() + SYSTEM_PICKER_GRACE_MS
    }

    /**
     * Call right before Google Play's update sheet opens over the app. The sheet is a short
     * Update / No thanks choice, so the grace is one minute, not the picker's five.
     */
    fun beginPlayUpdateSheet() {
        systemPickerUntil = SystemClock.elapsedRealtime() + PLAY_UPDATE_GRACE_MS
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
        // The copy notification keeps its own timeout: the user is leaving to paste.
        if (SystemClock.elapsedRealtime() < systemPickerUntil) return
        systemPickerUntil = 0L
        val prefs = settings ?: return
        if (prefs.appLockEnabled.value && prefs.lockOnLeave.value) {
            lock()
        }
    }

    private fun showLock() {
        UnlockSession.lockNow()
        // The copy notification keeps its own timeout: the user is leaving to paste.
        _isLocked.value = true
    }

    private fun canAuthenticate(context: Context): Boolean =
        BiometricAuthHelper(context).isAuthenticationAvailable()
}
