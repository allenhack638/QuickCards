package com.quickcards.app.core.data.backup

import com.quickcards.domain.BackupCard
import com.quickcards.domain.BackupContent
import com.quickcards.domain.CardFieldMask
import com.quickcards.domain.CardNetwork
import com.quickcards.domain.EntityKind
import com.quickcards.domain.LegacyCardNormalizer
import com.quickcards.domain.NetworkRuleSanitizer
import com.quickcards.domain.NormalizedCard

/** Normalizer pass every import runs before review; per-card results feed `card_repairs`. */
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

    /**
     * [phoneNetworks] are the networks already on the phone. Cards are checked against them
     * plus the backup's networks the phone does not have yet (matched by name, phone rules win,
     * the same way the import merges them), so a card on a backup-only network is not flagged.
     */
    fun normalize(content: BackupContent, phoneNetworks: List<CardNetwork>): NormalizedBackup {
        val networks = phoneNetworks + incomingNetworks(content, phoneNetworks)
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

    /** The backup's networks the import would add, with the importer's sanitised rules. */
    private fun incomingNetworks(content: BackupContent, phoneNetworks: List<CardNetwork>): List<CardNetwork> {
        val known = phoneNetworks.mapTo(mutableSetOf()) {
            NetworkRuleSanitizer.entityKey(EntityKind.NETWORK, it.name)
        }
        return content.networks.mapNotNull { item ->
            val name = NetworkRuleSanitizer.entityName(EntityKind.NETWORK, item.name)
            if (name.isEmpty() || !known.add(NetworkRuleSanitizer.entityKey(EntityKind.NETWORK, name))) {
                return@mapNotNull null
            }
            val lengths = NetworkRuleSanitizer.lengths(item.lengths).ifEmpty { "16" }
            CardNetwork(
                id = item.id,
                name = name,
                code = NetworkRuleSanitizer.code(item.code),
                prefixes = NetworkRuleSanitizer.prefixes(item.prefixes),
                lengths = lengths,
                cvvLength = if (item.cvv == 4) 4 else 3,
                luhn = item.luhn,
                groups = NetworkRuleSanitizer.groups(item.groups, lengths),
                cvvOptional = item.cvvOptional
            )
        }
    }
}
