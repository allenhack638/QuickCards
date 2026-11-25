package com.quickcards.app.feature.details

import com.quickcards.domain.CardField
import org.junit.Assert.assertEquals
import org.junit.Test

class CardStatusSheetTest {

    @Test
    fun checkNamesEveryFlaggedFieldInFormOrder() {
        assertEquals(listOf(CardField.EXPIRY), flaggedFields(setOf(CardField.EXPIRY)))
        assertEquals(
            listOf(CardField.NUMBER, CardField.EXPIRY, CardField.CVV),
            flaggedFields(setOf(CardField.CVV, CardField.NUMBER, CardField.EXPIRY))
        )
    }
}
