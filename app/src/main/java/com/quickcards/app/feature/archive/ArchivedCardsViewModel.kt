package com.quickcards.app.feature.archive

import android.app.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
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
import com.quickcards.app.core.ui.HomeCardMapper
import com.quickcards.app.core.ui.todayFlow
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

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
        val tagsShow: Boolean,
        val showNotes: Boolean,
        val expiresSoonMonths: Int
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
        settings.tagsShow,
        settings.showNotes,
        settings.expiresSoonMonths
    ) { maskNumbers, tagsShow, showNotes, expiresSoonMonths ->
        ListOptions(maskNumbers, tagsShow, showNotes, expiresSoonMonths)
    }

    private val options = combine(listOptions, settings.clipClearSec) { list, clipClearSec ->
        ArchivedCardsOptions(
            maskNumbers = list.maskNumbers,
            tagsShow = list.tagsShow,
            showNotes = list.showNotes,
            clipClearSec = clipClearSec,
            expiresSoonMonths = list.expiresSoonMonths
        )
    }

    private val search = combine(query, searchOn) { q, on -> q to on }

    val uiState: StateFlow<ArchivedCardsUiState> = combine(
        lists,
        options,
        search,
        todayFlow
    ) { lists, options, (query, on), today ->
        buildState(lists, options, query, on, today)
    }.flowOn(Dispatchers.Default).stateIn(
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

    private fun buildState(
        lists: Lists,
        options: ArchivedCardsOptions,
        query: String,
        searchOn: Boolean,
        today: LocalDate
    ): ArchivedCardsUiState {
        val mapper = HomeCardMapper(lists.networks, lists.holders, lists.banks, lists.types, lists.tags, bankFallback)
        val cards = lists.archived.map { card ->
            ArchivedCardItem(
                card = mapper.map(card, today, options.expiresSoonMonths),
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
