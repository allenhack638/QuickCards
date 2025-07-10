package com.quickcards.app.security

import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricAuthHelper(private val context: Context) {
    
    companion object {
        const val AUTHENTICATION_TITLE = "Authenticate"
        const val AUTHENTICATION_SUBTITLE = "Use your fingerprint or device lock to access QuickCards"
        const val AUTHENTICATION_DESCRIPTION = "Verify your identity to view your secure card information"
        const val AUTHENTICATION_NEGATIVE_BUTTON = "Cancel"
        
        const val CVV_AUTHENTICATION_TITLE = "View CVV"
        const val CVV_AUTHENTICATION_SUBTITLE = "Authenticate to view card CVV"
        const val CVV_AUTHENTICATION_DESCRIPTION = "Your CVV is sensitive information. Please authenticate to view it."
    }
    
    interface AuthenticationCallback {
        fun onAuthenticationSuccess()
        fun onAuthenticationError(errorCode: Int, errorMessage: String)
        fun onAuthenticationFailed()
    }
    
    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }
    
    /**
     * Check if ANY authentication method is available (biometric OR device credentials)
     * This is more secure than isBiometricAvailable() as it ensures authentication
     * is required even on devices without biometric sensors
     */
    fun isAuthenticationAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE -> {
                // No biometric hardware, but check if device credentials are available
                biometricManager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
            }
            BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE -> {
                // Biometric hardware unavailable, but device credentials might be available
                biometricManager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
            }
            BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED -> {
                // No biometric enrolled, but device credentials might be available
                biometricManager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
            }
            else -> false
        }
    }
    
    fun authenticateUser(
        activity: FragmentActivity,
        callback: AuthenticationCallback,
        title: String = AUTHENTICATION_TITLE,
        subtitle: String = AUTHENTICATION_SUBTITLE,
        description: String = AUTHENTICATION_DESCRIPTION
    ) {
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                super.onAuthenticationError(errorCode, errString)
                callback.onAuthenticationError(errorCode, errString.toString())
            }
            
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                super.onAuthenticationSucceeded(result)
                callback.onAuthenticationSuccess()
            }
            
            override fun onAuthenticationFailed() {
                super.onAuthenticationFailed()
                callback.onAuthenticationFailed()
            }
        })
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG or BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()
        
        biometricPrompt.authenticate(promptInfo)
    }
    
    fun authenticateForCVV(
        activity: FragmentActivity,
        callback: AuthenticationCallback
    ) {
        authenticateUser(
            activity,
            callback,
            CVV_AUTHENTICATION_TITLE,
            CVV_AUTHENTICATION_SUBTITLE,
            CVV_AUTHENTICATION_DESCRIPTION
        )
    }
}