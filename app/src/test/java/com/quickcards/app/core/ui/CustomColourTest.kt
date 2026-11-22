package com.quickcards.app.core.ui

import com.quickcards.app.core.data.model.Bank
import com.quickcards.app.core.data.repository.BankColourRules
import com.quickcards.app.core.ui.components.CustomColour
import com.quickcards.domain.ArtRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CustomColourTest {

    @Test
    fun theDefaultCustomColourKeepsWhiteTextAtSevenToOne() {
        assertTrue(CustomColour.readable(CustomColour.hex(CustomColour.DEFAULT_HUE, CustomColour.DEFAULT_LIGHTNESS)))
    }

    @Test
    fun aLightShadeIsTooLight() {
        assertFalse(CustomColour.readable(CustomColour.hex(248f, 0.86f)))
        assertFalse(CustomColour.readable("#FFFFFF"))
        assertFalse(CustomColour.readable("nope"))
    }

    @Test
    fun theLimitTickSplitsReadableFromTooLight() {
        for (hue in listOf(0f, 60f, 120f, 248f, 300f)) {
            val limit = CustomColour.limit(hue)
            assertTrue(CustomColour.readable(CustomColour.hex(hue, limit)))
            if (limit < 1f) assertFalse(CustomColour.readable(CustomColour.hex(hue, (limit + 0.02f).coerceAtMost(1f))))
        }
    }

    @Test
    fun aHexMapsBackToItsStripPositions() {
        val hex = CustomColour.hex(200f, 0.3f)
        val (hue, t) = CustomColour.position(hex)
        assertEquals(200f, hue, 2f)
        assertEquals(0.3f, t, 0.02f)
    }

    @Test
    fun theBankEditorShowsTheColourSavingWouldGive() {
        val banks = listOf(Bank(id = "1", name = "HDFC Bank"), Bank(id = "2", name = "SBI Card"))
        assertEquals("navy", BankColourRules.editorColour("HDFC", null, null, banks))
        assertEquals(ArtRules.PALETTE_ORDER[2], BankColourRules.editorColour("Saraswat Bank", null, null, banks))
        assertEquals("plum", BankColourRules.editorColour("Saraswat Bank", "plum", null, banks))
    }
}
