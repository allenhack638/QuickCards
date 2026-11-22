package com.quickcards.app.feature.details

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.quickcards.app.core.ui.CardCopyField
import com.quickcards.app.core.ui.CardFieldCopier
import com.quickcards.app.core.ui.HomeCard
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

/** Why the unlock prompt is shown; each maps to its own prompt subtitle. */
enum class UnlockReason { COPY, COPY_NOTE, SHOW_NUMBER, SHOW_CVV }

/** The biometric gate; false for a cancel, a failure or no lock (fail-closed). */
fun interface Unlock {
    suspend fun request(reason: UnlockReason): Boolean
}

/** One row of one card: what is revealed or marked Copied. */
data class FieldMark(val cardId: String, val field: CardCopyField)

/**
 * Copy and reveal on Card details. Both pass the unlock gate first and change nothing when it
 * fails. One instance serves a pane, so a reveal or Copied mark belongs to a card: showing another
 * card, leaving the pane ([clear]) or a lock drops it, and coming back asks again.
 */
class CardDetailsViewModel(private val locked: StateFlow<Boolean>) : ViewModel() {

    private val _copied = MutableStateFlow<FieldMark?>(null)
    val copied: StateFlow<FieldMark?> = _copied.asStateFlow()

    private val _revealed = MutableStateFlow<FieldMark?>(null)
    val revealed: StateFlow<FieldMark?> = _revealed.asStateFlow()

    private var shownId: String? = null

    init {
        locked.filter { it }.onEach { clear() }.launchIn(viewModelScope)
    }

    /** The pane now shows [cardId]; anything marked on another card is dropped. */
    fun show(cardId: String) {
        if (cardId == shownId) return
        shownId = cardId
        clear()
    }

    /** Returns what to announce, or null when nothing was copied. [unlock] and [copier] belong to the screen. */
    suspend fun copy(card: HomeCard, field: CardCopyField, unlock: Unlock, copier: CardFieldCopier): String? {
        if (CardCopyField.valueOf(card, field) == null) return null
        val reason = if (field == CardCopyField.NOTE) UnlockReason.COPY_NOTE else UnlockReason.COPY
        if (!unlock.request(reason) || locked.value) return null
        show(card.id)
        val announcement = copier.copy(card, field) ?: return null
        _copied.value = FieldMark(card.id, field)
        return announcement
    }

    suspend fun reveal(card: HomeCard, field: CardCopyField, unlock: Unlock): Boolean {
        if (!canReveal(card, field)) return false
        val reason = if (field == CardCopyField.CVV) UnlockReason.SHOW_CVV else UnlockReason.SHOW_NUMBER
        if (!unlock.request(reason) || locked.value) return false
        show(card.id)
        _revealed.value = FieldMark(card.id, field)
        return true
    }

    fun clearCopied(mark: FieldMark) {
        _copied.compareAndSet(mark, null)
    }

    fun hide() {
        _revealed.value = null
    }

    fun clear() {
        _revealed.value = null
        _copied.value = null
    }

    companion object {
        fun canReveal(card: HomeCard, field: CardCopyField): Boolean =
            (field == CardCopyField.NUMBER || field == CardCopyField.CVV) && CardCopyField.valueOf(card, field) != null
    }
}
