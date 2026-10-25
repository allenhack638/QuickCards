package com.quickcards.app.core.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BandColorTest {

    @Test
    fun paletteHasFifteenDistinctCuratedBands() {
        assertEquals(15, BandColor.ALL.size)
        assertEquals(BandColor.ALL.size, BandColor.ALL.distinct().size)
        assertEquals(
            listOf(
                BandColor.NAVY, BandColor.RUST, BandColor.FOREST, BandColor.PLUM,
                BandColor.GRAPHITE, BandColor.TEAL, BandColor.OCEAN, BandColor.INDIGO,
                BandColor.VIOLET, BandColor.MAGENTA, BandColor.WINE, BandColor.CRIMSON,
                BandColor.BURNT_ORANGE, BandColor.OLIVE, BandColor.MOSS
            ),
            BandColor.ALL
        )
    }

    @Test
    fun everyCuratedBandPassesTheWhiteTextGuard() {
        BandColor.ALL.forEach { key ->
            val hex = BandColor.hexFor(key)
            val contrast = BandColor.contrastWithWhite(hex)
            requireNotNull(contrast) { "$key has no contrast" }
            assertTrue(
                "$key ($hex) has only $contrast:1 with white",
                contrast >= BandColor.MIN_CONTRAST
            )
            assertTrue(BandColor.isReadable(hex))
        }
    }

    @Test
    fun exactBandHexMapsToItsBand() {
        BandColor.ALL.forEach { key ->
            assertEquals(key, BandColor.nearest(BandColor.hexFor(key)))
        }
    }

    @Test
    fun nearBandHexMapsToNearestBand() {
        assertEquals(BandColor.NAVY, BandColor.nearest("#1E3A8B"))
        assertEquals(BandColor.RUST, BandColor.nearest("#7C2D13"))
        assertEquals(BandColor.FOREST, BandColor.nearest("#14532E"))
        assertEquals(BandColor.PLUM, BandColor.nearest("#4C1D96"))
        assertEquals(BandColor.GRAPHITE, BandColor.nearest("#3F3F47"))
        assertEquals(BandColor.TEAL, BandColor.nearest("#0F5E5B"))
        assertEquals(BandColor.OCEAN, BandColor.nearest("#0C4A6F"))
        assertEquals(BandColor.INDIGO, BandColor.nearest("#3730A4"))
        assertEquals(BandColor.VIOLET, BandColor.nearest("#5B21B7"))
        assertEquals(BandColor.MAGENTA, BandColor.nearest("#861990"))
        assertEquals(BandColor.WINE, BandColor.nearest("#6B1E3F"))
        assertEquals(BandColor.CRIMSON, BandColor.nearest("#9F123A"))
        assertEquals(BandColor.BURNT_ORANGE, BandColor.nearest("#9A3413"))
        assertEquals(BandColor.OLIVE, BandColor.nearest("#4D5D1C"))
        assertEquals(BandColor.MOSS, BandColor.nearest("#3F6213"))
    }

    @Test
    fun slateLeftTheGridButStoredSlateCardsStillResolve() {
        assertEquals(false, BandColor.SLATE in BandColor.ALL)
        assertEquals("#334155", BandColor.hexFor(BandColor.SLATE))
    }

    @Test
    fun legacyDefaultsMapToTheClosestCuratedBand() {
        assertEquals(BandColor.INDIGO, BandColor.nearest("#2196F3"))
        assertEquals(BandColor.TEAL, BandColor.nearest("#00A651"))
        assertEquals(BandColor.GRAPHITE, BandColor.nearest("#1B2A4A"))
        assertEquals(BandColor.GRAPHITE, BandColor.nearest("#242A33"))
    }

    @Test
    fun acceptsHexWithAndWithoutHashOrAlpha() {
        assertEquals(BandColor.NAVY, BandColor.nearest("1e3a8a"))
        assertEquals(BandColor.RUST, BandColor.nearest("#FF7C2D12"))
        assertEquals(BandColor.INDIGO, BandColor.nearest("#2288CC"))
    }

    @Test
    fun invalidValuesFallBackToNavy() {
        assertEquals(BandColor.NAVY, BandColor.nearest(null))
        assertEquals(BandColor.NAVY, BandColor.nearest(""))
        assertEquals(BandColor.NAVY, BandColor.nearest("#ZZZZZZ"))
        assertEquals(BandColor.NAVY, BandColor.nearest("#12345"))
    }

    @Test
    fun hexForKnownCustomAndUnknownValues() {
        assertEquals("#1E3A8A", BandColor.hexFor("navy"))
        assertEquals("#3F3F46", BandColor.hexFor("GRAPHITE"))
        assertEquals("#0F5E5A", BandColor.hexFor("teal"))
        assertEquals("#9A3412", BandColor.hexFor("burnt-orange"))
        assertEquals("#123456", BandColor.hexFor("#123456"))
        assertEquals("#1E3A8A", BandColor.hexFor("unknown"))
    }

    @Test
    fun parsesAndFormatsSixDigitHex() {
        assertEquals("#0F5E5A", BandColor.parseHex("#0f5e5a"))
        assertEquals("#0F5E5A", BandColor.parseHex("0F5E5A"))
        assertEquals("#9A3412", BandColor.parseHex("  #9a3412  "))
        assertNull(BandColor.parseHex("#FFF"))
        assertNull(BandColor.parseHex("#GGGGGG"))
        assertNull(BandColor.parseHex(null))
        assertEquals("#0F5E5A", BandColor.formatHex(15, 94, 90))
        assertEquals("#FF0000", BandColor.formatHex(300, -4, 0))
    }

    @Test
    fun contrastGuardUsesTheWhiteTextRatio() {
        assertEquals(21.0, BandColor.contrastWithWhite("#000000")!!, 0.01)
        assertEquals(1.0, BandColor.contrastWithWhite("#FFFFFF")!!, 0.01)
        assertTrue(BandColor.isReadable("#1E3A8A"))
        assertFalse(BandColor.isReadable("#FFFF00"))
        assertNull(BandColor.contrastWithWhite("not a colour"))
    }

    @Test
    fun oldCuratedKeysStillResolve() {
        assertEquals("#1E3A8A", BandColor.hexFor("navy"))
        assertEquals("#7C2D12", BandColor.hexFor("rust"))
        assertEquals("#14532D", BandColor.hexFor("forest"))
        assertEquals("#4C1D95", BandColor.hexFor("plum"))
        assertEquals("#3F3F46", BandColor.hexFor("graphite"))
        assertTrue(BandColor.isCurated("navy"))
        assertFalse(BandColor.isCurated("#123456"))
    }

    @Test
    fun importKeepsReadableHexAndSnapsTooLightHex() {
        assertEquals(BandColor.NAVY, BandColor.importValue("navy"))
        assertEquals(BandColor.BURNT_ORANGE, BandColor.importValue("burnt-orange"))
        assertEquals("#0F5E5A", BandColor.importValue("#0f5e5a"))
        assertEquals("#123456", BandColor.importValue("#123456"))
        assertEquals("#7C2D12", BandColor.importValue("#FF7C2D12"))
        assertEquals(BandColor.MAGENTA, BandColor.importValue("#FFFFFF"))
        assertEquals(BandColor.BURNT_ORANGE, BandColor.importValue("#FFFF00"))
        assertEquals(BandColor.NAVY, BandColor.importValue("nonsense"))
        assertEquals(BandColor.NAVY, BandColor.importValue(null))
    }
}
