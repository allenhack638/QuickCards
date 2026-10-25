package com.quickcards.app.core.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LockGateTest {

    @Test
    fun overlaysAreVisibleOnlyWhileOpenAndUnlocked() {
        assertTrue(LockGate.visible(open = true, locked = false))
        assertFalse(LockGate.visible(open = true, locked = true))
        assertFalse(LockGate.visible(open = false, locked = false))
        assertFalse(LockGate.visible(open = false, locked = true))
    }
}
