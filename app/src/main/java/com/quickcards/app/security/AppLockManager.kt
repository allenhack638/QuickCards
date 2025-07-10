package com.quickcards.app.security

import android.app.Activity
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class AppLockManager private constructor(private val context: Context) : LifecycleEventObserver {
    
    companion object {
        private const val PREFS_NAME = "app_lock_prefs"
        private const val KEY_LOCK_TIMEOUT = "lock_timeout"
        private const val KEY_LAST_UNLOCK_TIME = "last_unlock_time"
        private const val DEFAULT_LOCK_TIMEOUT = 30000L // 30 seconds
        
        @Volatile
        private var INSTANCE: AppLockManager? = null
        
        fun getInstance(context: Context): AppLockManager {
            return INSTANCE ?: synchronized(this) {
                val instance = AppLockManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private var lockTimeoutJob: Job? = null
    private var isAppLocked = true
    private var lockCallback: (() -> Unit)? = null
    private var isLockCallbackEnabled = true // ✅ Control callback execution
    
    interface LockCallback {
        fun onAppLocked()
        fun onAppUnlocked()
    }
    
    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }
    
    fun setLockCallback(callback: () -> Unit) {
        this.lockCallback = callback
    }
    
    // ✅ Allow disabling callback to prevent loops
    fun setLockCallbackEnabled(enabled: Boolean) {
        isLockCallbackEnabled = enabled
    }
    
    fun setLockTimeout(timeoutMs: Long) {
        prefs.edit().putLong(KEY_LOCK_TIMEOUT, timeoutMs).apply()
    }
    
    fun getLockTimeout(): Long {
        return prefs.getLong(KEY_LOCK_TIMEOUT, DEFAULT_LOCK_TIMEOUT)
    }
    
    fun unlock() {
        isAppLocked = false
        prefs.edit().putLong(KEY_LAST_UNLOCK_TIME, System.currentTimeMillis()).apply()
        startLockTimer()
    }
    
    fun lock() {
        isAppLocked = true
        lockTimeoutJob?.cancel()
        // ✅ Only invoke callback if enabled and not already locked
        if (isLockCallbackEnabled && lockCallback != null) {
            lockCallback?.invoke()
        }
    }
    
    // ✅ Silent lock without callback (for internal use)
    fun lockSilently() {
        isAppLocked = true
        lockTimeoutJob?.cancel()
    }
    
    fun isLocked(): Boolean {
        return isAppLocked
    }
    
    private fun startLockTimer() {
        lockTimeoutJob?.cancel()
        lockTimeoutJob = CoroutineScope(Dispatchers.Main).launch {
            delay(getLockTimeout())
            // ✅ Use silent lock to prevent callback loops
            if (!isAppLocked) {
                lockSilently() // ✅ Changed from lock() to lockSilently()
            }
        }
    }
    
    override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
        when (event) {
            Lifecycle.Event.ON_STOP -> onAppBackgrounded()
            Lifecycle.Event.ON_START -> onAppForegrounded()
            else -> { /* Do nothing for other events */ }
        }
    }
    
    private fun onAppBackgrounded() {
        // App went to background, start lock timer
        if (!isAppLocked) {
            startLockTimer()
        }
    }
    
    private fun onAppForegrounded() {
        // App came to foreground, check if enough time has passed
        val lastUnlockTime = prefs.getLong(KEY_LAST_UNLOCK_TIME, 0L)
        val currentTime = System.currentTimeMillis()
        val timeSinceUnlock = currentTime - lastUnlockTime
        
        if (timeSinceUnlock > getLockTimeout()) {
            // ✅ Use silent lock to prevent unnecessary callbacks on foreground
            lockSilently()
        } else {
            // Reset timer since we're back in foreground
            lockTimeoutJob?.cancel()
        }
    }
    
    fun shouldRequireAuthentication(): Boolean {
        val lastUnlockTime = prefs.getLong(KEY_LAST_UNLOCK_TIME, 0L)
        val currentTime = System.currentTimeMillis()
        val timeSinceUnlock = currentTime - lastUnlockTime
        
        return isAppLocked || timeSinceUnlock > getLockTimeout()
    }
    
    // ✅ Helper method to reset state on app restart
    fun resetState() {
        lockTimeoutJob?.cancel()
        isAppLocked = true
        isLockCallbackEnabled = true
    }
}