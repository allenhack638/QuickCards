package com.quickcards.app.feature.manage

import com.quickcards.app.core.ui.entity.toManageCardItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quickcards.app.QuickCardsApplication
import com.quickcards.app.core.data.model.Bank
import com.quickcards.app.core.data.model.CardNetwork
import com.quickcards.app.core.data.model.CardType
import com.quickcards.app.core.data.model.Holder
import com.quickcards.app.core.data.model.Tag
import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.domain.EntityKind
import com.quickcards.domain.EntityUsage
import com.quickcards.domain.UsageCard
import com.quickcards.domain.UsageNetwork
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** A deleted list item, kept until the Undo action is dismissed. */
sealed class DeletedEntity(val kind: EntityKind, val name: String) {
    class HolderItem(val holder: Holder) : DeletedEntity(EntityKind.HOLDER, holder.name)
    class BankItem(val bank: Bank) : DeletedEntity(EntityKind.BANK, bank.name)
    class NetworkItem(val network: CardNetwork) : DeletedEntity(EntityKind.NETWORK, network.name)
    class TypeItem(val type: CardType) : DeletedEntity(EntityKind.TYPE, type.name)
    class TagItem(val tag: Tag) : DeletedEntity(EntityKind.TAG, tag.name)
}

class ManageViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = (application as QuickCardsApplication).repository

    private data class Lists(
        val holders: List<Holder>,
        val banks: List<Bank>,
        val networks: List<CardNetwork>,
        val types: List<CardType>,
        val tags: List<Tag>
    )

    private val lists = combine(
        repository.holders,
        repository.banks,
        repository.networks,
        repository.cardTypes,
        repository.tags
    ) { holders, banks, networks, types, tags ->
        Lists(holders, banks, networks, types, tags)
    }

    val state: StateFlow<ManageState> = combine(
        lists,
        repository.cards,
        repository.observeArchived()
    ) { lists, cards, archived ->
        buildState(lists, cards, archived.size)
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), ManageState())

    suspend fun delete(entity: ManagedEntity): DeletedEntity? = when {
        entity.holder != null && repository.deleteHolder(entity.id) ->
            DeletedEntity.HolderItem(entity.holder)

        entity.bank != null && repository.deleteBank(entity.id) ->
            DeletedEntity.BankItem(entity.bank)

        entity.network != null && repository.deleteNetwork(entity.id) ->
            DeletedEntity.NetworkItem(entity.network)

        entity.type != null && repository.deleteCardType(entity.id) ->
            DeletedEntity.TypeItem(entity.type)

        entity.tag != null && repository.deleteTag(entity.id) ->
            DeletedEntity.TagItem(entity.tag)

        else -> null
    }

    suspend fun undo(deleted: DeletedEntity) {
        when (deleted) {
            is DeletedEntity.HolderItem -> repository.restoreHolder(deleted.holder)
            is DeletedEntity.BankItem -> repository.restoreBank(deleted.bank)
            is DeletedEntity.NetworkItem -> repository.restoreNetwork(deleted.network)
            is DeletedEntity.TypeItem -> repository.restoreCardType(deleted.type)
            is DeletedEntity.TagItem -> repository.restoreTag(deleted.tag)
        }
    }

    private fun buildState(
        lists: Lists,
        cards: List<VaultCard>,
        archivedCount: Int
    ): ManageState {
        val usageCards = buildUsageCards(lists, cards)

        val holders = lists.holders.map { holder ->
            val used = EntityUsage.usedCards(EntityKind.HOLDER, holder.id, usageCards)
            ManagedEntity(
                id = holder.id,
                name = holder.name,
                usageCount = used.size,
                usedCards = used.map { it.toManageCardItem() },
                holder = holder
            )
        }
        val banks = lists.banks.map { bank ->
            val used = EntityUsage.usedCards(EntityKind.BANK, bank.id, usageCards)
            ManagedEntity(
                id = bank.id,
                name = bank.name,
                usageCount = used.size,
                usedCards = used.map { it.toManageCardItem() },
                bank = bank
            )
        }
        val networks = lists.networks.map { network ->
            val used = EntityUsage.usedCards(EntityKind.NETWORK, network.id, usageCards)
            ManagedEntity(
                id = network.id,
                name = network.name,
                usageCount = used.size,
                usedCards = used.map { it.toManageCardItem() },
                network = network
            )
        }
        val types = lists.types.map { type ->
            val used = EntityUsage.usedCards(EntityKind.TYPE, type.id, usageCards)
            ManagedEntity(
                id = type.id,
                name = type.name,
                usageCount = used.size,
                usedCards = used.map { it.toManageCardItem() },
                type = type
            )
        }
        val tags = lists.tags.map { tag ->
            val used = EntityUsage.usedCards(EntityKind.TAG, tag.id, usageCards)
            ManagedEntity(
                id = tag.id,
                name = tag.name,
                usageCount = used.size,
                usedCards = used.map { it.toManageCardItem() },
                tag = tag
            )
        }

        return ManageState(
            loading = false,
            archivedCount = archivedCount,
            holders = holders,
            banks = banks,
            networks = networks,
            types = types,
            tags = tags
        )
    }

    private fun buildUsageCards(lists: Lists, cards: List<VaultCard>): List<UsageCard> {
        val holdersById = lists.holders.associateBy { it.id }
        val banksById = lists.banks.associateBy { it.id }
        val typesById = lists.types.associateBy { it.id }
        val networkRefs = lists.networks.map { UsageNetwork(it.id, it.prefixes) }
        return cards.map { card ->
            UsageCard(
                id = card.id,
                number = card.number,
                bankName = card.bankId?.let { banksById[it]?.name } ?: "Bank",
                typeName = card.typeId?.let { typesById[it]?.name }.orEmpty(),
                holderId = card.holderId,
                holderName = card.holderId?.let { holdersById[it]?.name }.orEmpty(),
                bankId = card.bankId,
                typeId = card.typeId,
                networkId = EntityUsage.resolveNetworkId(
                    number = card.number,
                    networkAuto = card.networkAuto,
                    networkId = card.networkId,
                    networks = networkRefs
                ),
                tagIds = card.tagIds,
                bandColor = card.bandColor
            )
        }
    }
}
