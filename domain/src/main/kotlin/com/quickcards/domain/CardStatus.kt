package com.quickcards.domain

/** The one status chip a card shows, highest priority first (PRODUCT.md › Capabilities). */
enum class CardStatus { ARCHIVED, EXPIRED, CHECK, EXPIRES_SOON, BEST_TODAY, LAST_USED }

object CardStatusRules {

    fun of(
        archived: Boolean,
        expiry: ExpiryState?,
        needsCheck: Boolean,
        bestToday: Boolean,
        lastUsed: Boolean
    ): CardStatus? = when {
        archived -> CardStatus.ARCHIVED
        expiry == ExpiryState.EXPIRED -> CardStatus.EXPIRED
        needsCheck -> CardStatus.CHECK
        expiry == ExpiryState.EXPIRES_SOON -> CardStatus.EXPIRES_SOON
        bestToday -> CardStatus.BEST_TODAY
        lastUsed -> CardStatus.LAST_USED
        else -> null
    }
}
