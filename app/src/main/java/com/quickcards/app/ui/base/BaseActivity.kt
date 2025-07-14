package com.quickcards.app.ui.base

import android.os.Bundle
import android.view.MotionEvent
import androidx.fragment.app.FragmentActivity
import com.quickcards.app.security.UnifiedTimeoutManager

/**
 * Base Activity class that implements unified timeout management
 * All activities should extend this class to get automatic timeout handling
 */
abstract class BaseActivity : FragmentActivity() {
    
    private lateinit var timeoutManager: UnifiedTimeoutManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        timeoutManager = UnifiedTimeoutManager.getInstance(application)
    }
    
    override fun onResume() {
        super.onResume()
        timeoutManager.onAppForegrounded()
    }
    
    override fun onPause() {
        super.onPause()
        timeoutManager.onAppBackgrounded()
    }
    
    /**
     * Override dispatchTouchEvent to capture all touch events and reset the timeout
     * This ensures any user interaction resets the 30-second timer
     */
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        // Reset timeout on any touch event
        timeoutManager.onUserActivity()
        
        // Call the parent implementation
        return super.dispatchTouchEvent(ev)
    }
    
    /**
     * Get the timeout manager instance
     */
    protected fun getTimeoutManager(): UnifiedTimeoutManager {
        return timeoutManager
    }
    
    /**
     * Check if the app is currently locked
     */
    protected fun isAppLocked(): Boolean {
        return timeoutManager.isAppLocked()
    }
    
    /**
     * Manually lock the app
     */
    protected fun lockApp() {
        timeoutManager.lockApp()
    }
    
    /**
     * Unlock the app (call after successful authentication)
     */
    protected fun unlockApp() {
        timeoutManager.unlockApp()
    }
} 