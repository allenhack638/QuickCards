package com.quickcards.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CopyWindowPolicyTest {

    @Test
    fun activeUseForFiveMinutesKeepsTheCopyWindowOpen() {
        var unlockedUntil = CopyWindowPolicy.expiresAt(nowMillis = 0L, windowSeconds = 60)
        for (second in 1..300) {
            unlockedUntil = CopyWindowPolicy.extend(
                nowMillis = second * 1000L,
                unlockedUntilMillis = unlockedUntil,
                windowSeconds = 60
            )
        }
        assertTrue(CopyWindowPolicy.isOpen(nowMillis = 300_000L, unlockedUntilMillis = unlockedUntil))
    }

    @Test
    fun idleForSixtyOneSecondsClosesTheCopyWindow() {
        val unlockedUntil = CopyWindowPolicy.expiresAt(nowMillis = 0L, windowSeconds = 60)
        assertFalse(CopyWindowPolicy.isOpen(nowMillis = 61_000L, unlockedUntilMillis = unlockedUntil))
        assertEquals(
            unlockedUntil,
            CopyWindowPolicy.extend(61_000L, unlockedUntil, windowSeconds = 60)
        )
    }

    @Test
    fun zeroWindowNeverOpens() {
        assertEquals(0L, CopyWindowPolicy.windowMillis(0))
        assertFalse(CopyWindowPolicy.isOpen(0L, CopyWindowPolicy.expiresAt(0L, 0)))
    }
}
