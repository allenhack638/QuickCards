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
        if (raw.isNullOrBlank()) return null
        // Split camelCase and separators into words first, so "account" never reads as "cc"
        // and "company" never reads as "pan".
        val words = raw.replace(Regex("([a-z])([A-Z])"), "$1 $2").lowercase()
            .split(Regex("[^a-z0-9]+")).filter { it.isNotEmpty() }
        if (words.isEmpty() || words.any { it.startsWith("account") || it == "iban" || it == "routing" }) return null
        val s = words.joinToString("")
        val card = words.any { it == "cc" || it == "pan" || it.startsWith("card") || it == "cardholder" }
        val expiryWord = words.any { (it.startsWith("exp") && !it.startsWith("expen") && !it.startsWith("export")) || it == "valid" || it == "validity" } ||
            s.contains("validthru") || s.contains("validtill")
        return when {
            words.any { it == "mm" } && words.any { it == "yy" || it == "yyyy" } -> CardFieldKind.EXPIRY_DATE
            s.contains("cvv") || s.contains("cvc") || s.contains("csc") || s.contains("securitycode") ||
                s.contains("cardverification") -> CardFieldKind.CVV
            words == listOf("mm") -> CardFieldKind.EXPIRY_MONTH
            words == listOf("yy") || words == listOf("yyyy") -> CardFieldKind.EXPIRY_YEAR
            expiryWord && (words.any { it.startsWith("month") || it == "mm" } || s.endsWith("mm")) -> CardFieldKind.EXPIRY_MONTH
            expiryWord && (words.any { it.startsWith("year") || it == "yy" || it == "yyyy" } || s.endsWith("yy")) ->
                CardFieldKind.EXPIRY_YEAR
            expiryWord -> CardFieldKind.EXPIRY_DATE
            card && (words.any { it.startsWith("num") || it == "no" } || s.endsWith("no") || s == "pan" ||
                s.contains("cardno")) -> CardFieldKind.NUMBER
            card && words.any { it.contains("holder") || it == "name" } -> CardFieldKind.NAME
            s == "nameoncard" || s == "cardholder" -> CardFieldKind.NAME
            else -> null
        }
    }


    /** Fill values for a card; the year width follows the field's own hint or length when it has one. */
    fun valueFor(
        kind: CardFieldKind,
        number: String,
        expiry: String,
        cvv: String,
        name: String,
        twoDigitYear: Boolean = false,
        fourDigitYear: Boolean = false
    ): String? {
        val parts = expiry.split("/")
        val month = parts.getOrNull(0)?.trim().orEmpty()
        val year = parts.getOrNull(1)?.trim().orEmpty()
        val fullYear = if (year.length == 2) "20$year" else year
        return when (kind) {
            CardFieldKind.NUMBER -> number.filter(Char::isDigit)
            CardFieldKind.EXPIRY_MONTH -> month.ifEmpty { null }
            CardFieldKind.EXPIRY_YEAR -> (if (twoDigitYear) fullYear.takeLast(2) else fullYear).ifEmpty { null }
            CardFieldKind.EXPIRY_DATE -> if (month.isNotEmpty() && year.isNotEmpty()) {
                "$month/${if (fourDigitYear) fullYear else fullYear.takeLast(2)}"
            } else {
                null
            }
            CardFieldKind.CVV -> cvv.ifEmpty { null }
            CardFieldKind.NAME -> name.ifEmpty { null }
        }
    }
}
