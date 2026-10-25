package com.quickcards.domain

/**
 * The instant-copy window counts idle time only: every interaction restarts
 * it, so active use never needs another fingerprint.
 */
object CopyWindowPolicy {

    fun windowMillis(seconds: Int): Long = if (seconds <= 0) 0L else seconds * 1000L

    fun isOpen(nowMillis: Long, unlockedUntilMillis: Long): Boolean =
        unlockedUntilMillis > nowMillis

    fun expiresAt(nowMillis: Long, windowSeconds: Int): Long =
        nowMillis + windowMillis(windowSeconds)

    fun extend(nowMillis: Long, unlockedUntilMillis: Long, windowSeconds: Int): Long {
        if (!isOpen(nowMillis, unlockedUntilMillis)) return unlockedUntilMillis
        return maxOf(unlockedUntilMillis, expiresAt(nowMillis, windowSeconds))
    }
}
