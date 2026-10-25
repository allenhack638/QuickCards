package com.quickcards.app.core.security

import android.os.Build
import androidx.annotation.VisibleForTesting
import androidx.annotation.StringRes
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.quickcards.app.BuildConfig
import com.quickcards.app.R
import com.quickcards.app.core.data.settings.AppSettings
import com.quickcards.domain.CopyWindowPolicy
import com.quickcards.domain.TimeText
import kotlin.coroutines.resume
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** One authentication unlocks every copy for AppSettings.copyWindowSec seconds. */
object UnlockSession {

    @StringRes
    val TITLE = R.string.common_unlock_title

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val mutex = Mutex()

    @Volatile
    var unlockedUntil: Long = 0L
        private set

    @Volatile
    private var windowSeconds: Int = 0

    private val _isUnlocked = MutableStateFlow(false)
    val isUnlocked: StateFlow<Boolean> = _isUnlocked.asStateFlow()

    /** Stands in for the system prompt in instrumented tests. Ignored in release builds. */
    @VisibleForTesting
    @Volatile
    var testAuthenticator: (suspend () -> Boolean)? = null

    init {
        scope.launch {
            while (isActive) {
                delay(250)
                refresh()
            }
        }
    }

    fun lockNow() {
        unlockedUntil = 0L
        windowSeconds = 0
        _isUnlocked.value = false
    }

    fun unlockFor(seconds: Int) {
        if (seconds <= 0) {
            lockNow()
            return
        }
        windowSeconds = seconds
        unlockedUntil = CopyWindowPolicy.expiresAt(System.currentTimeMillis(), seconds)
        refresh()
    }

    fun onUserInteraction() {
        val now = System.currentTimeMillis()
        val currentUntil = unlockedUntil
        val seconds = windowSeconds
        if (seconds <= 0) return
        if (!CopyWindowPolicy.isOpen(now, currentUntil)) return
        val extended = CopyWindowPolicy.extend(now, currentUntil, seconds)
        if (extended != currentUntil) {
            unlockedUntil = extended
            refresh()
        }
    }

    /**
     * Instant inside the copy window unless [fresh]: export, erase, import and lowering a
     * protection always re-ask, so an unattended phone cannot be emptied.
     */
    suspend fun requireUnlock(
        activity: FragmentActivity,
        reason: String? = null,
        fresh: Boolean = false
    ): Boolean {
        if (!fresh && isWindowOpen()) return true
        return mutex.withLock {
            if (!fresh && isWindowOpen()) return@withLock true
            val seconds = AppSettings.get(activity).copyWindowSec.value
            testAuthenticator?.takeIf { BuildConfig.DEBUG }?.let { stand ->
                val granted = stand()
                if (granted) unlockFor(seconds)
                return@withLock granted
            }
            val helper = BiometricAuthHelper(activity)
            if (!helper.isAuthenticationAvailable()) return@withLock false
            val subtitle = reason ?: activity.getString(R.string.unlock_window_subtitle, TimeText.seconds(seconds))
            val granted = suspendCancellableCoroutine { continuation ->
                var finished = false
                fun finish(result: Boolean) {
                    if (finished) return
                    finished = true
                    if (continuation.isActive) continuation.resume(result)
                }
                val callback = object : BiometricAuthHelper.AuthenticationCallback {
                    override fun onAuthenticationSuccess() = finish(true)
                    override fun onAuthenticationError(errorCode: Int, errorMessage: String) = finish(false)
                    override fun onAuthenticationFailed() {
                        // The system prompt stays open for a retry.
                    }
                }
                try {
                    helper.authenticateForUnlock(activity, subtitle, callback)
                } catch (_: Exception) {
                    finish(false)
                }
            }
            if (granted) unlockFor(seconds)
            granted
        }
    }

    private fun isWindowOpen(): Boolean = unlockedUntil > System.currentTimeMillis()

    private fun refresh() {
        val open = CopyWindowPolicy.isOpen(System.currentTimeMillis(), unlockedUntil)
        if (_isUnlocked.value != open) _isUnlocked.value = open
    }
}

/** BiometricPrompt with device-credential fallback; the combined form needs API 30. */
fun BiometricAuthHelper.authenticateForUnlock(
    activity: FragmentActivity,
    subtitle: String,
    callback: BiometricAuthHelper.AuthenticationCallback
) {
    val executor = ContextCompat.getMainExecutor(activity)
    val prompt = BiometricPrompt(
        activity,
        executor,
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                callback.onAuthenticationError(errorCode, errString.toString())
            }

            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                callback.onAuthenticationSuccess()
            }

            override fun onAuthenticationFailed() {
                callback.onAuthenticationFailed()
            }
        }
    )
    val builder = BiometricPrompt.PromptInfo.Builder()
        .setTitle(activity.getString(UnlockSession.TITLE))
        .setSubtitle(subtitle)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        builder.setAllowedAuthenticators(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        )
    } else {
        @Suppress("DEPRECATION")
        builder.setDeviceCredentialAllowed(true)
    }
    prompt.authenticate(builder.build())
}
