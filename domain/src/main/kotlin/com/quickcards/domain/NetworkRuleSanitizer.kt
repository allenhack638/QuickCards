package com.quickcards.domain

import java.util.Locale

/**
 * Bounds untrusted network rules before they reach the database.
 *
 * Rules normally come from the network editor, which validates them. A backup file is also an
 * input, and an imported `lengths` value like `1-2147483647` used to expand into a huge set
 * whenever a card was rendered. [lengths] and [isValidLengths] only ever look at card lengths
 * (12–19) and cap the token count, so a crafted file can neither exhaust memory nor hang the UI.
 */
object NetworkRuleSanitizer {

    const val MAX_PREFIX_SPEC = 128
    const val MAX_LENGTH_SPEC = 64
    const val MAX_TOKENS = 16

    private val RANGE = Regex("""^(\d{1,3})\s*-\s*(\d{1,3})$""")
    private val DIGITS = Regex("""^\d{1,3}$""")
    private val PREFIX_TOKEN = Regex("""^(\d{1,8})(?:-(\d{1,8}))?$""")
    private val CODE_INVALID = Regex("""[^A-Z0-9 ]""")
    private val SPACES = Regex("""\s+""")

    /** A canonical, bounded length spec (`13,16,19`); empty when nothing usable remains. */
    fun lengths(spec: String): String {
        if (spec.length > MAX_LENGTH_SPEC) return ""
        val values = sortedSetOf<Int>()
        spec.replace(SPACES, "").split(",").forEach { raw ->
            if (values.size >= CardNumberRules.MAX_LENGTH - CardNumberRules.MIN_LENGTH + 1) {
                return@forEach
            }
            val token = raw.trim()
            val range = RANGE.matchEntire(token)
            if (range != null) {
                val first = range.groupValues[1].toIntOrNull()
                val second = range.groupValues[2].toIntOrNull()
                if (first != null && second != null && first <= second) {
                    val start = maxOf(CardNumberRules.MIN_LENGTH, first)
                    val end = minOf(CardNumberRules.MAX_LENGTH, second)
                    var length = start
                    while (length <= end) {
                        values.add(length)
                        length++
                    }
                }
            } else {
                token.toIntOrNull()
                    ?.takeIf { it in CardNumberRules.MIN_LENGTH..CardNumberRules.MAX_LENGTH }
                    ?.let(values::add)
            }
        }
        return values.joinToString(",")
    }

    /** Strict form used by the editor: every token must itself be a valid card length. */
    fun isValidLengths(spec: String): Boolean {
        if (spec.isEmpty() || spec.length > MAX_LENGTH_SPEC) return false
        val tokens = spec.replace(SPACES, "").split(",")
        if (tokens.isEmpty() || tokens.size > CardNumberRules.MAX_LENGTH - CardNumberRules.MIN_LENGTH + 1) {
            return false
        }
        var any = false
        for (token in tokens) {
            val range = RANGE.matchEntire(token)
            if (range != null) {
                val first = range.groupValues[1].toIntOrNull() ?: return false
                val second = range.groupValues[2].toIntOrNull() ?: return false
                if (first > second) return false
                if (first !in CardNumberRules.MIN_LENGTH..CardNumberRules.MAX_LENGTH) return false
                if (second !in CardNumberRules.MIN_LENGTH..CardNumberRules.MAX_LENGTH) return false
                any = true
            } else {
                val value = token.toIntOrNull() ?: return false
                if (value !in CardNumberRules.MIN_LENGTH..CardNumberRules.MAX_LENGTH) return false
                any = true
            }
        }
        return any
    }

    /** Keeps only numeric prefixes/ranges, drops junk and caps the spec's size. */
    fun prefixes(spec: String): String {
        if (spec.length > MAX_PREFIX_SPEC * 2) return ""
        val tokens = spec.replace(SPACES, "").split(",")
            .mapNotNull { token ->
                val match = PREFIX_TOKEN.matchEntire(token.trim()) ?: return@mapNotNull null
                val first = match.groupValues[1].toIntOrNull() ?: return@mapNotNull null
                val secondText = match.groupValues[2]
                if (secondText.isEmpty()) {
                    match.groupValues[1]
                } else {
                    val second = secondText.toIntOrNull() ?: return@mapNotNull null
                    if (first <= second) "$first-$second" else "$second-$first"
                }
            }
            .take(MAX_TOKENS)
        return tokens.joinToString(",").take(MAX_PREFIX_SPEC)
    }

    /** A display code that always satisfies the editor's `[A-Z0-9 ]{2,15}` rule. */
    fun code(raw: String): String {
        val cleaned = raw.uppercase(Locale.ROOT)
            .replace(CODE_INVALID, " ")
            .replace(SPACES, " ")
            .trim()
            .take(NetworkLabelMax)
            .trim()
        return if (cleaned.length < 2) "CARD" else cleaned
    }

    /** Keeps a custom grouping only when it is a real grouping of a supported card length. */
    fun groups(spec: String, lengthsSpec: String): String {
        if (spec.length > MAX_LENGTH_SPEC) return ""
        val parts = spec.split("-").mapNotNull { it.trim().toIntOrNull() }
        if (parts.isEmpty() || parts.size > 8) return ""
        if (parts.any { it <= 0 }) return ""
        val total = parts.sumOf { it.toLong() }
        if (total < CardNumberRules.MIN_LENGTH || total > CardNumberRules.MAX_LENGTH) return ""
        val allowed = CardNumberRules.parseLengths(lengthsSpec)
        return if (allowed.any { it.toLong() == total }) parts.joinToString("-") else ""
    }

    /** A bounded display name for an imported holder/bank/type/tag/network. */
    fun entityName(
        kind: EntityKind,
        raw: String,
        tagMaxLength: Int = EntityValidator.DEFAULT_TAG_MAX_LENGTH
    ): String = EntityValidator.normalizeName(kind, raw)
        .take(EntityValidator.nameLimit(kind, tagMaxLength))
}
