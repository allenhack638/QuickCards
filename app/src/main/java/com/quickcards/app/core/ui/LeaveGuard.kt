package com.quickcards.app.core.ui

import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Lets the open Add/Edit form answer before something outside it (a reminder tap) navigates away:
 * a dirty form asks "Discard changes?" and [confirmLeave] returns the answer; with no form open,
 * or a clean one, it is true at once.
 */
class LeaveGuard {
    private var ask: (suspend () -> Boolean)? = null

    fun register(ask: suspend () -> Boolean) {
        this.ask = ask
    }

    fun unregister(ask: suspend () -> Boolean) {
        if (this.ask === ask) this.ask = null
    }

    suspend fun confirmLeave(): Boolean = ask?.invoke() ?: true
}

val LocalLeaveGuard = staticCompositionLocalOf { LeaveGuard() }
