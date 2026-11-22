package com.quickcards.app.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.quickcards.domain.ArtPattern
import com.quickcards.domain.ArtRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PassColorsTest {

    private val documented = mapOf(
        "navy" to (0x0B1D57 to 0x2B4EC2), "ocean" to (0x04283F to 0x0F5D8B),
        "teal" to (0x032F36 to 0x0A616B), "forest" to (0x04362C to 0x0B644D),
        "moss" to (0x172509 to 0x43611F), "olive" to (0x22240A to 0x565C1D),
        "burnt-orange" to (0x3F1A03 to 0x8C4409), "rust" to (0x3A1208 to 0x983A1C),
        "crimson" to (0x3E0709 to 0xA3202A), "wine" to (0x46081C to 0xA81A43),
        "magenta" to (0x3A0A31 to 0x9B2580), "plum" to (0x2A0E3D to 0x6E2C9A),
        "violet" to (0x1F0D45 to 0x683AC0), "indigo" to (0x140F4A to 0x4338B8),
        "slate" to (0x141B26 to 0x3E4C61), "graphite" to (0x2A2D33 to 0x535963)
    )

    @Test
    fun everyStoredKeyResolvesToItsDaylightStops() {
        documented.forEach { (key, stops) ->
            val hue = PassColors.hue(key)
            assertEquals(key, stops.first, hue.c1.rgb())
            assertEquals(key, stops.second, hue.c2.rgb())
            assertEquals(key, ArtRules.patternFor(key), hue.pattern)
        }
    }

    @Test
    fun whiteDigitsReachSevenToOneOnBothStopsOfEveryHue() {
        documented.keys.forEach { key ->
            val hue = PassColors.hue(key)
            listOf(hue.c1, hue.c2).forEach { stop ->
                val ratio = ArtRules.contrastWithWhite(stop.rgb())
                assertTrue("$key ${"%06X".format(stop.rgb())} = $ratio", ratio >= ArtRules.MIN_DIGIT_CONTRAST)
            }
        }
    }

    @Test
    fun expiredCardsRenderInGraphiteWithLines() {
        val hue = PassColors.hue("navy", expired = true)
        assertEquals(0x535963, hue.c2.rgb())
        assertEquals(ArtPattern.LINES, hue.pattern)
    }

    @Test
    fun customHexIsTheLightStopWithADerivedDeepStop() {
        val hue = PassColors.hue("#204d79")
        assertEquals(0x204D79, hue.c2.rgb())
        assertEquals(ArtRules.customDeepStop(0x204D79), hue.c1.rgb())
        assertEquals(ArtRules.patternFor("#204D79"), hue.pattern)
    }

    @Test
    fun glowIsTheLightStopAtFortyFivePercent() {
        val hue = PassColors.hue("rust")
        assertEquals(hue.c2.copy(alpha = 0.45f), hue.glow)
    }

    @Test
    fun unknownKeysFallBackToNavy() {
        assertEquals(PassColors.hue("navy"), PassColors.hue("ochre"))
        assertEquals(PassColors.hue("navy"), PassColors.hue("#12345"))
    }

    private fun Color.rgb(): Int = toArgb() and 0xFFFFFF
}
