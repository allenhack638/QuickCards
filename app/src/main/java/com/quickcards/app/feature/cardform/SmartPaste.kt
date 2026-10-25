package com.quickcards.app.feature.cardform

import com.quickcards.domain.CardNetwork
import com.quickcards.domain.CardTextParser
import java.time.YearMonth

enum class SmartPasteField { NUMBER, EXPIRY }

/**
 * What a pasted or keyboard-scanned input should put into the form. Null
 * values mean "nothing recognised": the caller then applies that field's
 * normal digit filter.
 */
data class SmartPasteResult(
    val number: String? = null,
    val expiry: String? = null
)

/**
 * Recognises a single input event that inserted two or more characters (a
 * paste or a keyboard's scan commit) and turns it into a card number and an
 * expiry. A CVV is never returned: it is never read from pasted text.
 *
 * The field's own rules still run afterwards through the normal form updates,
 * so grouping, network detection, limits and validation are unchanged. A
 * single typed character returns an empty result, which keeps typing identical.
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
        // A paste replaces the field in one event. Parsing the resulting text
        // (rather than only the inserted slice) keeps a full replacement intact
        // when its tail happens to match the old value.
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

    /**
     * Compares the text before and after one input event. Typing or deleting a
     * single character leaves one character in the changed middle; a paste
     * leaves two or more.
     */
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
