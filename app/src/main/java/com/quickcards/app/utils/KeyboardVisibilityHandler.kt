package com.quickcards.app.utils

import android.content.Context
import android.view.ViewTreeObserver
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Utility class to handle keyboard visibility
 * Provides composables to detect when the keyboard is open
 */
object KeyboardVisibilityHandler {
    
    /**
     * Composable that provides keyboard visibility state
     * Returns true when keyboard is visible, false when hidden
     */
    @Composable
    fun rememberKeyboardVisibilityState(): State<Boolean> {
        val context = LocalContext.current
        val view = LocalView.current
        
        return remember { mutableStateOf(false) }.also { isKeyboardVisible ->
            DisposableEffect(view) {
                val listener = ViewTreeObserver.OnGlobalLayoutListener {
                    val isVisible = ViewCompat.getRootWindowInsets(view)
                        ?.isVisible(WindowInsetsCompat.Type.ime()) ?: false
                    isKeyboardVisible.value = isVisible
                }
                
                view.viewTreeObserver.addOnGlobalLayoutListener(listener)
                
                onDispose {
                    view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
                }
            }
        }
    }
    
    /**
     * Composable that provides keyboard height
     * Returns the height of the keyboard in pixels, 0 when hidden
     */
    @Composable
    fun rememberKeyboardHeight(): State<Int> {
        val context = LocalContext.current
        val view = LocalView.current
        
        return remember { mutableStateOf(0) }.also { keyboardHeight ->
            DisposableEffect(view) {
                val listener = ViewTreeObserver.OnGlobalLayoutListener {
                    val insets = ViewCompat.getRootWindowInsets(view)
                    val height = insets?.getInsets(WindowInsetsCompat.Type.ime())?.bottom ?: 0
                    keyboardHeight.value = height
                }
                
                view.viewTreeObserver.addOnGlobalLayoutListener(listener)
                
                onDispose {
                    view.viewTreeObserver.removeOnGlobalLayoutListener(listener)
                }
            }
        }
    }
} 