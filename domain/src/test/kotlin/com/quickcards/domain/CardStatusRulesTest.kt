package com.quickcards.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CardStatusRulesTest {

    @Test
    fun archivedWinsOverEverything() {
        assertEquals(CardStatus.ARCHIVED, CardStatusRules.of(true, ExpiryState.EXPIRED, true, true))
    }

    @Test
    fun followsPriorityOrder() {
        assertEquals(CardStatus.EXPIRED, CardStatusRules.of(false, ExpiryState.EXPIRED, true, true))
        assertEquals(CardStatus.CHECK, CardStatusRules.of(false, ExpiryState.EXPIRES_SOON, true, true))
        assertEquals(CardStatus.EXPIRES_SOON, CardStatusRules.of(false, ExpiryState.EXPIRES_SOON, false, true))
        assertEquals(CardStatus.LAST_USED, CardStatusRules.of(false, ExpiryState.VALID, false, true))
    }

    @Test
    fun noStatusForAPlainCard() {
        assertNull(CardStatusRules.of(false, ExpiryState.VALID, false, false))
        assertNull(CardStatusRules.of(false, null, false, false))
    }

    @Test
    fun lastUsedIsTheLatestCopy() {
        assertEquals("b", CardStatusRules.lastUsedId(mapOf("a" to 1L, "b" to 5L, "c" to 3L)))
        assertNull(CardStatusRules.lastUsedId(emptyMap()))
    }
}
