package com.quickcards.domain

/** Inactivity decision from the prototype's 500 ms app-lock interval. */
object AppLockPolicy {

    fun shouldLock(nowMillis: Long, lastActiveMillis: Long, afterSeconds: Int): Boolean {
        if (afterSeconds <= 0) return false
        return nowMillis - lastActiveMillis >= afterSeconds * 1000L
    }
}
