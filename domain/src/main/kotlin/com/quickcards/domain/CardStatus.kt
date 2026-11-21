package com.quickcards.domain

/** The one status chip a card shows, highest priority first (PRODUCT.md › Capabilities). */
enum class CardStatus { ARCHIVED, EXPIRED, CHECK, EXPIRES_SOON, LAST_USED }

object CardStatusRules {

    fun of(
        archived: Boolean,
        expiry: ExpiryState?,
        needsCheck: Boolean,
        lastUsed: Boolean
    ): CardStatus? = when {
        archived -> CardStatus.ARCHIVED
        expiry == ExpiryState.EXPIRED -> CardStatus.EXPIRED
        needsCheck -> CardStatus.CHECK
        expiry == ExpiryState.EXPIRES_SOON -> CardStatus.EXPIRES_SOON
        lastUsed -> CardStatus.LAST_USED
        else -> null
    }

    /** The most recently copied card id, from card id → last copy time. */
    fun lastUsedId(copiedAt: Map<String, Long>): String? = copiedAt.maxByOrNull { it.value }?.key
}
