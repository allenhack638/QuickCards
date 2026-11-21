package com.quickcards.app.core.ui

import com.quickcards.app.core.ui.components.MarkTone
import com.quickcards.app.core.ui.components.NetworkMarks
import com.quickcards.app.core.ui.components.markTone
import org.junit.Assert.assertEquals
import org.junit.Test

/** White on card art; full colour on surfaces except dark-theme wordmarks; Mastercard and Maestro always colour. */
class NetworkMarkToneTest {

    private val whiteOnArt = listOf("visa", "rupay", "amex", "diners", "discover", "jcb", "mir", "unionpay")
    private val whiteOnDarkSurface = listOf("visa", "rupay", "diners", "discover", "mir")
    private val boxed = listOf("amex", "jcb", "unionpay")

    @Test
    fun cardArtTurnsEveryMarkWhiteExceptMastercardAndMaestro() {
        whiteOnArt.forEach { assertEquals(it, MarkTone.WHITE, markTone(it, onArt = true, dark = false)) }
        whiteOnArt.forEach { assertEquals(it, MarkTone.WHITE, markTone(it, onArt = true, dark = true)) }
        assertEquals(MarkTone.COLOUR, markTone("mc", onArt = true, dark = false))
        assertEquals(MarkTone.COLOUR, markTone("maestro", onArt = true, dark = true))
    }

    @Test
    fun lightSurfacesShowFullColour() {
        NetworkMarks.ids.forEach { assertEquals(it, MarkTone.COLOUR, markTone(it, onArt = false, dark = false)) }
    }

    @Test
    fun darkSurfacesWhitenOnlyWordmarks() {
        whiteOnDarkSurface.forEach { assertEquals(it, MarkTone.WHITE, markTone(it, onArt = false, dark = true)) }
        boxed.forEach { assertEquals(it, MarkTone.COLOUR, markTone(it, onArt = false, dark = true)) }
        assertEquals(MarkTone.COLOUR, markTone("mc", onArt = false, dark = true))
        assertEquals(MarkTone.COLOUR, markTone("maestro", onArt = false, dark = true))
    }
}
