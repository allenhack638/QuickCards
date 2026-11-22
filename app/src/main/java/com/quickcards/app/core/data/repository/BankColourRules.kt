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

    /**
     * The colour the bank editor shows, and so the colour saving gives the bank ([before] is the
     * bank as stored, null for a new one): the colour [picked] by hand; else the new name's
     * built-in default when the name maps to a different one; else the bank's current colour;
     * else the built-in default for a catalog name; otherwise the auto-pick.
     */
    fun editorColour(name: String, picked: String?, before: Bank?, otherBanks: List<Bank>): String =
        picked
            ?: renamedDefault(name, before)
            ?: before?.let(::colourOf)
            ?: BankColours.defaultFor(name)
            ?: BankColours.autoPick(otherBanks.mapNotNull(::colourOf))

    /** What saving stores for the bank: null while it follows its built-in default, else [editorColour]. */
    fun colourToStore(name: String, picked: String?, before: Bank?, otherBanks: List<Bank>): String? {
        val default = BankColours.defaultFor(name)
        val followsDefault = picked == null && default != null &&
            (before?.color == null || BankColours.defaultFor(before.name) != default)
        return if (followsDefault) null else editorColour(name, picked, before, otherBanks)
    }

    private fun renamedDefault(name: String, before: Bank?): String? {
        val default = BankColours.defaultFor(name) ?: return null
        return default.takeIf { before == null || BankColours.defaultFor(before.name) != default }
    }
}
