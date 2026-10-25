package com.quickcards.domain

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AppLockPolicyTest {

    @Test
    fun zeroDisablesInactivityLock() {
        assertFalse(
            AppLockPolicy.shouldLock(
                nowMillis = 10_000_000L,
                lastActiveMillis = 0L,
                afterSeconds = 0
            )
        )
    }

    @Test
    fun doesNotLockBeforeTheThreshold() {
        assertFalse(
            AppLockPolicy.shouldLock(
                nowMillis = 59_999L,
                lastActiveMillis = 0L,
                afterSeconds = 60
            )
        )
    }

    @Test
    fun locksExactlyAtTheThreshold() {
        assertTrue(
            AppLockPolicy.shouldLock(
                nowMillis = 60_000L,
                lastActiveMillis = 0L,
                afterSeconds = 60
            )
        )
    }

    @Test
    fun locksAfterTenSecondsOfInactivity() {
        assertTrue(
            AppLockPolicy.shouldLock(
                nowMillis = 1_000L + 10_000L,
                lastActiveMillis = 1_000L,
                afterSeconds = 10
            )
        )
    }

    @Test
    fun activeUseForFiveMinutesNeverLocksWithOneMinuteSetting() {
        var lastActive = 0L
        var locked = false
        for (second in 1..300) {
            val now = second * 1000L
            if (AppLockPolicy.shouldLock(now, lastActive, 60)) {
                locked = true
                break
            }
            lastActive = now
        }
        assertFalse(locked)
        assertFalse(AppLockPolicy.shouldLock(nowMillis = 300_500L, lastActiveMillis = 300_000L, afterSeconds = 60))
    }

    @Test
    fun locksAfterSixtyOneSecondsIdle() {
        assertTrue(
            AppLockPolicy.shouldLock(
                nowMillis = 61_000L,
                lastActiveMillis = 0L,
                afterSeconds = 60
            )
        )
    }

    @Test
    fun ignoresClockJumpsBackwards() {
        assertFalse(
            AppLockPolicy.shouldLock(
                nowMillis = 5_000L,
                lastActiveMillis = 9_000L,
                afterSeconds = 10
            )
        )
    }
}
