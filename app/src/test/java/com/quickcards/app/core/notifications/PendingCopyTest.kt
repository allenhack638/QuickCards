package com.quickcards.app.core.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PendingCopyTest {

    private var clock = 1_000L
    private val pending = PendingCopy<String>(ttlMillis = 60_000, now = { clock })

    @Test
    fun aHeldCopyIsTakenOnce() {
        pending.hold("HDFC")

        assertEquals("HDFC", pending.take())
        assertNull(pending.take())
    }

    @Test
    fun aCopyHeldTooLongIsDropped() {
        pending.hold("HDFC")
        clock += 60_000

        assertNull(pending.take())
    }

    @Test
    fun aRequestWithoutACopyClearsAnEarlierOne() {
        pending.hold("HDFC")
        pending.hold(null)

        assertNull(pending.take())
    }
}
