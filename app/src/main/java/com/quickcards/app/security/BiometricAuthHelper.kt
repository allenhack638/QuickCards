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
        
        // Biometric error codes
        const val ERROR_HW_UNAVAILABLE = 1
        const val ERROR_UNABLE_TO_PROCESS = 2
        const val ERROR_TIMEOUT = 3
        const val ERROR_LOCKOUT = 4
        const val ERROR_LOCKOUT_PERMANENT = 5
        const val ERROR_USER_CANCELED = 10
        const val ERROR_NO_BIOMETRICS = 11
        const val ERROR_HW_NOT_PRESENT = 12
        const val ERROR_NEGATIVE_BUTTON = 13
        const val ERROR_NO_DEVICE_CREDENTIAL = 14
        const val ERROR_SECURITY_UPDATE_REQUIRED = 15
    }
    
    interface AuthenticationCallback {
        fun onAuthenticationSuccess()
        fun onAuthenticationError(errorCode: Int, errorMessage: String)
        fun onAuthenticationFailed()
    }
    
    /**
     * Get user-friendly error message for biometric error codes
     */
    fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            ERROR_HW_UNAVAILABLE -> "Biometric hardware is currently unavailable. Please try again later."
            ERROR_UNABLE_TO_PROCESS -> "Unable to process biometric authentication. Please try again."
            ERROR_TIMEOUT -> "Authentication timed out. Please try again."
            ERROR_LOCKOUT -> "Too many failed attempts. Please wait before trying again or use your device lock."
            ERROR_LOCKOUT_PERMANENT -> "Biometric authentication is permanently locked. Please use your device lock."
            ERROR_USER_CANCELED -> "Authentication was canceled."
            ERROR_NO_BIOMETRICS -> "No biometric authentication is set up. Please use your device lock."
            ERROR_HW_NOT_PRESENT -> "No biometric hardware is available. Please use your device lock."
            ERROR_NEGATIVE_BUTTON -> "Authentication was canceled."
            ERROR_NO_DEVICE_CREDENTIAL -> "No device lock is set up. Please set up a device lock in Settings."
            ERROR_SECURITY_UPDATE_REQUIRED -> "A security update is required. Please update your device."
            else -> "Authentication failed. Please try again."
        }
    }
    
    /**
     * Check if error code indicates a lockout that requires waiting
     */
    fun isLockoutError(errorCode: Int): Boolean {
        return errorCode == ERROR_LOCKOUT || errorCode == ERROR_LOCKOUT_PERMANENT
    }
    
    /**
     * Check if error code indicates user should use device lock instead
     */
    fun shouldUseDeviceLock(errorCode: Int): Boolean {
        return errorCode == ERROR_LOCKOUT || 
               errorCode == ERROR_LOCKOUT_PERMANENT || 
               errorCode == ERROR_NO_BIOMETRICS || 
               errorCode == ERROR_HW_NOT_PRESENT ||
               errorCode == ERROR_NO_DEVICE_CREDENTIAL
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
        android.util.Log.d("QuickCards", "Starting biometric authentication")
        
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                android.util.Log.e("QuickCards", "Biometric auth error: $errorCode - $errString")
                super.onAuthenticationError(errorCode, errString)
                callback.onAuthenticationError(errorCode, errString.toString())
            }
            
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                android.util.Log.d("QuickCards", "Biometric authentication succeeded")
                super.onAuthenticationSucceeded(result)
                callback.onAuthenticationSuccess()
            }
            
            override fun onAuthenticationFailed() {
                android.util.Log.w("QuickCards", "Biometric authentication failed")
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
        
        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            android.util.Log.e("QuickCards", "Failed to start biometric authentication: ${e.message}")
            callback.onAuthenticationError(-1, "Failed to start biometric authentication: ${e.message}")
        }
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
    
    /**
     * Authenticate using device credentials only (PIN, pattern, password)
     * This is useful as a fallback when biometric authentication is locked out
     */
    fun authenticateWithDeviceCredentials(
        activity: FragmentActivity,
        callback: AuthenticationCallback,
        title: String = "Device Lock Required",
        subtitle: String = "Enter your PIN, pattern, or password",
        description: String = "Use your device lock to continue"
    ) {
        android.util.Log.d("QuickCards", "Attempting device credential authentication")
        
        val executor = ContextCompat.getMainExecutor(context)
        val biometricPrompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                android.util.Log.e("QuickCards", "Device credential auth error: $errorCode - $errString")
                super.onAuthenticationError(errorCode, errString)
                callback.onAuthenticationError(errorCode, errString.toString())
            }
            
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                android.util.Log.d("QuickCards", "Device credential authentication succeeded")
                super.onAuthenticationSucceeded(result)
                callback.onAuthenticationSuccess()
            }
            
            override fun onAuthenticationFailed() {
                android.util.Log.w("QuickCards", "Device credential authentication failed")
                super.onAuthenticationFailed()
                callback.onAuthenticationFailed()
            }
        })
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setDescription(description)
            .setAllowedAuthenticators(BiometricManager.Authenticators.DEVICE_CREDENTIAL)
            .build()
        
        try {
            biometricPrompt.authenticate(promptInfo)
        } catch (e: Exception) {
            android.util.Log.e("QuickCards", "Failed to start device credential authentication: ${e.message}")
            callback.onAuthenticationError(-1, "Failed to start device credential authentication: ${e.message}")
        }
    }
    
    /**
     * Check if device credentials are available
     */
    fun isDeviceCredentialAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return biometricManager.canAuthenticate(BiometricManager.Authenticators.DEVICE_CREDENTIAL) == BiometricManager.BIOMETRIC_SUCCESS
    }
}