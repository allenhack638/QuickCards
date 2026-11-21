package com.quickcards.app.core.data.repository

import com.quickcards.app.core.data.model.Bank
import com.quickcards.domain.BankColours

/** Which colour a bank and a card show (PRODUCT › Card colour; DESIGN › Bank default colours). */
object BankColourRules {

    fun colourOf(bank: Bank): String? = bank.color ?: BankColours.defaultFor(bank.name)

    fun cardColour(manual: Boolean, own: String, bankColour: String?): String =
        if (manual || bankColour == null) own else bankColour

    /** Null for a catalog bank (its built-in default applies); otherwise the auto-picked colour. */
    fun pickFor(name: String, banks: List<Bank>): String? {
        if (BankColours.defaultFor(name) != null) return null
        return BankColours.autoPick(banks.mapNotNull(::colourOf))
    }
}
