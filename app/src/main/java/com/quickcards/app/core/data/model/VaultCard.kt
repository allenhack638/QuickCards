package com.quickcards.app.core.data.model

import com.quickcards.domain.CardField
import java.util.UUID

/** Decrypted card held only in memory; [Card] is its encrypted Room form. */
data class VaultCard(
    val id: String = UUID.randomUUID().toString(),
    val number: String,
    val expiry: String = "",
    val cvv: String = "",
    val holderId: String? = null,
    val bankId: String? = null,
    val networkId: String? = null,
    val networkAuto: Boolean = true,
    val typeId: String? = null,
    val nameOnCard: String = "",
    val cardName: String = "",
    val note: String = "",
    val dueDay: Int? = null,
    val bandColor: String = BandColor.NAVY,
    val sortOrder: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = createdAt,
    val tagIds: List<String> = emptyList(),
    val pinned: Boolean = false,
    val pinnedAt: Long? = null,
    val archived: Boolean = false,
    val archivedAt: Long? = null,
    val archiveReason: String? = null,
    val needsReview: Set<CardField> = emptySet()
)
