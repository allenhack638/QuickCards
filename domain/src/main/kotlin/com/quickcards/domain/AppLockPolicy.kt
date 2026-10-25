package com.quickcards.domain

/** Inactivity lock decision; 0 or less never locks. */
object AppLockPolicy {

    fun shouldLock(nowMillis: Long, lastActiveMillis: Long, afterSeconds: Int): Boolean {
        if (afterSeconds <= 0) return false
        return nowMillis - lastActiveMillis >= afterSeconds * 1000L
    }
}
