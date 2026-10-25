package com.quickcards.domain

import java.util.Locale

/**
 * A card value that the legacy repair job can rewrite. [NUMBER], [EXPIRY] and
 * [CVV] map to the `needsReview` bitmask stored on `cards` (1, 2 and 4).
 */
enum class CardField {
    NUMBER,
    EXPIRY,
    CVV
}

/** Encodes a [CardField] set as the `cards.needsReview` bitmask. */
object CardFieldMask {

    const val NUMBER = 1
    const val EXPIRY = 2
    const val CVV = 4

    fun encode(fields: Set<CardField>): Int = fields.fold(0) { mask, field ->
        mask or when (field) {
            CardField.NUMBER -> NUMBER
            CardField.EXPIRY -> EXPIRY
            CardField.CVV -> CVV
        }
    }

    fun decode(mask: Int): Set<CardField> = buildSet {
        if (mask and NUMBER != 0) add(CardField.NUMBER)
        if (mask and EXPIRY != 0) add(CardField.EXPIRY)
        if (mask and CVV != 0) add(CardField.CVV)
    }
}

data class NormalizedCard(
    val number: String,
    val expiry: String,
    val cvv: String,
    val fixed: Map<CardField, Pair<String, String>>,
    val needsReview: Set<CardField>
)

/**
 * Normalizes values stored by the pre-v2 app. It only repairs a value it can
 * prove valid against the phone's network rules; anything else is returned
 * unchanged and listed in [NormalizedCard.needsReview]. A clean value never
 * appears in [NormalizedCard.fixed], so running this twice changes nothing.
 *
 * Past expiry dates are valid: an expired card is real data. The output number
 * is digits only (the DB's storage form); grouping stays a display concern.
 */
object LegacyCardNormalizer {

    private val EXPIRY_SEPARATORS = setOf(' ', '/', '-', '.')
    private val DIGIT_GROUPS = Regex("""\d+""")
    private val CVV_SEPARATORS = Regex("""\s+""")

    fun normalize(
        number: String,
        expiry: String,
        cvv: String,
        networks: List<CardNetwork>
    ): NormalizedCard {
        val fixed = linkedMapOf<CardField, Pair<String, String>>()
        val needsReview = linkedSetOf<CardField>()

        val normalizedNumber = normalizeNumber(number, networks)
        if (normalizedNumber == null) {
            needsReview += CardField.NUMBER
        } else if (normalizedNumber != number) {
            fixed[CardField.NUMBER] = number to normalizedNumber
        }

        val normalizedExpiry = normalizeExpiry(expiry)
        if (normalizedExpiry == null) {
            needsReview += CardField.EXPIRY
        } else if (normalizedExpiry != expiry) {
            fixed[CardField.EXPIRY] = expiry to normalizedExpiry
        }

        val network = normalizedNumber
            ?.let { CardNumberRules.detect(it, networks) }
            ?: CardNumberRules.detect(number.filter { char -> char in '0'..'9' }, networks)
        val normalizedCvv = normalizeCvv(cvv, network)
        if (normalizedCvv == null) {
            needsReview += CardField.CVV
        } else if (normalizedCvv != cvv) {
            fixed[CardField.CVV] = cvv to normalizedCvv
        }

        return NormalizedCard(
            number = normalizedNumber ?: number,
            expiry = normalizedExpiry ?: expiry,
            cvv = normalizedCvv ?: cvv,
            fixed = fixed,
            needsReview = needsReview
        )
    }

    private fun normalizeNumber(raw: String, networks: List<CardNetwork>): String? {
        val digits = CardTextParser.numberDigits(raw) ?: return null
        if (digits.length !in 12..19) return null

        val network = CardNumberRules.detect(digits, networks)
        val lengthOk = network == null ||
            digits.length in CardNumberRules.parseLengths(network.lengths)
        if (!lengthOk) return null

        val luhnOk = when {
            network == null -> CardNumberRules.isLuhnValid(digits)
            !network.luhn -> true
            else -> CardNumberRules.isLuhnValid(digits)
        }
        return if (luhnOk) digits else null
    }

    /**
     * Accepts MM/YY, M/YY, MM//YY, MM-YY, MM.YY, MM YY, MMYY, MM/YYYY,
     * MMYYYY, MM / YY, YYYY-MM and YY/MM (only when the first part is greater
     * than 12 and the second is at most 12). Output is always MM/YY.
     */
    private fun normalizeExpiry(raw: String): String? {
        val value = raw.trim()
        if (value.isEmpty()) return null
        if (value.any { !it.isDigit() && it !in EXPIRY_SEPARATORS && !it.isWhitespace() }) {
            return null
        }

        val groups = DIGIT_GROUPS.findAll(value).map { it.value }.toList()
        if (groups.isEmpty()) return null

        val monthYear = when {
            groups.size == 1 -> normalizeSingleExpiryGroup(groups.single())
            groups.size == 2 -> normalizeTwoExpiryGroups(groups[0], groups[1])
            else -> null
        }
        return monthYear?.let { (month, year) -> canonicalExpiry(month, year) }
    }

    private fun normalizeSingleExpiryGroup(group: String): Pair<Int, Int>? = when (group.length) {
        4 -> group.substring(0, 2).toIntOrNull()?.let { month ->
            group.substring(2, 4).toIntOrNull()?.let { year -> month to year }
        }

        6 -> group.substring(0, 2).toIntOrNull()?.let { month ->
            group.substring(2, 6).toIntOrNull()?.let { year ->
                if (month in 1..12) month to year else {
                    group.substring(4, 6).toIntOrNull()?.let { fallbackMonth ->
                        group.substring(0, 4).toIntOrNull()?.let { fallbackYear ->
                            fallbackMonth to fallbackYear
                        }
                    }
                }
            }
        }

        else -> null
    }

    private fun normalizeTwoExpiryGroups(first: String, second: String): Pair<Int, Int>? {
        if (first.length == 4) {
            val year = first.toIntOrNull() ?: return null
            val month = second.toIntOrNull() ?: return null
            return if (second.length in 1..2) month to year else null
        }
        if (second.length == 4) {
            val month = first.toIntOrNull() ?: return null
            val year = second.toIntOrNull() ?: return null
            return if (first.length in 1..2) month to year else null
        }
        if (first.length !in 1..2 || second.length != 2) return null

        val firstValue = first.toIntOrNull() ?: return null
        val secondValue = second.toIntOrNull() ?: return null
        return when {
            firstValue in 1..12 -> firstValue to secondValue
            firstValue in 13..99 && secondValue in 1..12 -> secondValue to firstValue
            else -> null
        }
    }

    private fun canonicalExpiry(month: Int, year: Int): String? {
        if (month !in 1..12) return null
        val fullYear = when {
            year in 0..99 -> 2000 + year
            year in 1900..2999 -> year
            else -> return null
        }
        return String.format(Locale.ROOT, "%02d/%02d", month, fullYear % 100)
    }

    private fun normalizeCvv(raw: String, network: CardNetwork?): String? {
        val compact = raw.replace(CVV_SEPARATORS, "")
        if (compact.isEmpty() || compact.any { it !in '0'..'9' }) return null
        val expected = network?.cvvLength
        val validLength = if (expected != null) {
            compact.length == expected
        } else {
            compact.length in 3..4
        }
        return if (validLength) compact else null
    }
}
