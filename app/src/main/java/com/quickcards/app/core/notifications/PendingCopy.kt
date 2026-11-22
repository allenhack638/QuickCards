package com.quickcards.app.core.notifications

import android.os.SystemClock

/**
 * The card whose copy notification waits for the permission answer. It lives in the process, not
 * in the screen, so a rotation during the system dialog keeps it; it is memory only and expires
 * after [ttlMillis], so an answer that never comes cannot post it later.
 */
internal class PendingCopy<T : Any>(
    private val ttlMillis: Long,
    private val now: () -> Long = SystemClock::elapsedRealtime
) {
    private var held: T? = null
    private var until = 0L

    @Synchronized
    fun hold(value: T?) {
        held = value
        until = now() + ttlMillis
    }

    /** The held value once, or null when nothing is held or it expired. */
    @Synchronized
    fun take(): T? {
        val value = held?.takeIf { now() < until }
        held = null
        return value
    }
}
