package com.quickcards.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.withResumed
import com.quickcards.app.BuildConfig
import com.quickcards.app.core.security.AppLockController
import com.quickcards.app.core.security.UnlockSession
import com.quickcards.app.feature.lock.LockScreen
import kotlinx.coroutines.launch

/**
 * The lock screen over the whole app. It asks once per lock, as soon as the app is in the
 * foreground; a cancel leaves the lock screen with its Unlock button rather than re-prompting.
 */
@Composable
internal fun AppLockHost(locked: Boolean) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    var autoPrompted by remember { mutableStateOf(false) }

    fun promptUnlock() {
        val host = context as? FragmentActivity ?: return
        scope.launch {
            if (UnlockSession.requireUnlock(host)) AppLockController.unlock()
        }
    }

    LaunchedEffect(locked) {
        if (!locked) {
            autoPrompted = false
            return@LaunchedEffect
        }
        focusManager.clearFocus(force = true)
        if (autoPrompted) return@LaunchedEffect
        if (!AppLockController.autoPromptOnLock && BuildConfig.DEBUG) return@LaunchedEffect
        lifecycleOwner.lifecycle.withResumed { }
        autoPrompted = true
        promptUnlock()
    }
    if (locked) LockScreen(onUnlock = ::promptUnlock)
}
