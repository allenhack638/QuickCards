package com.quickcards.app.core.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class CopyNotificationTextTest {

    private fun text(expiry: String, cvv: String) =
        CopyNotificationText.text(expiry, cvv, { "Expiry $it" }, { "CVV $it" }, " · ")

    @Test
    fun bothValuesShowInOrder() {
        assertEquals("Expiry 12/29 · CVV 381", text("12/29", "381"))
    }

    @Test
    fun theTextIsNeverMasked() {
        assertFalse(text("12/29", "4821").contains("•"))
    }

    @Test
    fun aBlankValueIsLeftOutAndNoValuesMeansNoText() {
        assertEquals("CVV 123", text("", "123"))
        assertEquals("Expiry 12/29", text("12/29", " "))
        assertEquals("", text("", ""))
    }
}
