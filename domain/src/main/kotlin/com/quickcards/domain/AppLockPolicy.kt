package com.quickcards.domain

/** Inactivity lock decision; 0 or less never locks. */
object AppLockPolicy {

    fun shouldLock(nowMillis: Long, lastActiveMillis: Long, afterSeconds: Int): Boolean {
        if (afterSeconds <= 0) return false
        return nowMillis - lastActiveMillis >= afterSeconds * 1000L
    }

    /**
     * Decides on coming back to the foreground. [pickerUntilMillis] is the end of a system
     * picker grace (0 when none). An expired grace counts as an ordinary leave, and the
     * inactivity timer is always checked against the last touch before leaving, because the
     * process may have been frozen and the idle poll never ran.
     */
    fun shouldLockOnReturn(
        nowMillis: Long,
        pickerUntilMillis: Long,
        lastActiveMillis: Long,
        afterSeconds: Int,
        lockOnLeave: Boolean
    ): Boolean {
        if (pickerUntilMillis != 0L && nowMillis >= pickerUntilMillis && lockOnLeave) return true
        return shouldLock(nowMillis, lastActiveMillis, afterSeconds)
    }
}
