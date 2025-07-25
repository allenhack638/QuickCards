package com.quickcards.app.security

import android.content.Context
import androidx.fragment.app.FragmentActivity
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class CardOperationAuthManager private constructor(private val context: Context) {
    
    companion object {
        @Volatile
        private var INSTANCE: CardOperationAuthManager? = null
        
        fun getInstance(context: Context): CardOperationAuthManager {
            return INSTANCE ?: synchronized(this) {
                val instance = CardOperationAuthManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
    
    private val biometricAuthHelper = BiometricAuthHelper(context)
    
    /**
     * Authenticate user before performing card operations (edit/delete)
     * Returns true if authentication was successful, false otherwise
     */
    suspend fun authenticateForCardOperation(
        activity: FragmentActivity,
        operation: String = "card operation"
    ): Boolean {
        return suspendCancellableCoroutine { continuation ->
            if (!biometricAuthHelper.isAuthenticationAvailable()) {
                // If no authentication is available, allow the operation
                continuation.resume(true)
                return@suspendCancellableCoroutine
            }
            
            biometricAuthHelper.authenticateUser(
                activity = activity,
                title = "Authenticate for $operation",
                subtitle = "Verify your identity to $operation",
                description = "Use your fingerprint, face, or device lock to continue",
                callback = object : BiometricAuthHelper.AuthenticationCallback {
                    override fun onAuthenticationSuccess() {
                        continuation.resume(true)
                    }
                    
                    override fun onAuthenticationError(errorCode: Int, errorMessage: String) {
                        continuation.resume(false)
                    }
                    
                    override fun onAuthenticationFailed() {
                        continuation.resume(false)
                    }
                }
            )
        }
    }
    
    /**
     * Authenticate user before editing a card
     */
    suspend fun authenticateForCardEdit(activity: FragmentActivity): Boolean {
        return authenticateForCardOperation(activity, "edit card")
    }
    
    /**
     * Authenticate user before deleting a card
     */
    suspend fun authenticateForCardDelete(activity: FragmentActivity): Boolean {
        return authenticateForCardOperation(activity, "delete card")
    }
}