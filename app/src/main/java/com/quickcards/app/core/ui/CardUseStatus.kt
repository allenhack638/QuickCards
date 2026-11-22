package com.quickcards.app.core.ui

import com.quickcards.domain.CardStatus
import com.quickcards.domain.CardStatusRules

/** The one chip a card shows on its tile and on Card details, and which card is "Last used". */
object CardUseStatus {

    /** The last [limit] copied cards still on the list, newest first. */
    fun recentCards(cards: List<HomeCard>, copiedAt: Map<String, Long>, limit: Int = RECENT_LIMIT): List<HomeCard> =
        cards.filter { !it.card.archived && it.id in copiedAt }
            .sortedByDescending { copiedAt.getValue(it.id) }
            .take(limit)

    /** Last used never sits beside a due bill. */
    fun of(card: HomeCard, lastUsedId: String?): CardStatus? = CardStatusRules.of(
        archived = card.card.archived,
        expiry = card.expiryState,
        needsCheck = card.card.needsReview.isNotEmpty(),
        lastUsed = card.id == lastUsedId && card.dueInDays == null
    )

    private const val RECENT_LIMIT = 3
}
