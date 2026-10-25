package com.quickcards.app.core.data.model

import java.util.Locale

/**
 * The pass bands: fifteen curated keys plus a stored custom `#RRGGBB`.
 *
 * The card's `bandColor` column is TEXT, so a custom colour needs no schema
 * change. Legacy cards stored arbitrary hex colours; [nearest] maps those onto
 * the closest curated band, and [importValue] keeps a readable hex exactly.
 */
object BandColor {

    const val NAVY = "navy"
    const val RUST = "rust"
    const val FOREST = "forest"
    const val PLUM = "plum"
    const val GRAPHITE = "graphite"
    const val TEAL = "teal"
    const val OCEAN = "ocean"
    const val INDIGO = "indigo"
    const val VIOLET = "violet"
    const val MAGENTA = "magenta"
    const val WINE = "wine"
    const val CRIMSON = "crimson"
    const val BURNT_ORANGE = "burnt-orange"
    const val OLIVE = "olive"
    const val MOSS = "moss"
    const val SLATE = "slate"

    /**
     * Grid order: the original five first, then the ten additions. Fifteen
     * curated bands plus the Custom swatch fill two rows of eight. [SLATE] left
     * the grid (too close to graphite) but still resolves for stored cards.
     */
    val ALL = listOf(
        NAVY, RUST, FOREST, PLUM, GRAPHITE,
        TEAL, OCEAN, INDIGO, VIOLET, MAGENTA,
        WINE, CRIMSON, BURNT_ORANGE, OLIVE, MOSS
    )

    /** Minimum contrast for the white band header and tag chips. */
    const val MIN_CONTRAST = 4.5

    private val HEX = mapOf(
        NAVY to "#1E3A8A",
        RUST to "#7C2D12",
        FOREST to "#14532D",
        PLUM to "#4C1D95",
        GRAPHITE to "#3F3F46",
        TEAL to "#0F5E5A",
        OCEAN to "#0C4A6E",
        INDIGO to "#3730A3",
        VIOLET to "#5B21B6",
        MAGENTA to "#86198F",
        WINE to "#6B1E3E",
        CRIMSON to "#9F1239",
        BURNT_ORANGE to "#9A3412",
        OLIVE to "#4D5D1B",
        MOSS to "#3F6212",
        SLATE to "#334155"
    )

    private val CUSTOM = Regex("[0-9a-fA-F]{6}")
    private val ANY_HEX = Regex("[0-9a-fA-F]+")

    fun isCurated(key: String): Boolean =
        HEX.containsKey(key.trim().lowercase(Locale.ROOT))

    /**
     * The colour behind a stored band key. A curated key resolves to its hex;
     * a stored custom `#RRGGBB` is returned as-is; anything else is navy.
     */
    fun hexFor(band: String): String {
        val key = band.trim().lowercase(Locale.ROOT)
        HEX[key]?.let { return it }
        return parseHex(band) ?: HEX.getValue(NAVY)
    }

    /** Normalises a six-digit hex to uppercase `#RRGGBB`, or null when invalid. */
    fun parseHex(value: String?): String? {
        val trimmed = value?.trim().orEmpty().removePrefix("#")
        if (!trimmed.matches(CUSTOM)) return null
        return "#${trimmed.uppercase(Locale.ROOT)}"
    }

    fun formatHex(red: Int, green: Int, blue: Int): String = String.format(
        Locale.ROOT,
        "#%02X%02X%02X",
        red.coerceIn(0, 255),
        green.coerceIn(0, 255),
        blue.coerceIn(0, 255)
    )

    /** WCAG 2.1 contrast ratio between [value] and white, or null when invalid. */
    fun contrastWithWhite(value: String?): Double? {
        val colour = parseColour(value) ?: return null
        val luminance = relativeLuminance(colour)
        return 1.05 / (luminance + 0.05)
    }

    fun isReadable(value: String?): Boolean =
        (contrastWithWhite(value) ?: 0.0) >= MIN_CONTRAST

    /**
     * Import mapping: keep a readable custom hex exactly, otherwise snap the
     * colour to the nearest of the fifteen curated bands.
     */
    fun importValue(value: String?): String {
        val trimmed = value?.trim().orEmpty()
        if (trimmed.isEmpty()) return NAVY
        val lower = trimmed.lowercase(Locale.ROOT)
        if (lower in ALL) return lower
        val custom = parseHex(trimmed)
            ?: parseColour(trimmed)?.let { formatHex(it.first, it.second, it.third) }
            ?: return NAVY
        return if (isReadable(custom)) custom else nearest(custom)
    }

    fun nearest(hex: String?): String {
        val colour = parseColour(hex) ?: return NAVY
        var best = NAVY
        var bestDistance = Long.MAX_VALUE
        for (band in ALL) {
            val candidate = parseColour(HEX.getValue(band)) ?: continue
            val distance = distanceSquared(colour, candidate)
            if (distance < bestDistance) {
                best = band
                bestDistance = distance
            }
        }
        return best
    }

    private fun relativeLuminance(colour: Triple<Int, Int, Int>): Double {
        fun channel(value: Int): Double {
            val scaled = value / 255.0
            return if (scaled <= 0.04045) scaled / 12.92 else Math.pow((scaled + 0.055) / 1.055, 2.4)
        }

        val red = channel(colour.first)
        val green = channel(colour.second)
        val blue = channel(colour.third)
        return 0.2126 * red + 0.7152 * green + 0.0722 * blue
    }

    private fun parseColour(hex: String?): Triple<Int, Int, Int>? {
        if (hex == null) return null
        var value = hex.trim().removePrefix("#")
        if (!value.matches(ANY_HEX)) return null
        value = when (value.length) {
            3 -> value.map { "$it$it" }.joinToString("")
            6 -> value
            8 -> value.takeLast(6)
            else -> return null
        }
        val red = value.substring(0, 2).toInt(16)
        val green = value.substring(2, 4).toInt(16)
        val blue = value.substring(4, 6).toInt(16)
        return Triple(red, green, blue)
    }

    private fun distanceSquared(
        first: Triple<Int, Int, Int>,
        second: Triple<Int, Int, Int>
    ): Long {
        val red = (first.first - second.first).toLong()
        val green = (first.second - second.second).toLong()
        val blue = (first.third - second.third).toLong()
        return red * red + green * green + blue * blue
    }
}
