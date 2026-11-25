package com.quickcards.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class CardStatusRulesTest {

    @Test
    fun archivedWinsOverEverything() {
        assertEquals(CardStatus.ARCHIVED, CardStatusRules.of(true, ExpiryState.EXPIRED, true, true, true))
    }

    @Test
    fun followsPriorityOrder() {
        assertEquals(CardStatus.EXPIRED, CardStatusRules.of(false, ExpiryState.EXPIRED, true, true, true))
        assertEquals(CardStatus.CHECK, CardStatusRules.of(false, ExpiryState.EXPIRES_SOON, true, true, true))
        assertEquals(CardStatus.EXPIRES_SOON, CardStatusRules.of(false, ExpiryState.EXPIRES_SOON, false, true, true))
        assertEquals(CardStatus.BEST_TODAY, CardStatusRules.of(false, ExpiryState.VALID, false, true, true))
        assertEquals(CardStatus.LAST_USED, CardStatusRules.of(false, ExpiryState.VALID, false, false, true))
    }

    @Test
    fun noStatusForAPlainCard() {
        assertNull(CardStatusRules.of(false, ExpiryState.VALID, false, false, false))
        assertNull(CardStatusRules.of(false, null, false, false, false))
    }
}
