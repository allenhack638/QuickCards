package com.quickcards.app.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.flow.StateFlow

/**
 * Collects only while the screen is started, so a `WhileSubscribed` flow behind it stops when the
 * app goes to the background, and keeps its last value meanwhile.
 */
@Composable
fun <T> StateFlow<T>.collectWhileStarted(): State<T> {
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val initial = remember(this) { value }
    return produceState(initialValue = initial, this, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            collect { value = it }
        }
    }
}
