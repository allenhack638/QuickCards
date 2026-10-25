package com.quickcards.domain

enum class ExpiredPlacement {
    KEEP,
    END,
    HIDE
}

enum class ArchivedVisibility {
    ONLY_ARCHIVE,
    IN_SEARCH
}

/**
 * The stable list order shared by the cards list and its tests. Callers supply
 * the per-card facts; this object only decides visibility and order.
 *
 * - [ExpiredPlacement.KEEP] leaves the input order untouched.
 * - [ExpiredPlacement.END] moves expired, unpinned cards after every other
 *   card. Pinned cards never move, so a pinned expired card stays in the
 *   pinned block at the top.
 * - [ExpiredPlacement.HIDE] removes expired cards unless the user is searching
 *   or the Expired status filter is on.
 * - A non-empty `statusFilter` keeps only cards in it and never reorders.
 * - Archived cards are removed unless searching with
 *   [ArchivedVisibility.IN_SEARCH].
 */
object CardListOrder {

    fun <T> arrangeCards(
        cards: List<T>,
        isPinned: (T) -> Boolean,
        state: (T) -> ExpiryState?,
        isArchived: (T) -> Boolean,
        placement: ExpiredPlacement,
        archived: ArchivedVisibility,
        searching: Boolean,
        statusFilter: Set<ExpiryState>
    ): List<T> {
        val visible = cards.filter { card ->
            if (isArchived(card) &&
                !(searching && archived == ArchivedVisibility.IN_SEARCH)
            ) {
                return@filter false
            }
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
