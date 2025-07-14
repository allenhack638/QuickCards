package com.quickcards.app.security

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicLong

class UnifiedTimeoutManager(application: Application) : ViewModel() {
    
    companion object {
        private var instance: UnifiedTimeoutManager? = null
        private const val TIMEOUT_DURATION = 30000L // 30 seconds in milliseconds
        private const val PREFS_NAME = "unified_timeout_prefs"
        private const val KEY_LAST_ACTIVITY_TIME = "last_activity_time"
        private const val KEY_BACKGROUND_START_TIME = "background_start_time"
        private const val KEY_IS_LOCKED = "is_locked"
        
        fun getInstance(application: Application): UnifiedTimeoutManager {
            if (instance == null) {
                instance = UnifiedTimeoutManager(application)
            }
            return instance!!
        }
    }
    
    private val prefs: SharedPreferences = application.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _isLocked = MutableLiveData<Boolean>(false)
    val isLocked: LiveData<Boolean> = _isLocked
    
    private val _remainingTime = MutableLiveData<Long>(TIMEOUT_DURATION)
    val remainingTime: LiveData<Long> = _remainingTime
    
    private var timeoutJob: Job? = null
    private var backgroundStartTime: Long = 0
    private var lastActivityTime: Long = System.currentTimeMillis()
    
    init {
        // Initialize state from preferences
        _isLocked.value = prefs.getBoolean(KEY_IS_LOCKED, false)
        lastActivityTime = prefs.getLong(KEY_LAST_ACTIVITY_TIME, System.currentTimeMillis())
        backgroundStartTime = prefs.getLong(KEY_BACKGROUND_START_TIME, 0)
        
        // Start the timeout monitoring
        startTimeoutMonitoring()
    }
    
    /**
     * Called when user interacts with the app (touch events, etc.)
     * Resets the timeout timer
     */
    fun onUserActivity() {
        if (_isLocked.value == true) return // Don't reset if already locked
        
        lastActivityTime = System.currentTimeMillis()
        prefs.edit().putLong(KEY_LAST_ACTIVITY_TIME, lastActivityTime).apply()
        
        // Restart the timeout monitoring
        restartTimeoutMonitoring()
    }
    
    /**
     * Called when app goes to background
     */
    fun onAppBackgrounded() {
        backgroundStartTime = System.currentTimeMillis()
        prefs.edit().putLong(KEY_BACKGROUND_START_TIME, backgroundStartTime).apply()
        
        // Cancel the current timeout job since we're going to background
        timeoutJob?.cancel()
    }
    
    /**
     * Called when app returns to foreground
     */
    fun onAppForegrounded() {
        if (_isLocked.value == true) return // Don't process if already locked
        
        val currentTime = System.currentTimeMillis()
        val backgroundDuration = if (backgroundStartTime > 0) {
            currentTime - backgroundStartTime
        } else {
            0L
        }
        
        // Add background time to the idle time
        val totalIdleTime = currentTime - lastActivityTime + backgroundDuration
        
        if (totalIdleTime >= TIMEOUT_DURATION) {
            // App was in background too long, lock immediately
            lockApp()
        } else {
            // Resume timeout monitoring with remaining time
            val remainingTime = TIMEOUT_DURATION - totalIdleTime
            startTimeoutMonitoring(remainingTime)
        }
        
        // Reset background start time
        backgroundStartTime = 0
        prefs.edit().putLong(KEY_BACKGROUND_START_TIME, 0).apply()
    }
    
    /**
     * Manually lock the app
     */
    fun lockApp() {
        _isLocked.value = true
        prefs.edit().putBoolean(KEY_IS_LOCKED, true).apply()
        timeoutJob?.cancel()
    }
    
    /**
     * Unlock the app (called after successful authentication)
     */
    fun unlockApp() {
        _isLocked.value = false
        prefs.edit().putBoolean(KEY_IS_LOCKED, false).apply()
        onUserActivity() // Reset the timer
    }
    
    /**
     * Start or restart the timeout monitoring
     */
    private fun startTimeoutMonitoring(initialRemainingTime: Long = TIMEOUT_DURATION) {
        timeoutJob?.cancel()
        
        timeoutJob = viewModelScope.launch {
            var remainingTime = initialRemainingTime
            
            while (remainingTime > 0 && _isLocked.value != true) {
                _remainingTime.value = remainingTime
                delay(1000) // Update every second
                remainingTime -= 1000
            }
            
            if (remainingTime <= 0 && _isLocked.value != true) {
                lockApp()
            }
        }
    }
    
    /**
     * Restart timeout monitoring from the beginning
     */
    private fun restartTimeoutMonitoring() {
        startTimeoutMonitoring(TIMEOUT_DURATION)
    }
    
    /**
     * Get the current timeout duration in milliseconds
     */
    fun getTimeoutDuration(): Long = TIMEOUT_DURATION
    
    /**
     * Check if the app is currently locked
     */
    fun isAppLocked(): Boolean = _isLocked.value ?: false
    
    override fun onCleared() {
        super.onCleared()
        timeoutJob?.cancel()
    }
} 