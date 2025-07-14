package com.quickcards.app.security

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class CVVVisibilityManager : ViewModel() {
    
    companion object {
        private var instance: CVVVisibilityManager? = null
        
        fun getInstance(): CVVVisibilityManager {
            if (instance == null) {
                instance = CVVVisibilityManager()
            }
            return instance!!
        }
    }
    
    private val _visibleCardId = MutableLiveData<String?>(null)
    val visibleCardId: LiveData<String?> = _visibleCardId
    
    private val _cvvTimer = MutableLiveData<Int>(0)
    val cvvTimer: LiveData<Int> = _cvvTimer
    
    private var hideJob: Job? = null
    private val CVV_VISIBILITY_DURATION = 5000L // 5 seconds
    
    /**
     * Show CVV for a specific card and hide any previously visible CVV
     */
    fun showCVV(cardId: String) {
        // Cancel any existing hide job
        hideJob?.cancel()
        
        // Hide any previously visible CVV
        _visibleCardId.value = null
        _cvvTimer.value = 0
        
        // Show new CVV
        _visibleCardId.value = cardId
        _cvvTimer.value = 5
        
        // Start timer to hide CVV after 5 seconds
        hideJob = viewModelScope.launch {
            repeat(5) {
                delay(1000)
                _cvvTimer.value = _cvvTimer.value?.minus(1) ?: 0
            }
            hideCVV()
        }
    }
    
    /**
     * Hide CVV manually
     */
    fun hideCVV() {
        hideJob?.cancel()
        _visibleCardId.value = null
        _cvvTimer.value = 0
    }
    
    /**
     * Check if a specific card's CVV is currently visible
     */
    fun isCVVVisible(cardId: String): Boolean {
        return _visibleCardId.value == cardId
    }
    
    /**
     * Get the remaining time for CVV visibility
     */
    fun getRemainingTime(): Int {
        return _cvvTimer.value ?: 0
    }
    
    /**
     * Reset the timer for the currently visible CVV
     */
    fun resetTimer() {
        if (_visibleCardId.value != null) {
            showCVV(_visibleCardId.value!!)
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        hideJob?.cancel()
    }
} 