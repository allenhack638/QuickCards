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
import com.quickcards.app. data.database.QuickCardsDatabase
import com.quickcards.app.security.AppLockManager
import com.quickcards.app.security.BiometricAuthHelper
import com.quickcards.app.security.UnifiedTimeoutManager
import com.quickcards.app.ui.base.BaseActivity
import com.quickcards.app.ui.components.LockScreen
import com.quickcards.app.ui.screens.MainScreen
import com.quickcards.app.ui.theme.QuickCardsTheme
import com.quickcards.app.viewmodel.BankViewModel
import kotlinx.coroutines.launch
import android.widget.Toast

class MainActivity : BaseActivity() {
    
    private lateinit var biometricAuthHelper: BiometricAuthHelper
    private lateinit var appLockManager: AppLockManager
    private lateinit var database: QuickCardsDatabase
    private lateinit var timeoutManager: UnifiedTimeoutManager
    private lateinit var bankViewModel: BankViewModel
    private var isAuthenticated = false
    private var isAuthenticationInProgress = false
    private var hasInitialAuthCompleted = false
    private var showLockScreen = true // Always start with lock screen
    
    companion object {
        private const val TAG = "MainActivity"
        private const val DEBUG = false
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        debugLog("onCreate() called")
        
        biometricAuthHelper = BiometricAuthHelper(this)
        appLockManager = AppLockManager.getInstance(this)
        database = QuickCardsDatabase.getDatabase(this, lifecycleScope)
        timeoutManager = UnifiedTimeoutManager.getInstance(application)
        bankViewModel = BankViewModel(application)
        
        // Ensure default banks exist
        bankViewModel.ensureDefaultBanksExist()
        
        // Always start with lock screen - unified UI approach
        showMainContent()
        
        // Perform initial authentication check
        performInitialAuthenticationCheck()
    }
    
    override fun onResume() {
        super.onResume()
        debugLog("onResume() - isAuthenticated: $isAuthenticated, hasInitialAuthCompleted: $hasInitialAuthCompleted, isAuthenticationInProgress: $isAuthenticationInProgress")
        
        // Check if we need to show lock screen (timeout or initial launch)
        if (!isAuthenticated && !isAuthenticationInProgress) {
            showLockScreen = true
            performInitialAuthenticationCheck()
        }
    }
    
    private fun performInitialAuthenticationCheck() {
        debugLog("performInitialAuthenticationCheck() called")
        
        if (biometricAuthHelper.isAuthenticationAvailable()) {
            // Authentication is available - require it
            debugLog("Authentication available - requiring authentication for app access")
            authenticateUser()
        } else {
            // No authentication available - show content but prompt user to set up security
            debugLog("No authentication available - showing content with security recommendation")
            isAuthenticated = true
            hasInitialAuthCompleted = true
            showLockScreen = false
            appLockManager.unlock()
            timeoutManager.unlockApp()
            showMainContent()
            showSecuritySetupRecommendation()
        }
    }
    
    private fun authenticateUser() {
        debugLog("authenticateUser() called - inProgress: $isAuthenticationInProgress")
        
        // Prevent multiple simultaneous authentication attempts
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
                    showLockScreen = false
                    appLockManager.unlock()
                    timeoutManager.unlockApp()
                    showMainContent()
                }
                
                override fun onAuthenticationError(errorCode: Int, errorMessage: String) {
                    debugLog("Authentication ERROR: code=$errorCode, message=$errorMessage")
                    isAuthenticationInProgress = false
                    // Keep showing lock screen for any authentication error
                    showLockScreen = true
                    showMainContent()
                }
                
                override fun onAuthenticationFailed() {
                    debugLog("Authentication FAILED")
                    isAuthenticationInProgress = false
                    // Keep showing lock screen for failed authentication
                    showLockScreen = true
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
                    // Unified lock screen logic
                    if (showLockScreen || !isAuthenticated) {
                        // Show lock screen for initial launch or when locked
                        LockScreen(
                            onUnlock = {
                                timeoutManager.unlockApp()
                                isAuthenticated = true
                                hasInitialAuthCompleted = true
                                showLockScreen = false
                                showMainContent()
                            }
                        )
                    } else {
                        // Show main content when authenticated
                        // Observe the unified timeout manager for idle locking
                        val isLocked by timeoutManager.isLocked.observeAsState(false)
                        
                        if (isLocked) {
                            // Show lock screen when app times out
                            showLockScreen = true
                            isAuthenticated = false
                            LockScreen(
                                onUnlock = {
                                    timeoutManager.unlockApp()
                                    isAuthenticated = true
                                    showLockScreen = false
                                    showMainContent()
                                }
                            )
                        } else {
                            // Show main content when unlocked
                            MainScreen()
                        }
                    }
                }
            }
        }
    }
    
    override fun onPause() {
        super.onPause()
        debugLog("onPause() called")
        // Reset authentication state when app goes to background
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