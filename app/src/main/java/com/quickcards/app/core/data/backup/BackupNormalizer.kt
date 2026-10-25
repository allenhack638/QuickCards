package com.quickcards.app.core.data.backup

import com.quickcards.domain.BackupCard
import com.quickcards.domain.BackupContent
import com.quickcards.domain.CardFieldMask
import com.quickcards.domain.CardNetwork
import com.quickcards.domain.LegacyCardNormalizer
import com.quickcards.domain.NormalizedCard

/**
 * The normalizer pass every import (format 1 and 2) runs before its review.
 * The returned content carries the repaired values and the review mask; the
 * per-card results let the importer write the matching `card_repairs` rows.
 */
data class NormalizedBackupCard(
    val card: BackupCard,
    val result: NormalizedCard
)

data class NormalizedBackup(
    val content: BackupContent,
    val cards: List<NormalizedBackupCard>
) {
    val formatsFixed: Int get() = cards.count { it.result.fixed.isNotEmpty() }
    val needCheck: Int get() = cards.count { it.result.needsReview.isNotEmpty() }
}

object BackupNormalizer {

    fun normalize(content: BackupContent, networks: List<CardNetwork>): NormalizedBackup {
        val cards = content.cards.map { card ->
            val result = LegacyCardNormalizer.normalize(
                number = card.number,
                expiry = card.exp,
                cvv = card.cvv,
                networks = networks
            )
            NormalizedBackupCard(
                card = card.copy(
                    number = result.number,
                    exp = result.expiry,
                    cvv = result.cvv,
                    needsReview = CardFieldMask.encode(result.needsReview)
                ),
                result = result
            )
        }
        return NormalizedBackup(
            content = content.copy(cards = cards.map { it.card }),
            cards = cards
        )
    }
}
