package com.quickcards.app.feature.cards

import com.quickcards.app.core.ui.HomeCard
import com.quickcards.domain.CardFilters
import com.quickcards.domain.CardListOrder
import com.quickcards.domain.CardQuery
import com.quickcards.domain.ExpiredPlacement
import com.quickcards.domain.ExpiryState
import com.quickcards.domain.FilterOption

/** Stable ids for the Status filter group, shared by the view model and sheet. */
object CardsStatusFilter {
    const val EXPIRED = "expired"
    const val SOON = "soon"

    fun state(id: String): ExpiryState? = when (id) {
        EXPIRED -> ExpiryState.EXPIRED
        SOON -> ExpiryState.EXPIRES_SOON
        else -> null
    }

    fun id(state: ExpiryState?): String? = when (state) {
        ExpiryState.EXPIRED -> EXPIRED
        ExpiryState.EXPIRES_SOON -> SOON
        else -> null
    }

    fun matches(state: ExpiryState?, ids: Set<String>): Boolean {
        if (ids.isEmpty()) return true
        val id = id(state) ?: return false
        return id in ids
    }

    fun states(ids: Set<String>): Set<ExpiryState> =
        ids.mapNotNull(::state).toSet()
}

data class CardsListOptions(
    val placement: ExpiredPlacement,
    val searching: Boolean,
    val status: Set<ExpiryState>,
    val reviewOnly: Boolean
)

/** Why a non-empty vault renders no passes. */
enum class CardsEmptyNote { NONE, HIDDEN_EXPIRED, ONLY_ARCHIVED }

/**
 * Search and filters, then CardListOrder. Status counts ignore archived cards; the review
 * count only counts cards the main list can show.
 */
object CardsListState {

    fun visibleCards(
        cards: List<HomeCard>,
        filters: CardFilters,
        query: String,
        options: CardsListOptions
    ): List<HomeCard> {
        val pool = if (options.reviewOnly) {
            cards.filter { it.card.needsReview.isNotEmpty() }
        } else {
            cards
        }
        val visibleIds = CardQuery.visibleCards(
            pool.map { it.entry },
            filters,
            query
        ).mapTo(mutableSetOf()) { it.id }

        return CardListOrder.arrangeCards(
            cards = pool.filter { it.id in visibleIds },
            isPinned = { it.card.pinned },
            state = { it.expiryState },
            isArchived = { it.card.archived },
            placement = options.placement,
            searching = options.searching,
            statusFilter = options.status
        )
    }

    fun statusOptions(
        cards: List<HomeCard>,
        expiredLabel: String,
        soonLabel: String
    ): List<FilterOption> {
        val active = cards.filterNot { it.card.archived }
        val expired = active.count { it.expiryState == ExpiryState.EXPIRED }
        val soon = active.count { it.expiryState == ExpiryState.EXPIRES_SOON }
        return buildList {
            if (expired > 0) {
                add(FilterOption(CardsStatusFilter.EXPIRED, expiredLabel, expired))
            }
            if (soon > 0) {
                add(FilterOption(CardsStatusFilter.SOON, soonLabel, soon))
            }
        }
    }

    fun needsCheckCount(cards: List<HomeCard>): Int =
        cards.count { !it.card.archived && it.card.needsReview.isNotEmpty() }

    /**
     * Only for the untouched list (all archived, or all expired and hidden); a search or
     * filter that matched nothing keeps the normal no-results copy.
     */
    fun emptyNote(
        cards: List<HomeCard>,
        placement: ExpiredPlacement,
        searching: Boolean,
        filtersActive: Boolean
    ): CardsEmptyNote {
        if (searching || filtersActive) return CardsEmptyNote.NONE
        val active = cards.filterNot { it.card.archived }
        if (active.isEmpty()) {
            return if (cards.any { it.card.archived }) {
                CardsEmptyNote.ONLY_ARCHIVED
            } else {
                CardsEmptyNote.NONE
            }
        }
        val allHiddenExpired = placement == ExpiredPlacement.HIDE &&
            active.all { it.expiryState == ExpiryState.EXPIRED }
        return if (allHiddenExpired) {
            CardsEmptyNote.HIDDEN_EXPIRED
        } else {
            CardsEmptyNote.NONE
        }
    }
}
