package com.quickcards.domain

/** What autofill needs to order a card; no card data. */
data class AutofillCandidate(
    val id: String,
    val pinnedAt: Long?,
    val sortOrder: Int,
    val lastUsed: Long?
)

/** Which cards autofill suggests, in what order, and what typing in the number field narrows. */
object AutofillChoice {

    const val MAX_SUGGESTIONS = 5
    private const val LEADING_DIGITS = 6
    private const val TRAILING_DIGITS = 4

    /** Pinned cards in pin order, then the most recently used, then the list order. */
    fun rank(cards: List<AutofillCandidate>): List<AutofillCandidate> = cards.sortedWith(
        compareBy<AutofillCandidate> { it.pinnedAt == null }
            .thenBy { it.pinnedAt ?: Long.MAX_VALUE }
            .thenByDescending { it.lastUsed ?: Long.MIN_VALUE }
            .thenBy { it.sortOrder }
    )

    fun top(cards: List<AutofillCandidate>): List<AutofillCandidate> = rank(cards).take(MAX_SUGGESTIONS)

    /**
     * A regex the system matches against what the user types in the number field: a prefix of
     * the first six digits or of the last four, spaces allowed. Only those ten digits, the part
     * a card already shows publicly, ever leave the vault in a filter.
     */
    fun typeFilter(number: String): String {
        val digits = number.filter(Char::isDigit)
        fun prefixes(part: String): String =
            part.reversed().fold("") { inner, c -> "(?:$c\\s?$inner)?" }
        val first = prefixes(digits.take(LEADING_DIGITS))
        val last = prefixes(digits.takeLast(TRAILING_DIGITS))
        return "^\\s*(?:$first|$last)\\s*$"
    }
}
