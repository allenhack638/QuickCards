package com.quickcards.app.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.quickcards.domain.ArtPattern
import com.quickcards.domain.ArtRules
import java.util.Locale

val BrandRed = Color(0xFFC8202B)
val BrandOnRed = Color(0xFFFFFFFF)

/** Daylight chrome roles (DESIGN.md › Colors). Material roles are mapped from these in Theme.kt. */
@Immutable
data class QcColors(
    val dark: Boolean,
    val bg: Color,
    val surface: Color,
    val surfaceHi: Color,
    val tonal: Color,
    val tonal2: Color,
    /** Tonal steps on raised surfaces (sheets, dialogs, menus). */
    val tonalHi: Color,
    val tonalHi2: Color,
    val line: Color,
    val outline: Color,
    val ink: Color,
    val ink2: Color,
    /** Disabled tone only; exempt from contrast. */
    val ink3: Color,
    val inkPh: Color,
    val accent: Color,
    val brand: Color,
    val onBrand: Color,
    val danger: Color,
    val dangerBg: Color,
    val onDanger: Color,
    val ok: Color,
    val warn: Color,
    val select: Color,
    val onSelect: Color,
    val inverse: Color,
    val onInverse: Color,
    val inverseAccent: Color,
    val scrim: Color,
    val state: Color,
    val shadow: Color
)

val LightQcColors = QcColors(
    dark = false,
    bg = Color(0xFFF4F6F9),
    surface = Color(0xFFFFFFFF),
    surfaceHi = Color(0xFFFFFFFF),
    tonal = Color(0xFFEEF1F5),
    tonal2 = Color(0xFFE3E7ED),
    tonalHi = Color(0xFFEEF1F5),
    tonalHi2 = Color(0xFFE3E7ED),
    line = Color(0xFFE3E7ED),
    outline = Color(0xFF7A8392),
    ink = Color(0xFF0D1117),
    ink2 = Color(0xFF4F5866),
    ink3 = Color(0xFF9AA1AC),
    inkPh = Color(0xFF6B7482),
    accent = Color(0xFF1F4FE0),
    brand = BrandRed,
    onBrand = BrandOnRed,
    danger = Color(0xFFB42318),
    dangerBg = Color(0xFFFCEDEB),
    onDanger = Color(0xFFFFFFFF),
    ok = Color(0xFF1B7F4B),
    warn = Color(0xFF9A5B00),
    select = Color(0xFF0D1117),
    onSelect = Color(0xFFFFFFFF),
    inverse = Color(0xFF151A22),
    onInverse = Color(0xFFF2F4F7),
    inverseAccent = Color(0xFFA9BFFF),
    scrim = Color(0x5C0D1117),
    state = Color(0x140D1117),
    shadow = Color(0xFF182440)
)

val DarkQcColors = QcColors(
    dark = true,
    bg = Color(0xFF0B0D11),
    surface = Color(0xFF14171D),
    surfaceHi = Color(0xFF1A1E25),
    tonal = Color(0xFF1C2028),
    tonal2 = Color(0xFF262B34),
    tonalHi = Color(0xFF2A2F39),
    tonalHi2 = Color(0xFF343A45),
    line = Color(0xFF232831),
    outline = Color(0xFF6B7482),
    ink = Color(0xFFF2F4F7),
    ink2 = Color(0xFF9AA3B0),
    ink3 = Color(0xFF4F5764),
    inkPh = Color(0xFF808895),
    accent = Color(0xFF8FAEFF),
    brand = BrandRed,
    onBrand = BrandOnRed,
    danger = Color(0xFFFF8A7A),
    dangerBg = Color(0xFF3A1714),
    onDanger = Color(0xFF1A0503),
    ok = Color(0xFF6FD3A0),
    warn = Color(0xFFF2B45C),
    select = Color(0xFFF2F4F7),
    onSelect = Color(0xFF0B0D11),
    inverse = Color(0xFFE6E9EE),
    onInverse = Color(0xFF0D1117),
    inverseAccent = Color(0xFF1F4FE0),
    scrim = Color(0x99000000),
    state = Color(0x1AF2F4F7),
    shadow = Color(0xFF000000)
)

val LocalQcColors = staticCompositionLocalOf { LightQcColors }

/** A card-art hue: the 135° gradient from the deep stop [c1] to the light stop [c2]. */
@Immutable
data class ArtHue(val c1: Color, val c2: Color, val pattern: ArtPattern) {
    val glow: Color get() = c2.copy(alpha = 0.45f)
}

/** Daylight card art: the 16 jewel hues keyed by the stored `cardColor`, plus the on-art inks. */
object PassColors {
    private val hues: Map<String, Pair<Color, Color>> = mapOf(
        "navy" to (Color(0xFF0B1D57) to Color(0xFF2B4EC2)),
        "ocean" to (Color(0xFF04283F) to Color(0xFF0F5D8B)),
        "teal" to (Color(0xFF032F36) to Color(0xFF0A616B)),
        "forest" to (Color(0xFF04362C) to Color(0xFF0B644D)),
        "moss" to (Color(0xFF172509) to Color(0xFF43611F)),
        "olive" to (Color(0xFF22240A) to Color(0xFF565C1D)),
        "burnt-orange" to (Color(0xFF3F1A03) to Color(0xFF8C4409)),
        "rust" to (Color(0xFF3A1208) to Color(0xFF983A1C)),
        "crimson" to (Color(0xFF3E0709) to Color(0xFFA3202A)),
        "wine" to (Color(0xFF46081C) to Color(0xFFA81A43)),
        "magenta" to (Color(0xFF3A0A31) to Color(0xFF9B2580)),
        "plum" to (Color(0xFF2A0E3D) to Color(0xFF6E2C9A)),
        "violet" to (Color(0xFF1F0D45) to Color(0xFF683AC0)),
        "indigo" to (Color(0xFF140F4A) to Color(0xFF4338B8)),
        "slate" to (Color(0xFF141B26) to Color(0xFF3E4C61)),
        ArtRules.GRAPHITE to (Color(0xFF2A2D33) to Color(0xFF535963))
    )

    val OnArt = Color(0xFFFFFFFF)
    val OnArtSecondary = OnArt.copy(alpha = 0.80f)
    val OnArtDanger = Color(0xFFFFCBC2)
    val OnArtFill = OnArt.copy(alpha = 0.14f)
    val OnArtFillPressed = OnArt.copy(alpha = 0.24f)
    val OnArtFillRing = OnArt.copy(alpha = 0.26f)
    val OnArtPressed = OnArt.copy(alpha = 0.10f)
    val Sheen = OnArt.copy(alpha = 0.20f)
    val EdgeLight = OnArt.copy(alpha = 0.22f)
    val EdgeDark = Color(0x40000000)
    val ExpiredShade = Color(0x3D000000)
    val ContactShadowLight = Color(0x1A0D1117)
    val ContactShadowDark = Color(0x66000000)
    val EmvLight = Color(0xFFF3DFA6)
    val EmvDeep = Color(0xFFC9A24E)
    val EmvEnd = Color(0xFFE8CF8A)
    val EmvLine = Color(0x735A3E0A)
    val EmvShadow = Color(0x40000000)

    /** The art for a stored key or custom `#RRGGBB` (its light stop); expired cards are graphite. */
    fun hue(stored: String, expired: Boolean = false): ArtHue {
        val key = if (expired) ArtRules.GRAPHITE else stored.trim().lowercase(Locale.ROOT)
        hues[key]?.let { (c1, c2) -> return ArtHue(c1, c2, ArtRules.patternFor(key)) }
        val custom = customRgb(key) ?: return hue("navy")
        return ArtHue(
            c1 = opaque(ArtRules.customDeepStop(custom)),
            c2 = opaque(custom),
            pattern = ArtRules.patternFor(key)
        )
    }

    /** The light stop: the colour a flat swatch or chip shows for this key. */
    fun band(stored: String): Color = hue(stored).c2

    private fun customRgb(key: String): Int? {
        val value = key.removePrefix("#")
        if (!value.matches(Regex("[0-9a-f]{6}"))) return null
        return value.toInt(16)
    }

    private fun opaque(rgb: Int): Color = Color(0xFF000000.toInt() or rgb)
}
