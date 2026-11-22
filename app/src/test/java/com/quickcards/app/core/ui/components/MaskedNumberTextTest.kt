package com.quickcards.app.core.ui.components

import com.quickcards.app.core.ui.theme.QcText
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MaskedNumberTextTest {

    @Test
    fun stylesOnlyTheMaskAndTheLastFour() {
        val text = maskedNumberText("Delete Axis Bank •• 4444?")
        assertEquals("Delete Axis Bank •• 4444?", text.text)
        val spans = text.spanStyles.map { it.start to it.end }
        val dots = text.text.indexOf("••")
        assertEquals(listOf(dots to dots + 2, dots + 2 to dots + 7), spans)
        assertEquals(QcText.MonoDotsF3.letterSpacing, text.spanStyles[0].item.letterSpacing)
    }

    @Test
    fun textWithoutAMaskIsLeftPlain() {
        assertTrue(maskedNumberText("Delete this card?").spanStyles.isEmpty())
    }
}
