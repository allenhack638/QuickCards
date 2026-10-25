package com.quickcards.app.feature.cardform

import com.quickcards.domain.CardNetwork
import com.quickcards.domain.CardTextParser
import java.time.YearMonth

enum class SmartPasteField { NUMBER, EXPIRY }

/** Null means nothing recognised; the caller then applies the field's normal filter. */
data class SmartPasteResult(
    val number: String? = null,
    val expiry: String? = null
)

/**
 * Turns a multi-character insert (paste or scan commit) into a number and expiry, never a
 * CVV. Field rules still run afterwards; single typed characters return an empty result.
 */
object SmartPaste {

    fun apply(
        previous: String,
        incoming: String,
        field: SmartPasteField,
        networks: List<CardNetwork>,
        today: YearMonth = YearMonth.now(),
        expiryEmpty: Boolean = true
    ): SmartPasteResult {
        if (!insertedAtLeastTwoCharacters(previous, incoming)) return SmartPasteResult()
        // Parse the whole result, not the inserted slice, so a replacement whose tail
        // matches the old value stays intact.
        val parsed = CardTextParser.parse(incoming.lines(), networks, today)
        return when (field) {
            SmartPasteField.NUMBER -> {
                if (parsed.number == null) {
                    SmartPasteResult()
                } else {
                    SmartPasteResult(
                        number = parsed.number,
                        expiry = if (expiryEmpty) parsed.expiry else null
                    )
                }
            }

            SmartPasteField.EXPIRY -> SmartPasteResult(expiry = parsed.expiry)
        }
    }

    /** A paste changes two or more characters in one event; typing changes one. */
    private fun insertedAtLeastTwoCharacters(previous: String, incoming: String): Boolean {
        var prefix = 0
        val prefixLimit = minOf(previous.length, incoming.length)
        while (prefix < prefixLimit && previous[prefix] == incoming[prefix]) prefix++

        var suffix = 0
        val suffixLimit = minOf(previous.length - prefix, incoming.length - prefix)
        while (suffix < suffixLimit &&
            previous[previous.length - 1 - suffix] == incoming[incoming.length - 1 - suffix]
        ) {
            suffix++
        }
        return incoming.length - prefix - suffix >= 2
    }
}
