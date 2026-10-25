package com.quickcards.domain

import java.time.YearMonth

enum class Field {
    HOLDER,
    NUMBER,
    EXPIRY,
    CVV,
    BANK
}

sealed interface CardError {
    data object HolderRequired : CardError
    data object NumberRequired : CardError
    data class NetworkLength(val networkName: String, val lengths: String) : CardError
    data object NumberLength : CardError
    data object Luhn : CardError
    data class Duplicate(val title: String, val holderName: String?) : CardError
    data object ExpiryFormat : CardError
    data object Expired : CardError
    data object ExpiryYear : CardError
    data class CvvLength(val digits: Int) : CardError
    data object CvvUnknown : CardError
    data object BankRequired : CardError
}

data class CardDraft(
    val holderId: String? = null,
    val number: String = "",
    val expiry: String = "",
    val cvv: String = "",
    val bankId: String? = null,
    val typeId: String? = null,
    val tagIds: List<String> = emptyList(),
    val note: String = "",
    val dueDay: Int? = null
)

data class ExistingCardRef(
    val title: String,
    val holderName: String? = null
)

object CardValidator {

    const val NOTE_MAX_LENGTH = 80

    private val EXPIRY = Regex("""^(\d{2})/(\d{2})$""")

    fun validate(
        draft: CardDraft,
        networks: List<CardNetwork> = DefaultNetworks.all,
        today: YearMonth = YearMonth.now(),
        existing: (String) -> ExistingCardRef? = { null }
    ): Map<Field, CardError> {
        val errors = linkedMapOf<Field, CardError>()
        val network = CardNumberRules.detect(draft.number, networks)

        if (draft.holderId.isNullOrEmpty()) {
            errors[Field.HOLDER] = CardError.HolderRequired
        }

        if (draft.number.isEmpty()) {
            errors[Field.NUMBER] = CardError.NumberRequired
        } else {
            val lengths = network?.let { CardNumberRules.parseLengths(it.lengths) } ?: emptyList()
            when {
                network != null && draft.number.length !in lengths ->
                    errors[Field.NUMBER] = CardError.NetworkLength(
                        networkName = network.name,
                        lengths = CardNumberRules.lengthsText(network.lengths)
                    )

                network == null && (draft.number.length < 12 || draft.number.length > 19) ->
                    errors[Field.NUMBER] = CardError.NumberLength

                (network?.luhn ?: true) && !CardNumberRules.isLuhnValid(draft.number) ->
                    errors[Field.NUMBER] = CardError.Luhn

                else -> {
                    val match = existing(draft.number)
                    if (match != null) {
                        errors[Field.NUMBER] = CardError.Duplicate(
                            title = match.title,
                            holderName = match.holderName?.takeIf { it.isNotEmpty() }
                        )
                    }
                }
            }
        }

        validateExpiry(draft.expiry, today)?.let { errors[Field.EXPIRY] = it }

        val wantedCvv = network?.cvvLength
        val invalidCvv = if (wantedCvv != null) {
            draft.cvv.length != wantedCvv
        } else {
            draft.cvv.length !in 3..4
        }
        if (invalidCvv) {
            errors[Field.CVV] = if (wantedCvv != null) {
                CardError.CvvLength(wantedCvv)
            } else {
                CardError.CvvUnknown
            }
        }

        if (draft.bankId.isNullOrEmpty()) {
            errors[Field.BANK] = CardError.BankRequired
        }

        return errors
    }

    fun normalizeExpiry(input: String): String {
        var digits = input.filter { it in '0'..'9' }
        if (digits.length == 6) digits = digits.take(2) + digits.takeLast(2)
        digits = digits.take(4)
        if (digits.length == 1 && digits[0] > '1') digits = "0$digits"
        return if (digits.length > 2) digits.take(2) + "/" + digits.drop(2) else digits
    }

    private fun validateExpiry(expiry: String, today: YearMonth): CardError? {
        val match = EXPIRY.matchEntire(expiry)
        val month = match?.groupValues?.get(1)?.toIntOrNull()
        val shortYear = match?.groupValues?.get(2)?.toIntOrNull()
        if (month == null || shortYear == null || month !in 1..12) return CardError.ExpiryFormat

        val year = 2000 + shortYear
        return when {
            year < today.year || (year == today.year && month < today.monthValue) ->
                CardError.Expired

            year > today.year + 20 -> CardError.ExpiryYear
            else -> null
        }
    }
}
