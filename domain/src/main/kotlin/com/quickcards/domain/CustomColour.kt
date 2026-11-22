package com.quickcards.domain

import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * Custom card colours (DESIGN.md › Card colour): a hue and a lightness on fixed saturation give
 * the light stop; white card text must keep 7:1 on it.
 */
object CustomColour {
    private const val SATURATION = 0.58
    private const val L0 = 0.14
    private const val L1 = 0.72
    const val DEFAULT_HUE = 248f
    const val DEFAULT_LIGHTNESS = 0.4f

    /** The light stop `#RRGGBB` for a hue (0–360) and a lightness position (0–1). */
    fun hex(hue: Float, t: Float): String = format(hsl(hue.toDouble(), SATURATION, L0 + (L1 - L0) * t))

    /** Strip colours: the hue strip at 40% lightness, the lightness strip at its ends and middle. */
    fun hueStop(hue: Float): String = format(hsl(hue.toDouble(), SATURATION, 0.40))

    fun readable(hex: String): Boolean = parse(hex)?.let { ArtRules.contrastWithWhite(it) >= ArtRules.MIN_DIGIT_CONTRAST } ?: false

    /** The lightness position where white text drops below 7:1 for this hue. */
    fun limit(hue: Float): Float {
        for (i in 0..STEPS) {
            if (!readable(hex(hue, i / STEPS.toFloat()))) return ((i - 1) / STEPS.toFloat()).coerceAtLeast(0f)
        }
        return 1f
    }

    /** Hue and lightness position of any hex, for the strip thumbs. */
    fun position(hex: String): Pair<Float, Float> {
        val rgb = parse(hex) ?: return DEFAULT_HUE to DEFAULT_LIGHTNESS
        val r = (rgb shr 16 and 0xFF) / 255.0
        val g = (rgb shr 8 and 0xFF) / 255.0
        val b = (rgb and 0xFF) / 255.0
        val hi = max(r, max(g, b))
        val lo = min(r, min(g, b))
        val d = hi - lo
        val h = when {
            d == 0.0 -> DEFAULT_HUE.toDouble()
            hi == r -> 60 * (((g - b) / d).mod(6.0))
            hi == g -> 60 * ((b - r) / d + 2)
            else -> 60 * ((r - g) / d + 4)
        }
        val t = (((hi + lo) / 2 - L0) / (L1 - L0)).coerceIn(0.0, 1.0)
        return h.toFloat() to t.toFloat()
    }

    fun parse(hex: String?): Int? {
        val digits = hex?.trim()?.removePrefix("#") ?: return null
        if (!digits.matches(HEX)) return null
        return digits.toInt(16)
    }

    private fun format(rgb: Int): String = String.format(Locale.ROOT, "#%06X", rgb)

    private fun hsl(h: Double, s: Double, l: Double): Int {
        val a = s * min(l, 1 - l)
        fun f(n: Int): Int {
            val k = (n + h / 30) % 12
            val v = l - a * max(-1.0, min(k - 3, min(9 - k, 1.0)))
            return (v * 255).roundToInt().coerceIn(0, 255)
        }
        return (f(0) shl 16) or (f(8) shl 8) or f(4)
    }

    private const val STEPS = 200
    private val HEX = Regex("[0-9a-fA-F]{6}")
}
