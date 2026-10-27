package com.quickcards.app.core.security

import org.junit.Assert.assertEquals
import org.junit.Test

class ClipboardExpiryTest {

    private val tokenFor: (String) -> String? = { value -> "token:$value" }

    @Test
    fun aNullReadKeepsTheRecordForTheNextFocusedRead() {
        assertEquals(
            ClipboardExpiryAction.KEEP_RECORD,
            ClipboardExpiry.decide(current = null, expectedToken = "token:secret", tokenFor = tokenFor)
        )
    }

    @Test
    fun aFocusedReadOfOurValueClearsAndForgets() {
        assertEquals(
            ClipboardExpiryAction.CLEAR_AND_FORGET,
            ClipboardExpiry.decide(current = "secret", expectedToken = "token:secret", tokenFor = tokenFor)
        )
    }

    @Test
    fun aDifferentValueIsForgottenWithoutClearing() {
        assertEquals(
            ClipboardExpiryAction.FORGET_RECORD,
            ClipboardExpiry.decide(current = "someone else", expectedToken = "token:secret", tokenFor = tokenFor)
        )
    }

    @Test
    fun aMissingTokenIsForgotten() {
        assertEquals(
            ClipboardExpiryAction.FORGET_RECORD,
            ClipboardExpiry.decide(current = "secret", expectedToken = null, tokenFor = tokenFor)
        )
    }
}
