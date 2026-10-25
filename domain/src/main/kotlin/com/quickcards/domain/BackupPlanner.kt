package com.quickcards.domain

import java.util.Locale

enum class MergeMode {
    UPDATE,
    KEEP
}

enum class CardDisposition {
    ADD,
    UPDATE,
    KEEP,
    SAME
}

data class SnapshotItem(
    val id: String,
    val name: String
)

data class PhoneCard(
    val id: String,
    val number: String,
    val holderId: String? = null,
    val bankId: String? = null,
    val networkAuto: Boolean = true,
    val networkId: String? = null,
    val typeId: String? = null,
    val variant: String = "",
    val exp: String = "",
    val cvv: String = "",
    val nameOnCard: String = "",
    val color: String = "navy",
    val note: String = "",
    val dueDay: Int? = null,
    val tagIds: List<String> = emptyList(),
    val pinned: Boolean = false,
    val pinnedAt: Long? = null,
    val archived: Boolean = false,
    val archivedAt: Long? = null,
    val archiveReason: String = ""
)

data class PhoneSnapshot(
    val holders: List<SnapshotItem> = emptyList(),
    val banks: List<SnapshotItem> = emptyList(),
    val networks: List<SnapshotItem> = emptyList(),
    val types: List<SnapshotItem> = emptyList(),
    val tags: List<SnapshotItem> = emptyList(),
    val cards: List<PhoneCard> = emptyList()
)

/** A card with backup ids mapped to phone ids; a null id means the item will be created. */
data class ResolvedCard(
    val holderId: String?,
    val bankId: String?,
    val networkAuto: Boolean,
    val networkId: String?,
    val typeId: String?,
    val tagIds: List<String>,
    val variant: String,
    val exp: String,
    val cvv: String,
    val nameOnCard: String,
    val color: String,
    val note: String,
    val dueDay: Int?,
    val pinned: Boolean = false,
    val pinnedAt: Long? = null,
    val archived: Boolean = false,
    val archivedAt: Long? = null,
    val archiveReason: String = ""
)

data class PlannedCard(
    val card: BackupCard,
    val existingId: String?,
    val resolved: ResolvedCard,
    val changed: Boolean
) {
    val isNew: Boolean get() = existingId == null

    fun disposition(mode: MergeMode): CardDisposition =
        BackupPlanner.disposition(existingId, changed, mode)
}

data class BackupPlan(
    val cards: List<PlannedCard>,
    val newListCounts: Map<BackupSection, Int>
) {
    val newCards: Int get() = cards.count { it.isNew }
    val changedCards: Int get() = cards.count { !it.isNew && it.changed }
    val sameCards: Int get() = cards.count { !it.isNew && !it.changed }

    fun newCount(section: BackupSection): Int = newListCounts[section] ?: 0
}

/**
 * Lists match by name ignoring case, cards by digits-only number (the same input as the
 * stored PAN hash).
 */
object BackupPlanner {

    fun plan(content: BackupContent, phone: PhoneSnapshot): BackupPlan {
        val holderMap = mapIds(content.holders, phone.holders)
        val bankMap = mapIds(content.banks.map { BackupNamed(it.id, it.name) }, phone.banks)
        val typeMap = mapIds(content.types, phone.types)
        val tagMap = mapIds(content.tags, phone.tags)
        val networkMap = mapIds(content.networks.map { BackupNamed(it.id, it.name) }, phone.networks)

        val phoneByNumber = phone.cards
            .groupBy { normalizeNumber(it.number) }
            .filterKeys { it.isNotEmpty() }
        val legacyNetworkIds = phone.networks.associate { normalizeName(it.name) to it.id }

        val planned = content.cards.map { card ->
            val existing = phoneByNumber[normalizeNumber(card.number)]?.firstOrNull()
            val resolved = resolve(
                card,
                holderMap,
                bankMap,
                typeMap,
                tagMap,
                networkMap,
                legacyNetworkIds
            )
            PlannedCard(
                card = card,
                existingId = existing?.id,
                resolved = resolved,
                changed = existing != null && differs(existing, resolved)
            )
        }

        val newListCounts = linkedMapOf(
            BackupSection.HOLDERS to countNew(content.holders, phone.holders),
            BackupSection.BANKS to countNew(content.banks.map { BackupNamed(it.id, it.name) }, phone.banks),
            BackupSection.NETWORKS to countNew(content.networks.map { BackupNamed(it.id, it.name) }, phone.networks),
            BackupSection.TYPES to countNew(content.types, phone.types),
            BackupSection.TAGS to countNew(content.tags, phone.tags)
        )

        return BackupPlan(cards = planned, newListCounts = newListCounts)
    }

    fun resolve(
        card: BackupCard,
        holderMap: Map<String, String?>,
        bankMap: Map<String, String?>,
        typeMap: Map<String, String?>,
        tagMap: Map<String, String?>,
        networkMap: Map<String, String?>,
        legacyNetworkIds: Map<String, String> = emptyMap()
    ): ResolvedCard {
        val legacyNetworkId = card.legacyIssuer
            ?.let { legacyNetworkIds[normalizeName(it)] }
        val networkAuto = if (card.legacyIssuer != null) {
            legacyNetworkId == null
        } else {
            card.networkAuto
        }
        val networkId = when {
            card.legacyIssuer != null -> legacyNetworkId
            card.networkAuto -> null
            else -> card.networkId?.let { networkMap[it] }
        }
        return ResolvedCard(
            holderId = card.holderId?.let { holderMap[it] },
            bankId = card.bankId?.let { bankMap[it] },
            networkAuto = networkAuto,
            networkId = networkId,
            typeId = card.typeId?.let { typeMap[it] },
            tagIds = card.tagIds.mapNotNull { tagMap[it] }.distinct().sorted(),
            variant = card.variant,
            exp = card.exp,
            cvv = card.cvv,
            nameOnCard = card.nameOnCard,
            color = card.color,
            note = card.note,
            dueDay = card.dueDay,
            pinned = card.pinned,
            pinnedAt = if (card.pinned) card.pinnedAt else null,
            archived = card.archived,
            archivedAt = if (card.archived) card.archivedAt else null,
            archiveReason = if (card.archived) card.archiveReason else ""
        )
    }

    fun differs(existing: PhoneCard, resolved: ResolvedCard): Boolean {
        if (existing.holderId != resolved.holderId) return true
        if (existing.bankId != resolved.bankId) return true
        if (existing.networkAuto != resolved.networkAuto) return true
        if (existing.networkId != resolved.networkId) return true
        if (existing.typeId != resolved.typeId) return true
        if (existing.variant != resolved.variant) return true
        if (existing.exp != resolved.exp) return true
        if (existing.cvv != resolved.cvv) return true
        if (existing.nameOnCard != resolved.nameOnCard) return true
        if (existing.color != resolved.color) return true
        if (existing.note != resolved.note) return true
        if (existing.dueDay != resolved.dueDay) return true
        if (existing.pinned != resolved.pinned) return true
        if (existing.pinnedAt != resolved.pinnedAt) return true
        if (existing.archived != resolved.archived) return true
        if (existing.archivedAt != resolved.archivedAt) return true
        if (existing.archiveReason != resolved.archiveReason) return true
        if (existing.tagIds.toSet() != resolved.tagIds.toSet()) return true
        return false
    }

    fun disposition(existingId: String?, changed: Boolean, mode: MergeMode): CardDisposition =
        when {
            existingId == null -> CardDisposition.ADD
            !changed -> CardDisposition.SAME
            mode == MergeMode.UPDATE -> CardDisposition.UPDATE
            else -> CardDisposition.KEEP
        }

    fun normalizeNumber(number: String): String = number.filter { it in '0'..'9' }

    private fun mapIds(
        incoming: List<BackupNamed>,
        existing: List<SnapshotItem>
    ): Map<String, String?> {
        val byName = existing.associate { normalizeName(it.name) to it.id }
        return incoming.associate { it.id to byName[normalizeName(it.name)] }
    }

    private fun countNew(incoming: List<BackupNamed>, existing: List<SnapshotItem>): Int {
        val names = existing.mapTo(mutableSetOf()) { normalizeName(it.name) }
        return incoming.count { normalizeName(it.name) !in names }
    }

    private fun normalizeName(name: String): String =
        name.trim().replace(Regex("""\s+"""), " ").lowercase(Locale.ROOT)
}
