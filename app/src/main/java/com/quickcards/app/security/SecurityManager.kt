package com.quickcards.app.security

import android.app.Activity
import android.view.WindowManager
import android.util.Log

/**
 * SecurityManager handles app-wide security features including
 * screenshot and screen recording prevention
 */
class SecurityManager {
    
    companion object {
        private const val TAG = "SecurityManager"
        
        /**
         * Enable screenshot and screen recording prevention
         * This sets FLAG_SECURE which prevents:
         * - Screenshots
         * - Screen recording
         * - Content appearing in recent apps preview
         */
        fun enableScreenshotPrevention(activity: Activity) {
            try {
                activity.window.setFlags(
                    WindowManager.LayoutParams.FLAG_SECURE,
                    WindowManager.LayoutParams.FLAG_SECURE
                )
                Log.d(TAG, "Screenshot and screen recording prevention enabled")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to enable screenshot prevention", e)
            }
        }
        
        /**
         * Disable screenshot and screen recording prevention
         * Note: This should only be used for debugging purposes
         */
        fun disableScreenshotPrevention(activity: Activity) {
            try {
                activity.window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
                Log.d(TAG, "Screenshot and screen recording prevention disabled")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to disable screenshot prevention", e)
            }
        }
        
        /**
         * Check if screenshot prevention is currently enabled
         */
        fun isScreenshotPreventionEnabled(activity: Activity): Boolean {
            return (activity.window.attributes.flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
        }
        
        /**
         * Ensure security flags are maintained when app is resumed
         * This should be called in onResume() to ensure flags weren't cleared
         */
        fun ensureSecurityFlags(activity: Activity) {
            if (!isScreenshotPreventionEnabled(activity)) {
                Log.w(TAG, "Security flags were cleared, re-enabling screenshot prevention")
                enableScreenshotPrevention(activity)
            }
        }
        
        /**
         * Get security status information for debugging
         */
        fun getSecurityStatus(activity: Activity): String {
            val isEnabled = isScreenshotPreventionEnabled(activity)
            return "Screenshot Prevention: ${if (isEnabled) "ENABLED" else "DISABLED"}"
        }
    }
} 