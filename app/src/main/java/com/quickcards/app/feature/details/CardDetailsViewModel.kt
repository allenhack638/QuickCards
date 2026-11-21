package com.quickcards.app.feature.details

import androidx.lifecycle.ViewModel
import com.quickcards.app.core.ui.HomeCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The Card details rows, in screen order. */
enum class DetailsField { NUMBER, EXPIRY, CVV, HOLDER, NOTE }

/** Why the unlock prompt is shown; each maps to its own prompt subtitle. */
enum class UnlockReason { COPY, COPY_NOTE, SHOW_NUMBER, SHOW_CVV }

/** The biometric gate; false for a cancel, a failure or no lock (fail-closed). */
fun interface Unlock {
    suspend fun request(reason: UnlockReason): Boolean
}

/** Puts one value on the clipboard (sensitive, timed) and records the card's use. */
fun interface DetailsCopier {
    fun copy(card: HomeCard, field: DetailsField, value: String)
}

/** Copy and reveal on Card details: both pass the unlock gate first and change nothing when it fails. */
class CardDetailsViewModel(private val copier: DetailsCopier) : ViewModel() {

    private val _copied = MutableStateFlow<DetailsField?>(null)
    val copied: StateFlow<DetailsField?> = _copied.asStateFlow()

    private val _revealed = MutableStateFlow<DetailsField?>(null)
    val revealed: StateFlow<DetailsField?> = _revealed.asStateFlow()

    suspend fun copy(card: HomeCard, field: DetailsField, unlock: Unlock): Boolean {
        val value = valueOf(card, field) ?: return false
        val reason = if (field == DetailsField.NOTE) UnlockReason.COPY_NOTE else UnlockReason.COPY
        if (!unlock.request(reason)) return false
        copier.copy(card, field, value)
        _copied.value = field
        return true
    }

    suspend fun reveal(card: HomeCard, field: DetailsField, unlock: Unlock): Boolean {
        if (!canReveal(card, field)) return false
        val reason = if (field == DetailsField.CVV) UnlockReason.SHOW_CVV else UnlockReason.SHOW_NUMBER
        if (!unlock.request(reason)) return false
        _revealed.value = field
        return true
    }

    fun clearCopied(field: DetailsField) {
        _copied.compareAndSet(field, null)
    }

    fun hide() {
        _revealed.value = null
    }

    companion object {
        fun valueOf(card: HomeCard, field: DetailsField): String? = when (field) {
            DetailsField.NUMBER -> card.card.number
            DetailsField.EXPIRY -> card.card.expiry
            DetailsField.CVV -> card.card.cvv
            DetailsField.HOLDER -> card.holderName
            DetailsField.NOTE -> card.card.note
        }.takeIf { it.isNotBlank() }

        fun canReveal(card: HomeCard, field: DetailsField): Boolean =
            (field == DetailsField.NUMBER || field == DetailsField.CVV) && valueOf(card, field) != null
    }
}
