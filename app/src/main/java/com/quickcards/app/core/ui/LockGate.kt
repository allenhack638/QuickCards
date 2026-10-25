package com.quickcards.app.core.ui

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * True while [LockScreen] covers the app. Every Dialog, bottom sheet and
 * dropdown checks it so no overlay window can outlive the lock.
 */
val LocalAppLocked = staticCompositionLocalOf { false }

object LockGate {

    /** An overlay is visible only while it is open and the app is unlocked. */
    fun visible(open: Boolean, locked: Boolean): Boolean = open && !locked
}
