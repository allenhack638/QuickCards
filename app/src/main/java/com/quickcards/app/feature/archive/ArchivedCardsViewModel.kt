package com.quickcards.app.feature.archive

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quickcards.app.QuickCardsApplication
import com.quickcards.app.R
import com.quickcards.app.core.data.model.Bank
import com.quickcards.app.core.data.model.CardNetwork
import com.quickcards.app.core.data.model.CardType
import com.quickcards.app.core.data.model.Holder
import com.quickcards.app.core.data.model.Tag
import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.app.core.data.model.toDomain
import com.quickcards.app.core.ui.HomeCard
import com.quickcards.domain.CardNumberRules
import com.quickcards.domain.ExpiryStatus
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ArchivedCardsViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as QuickCardsApplication
    private val repository = app.repository
    private val settings = app.settings
    private val bankFallback = application.getString(R.string.cards_bank_fallback)

    private val query = MutableStateFlow("")
    private val searchOn = MutableStateFlow(false)

    private data class PartialLists(
        val archived: List<VaultCard>,
        val holders: List<Holder>,
        val banks: List<Bank>,
        val networks: List<CardNetwork>,
        val types: List<CardType>
    )

    private data class Lists(
        val archived: List<VaultCard>,
        val holders: List<Holder>,
        val banks: List<Bank>,
        val networks: List<CardNetwork>,
        val types: List<CardType>,
        val tags: List<Tag>
    )

    private data class ListOptions(
        val maskNumbers: Boolean,
        val showCardType: Boolean,
        val tagsShow: Boolean,
        val showNotes: Boolean,
        val expiresSoonMonths: Int
    )

    private data class CopyOptions(
        val clipClearSec: Int,
        val notifEnabled: Boolean,
        val notifSec: Int,
        val notifShowExpiry: Boolean,
        val notifShowCvv: Boolean
    )

    private val lists = combine(
        repository.observeArchived(),
        repository.holders,
        repository.banks,
        repository.networks,
        repository.cardTypes
    ) { archived, holders, banks, networks, types ->
        PartialLists(archived, holders, banks, networks, types)
    }.combine(repository.tags) { partial, tags ->
        Lists(
            archived = partial.archived,
            holders = partial.holders,
            banks = partial.banks,
            networks = partial.networks,
            types = partial.types,
            tags = tags
        )
    }

    private val listOptions = combine(
        settings.maskNumbers,
        settings.showCardType,
        settings.tagsShow,
        settings.showNotes,
        settings.expiresSoonMonths
    ) { maskNumbers, showCardType, tagsShow, showNotes, expiresSoonMonths ->
        ListOptions(maskNumbers, showCardType, tagsShow, showNotes, expiresSoonMonths)
    }

    private val copyOptions = combine(
        settings.clipClearSec,
        settings.notifEnabled,
        settings.notifSec,
        settings.notifShowExpiry,
        settings.notifShowCvv
    ) { clipClearSec, notifEnabled, notifSec, notifShowExpiry, notifShowCvv ->
        CopyOptions(clipClearSec, notifEnabled, notifSec, notifShowExpiry, notifShowCvv)
    }

    private val options = combine(listOptions, copyOptions) { list, copy ->
        ArchivedCardsOptions(
            maskNumbers = list.maskNumbers,
            showCardType = list.showCardType,
            tagsShow = list.tagsShow,
            showNotes = list.showNotes,
            clipClearSec = copy.clipClearSec,
            notifEnabled = copy.notifEnabled,
            notifSec = copy.notifSec,
            notifShowExpiry = copy.notifShowExpiry,
            notifShowCvv = copy.notifShowCvv,
            expiresSoonMonths = list.expiresSoonMonths
        )
    }

    val uiState: StateFlow<ArchivedCardsUiState> = combine(
        lists,
        options,
        query,
        searchOn
    ) { lists, options, query, on ->
        buildState(lists, options, query, on)
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        ArchivedCardsUiState()
    )

    fun setSearchActive(active: Boolean) {
        searchOn.value = active
        if (!active) query.value = ""
    }

    fun setQuery(value: String) {
        query.value = value
    }

    fun restore(cardId: String) {
        viewModelScope.launch { repository.restore(cardId) }
    }

    fun deleteCard(item: ArchivedCardItem) {
        viewModelScope.launch { repository.deleteCard(item.card.id) }
    }

    fun restoreDeleted(card: VaultCard) {
        viewModelScope.launch { repository.restoreCard(card) }
    }

    private fun buildState(
        lists: Lists,
        options: ArchivedCardsOptions,
        query: String,
        searchOn: Boolean
    ): ArchivedCardsUiState {
        val domainNetworks = lists.networks.map { it.toDomain() }
        val networksById = lists.networks.associateBy { it.id }
        val holdersById = lists.holders.associateBy { it.id }
        val banksById = lists.banks.associateBy { it.id }
        val typesById = lists.types.associateBy { it.id }
        val tagsById = lists.tags.associateBy { it.id }
        val today = LocalDate.now()

        val cards = lists.archived.map { card ->
            val resolved = if (card.networkAuto) {
                CardNumberRules.detect(card.number, domainNetworks)
            } else {
                card.networkId?.let { networksById[it]?.toDomain() }
            }
            ArchivedCardItem(
                card = HomeCard(
                    card = card,
                    bankName = card.bankId?.let { banksById[it]?.name } ?: bankFallback,
                    holderName = card.holderId?.let { holdersById[it]?.name }.orEmpty(),
                    typeName = card.typeId?.let { typesById[it]?.name },
                    network = resolved,
                    networkId = resolved?.id,
                    tagNames = card.tagIds.mapNotNull { tagsById[it]?.name },
                    expiryState = ExpiryStatus.expiryState(
                        expiry = card.expiry,
                        today = today,
                        soonMonths = options.expiresSoonMonths
                    )
                ),
                reason = card.archiveReason?.takeIf { it.isNotBlank() }
            )
        }

        return buildArchivedCardsState(
            cards = cards,
            query = if (searchOn) query else "",
            searchOn = searchOn,
            options = options
        )
    }
}
