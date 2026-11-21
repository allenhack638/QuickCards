package com.quickcards.app.feature.details

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import com.quickcards.app.QuickCardsApplication
import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.app.core.data.repository.VaultRepository

/** The card actions that change the vault, each with what its Undo needs. */
class CardActionsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as QuickCardsApplication).repository

    data class PinOutcome(val pinned: Boolean, val previous: VaultRepository.CardPinState)

    data class ArchiveOutcome(val cardId: String, val previousPin: VaultRepository.CardPinState?)

    suspend fun togglePin(cardId: String, pin: Boolean): PinOutcome? {
        val previous = repository.pinState(cardId) ?: return null
        if (pin) repository.pin(cardId) else repository.unpin(cardId)
        return PinOutcome(pin, previous)
    }

    suspend fun undoPin(outcome: PinOutcome) = repository.setPinned(outcome.previous)

    suspend fun archive(cardId: String, reason: String?): ArchiveOutcome {
        val previous = repository.pinState(cardId)
        repository.archive(cardId, reason)
        return ArchiveOutcome(cardId, previous)
    }

    suspend fun undoArchive(outcome: ArchiveOutcome) {
        repository.restore(outcome.cardId)
        outcome.previousPin?.let { repository.setPinned(it) }
    }

    suspend fun restore(cardId: String) = repository.restore(cardId)

    suspend fun rearchive(cardId: String, reason: String?) = repository.archive(cardId, reason)

    suspend fun delete(cardId: String) = repository.deleteCard(cardId)

    suspend fun undoDelete(card: VaultCard) {
        repository.restoreCard(card)
    }
}
