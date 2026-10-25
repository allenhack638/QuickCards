package com.quickcards.domain

/** What a form field asks for, decided from its autofill hints and, failing those, its names. */
enum class CardFieldKind { NUMBER, EXPIRY_MONTH, EXPIRY_YEAR, EXPIRY_DATE, CVV, NAME }

/**
 * Classifies checkout fields without any Android types so the rules are unit-testable.
 * Explicit autofill hints and HTML `autocomplete` tokens win; view ids and hint text are a
 * fallback for apps that never set hints. Anything unrecognised is left alone.
 */
object AutofillFields {

    fun classify(
        autofillHints: List<String>,
        htmlAutocomplete: String?,
        idEntry: String?,
        hintText: String?
    ): CardFieldKind? {
        for (hint in autofillHints) fromHint(hint)?.let { return it }
        htmlAutocomplete?.let { fromHint(it) }?.let { return it }
        return fromName(idEntry) ?: fromName(hintText)
    }

    private fun fromHint(raw: String): CardFieldKind? = when (raw.trim().lowercase()) {
        "creditcardnumber", "cc-number" -> CardFieldKind.NUMBER
        "creditcardexpirationmonth", "cc-exp-month" -> CardFieldKind.EXPIRY_MONTH
        "creditcardexpirationyear", "cc-exp-year" -> CardFieldKind.EXPIRY_YEAR
        "creditcardexpirationdate", "cc-exp" -> CardFieldKind.EXPIRY_DATE
        "creditcardsecuritycode", "cc-csc" -> CardFieldKind.CVV
        "name", "cc-name", "personname" -> CardFieldKind.NAME
        else -> null
    }

    private fun fromName(raw: String?): CardFieldKind? {
        val s = raw?.lowercase()?.replace(Regex("[^a-z0-9]"), "") ?: return null
        if (s.isEmpty()) return null
        val card = s.contains("card") || s.contains("cc") || s.contains("pan")
        return when {
            s.contains("mm") && s.contains("yy") -> CardFieldKind.EXPIRY_DATE
            s.contains("cvv") || s.contains("cvc") || s.contains("csc") || s.contains("securitycode") ||
                s.contains("cardverification") -> CardFieldKind.CVV
            (s.contains("exp") || s.contains("expiry") || s.contains("expiration") || s.contains("valid")) &&
                (s.contains("month") || s.endsWith("mm")) -> CardFieldKind.EXPIRY_MONTH
            (s.contains("exp") || s.contains("expiry") || s.contains("expiration") || s.contains("valid")) &&
                (s.contains("year") || s.endsWith("yy") || s.endsWith("yyyy")) -> CardFieldKind.EXPIRY_YEAR
            s.contains("exp") || s.contains("expiry") || s.contains("expiration") || s.contains("validthru") ||
                s.contains("validtill") -> CardFieldKind.EXPIRY_DATE
            card && (s.contains("number") || s.contains("num") || s.endsWith("no") || s == "pan" ||
                s.contains("cardno")) -> CardFieldKind.NUMBER
            card && (s.contains("holder") || s.contains("name")) -> CardFieldKind.NAME
            s == "nameoncard" || s == "cardholder" -> CardFieldKind.NAME
            else -> null
        }
    }

    /** Fill values for a card; [twoDigitYear] follows the field's own hint when it has one. */
    fun valueFor(kind: CardFieldKind, number: String, expiry: String, cvv: String, name: String, twoDigitYear: Boolean = false): String? {
        val parts = expiry.split("/")
        val month = parts.getOrNull(0)?.trim().orEmpty()
        val year = parts.getOrNull(1)?.trim().orEmpty()
        val fullYear = if (year.length == 2) "20$year" else year
        return when (kind) {
            CardFieldKind.NUMBER -> number.filter(Char::isDigit)
            CardFieldKind.EXPIRY_MONTH -> month.ifEmpty { null }
            CardFieldKind.EXPIRY_YEAR -> (if (twoDigitYear) fullYear.takeLast(2) else fullYear).ifEmpty { null }
            CardFieldKind.EXPIRY_DATE -> if (month.isNotEmpty() && year.isNotEmpty()) "$month/${fullYear.takeLast(2)}" else null
            CardFieldKind.CVV -> cvv.ifEmpty { null }
            CardFieldKind.NAME -> name.ifEmpty { null }
        }
    }
}
