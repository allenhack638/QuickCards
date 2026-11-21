package com.quickcards.domain

import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt

/** The stroke pattern drawn over card art; the order is the custom-colour hash order. */
enum class ArtPattern { RINGS, LINES, CONTOURS, ROSETTE, WEAVE }

/** Card-art rules shared by every surface (DESIGN.md › Card Art). Keys are the stored `cardColor` values. */
object ArtRules {

    const val GRAPHITE = "graphite"

    /** White digits on art must reach this ratio against the light stop. */
    const val MIN_DIGIT_CONTRAST = 7.0

    /** Fixed palette order (cool first); graphite is excluded because it marks expired cards. */
    val PALETTE_ORDER: List<String> = listOf(
        "navy", "ocean", "teal", "forest", "moss", "olive", "burnt-orange", "rust",
        "crimson", "wine", "magenta", "plum", "violet", "indigo", "slate"
    )

    private val patterns: Map<String, ArtPattern> = mapOf(
        "navy" to ArtPattern.RINGS,
        "ocean" to ArtPattern.ROSETTE,
        "teal" to ArtPattern.CONTOURS,
        "forest" to ArtPattern.LINES,
        "moss" to ArtPattern.WEAVE,
        "olive" to ArtPattern.CONTOURS,
        "burnt-orange" to ArtPattern.ROSETTE,
        "rust" to ArtPattern.LINES,
        "crimson" to ArtPattern.WEAVE,
        "wine" to ArtPattern.LINES,
        "magenta" to ArtPattern.RINGS,
        "plum" to ArtPattern.WEAVE,
        "violet" to ArtPattern.ROSETTE,
        "indigo" to ArtPattern.CONTOURS,
        "slate" to ArtPattern.RINGS,
        GRAPHITE to ArtPattern.LINES
    )

    private val HEX = Regex("#?[0-9a-fA-F]{6}")

    /** A band's fixed pattern; a custom `#RRGGBB` (the light stop) hashes to a stable one. */
    fun patternFor(stored: String): ArtPattern {
        val key = stored.trim().lowercase(Locale.ROOT)
        patterns[key]?.let { return it }
        if (!key.matches(HEX)) return ArtPattern.RINGS
        val entries = ArtPattern.entries
        return entries[(fnv1a(key.removePrefix("#")) % entries.size).toInt()]
    }

    /** FNV-1a 32-bit of the upper-case hex digits, as an unsigned value. */
    fun fnv1a(hexDigits: String): Long {
        var hash = 0x811c9dc5L
        for (ch in hexDigits.uppercase(Locale.ROOT)) {
            hash = hash xor ch.code.toLong()
            hash = (hash * 0x01000193L) and 0xFFFFFFFFL
        }
        return hash
    }

    /** The deep stop for a custom light stop: same hue, saturation 66%, lightness × 0.38. */
    fun customDeepStop(lightRgb: Int): Int {
        val r = (lightRgb shr 16 and 0xFF) / 255.0
        val g = (lightRgb shr 8 and 0xFF) / 255.0
        val b = (lightRgb and 0xFF) / 255.0
        val hi = max(r, max(g, b))
        val lo = min(r, min(g, b))
        val l = (hi + lo) / 2
        val d = hi - lo
        val h = when {
            d == 0.0 -> 0.0
            hi == r -> 60 * (((g - b) / d).mod(6.0))
            hi == g -> 60 * ((b - r) / d + 2)
            else -> 60 * ((r - g) / d + 4)
        }
        return hsl(h, 0.66, l * 0.38)
    }

    /** WCAG 2.1 contrast of white over an opaque `0xRRGGBB` colour. */
    fun contrastWithWhite(rgb: Int): Double = 1.05 / (luminance(rgb) + 0.05)

    private fun luminance(rgb: Int): Double {
        fun channel(v: Int): Double {
            val s = v / 255.0
            return if (s <= 0.04045) s / 12.92 else ((s + 0.055) / 1.055).pow(2.4)
        }
        return 0.2126 * channel(rgb shr 16 and 0xFF) +
            0.7152 * channel(rgb shr 8 and 0xFF) +
            0.0722 * channel(rgb and 0xFF)
    }

    private fun hsl(h: Double, s: Double, l: Double): Int {
        val a = s * min(l, 1 - l)
        fun f(n: Int): Int {
            val k = (n + h / 30) % 12
            val v = l - a * max(-1.0, min(k - 3, min(9 - k, 1.0)))
            return (v * 255).roundToInt().coerceIn(0, 255)
        }
        return (f(0) shl 16) or (f(8) shl 8) or f(4)
    }
}
