package com.quickcards.app.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class PassColorsTest {

    @Test
    fun bandResolvesEveryDocumentedKey() {
        assertEquals(Color(0xFF1E3A8A), PassColors.band("navy"))
        assertEquals(Color(0xFF7C2D12), PassColors.band("rust"))
        assertEquals(Color(0xFF14532D), PassColors.band("forest"))
        assertEquals(Color(0xFF4C1D95), PassColors.band("plum"))
        assertEquals(Color(0xFF3F3F46), PassColors.band("graphite"))
        assertEquals(Color(0xFF0F5E5A), PassColors.band("teal"))
        assertEquals(Color(0xFF0C4A6E), PassColors.band("ocean"))
        assertEquals(Color(0xFF3730A3), PassColors.band("indigo"))
        assertEquals(Color(0xFF5B21B6), PassColors.band("violet"))
        assertEquals(Color(0xFF86198F), PassColors.band("magenta"))
        assertEquals(Color(0xFF6B1E3E), PassColors.band("wine"))
        assertEquals(Color(0xFF9F1239), PassColors.band("crimson"))
        assertEquals(Color(0xFF9A3412), PassColors.band("burnt-orange"))
        assertEquals(Color(0xFF4D5D1B), PassColors.band("olive"))
        assertEquals(Color(0xFF3F6212), PassColors.band("moss"))
        assertEquals(Color(0xFF334155), PassColors.band("slate"))
    }

    @Test
    fun bandParsesStoredCustomHex() {
        assertEquals(Color(0xFF123456), PassColors.band("#123456"))
        assertEquals(Color(0xFF0F5E5A), PassColors.band("#0f5e5a"))
        assertEquals(Color(0xFF123456), PassColors.band("123456"))
    }

    @Test
    fun bandFallsBackToNavyForUnknownKeys() {
        assertEquals(PassColors.Navy, PassColors.band(""))
        assertEquals(PassColors.Navy, PassColors.band("ochre"))
        assertEquals(PassColors.Navy, PassColors.band("NAVY"))
        assertEquals(PassColors.Navy, PassColors.band("#12345"))
        assertEquals(PassColors.Navy, PassColors.band("#GGGGGG"))
    }

    @Test
    fun bodyTintMixesBandIntoTheMatchingContainer() {
        assertEquals(
            lerp(DarkSurfaceContainer, PassColors.Plum, 0.12f),
            PassColors.bodyTint(PassColors.Plum, dark = true)
        )
        assertEquals(
            lerp(LightSurfaceContainer, PassColors.Plum, 0.06f),
            PassColors.bodyTint(PassColors.Plum, dark = false)
        )
        assertNotEquals(
            PassColors.bodyTint(PassColors.Plum, dark = true),
            PassColors.bodyTint(PassColors.Plum, dark = false)
        )
    }

    @Test
    fun edgeMixesBandIntoTheThemeEdgeBase() {
        assertEquals(
            lerp(DarkPassEdgeBase, PassColors.Forest, 0.60f),
            PassColors.edge(PassColors.Forest, dark = true)
        )
        assertEquals(
            lerp(LightPassEdgeBase, PassColors.Forest, 0.35f),
            PassColors.edge(PassColors.Forest, dark = false)
        )
    }

    @Test
    fun perforationUsesOnSurfaceAtThemeAlpha() {
        assertEquals(
            DarkOnSurface.copy(alpha = 0.24f),
            PassColors.perforation(dark = true)
        )
        assertEquals(
            LightOnSurface.copy(alpha = 0.22f),
            PassColors.perforation(dark = false)
        )
    }

    @Test
    fun expiredTokensUseTheDocumentedGraphiteValues() {
        val dark = PassColors.expired(dark = true)
        assertEquals(DarkPassExpiredBand, dark.band)
        assertEquals(DarkPassExpiredBandEnd, dark.bandEnd)
        assertEquals(DarkPassExpiredSheen, dark.sheen)
        assertEquals(DarkPassExpiredBody, dark.body)
        assertEquals(DarkPassExpiredEdge, dark.edge)
        assertEquals(DarkPassExpiredTag, dark.tag)

        val light = PassColors.expired(dark = false)
        assertEquals(LightPassExpiredBand, light.band)
        assertEquals(LightPassExpiredBandEnd, light.bandEnd)
        assertEquals(LightPassExpiredSheen, light.sheen)
        assertEquals(LightPassExpiredBody, light.body)
        assertEquals(LightPassExpiredEdge, light.edge)
        assertEquals(LightPassExpiredTag, light.tag)
    }
}
