package com.quickcards.app.core.ui.entity

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
import com.quickcards.domain.EntityBefore
import com.quickcards.domain.EntityDraft
import com.quickcards.domain.EntityKind
import com.quickcards.domain.EntityRef
import com.quickcards.domain.EntityUsage
import com.quickcards.domain.EntityValidator
import com.quickcards.domain.UsageCard
import com.quickcards.domain.UsageNetwork
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.util.Locale

/**
 * Repository-backed data for EntityDialog. The dialog keeps its own form state;
 * this class supplies the lists, original values and the save operation, and is
 * usable from the card form as well as the Manage list editors.
 */
data class EntityEditorData(
    val ready: Boolean = false,
    val holders: List<Holder> = emptyList(),
    val banks: List<Bank> = emptyList(),
    val networks: List<CardNetwork> = emptyList(),
    val types: List<CardType> = emptyList(),
    val tags: List<Tag> = emptyList(),
    val usageCards: List<UsageCard> = emptyList()
) {
    fun refs(kind: EntityKind): List<EntityRef> = when (kind) {
        EntityKind.HOLDER -> holders.map { EntityRef(it.name, it.id) }
        EntityKind.BANK -> banks.map { EntityRef(it.name, it.id) }
        EntityKind.NETWORK -> networks.map { EntityRef(it.name, it.id, it.code) }
        EntityKind.TYPE -> types.map { EntityRef(it.name, it.id) }
        EntityKind.TAG -> tags.map { EntityRef(it.name, it.id) }
    }

    fun before(kind: EntityKind, id: String?): EntityBefore? {
        if (id == null) return null
        return when (kind) {
            EntityKind.HOLDER -> holders.firstOrNull { it.id == id }?.let {
                EntityBefore(name = it.name)
            }

            EntityKind.BANK -> banks.firstOrNull { it.id == id }?.let {
                EntityBefore(name = it.name)
            }

            EntityKind.NETWORK -> networks.firstOrNull { it.id == id }?.let {
                EntityBefore(
                    name = it.name,
                    code = it.code,
                    prefixes = it.prefixes,
                    lengths = it.lengths,
                    cvvLength = it.cvvLength,
                    luhn = it.luhn
                )
            }

            EntityKind.TYPE -> types.firstOrNull { it.id == id }?.let {
                EntityBefore(name = it.name)
            }

            EntityKind.TAG -> tags.firstOrNull { it.id == id }?.let {
                EntityBefore(name = it.name)
            }
        }
    }

    fun usedCards(kind: EntityKind, id: String?): List<UsageCard> {
        if (id == null) return emptyList()
        return EntityUsage.usedCards(kind, id, usageCards)
    }
}

class EntityEditorViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as QuickCardsApplication
    private val repository = app.repository
    private val settings = app.settings

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

    val data: StateFlow<EntityEditorData> = combine(lists, repository.cards) { lists, cards ->
        EntityEditorData(
            ready = true,
            holders = lists.holders,
            banks = lists.banks,
            networks = lists.networks,
            types = lists.types,
            tags = lists.tags,
            usageCards = buildUsageCards(lists, cards)
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EntityEditorData())

    val tagMaxLen: StateFlow<Int> = settings.tagsMaxLen

    suspend fun save(
        kind: EntityKind,
        editingId: String?,
        draft: EntityDraft
    ): Result<String> = runCatching {
        val name = EntityValidator.normalizeName(kind, draft.name)
        when (kind) {
            EntityKind.HOLDER -> if (editingId == null) {
                repository.createHolder(name).id
            } else {
                repository.renameHolder(editingId, name)
                editingId
            }

            EntityKind.BANK -> if (editingId == null) {
                repository.createBank(name).id
            } else {
                repository.renameBank(editingId, name)
                editingId
            }

            EntityKind.TYPE -> if (editingId == null) {
                repository.createCardType(name).id
            } else {
                repository.renameCardType(editingId, name)
                editingId
            }

            EntityKind.TAG -> if (editingId == null) {
                repository.createTag(name).id
            } else {
                repository.renameTag(editingId, name)
                editingId
            }

            EntityKind.NETWORK -> saveNetwork(editingId, name, draft)
        }
    }

    private suspend fun saveNetwork(
        editingId: String?,
        name: String,
        draft: EntityDraft
    ): String {
        val code = draft.code.trim().uppercase(Locale.ROOT)
        val prefixes = draft.prefixes.replace(WHITESPACE, "")
        val lengths = draft.lengths.replace(WHITESPACE, "")
        if (editingId == null) {
            return repository.createNetwork(
                CardNetwork(
                    name = name,
                    code = code,
                    prefixes = prefixes,
                    lengths = lengths,
                    cvvLength = draft.cvvLength,
                    luhn = draft.luhn,
                    groups = ""
                )
            ).id
        }
        val existing = repository.getNetwork(editingId)
            ?: error("Unknown card network")
        repository.updateNetwork(
            existing.copy(
                name = name,
                code = code,
                prefixes = prefixes,
                lengths = lengths,
                cvvLength = draft.cvvLength,
                luhn = draft.luhn
            )
        )
        return editingId
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

    private companion object {
        val WHITESPACE = Regex("""\s+""")
    }
}
