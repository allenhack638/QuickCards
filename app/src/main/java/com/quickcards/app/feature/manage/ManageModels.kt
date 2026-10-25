package com.quickcards.app.feature.manage

import com.quickcards.app.core.data.model.Bank
import com.quickcards.app.core.data.model.CardNetwork
import com.quickcards.app.core.data.model.CardType
import com.quickcards.app.core.data.model.Holder
import com.quickcards.app.core.data.model.Tag
import com.quickcards.app.core.ui.entity.ManageCardItem
import com.quickcards.domain.EntityKind

/**
 * One row of a list editor. Exactly one of the raw fields is set, so Undo can
 * restore the deleted record unchanged.
 */
data class ManagedEntity(
    val id: String,
    val name: String,
    val usageCount: Int,
    val usedCards: List<ManageCardItem>,
    val holder: Holder? = null,
    val bank: Bank? = null,
    val network: CardNetwork? = null,
    val type: CardType? = null,
    val tag: Tag? = null
)

data class ManageCounts(
    val holders: Int = 0,
    val banks: Int = 0,
    val networks: Int = 0,
    val types: Int = 0,
    val tags: Int = 0
) {
    fun count(kind: EntityKind): Int = when (kind) {
        EntityKind.HOLDER -> holders
        EntityKind.BANK -> banks
        EntityKind.NETWORK -> networks
        EntityKind.TYPE -> types
        EntityKind.TAG -> tags
    }
}

data class ManageState(
    val loading: Boolean = true,
    val archivedCount: Int = 0,
    val holders: List<ManagedEntity> = emptyList(),
    val banks: List<ManagedEntity> = emptyList(),
    val networks: List<ManagedEntity> = emptyList(),
    val types: List<ManagedEntity> = emptyList(),
    val tags: List<ManagedEntity> = emptyList()
) {
    fun list(kind: EntityKind): List<ManagedEntity> = when (kind) {
        EntityKind.HOLDER -> holders
        EntityKind.BANK -> banks
        EntityKind.NETWORK -> networks
        EntityKind.TYPE -> types
        EntityKind.TAG -> tags
    }

    fun counts(): ManageCounts = ManageCounts(
        holders = holders.size,
        banks = banks.size,
        networks = networks.size,
        types = types.size,
        tags = tags.size
    )
}

val EntityKind.routeKey: String
    get() = when (this) {
        EntityKind.HOLDER -> "holders"
        EntityKind.BANK -> "banks"
        EntityKind.NETWORK -> "networks"
        EntityKind.TYPE -> "types"
        EntityKind.TAG -> "tags"
    }

fun entityKindFromRoute(value: String?): EntityKind? = when (value) {
    "holders" -> EntityKind.HOLDER
    "banks" -> EntityKind.BANK
    "networks" -> EntityKind.NETWORK
    "types" -> EntityKind.TYPE
    "tags" -> EntityKind.TAG
    else -> null
}
