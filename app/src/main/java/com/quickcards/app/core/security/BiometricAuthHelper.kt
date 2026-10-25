package com.quickcards.app.core.security

import android.app.KeyguardManager
import android.content.Context
import android.os.Build
import androidx.biometric.BiometricManager

class BiometricAuthHelper(private val context: Context) {

    interface AuthenticationCallback {
        fun onAuthenticationSuccess()
        fun onAuthenticationError(errorCode: Int, errorMessage: String)
        fun onAuthenticationFailed()
    }

    /**
     * `BIOMETRIC_STRONG or DEVICE_CREDENTIAL` is only supported from API 30, so API 26-29
     * asks the keyguard directly (matching `setDeviceCredentialAllowed(true)` there).
     */
    fun isAuthenticationAvailable(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            val keyguard = context.getSystemService(KeyguardManager::class.java)
            return keyguard?.isDeviceSecure == true
        }
        return BiometricManager.from(context).canAuthenticate(
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
        ) == BiometricManager.BIOMETRIC_SUCCESS
    }
}
