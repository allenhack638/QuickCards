package com.quickcards.app.core.notifications

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CopyNotificationTextTest {

    private val masked = { field: CopyField -> if (field == CopyField.EXPIRY) "Expiry ••/••" else "CVV •••" }

    @Test
    fun bothFieldsShowMaskedPlaceholdersInOrder() {
        val fields = CopyNotificationText.fields("12/29", "482")

        assertEquals(listOf(CopyField.EXPIRY, CopyField.CVV), fields)
        assertEquals("Expiry ••/•• · CVV •••", CopyNotificationText.text(fields, masked, " · "))
    }

    @Test
    fun theTextNeverContainsTheValues() {
        val text = CopyNotificationText.text(CopyNotificationText.fields("12/29", "4821"), masked, " · ")

        assertFalse(text.contains("12/29"))
        assertFalse(text.contains("4821"))
        assertFalse(text.any(Char::isDigit))
    }

    @Test
    fun aBlankFieldIsLeftOutAndNoFieldsMeansNoNotification() {
        assertEquals(listOf(CopyField.CVV), CopyNotificationText.fields("", "123"))
        assertEquals(listOf(CopyField.EXPIRY), CopyNotificationText.fields("12/29", " "))
        assertTrue(CopyNotificationText.fields("", "").isEmpty())
    }
}
