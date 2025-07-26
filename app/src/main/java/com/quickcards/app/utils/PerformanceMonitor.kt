package com.quickcards.app.utils

import android.util.Log

/**
 * Performance monitoring utility for tracking app performance
 */
object PerformanceMonitor {
    
    private const val TAG = "PerformanceMonitor"
    private const val SLOW_OPERATION_THRESHOLD = 1000L // 1 second
    
    /**
     * Log operation start time
     */
    fun startOperation(operationName: String): Long {
        val startTime = System.currentTimeMillis()
        Log.d(TAG, "Starting operation: $operationName")
        return startTime
    }
    
    /**
     * Log operation completion time
     */
    fun endOperation(operationName: String, startTime: Long) {
        val duration = System.currentTimeMillis() - startTime
        
        if (duration > SLOW_OPERATION_THRESHOLD) {
            Log.w(TAG, "Slow operation detected: $operationName took ${duration}ms")
        } else {
            Log.d(TAG, "Operation completed: $operationName took ${duration}ms")
        }
    }
    
    /**
     * Log database operation performance
     */
    fun logDatabaseOperation(operationName: String, duration: Long) {
        if (duration > SLOW_OPERATION_THRESHOLD) {
            Log.w(TAG, "Slow database operation: $operationName took ${duration}ms")
        } else {
            Log.d(TAG, "Database operation: $operationName took ${duration}ms")
        }
    }
    
    /**
     * Log encryption/decryption operation performance
     */
    fun logCryptoOperation(operationName: String, duration: Long) {
        if (duration > SLOW_OPERATION_THRESHOLD) {
            Log.w(TAG, "Slow crypto operation: $operationName took ${duration}ms")
        } else {
            Log.d(TAG, "Crypto operation: $operationName took ${duration}ms")
        }
    }
    
    /**
     * Log UI operation performance
     */
    fun logUIOperation(operationName: String, duration: Long) {
        if (duration > SLOW_OPERATION_THRESHOLD) {
            Log.w(TAG, "Slow UI operation: $operationName took ${duration}ms")
        } else {
            Log.d(TAG, "UI operation: $operationName took ${duration}ms")
        }
    }
    
    /**
     * Log memory usage
     */
    fun logMemoryUsage(tag: String = TAG) {
        val runtime = Runtime.getRuntime()
        val usedMemory = runtime.totalMemory() - runtime.freeMemory()
        val maxMemory = runtime.maxMemory()
        val usedPercentage = (usedMemory * 100 / maxMemory).toInt()
        
        Log.d(tag, "Memory usage: ${usedMemory / 1024 / 1024}MB / ${maxMemory / 1024 / 1024}MB ($usedPercentage%)")
    }
    
    /**
     * Monitor cache performance
     */
    fun logCacheStats(cacheName: String, size: Int, hitRate: Double) {
        Log.d(TAG, "Cache stats for $cacheName: size=$size, hitRate=${String.format("%.2f", hitRate)}%")
    }
} 