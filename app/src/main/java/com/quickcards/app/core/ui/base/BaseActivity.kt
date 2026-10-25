package com.quickcards.app.core.ui.base

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.fragment.app.FragmentActivity
import com.quickcards.app.BuildConfig

abstract class BaseActivity : FragmentActivity() {

    final override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applySecureWindow()
        onSecureCreate(savedInstanceState)
    }

    protected open fun onSecureCreate(savedInstanceState: Bundle?) {}

    final override fun onStart() {
        super.onStart()
        applySecureWindow()
    }

    final override fun onResume() {
        super.onResume()
        applySecureWindow()
    }

    private fun applySecureWindow() {
        // Tapjacking: drop touches while another app draws over us, and on
        // Android 12+ hide non-system overlays outright.
        window.decorView.filterTouchesWhenObscured = true
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.setHideOverlayWindows(true)
        }
        if (!BuildConfig.SECURE_WINDOW) return

        window.setFlags(
            WindowManager.LayoutParams.FLAG_SECURE,
            WindowManager.LayoutParams.FLAG_SECURE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            setRecentsScreenshotEnabled(false)
        }
    }
}
