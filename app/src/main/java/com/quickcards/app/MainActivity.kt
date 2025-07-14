package com.quickcards.app

import android.os.Bundle
import android.util.Log
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.quickcards.app.data.database.QuickCardsDatabase
import com.quickcards.app.security.AppLockManager
import com.quickcards.app.security.BiometricAuthHelper
import com.quickcards.app.security.UnifiedTimeoutManager
import com.quickcards.app.ui.base.BaseActivity
import com.quickcards.app.ui.components.LockScreen
import com.quickcards.app.ui.screens.MainScreen
import com.quickcards.app.ui.theme.QuickCardsTheme
import kotlinx.coroutines.launch
import android.widget.Toast

class MainActivity : BaseActivity() {
    
    private lateinit var biometricAuthHelper: BiometricAuthHelper
    private lateinit var appLockManager: AppLockManager
    private lateinit var database: QuickCardsDatabase
    private lateinit var timeoutManager: UnifiedTimeoutManager
    private var isAuthenticated = false
    private var isAuthenticationInProgress = false // ✅ Prevent multiple auth attempts
    private var hasInitialAuthCompleted = false // ✅ Track initial authentication
    private var showInitialLockScreen = false // NEW: Track if LockScreen should be shown on denied auth
    
    companion object {
        private const val TAG = "MainActivity"
        private const val DEBUG = false // ✅ Disabled for better performance
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        debugLog("onCreate() called")
        
        biometricAuthHelper = BiometricAuthHelper(this)
        appLockManager = AppLockManager.getInstance(this)
        database = QuickCardsDatabase.getDatabase(this, lifecycleScope)
        timeoutManager = UnifiedTimeoutManager.getInstance(application)
        
        // ✅ Fixed: Remove problematic lock callback that caused the loop
        // The callback should only handle UI updates, not trigger new auth checks
        
        performInitialAuthenticationCheck()
    }
    
    override fun onResume() {
        super.onResume()
        debugLog("onResume() - isAuthenticated: $isAuthenticated, hasInitialAuthCompleted: $hasInitialAuthCompleted, isAuthenticationInProgress: $isAuthenticationInProgress")
        
        // The unified timeout manager handles background/foreground transitions
        // We only need to check the initial authentication
        if (!hasInitialAuthCompleted && !isAuthenticationInProgress && !showInitialLockScreen) {
            performInitialAuthenticationCheck()
        }
    }
    
    private fun performInitialAuthenticationCheck() {
        debugLog("performInitialAuthenticationCheck() called")
        
        if (biometricAuthHelper.isAuthenticationAvailable()) {
            // Authentication is available - ALWAYS require it regardless of card count
            debugLog("Authentication available - requiring authentication for app access")
            authenticateUser()
        } else {
            // No authentication available - show content but prompt user to set up security
            debugLog("No authentication available - showing content with security recommendation")
            isAuthenticated = true
            hasInitialAuthCompleted = true
            appLockManager.unlock()
            // Unlock the unified timeout manager as well
            timeoutManager.unlockApp()
            showMainContent()
            showSecuritySetupRecommendation()
        }
    }
    
    private fun authenticateUser() {
        debugLog("authenticateUser() called - inProgress: $isAuthenticationInProgress")
        
        // ✅ Prevent multiple simultaneous authentication attempts
        if (isAuthenticationInProgress) {
            debugLog("Authentication already in progress, skipping")
            return
        }
        
        isAuthenticationInProgress = true
        
        biometricAuthHelper.authenticateUser(
            this,
            object : BiometricAuthHelper.AuthenticationCallback {
                override fun onAuthenticationSuccess() {
                    debugLog("Authentication SUCCESS")
                    isAuthenticated = true
                    isAuthenticationInProgress = false
                    hasInitialAuthCompleted = true
                    appLockManager.unlock()
                    // Unlock the unified timeout manager as well
                    timeoutManager.unlockApp()
                    showInitialLockScreen = false // Reset flag
                    showMainContent()
                }
                
                override fun onAuthenticationError(errorCode: Int, errorMessage: String) {
                    debugLog("Authentication ERROR: code=$errorCode, message=$errorMessage")
                    isAuthenticationInProgress = false
                    // If user cancels, show LockScreen instead of finishing
                    if (errorCode == 13) { // User cancelled
                        debugLog("User cancelled authentication, showing LockScreen")
                        showInitialLockScreen = true
                        showMainContent()
                    } else {
                        // For other errors, try again after a delay
                        debugLog("Authentication error, retrying after delay")
                        lifecycleScope.launch {
                            kotlinx.coroutines.delay(1000)
                            if (!isAuthenticated && !showInitialLockScreen) {
                                authenticateUser()
                            }
                        }
                    }
                }
                
                override fun onAuthenticationFailed() {
                    debugLog("Authentication FAILED")
                    isAuthenticationInProgress = false
                    // Show LockScreen for failed authentication
                    showInitialLockScreen = true
                    showMainContent()
                }
            }
        )
    }
    
    private fun showSecuritySetupRecommendation() {
        Toast.makeText(
            this, 
            "For better security, consider setting up device authentication (fingerprint, PIN, or pattern) in your device Settings",
            Toast.LENGTH_LONG
        ).show()
    }
    
    private fun showMainContent() {
        debugLog("showMainContent() called")
        setContent {
            QuickCardsTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    // Show LockScreen if initial authentication was denied
                    if (showInitialLockScreen) {
                        LockScreen(
                            onUnlock = {
                                timeoutManager.unlockApp()
                                isAuthenticated = true
                                hasInitialAuthCompleted = true
                                showInitialLockScreen = false
                                showMainContent()
                            }
                        )
                    } else if (hasInitialAuthCompleted) {
                        // Observe the unified timeout manager
                        val isLocked by timeoutManager.isLocked.observeAsState(false)
                        
                        if (isLocked) {
                            // Show lock screen when app is locked
                            LockScreen(
                                onUnlock = {
                                    timeoutManager.unlockApp()
                                    isAuthenticated = true
                                }
                            )
                        } else {
                            // Show main content when unlocked
                            MainScreen()
                        }
                    } else {
                        // Show main content during initial authentication
                        MainScreen()
                    }
                }
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        debugLog("onPause() called")
        // ✅ Reset authentication state when app goes to background
        // The AppLockManager will handle timing
    }
    
    override fun onDestroy() {
        super.onDestroy()
        debugLog("onDestroy() called")
        isAuthenticationInProgress = false
    }
    
    private fun debugLog(message: String) {
        if (DEBUG) {
            Log.d(TAG, message)
        }
    }
}