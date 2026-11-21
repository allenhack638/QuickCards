package com.quickcards.app.core.ui

import com.quickcards.app.core.ui.components.MarkTone
import com.quickcards.app.core.ui.components.NetworkMarks
import com.quickcards.app.core.ui.components.markTone
import org.junit.Assert.assertEquals
import org.junit.Test

/** White on card art and dark surfaces, colour on light ones; Mastercard and Maestro always colour. */
class NetworkMarkToneTest {

    private val whiteOnArt = listOf("visa", "rupay", "amex", "diners", "discover", "jcb", "mir", "unionpay")

    @Test
    fun cardArtTurnsEveryMarkWhiteExceptMastercardAndMaestro() {
        whiteOnArt.forEach { assertEquals(it, MarkTone.WHITE, markTone(it, onArt = true, dark = false)) }
        assertEquals(MarkTone.COLOUR, markTone("mc", onArt = true, dark = false))
        assertEquals(MarkTone.COLOUR, markTone("maestro", onArt = true, dark = true))
    }

    @Test
    fun lightSurfacesShowFullColour() {
        NetworkMarks.ids.forEach { assertEquals(it, MarkTone.COLOUR, markTone(it, onArt = false, dark = false)) }
    }

    @Test
    fun darkSurfacesFollowTheArtRule() {
        whiteOnArt.forEach { assertEquals(it, MarkTone.WHITE, markTone(it, onArt = false, dark = true)) }
        assertEquals(MarkTone.COLOUR, markTone("mc", onArt = false, dark = true))
    }
}
