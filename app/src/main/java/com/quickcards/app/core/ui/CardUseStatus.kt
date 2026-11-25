package com.quickcards.app.core.ui

import com.quickcards.domain.BestCardRules
import com.quickcards.domain.CardStatus
import com.quickcards.domain.CardStatusRules
import com.quickcards.domain.ExpiryState
import java.time.LocalDate

/** The one chip a card shows on its tile and on Card details, and which card is "Last used". */
object CardUseStatus {

    /** The last [limit] copied cards still on the list, newest first. */
    fun recentCards(cards: List<HomeCard>, copiedAt: Map<String, Long>, limit: Int = RECENT_LIMIT): List<HomeCard> =
        cards.filter { !it.card.archived && it.id in copiedAt }
            .sortedByDescending { copiedAt.getValue(it.id) }
            .take(limit)

    /**
     * Marks today's best card among the credit cards that could show its chip (active, no other status,
     * no due strip);
     * ties and lone cards mark none.
     */
    fun withBestToday(cards: List<HomeCard>, today: LocalDate): List<HomeCard> {
        val candidates = cards.filter {
            it.typeName?.trim().equals(CREDIT_TYPE_NAME, ignoreCase = true) &&
                !it.card.archived &&
                it.card.needsReview.isEmpty() &&
                it.dueInDays == null &&
                it.expiryState != ExpiryState.EXPIRED &&
                it.expiryState != ExpiryState.EXPIRES_SOON
        }.map { BestCardRules.Candidate(it.id, it.card.statementDay, it.card.dueDay) }
        val best = BestCardRules.best(candidates, today) ?: return cards
        return cards.map { if (it.id == best.id) it.copy(bestDays = best.days.toInt()) else it }
    }

    /** Last used never sits beside a due bill. */
    fun of(card: HomeCard, lastUsedId: String?): CardStatus? = CardStatusRules.of(
        archived = card.card.archived,
        expiry = card.expiryState,
        needsCheck = card.card.needsReview.isNotEmpty(),
        bestToday = card.bestDays != null,
        lastUsed = card.id == lastUsedId && card.dueInDays == null
    )

    private const val RECENT_LIMIT = 3

    /** The built-in type's name; matched by name so a restored or recreated "Credit" type counts too. */
    private const val CREDIT_TYPE_NAME = "Credit"
}
