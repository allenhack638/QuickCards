package com.quickcards.app.core.ui.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import java.util.Locale

val BrandRed = Color(0xFFC8202B)
val BrandOnRed = Color(0xFFFFFFFF)

// Network marks sit on a white plate so brand colours read on every band and theme.
val NetworkPlate = Color(0xFFFFFFFF)
val NetworkPlateEdge = Color(0x1F000000)

val DarkBackground = Color(0xFF0B0B0C)
val DarkSurfaceContainerLow = Color(0xFF121214)
val DarkSurfaceContainer = Color(0xFF161618)
val DarkSurfaceContainerHigh = Color(0xFF1F1F22)
val DarkSurfaceContainerHighest = Color(0xFF2A2A2E)
val DarkOnSurface = Color(0xFFF4F4F5)
val DarkOutline = Color(0xFF6E6E76)
val DarkOutlineVariant = Color(0xFF2A2A2E)
val DarkInverseSurface = Color(0xFFF4F4F5)
val DarkInverseOnSurface = Color(0xFF18181B)
val DarkInversePrimary = Color(0xFFB3161F)
val DarkError = Color(0xFFF47C82)
val DarkSelected = Color(0xFFF4F4F5)
val DarkOnSelected = Color(0xFF0B0B0C)
val DarkIndicator = Color(0xFF2A2A2E)
val DarkPassEdgeBase = Color(0xFF45454C)

val DarkPassExpiredBand = Color(0xFF34343A)
val DarkPassExpiredBandEnd = Color(0xFF45454C)
val DarkPassExpiredSheen = Color(0x1AFFFFFF)
val DarkPassExpiredBody = Color(0xFF1B1B1E)
val DarkPassExpiredEdge = Color(0xFF45454C)
val DarkPassExpiredTag = Color(0xFF3F3F46)

val LightBackground = Color(0xFFF7F7F8)
val LightSurfaceContainerLow = Color(0xFFFBFBFC)
val LightSurfaceContainer = Color(0xFFFFFFFF)
val LightSurfaceContainerHigh = Color(0xFFEFEFF2)
val LightSurfaceContainerHighest = Color(0xFFE4E4E9)
val LightOnSurface = Color(0xFF18181B)
val LightOutline = Color(0xFF8A8A93)
val LightOutlineVariant = Color(0xFFE2E2E7)
val LightInverseSurface = Color(0xFF2E2E33)
val LightInverseOnSurface = Color(0xFFF4F4F5)
val LightInversePrimary = Color(0xFFFFB3AE)
val LightError = Color(0xFFB3261E)
val LightSelected = Color(0xFF18181B)
val LightOnSelected = Color(0xFFFFFFFF)
val LightIndicator = Color(0xFFE4E4E9)
val LightPassEdgeBase = Color(0xFFCFCFD6)

val LightPassExpiredBand = Color(0xFF4A4A52)
val LightPassExpiredBandEnd = Color(0xFF5A5A63)
val LightPassExpiredSheen = Color(0x1AFFFFFF)
val LightPassExpiredBody = Color(0xFFEDEDF0)
val LightPassExpiredEdge = Color(0xFFC9C9D1)
val LightPassExpiredTag = Color(0xFF52525B)

@Immutable
data class QcColors(
    val selected: Color,
    val onSelected: Color,
    val indicator: Color,
    val perfLine: Color
)

val DarkQcColors = QcColors(
    selected = DarkSelected,
    onSelected = DarkOnSelected,
    indicator = DarkIndicator,
    perfLine = DarkOnSurface.copy(alpha = 0.24f)
)

val LightQcColors = QcColors(
    selected = LightSelected,
    onSelected = LightOnSelected,
    indicator = LightIndicator,
    perfLine = LightOnSurface.copy(alpha = 0.22f)
)

val LocalQcColors = staticCompositionLocalOf { LightQcColors }

@Immutable
data class ExpiredPassColors(
    val band: Color,
    val bandEnd: Color,
    val sheen: Color,
    val body: Color,
    val edge: Color,
    val tag: Color
)

object PassColors {
    val Navy = Color(0xFF1E3A8A)
    val Rust = Color(0xFF7C2D12)
    val Forest = Color(0xFF14532D)
    val Plum = Color(0xFF4C1D95)
    val Graphite = Color(0xFF3F3F46)
    val Teal = Color(0xFF0F5E5A)
    val Ocean = Color(0xFF0C4A6E)
    val Indigo = Color(0xFF3730A3)
    val Violet = Color(0xFF5B21B6)
    val Magenta = Color(0xFF86198F)
    val Wine = Color(0xFF6B1E3E)
    val Crimson = Color(0xFF9F1239)
    val BurntOrange = Color(0xFF9A3412)
    val Olive = Color(0xFF4D5D1B)
    val Moss = Color(0xFF3F6212)
    val Slate = Color(0xFF334155)

    /** The conic outline of the Custom swatch, drawn from the palette itself. */
    val CustomRing: List<Color> = listOf(
        Crimson, BurntOrange, Olive, Teal, Ocean, Indigo, Violet, Magenta, Crimson
    )

    fun band(key: String): Color {
        val normalized = key.trim().lowercase(Locale.ROOT)
        return when (normalized) {
            "navy" -> Navy
            "rust" -> Rust
            "forest" -> Forest
            "plum" -> Plum
            "graphite" -> Graphite
            "teal" -> Teal
            "ocean" -> Ocean
            "indigo" -> Indigo
            "violet" -> Violet
            "magenta" -> Magenta
            "wine" -> Wine
            "crimson" -> Crimson
            "burnt-orange" -> BurntOrange
            "olive" -> Olive
            "moss" -> Moss
            "slate" -> Slate
            else -> customColor(key) ?: Navy
        }
    }

    private fun customColor(key: String): Color? {
        val value = key.trim().removePrefix("#")
        if (!value.matches(Regex("[0-9a-fA-F]{6}"))) return null
        return Color(0xFF000000L or value.toLong(16))
    }

    fun bodyTint(band: Color, dark: Boolean): Color = lerp(
        if (dark) DarkSurfaceContainer else LightSurfaceContainer,
        band,
        if (dark) 0.12f else 0.06f
    )

    /** color-mix(band N%, surface container) from quickcards-v2.css. */
    fun mix(band: Color, fraction: Float, dark: Boolean): Color = lerp(
        if (dark) DarkSurfaceContainer else LightSurfaceContainer,
        band,
        fraction
    )

    fun edge(band: Color, dark: Boolean): Color = if (dark) {
        lerp(DarkPassEdgeBase, band, 0.60f)
    } else {
        lerp(LightPassEdgeBase, band, 0.35f)
    }

    fun perforation(dark: Boolean): Color = if (dark) {
        DarkOnSurface.copy(alpha = 0.24f)
    } else {
        LightOnSurface.copy(alpha = 0.22f)
    }

    /** The six neutral tokens that replace every hue on an expired pass. */
    fun expired(dark: Boolean): ExpiredPassColors = if (dark) {
        ExpiredPassColors(
            band = DarkPassExpiredBand,
            bandEnd = DarkPassExpiredBandEnd,
            sheen = DarkPassExpiredSheen,
            body = DarkPassExpiredBody,
            edge = DarkPassExpiredEdge,
            tag = DarkPassExpiredTag
        )
    } else {
        ExpiredPassColors(
            band = LightPassExpiredBand,
            bandEnd = LightPassExpiredBandEnd,
            sheen = LightPassExpiredSheen,
            body = LightPassExpiredBody,
            edge = LightPassExpiredEdge,
            tag = LightPassExpiredTag
        )
    }
}
