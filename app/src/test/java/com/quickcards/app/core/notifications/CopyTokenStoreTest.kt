package com.quickcards.app.core.notifications

import java.security.SecureRandom
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CopyTokenStoreTest {

    private var clock = 1_000L
    private val store = CopyTokenStore(now = { clock }, random = SecureRandom())

    @Test
    fun aLiveTokenReturnsEachField() {
        val token = store.issue("HDFC Bank ••1111", "12/29", "123", ttlMillis = 30_000)

        assertEquals("12/29", store.value(token, CopyField.EXPIRY)?.text)
        assertEquals("123", store.value(token, CopyField.CVV)?.text)
        assertEquals("HDFC Bank ••1111", store.value(token, CopyField.CVV)?.title)
    }

    @Test
    fun theTokenExpiresWithTheNotificationAndForgetsTheValues() {
        val token = store.issue("t", "12/29", "123", ttlMillis = 30_000)
        clock += 30_000

        assertNull(store.value(token, CopyField.CVV))
        clock -= 30_000
        assertNull(store.value(token, CopyField.CVV))
    }

    @Test
    fun aNewCopyRevokesTheOldToken() {
        val old = store.issue("a", "01/27", "111", ttlMillis = 30_000)
        val new = store.issue("b", "12/29", "123", ttlMillis = 30_000)

        assertNotEquals(old, new)
        assertNull(store.value(old, CopyField.CVV))
        assertEquals("123", store.value(new, CopyField.CVV)?.text)
    }

    @Test
    fun wrongMissingOrMalformedTokensCopyNothing() {
        store.issue("t", "12/29", "123", ttlMillis = 30_000)

        assertNull(store.value(null, CopyField.CVV))
        assertNull(store.value("", CopyField.CVV))
        assertNull(store.value("not base64 !!", CopyField.CVV))
        assertNull(store.value("AAAAAAAAAAAAAAAAAAAAAA", CopyField.CVV))
    }

    @Test
    fun clearAndBlankValuesCopyNothing() {
        val token = store.issue("t", "12/29", "", ttlMillis = 30_000)
        assertNull(store.value(token, CopyField.CVV))

        store.clear()
        assertNull(store.value(token, CopyField.EXPIRY))
    }
}
