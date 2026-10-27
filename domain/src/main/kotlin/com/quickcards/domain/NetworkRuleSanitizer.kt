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
        return CardNumberRules.parseLengths(spec).joinToString(",")
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

    /** Keeps whole numeric prefixes/ranges only, dropping junk, reversed ranges and overflow. */
    fun prefixes(spec: String): String {
        if (spec.length > MAX_PREFIX_SPEC * 2) return ""
        val kept = mutableListOf<String>()
        var joinedLength = 0
        for (raw in spec.replace(SPACES, "").split(",")) {
            val token = raw.trim()
            val match = PREFIX_TOKEN.matchEntire(token) ?: continue
            val firstText = match.groupValues[1]
            val secondText = match.groupValues[2]
            val normalized = if (secondText.isEmpty()) {
                firstText
            } else {
                val first = firstText.toIntOrNull() ?: continue
                val second = secondText.toIntOrNull() ?: continue
                if (first > second) continue
                "$firstText-$secondText"
            }
            val extra = if (kept.isEmpty()) normalized.length else normalized.length + 1
            if (joinedLength + extra > MAX_PREFIX_SPEC) break
            kept += normalized
            joinedLength += extra
            if (kept.size >= MAX_TOKENS) break
        }
        return kept.joinToString(",")
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

    /** Stable comparison key shared by imported and existing entity names. */
    fun entityKey(
        kind: EntityKind,
        raw: String,
        tagMaxLength: Int = EntityValidator.DEFAULT_TAG_MAX_LENGTH
    ): String = entityName(kind, raw, tagMaxLength).lowercase(Locale.ROOT)

    /** A bounded display name for an imported holder/bank/type/tag/network. */
    fun entityName(
        kind: EntityKind,
        raw: String,
        tagMaxLength: Int = EntityValidator.DEFAULT_TAG_MAX_LENGTH
    ): String = EntityValidator.normalizeName(kind, raw)
        .takeCodePoints(EntityValidator.nameLimit(kind, tagMaxLength))

    private fun String.takeCodePoints(max: Int): String {
        if (max <= 0) return ""
        if (codePointCount(0, length) <= max) return this
        return substring(0, offsetByCodePoints(0, max))
    }
}
