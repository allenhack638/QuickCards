package com.quickcards.domain

enum class ExpiredPlacement {
    KEEP,
    END,
    HIDE
}

/**
 * END moves expired unpinned cards last (pinned cards never move); HIDE drops expired cards
 * unless searching or filtering Expired; a status filter never reorders; archived cards
 * appear in search.
 */
object CardListOrder {

    fun <T> arrangeCards(
        cards: List<T>,
        isPinned: (T) -> Boolean,
        state: (T) -> ExpiryState?,
        isArchived: (T) -> Boolean,
        placement: ExpiredPlacement,
        searching: Boolean,
        statusFilter: Set<ExpiryState>
    ): List<T> {
        val visible = cards.filter { card ->
            if (isArchived(card) && !searching) return@filter false
            if (statusFilter.isNotEmpty()) {
                val cardState = state(card)
                if (cardState == null || cardState !in statusFilter) return@filter false
            }
            true
        }

        return when (placement) {
            ExpiredPlacement.KEEP -> visible

            ExpiredPlacement.HIDE -> {
                if (searching || ExpiryState.EXPIRED in statusFilter) {
                    visible
                } else {
                    visible.filter { state(it) != ExpiryState.EXPIRED }
                }
            }

            ExpiredPlacement.END -> {
                val expiredUnpinned = visible.filter { card ->
                    !isPinned(card) && state(card) == ExpiryState.EXPIRED
                }
                if (expiredUnpinned.isEmpty()) {
                    visible
                } else {
                    visible.filter { card ->
                        isPinned(card) || state(card) != ExpiryState.EXPIRED
                    } + expiredUnpinned
                }
            }
        }
    }
}
