package com.quickcards.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ArtRulesTest {

    @Test
    fun `each band has its fixed pattern`() {
        val expected = mapOf(
            "navy" to ArtPattern.RINGS, "ocean" to ArtPattern.ROSETTE, "teal" to ArtPattern.CONTOURS,
            "forest" to ArtPattern.LINES, "moss" to ArtPattern.WEAVE, "olive" to ArtPattern.CONTOURS,
            "burnt-orange" to ArtPattern.ROSETTE, "rust" to ArtPattern.LINES, "crimson" to ArtPattern.WEAVE,
            "wine" to ArtPattern.LINES, "magenta" to ArtPattern.RINGS, "plum" to ArtPattern.WEAVE,
            "violet" to ArtPattern.ROSETTE, "indigo" to ArtPattern.CONTOURS, "slate" to ArtPattern.RINGS,
            "graphite" to ArtPattern.LINES
        )
        expected.forEach { (band, pattern) -> assertEquals(band, pattern, ArtRules.patternFor(band)) }
    }

    @Test
    fun `neighbouring palette colours never share a pattern`() {
        ArtRules.PALETTE_ORDER.zipWithNext().forEach { (a, b) ->
            assertTrue("$a/$b", ArtRules.patternFor(a) != ArtRules.patternFor(b))
        }
    }

    @Test
    fun `a custom hex hashes to the prototype's pattern, ignoring case and the hash sign`() {
        assertEquals(ArtPattern.ROSETTE, ArtRules.patternFor("#7A3E9D"))
        assertEquals(ArtPattern.WEAVE, ArtRules.patternFor("#123456"))
        assertEquals(ArtPattern.RINGS, ArtRules.patternFor("#0f5d8b"))
        assertEquals(ArtPattern.CONTOURS, ArtRules.patternFor("AB12CD"))
        assertEquals(ArtPattern.CONTOURS, ArtRules.patternFor("#000000"))
    }

    @Test
    fun `fnv-1a matches the prototype hash`() {
        assertEquals(4039658848L, ArtRules.fnv1a("7A3E9D"))
        assertEquals(8474323L, ArtRules.fnv1a("2B4EC2"))
    }

    @Test
    fun `unknown keys fall back to navy's pattern`() {
        assertEquals(ArtPattern.RINGS, ArtRules.patternFor("ochre"))
        assertEquals(ArtPattern.RINGS, ArtRules.patternFor(""))
    }

    @Test
    fun `a custom light stop derives the prototype's deep stop`() {
        assertClose(0x0A1D30, ArtRules.customDeepStop(0x204D79))
        assertClose(0x280808, ArtRules.customDeepStop(0x651B1B))
        assertClose(0x0C380C, ArtRules.customDeepStop(0x258D25))
        assertClose(0x2F0D40, ArtRules.customDeepStop(0x7A2BA1))
    }

    @Test
    fun `contrast with white follows WCAG`() {
        assertEquals(21.0, ArtRules.contrastWithWhite(0x000000), 0.01)
        assertEquals(1.0, ArtRules.contrastWithWhite(0xFFFFFF), 0.01)
        assertEquals(7.07, ArtRules.contrastWithWhite(0x2B4EC2), 0.01)
        assertEquals(8.71, ArtRules.contrastWithWhite(0x3E4C61), 0.01)
    }

    private fun assertClose(expected: Int, actual: Int) {
        for (shift in intArrayOf(16, 8, 0)) {
            val e = expected shr shift and 0xFF
            val a = actual shr shift and 0xFF
            assertTrue("%06X vs %06X".format(expected, actual), abs(e - a) <= 1)
        }
    }
}
