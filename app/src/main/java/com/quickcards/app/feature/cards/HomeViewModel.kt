package com.quickcards.app.feature.cards

import android.app.Application
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import java.time.temporal.ChronoUnit
import com.quickcards.domain.ReminderRules
import com.quickcards.app.core.reminders.PaidStore
import kotlinx.coroutines.flow.flowOn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.quickcards.app.QuickCardsApplication
import com.quickcards.app.R
import com.quickcards.app.core.autofill.CardUsage
import com.quickcards.app.core.data.model.Bank
import com.quickcards.app.core.data.model.CardNetwork
import com.quickcards.app.core.data.model.CardType
import com.quickcards.app.core.data.model.Holder
import com.quickcards.app.core.data.model.Tag
import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.app.core.ui.CardUseStatus
import com.quickcards.app.core.ui.HomeCard
import com.quickcards.app.core.ui.HomeCardMapper
import com.quickcards.app.core.ui.todayFlow
import com.quickcards.app.core.data.repository.RepairSummary
import com.quickcards.app.core.data.settings.AppSettingsDefaults
import com.quickcards.domain.CardFilters
import com.quickcards.domain.CardQuery
import com.quickcards.domain.ExpiredPlacement
import com.quickcards.domain.FilterGroup
import com.quickcards.domain.FilterGroupOptions
import com.quickcards.domain.FilterOption
import com.quickcards.domain.NamedRef
import java.time.LocalDate
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HomeOptions(
    val maskNumbers: Boolean = AppSettingsDefaults.MASK_NUMBERS,
    val tagsShow: Boolean = AppSettingsDefaults.TAGS_SHOW,
    val clipClearSec: Int = AppSettingsDefaults.CLIP_CLEAR_SEC,
    val expiredCards: ExpiredPlacement = AppSettingsDefaults.EXPIRED_CARDS,
    val expiresSoonMonths: Int = AppSettingsDefaults.EXPIRES_SOON_MONTHS,
    val showNotes: Boolean = AppSettingsDefaults.SHOW_NOTES
)

data class HomeUiState(
    val loading: Boolean = true,
    val hasAnyCards: Boolean = false,
    /** Every card, archived ones included: what Card details and the actions sheet can open. */
    val allCards: List<HomeCard> = emptyList(),
    val cards: List<HomeCard> = emptyList(),
    val visibleCards: List<HomeCard> = emptyList(),
    val holderChips: List<NamedRef> = emptyList(),
    val filterGroups: List<FilterGroupOptions> = emptyList(),
    val statusOptions: List<FilterOption> = emptyList(),
    val filters: CardFilters = CardFilters(),
    val statusFilter: Set<String> = emptySet(),
    val reviewOnly: Boolean = false,
    val needsCheckCount: Int = 0,
    val emptyNote: CardsEmptyNote = CardsEmptyNote.NONE,
    val repairSummary: RepairSummary? = null,
    /** The last three copied cards, newest first; the first one is "Last used". */
    val recentCards: List<HomeCard> = emptyList(),
    /** When the "Last used" card was copied. */
    val lastUsedAt: Long? = null,
    /** The day every status (expiry, due strip, Best today) was worked out for. */
    val today: LocalDate = LocalDate.now(),
    val showTips: Boolean = false,
    val searchOn: Boolean = false,
    val query: String = "",
    val options: HomeOptions = HomeOptions()
) {
    val lastUsedId: String? get() = recentCards.firstOrNull()?.id

    fun filterLabel(group: FilterGroup, id: String): String {
        val option = filterGroups.firstOrNull { it.group == group }
            ?.options?.firstOrNull { it.id == id }
        return option?.label ?: id
    }
}

class HomeViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as QuickCardsApplication
    private val repository = app.repository
    private val settings = app.settings
    private val bankFallback = application.getString(R.string.cards_bank_fallback)

    private val filters = MutableStateFlow(CardFilters())
    private val statusFilters = MutableStateFlow<Set<String>>(emptySet())
    private val reviewOnly = MutableStateFlow(false)
    private val searchOn = MutableStateFlow(false)
    private val query = MutableStateFlow("")

    /** Null until read from disk; Home stays loading until it and the copy times are known. */
    private val tipsSeen = MutableStateFlow<Boolean?>(null)

    private val _keyUnavailable = MutableStateFlow(false)

    /** True when the Keystore key can no longer open the vault. */
    val keyUnavailable: StateFlow<Boolean> = _keyUnavailable.asStateFlow()

    init {
        retryKeyCheck()
        viewModelScope.launch(Dispatchers.IO) {
            val seen = FirstRunTips.seen(application)
            tipsSeen.value = seen
            if (!seen) {
                repository.cards.first { it.isNotEmpty() }
                finishTips()
            }
        }
    }

    fun retryKeyCheck() {
        viewModelScope.launch(Dispatchers.IO) {
            _keyUnavailable.value = !app.keySentinel.isKeyAvailable()
        }
    }

    private data class Lists(
        val cards: List<VaultCard>,
        val holders: List<Holder>,
        val banks: List<Bank>,
        val networks: List<CardNetwork>,
        val types: List<CardType>
    )

    private data class CoreOptions(
        val maskNumbers: Boolean,
        val tagsShow: Boolean
    )

    private data class ListOptions(
        val expiredCards: ExpiredPlacement,
        val expiresSoonMonths: Int,
        val showNotes: Boolean
    )

    private data class SearchState(val on: Boolean, val query: String)

    private val lists = combine(
        repository.cards,
        repository.holders,
        repository.banks,
        repository.networks,
        repository.cardTypes
    ) { cards, holders, banks, networks, types ->
        Lists(cards, holders, banks, networks, types)
    }

    private val coreOptions = combine(
        settings.maskNumbers,
        settings.tagsShow
    ) { mask, tagsShow ->
        CoreOptions(mask, tagsShow)
    }

    private val listOptions = combine(
        settings.expiredCards,
        settings.expiresSoonMonths,
        settings.showNotes
    ) { expiredCards, expiresSoonMonths, showNotes ->
        ListOptions(expiredCards, expiresSoonMonths, showNotes)
    }

    private val options = combine(
        coreOptions,
        settings.clipClearSec,
        listOptions
    ) { core, clipClearSec, list ->
        HomeOptions(
            maskNumbers = core.maskNumbers,
            tagsShow = core.tagsShow,
            clipClearSec = clipClearSec,
            expiredCards = list.expiredCards,
            expiresSoonMonths = list.expiresSoonMonths,
            showNotes = list.showNotes
        )
    }

    private val searchState = combine(searchOn, query) { on, text -> SearchState(on, text) }

    private data class Extras(val usage: Map<String, Long>, val tipsSeen: Boolean)

    private val extras = combine(CardUsage.observe(application), tipsSeen.filterNotNull()) { used, seen -> Extras(used, seen) }

    private data class UiFilters(
        val cardFilters: CardFilters,
        val search: SearchState,
        val status: Set<String>,
        val reviewOnly: Boolean
    )

    private val uiFilters = combine(
        filters,
        searchState,
        statusFilters,
        reviewOnly
    ) { cardFilters, search, status, review ->
        UiFilters(cardFilters, search, status, review)
    }

    private val expiredLabel = application.getString(R.string.cards_status_expired)
    private val soonLabel = application.getString(R.string.cards_status_expires_soon)

    private data class DueInputs(val enabled: Boolean, val paid: Set<String>, val day: LocalDate)

    private val dueInputs = combine(
        settings.paymentReminders,
        flow { emitAll(PaidStore.paid(application)) }.flowOn(Dispatchers.IO),
        todayFlow
    ) { enabled, paid, day -> DueInputs(enabled, paid, day) }

    private val listsWithDue = combine(lists, dueInputs, extras) { l, d, e -> Triple(l, d, e) }

    val uiState: StateFlow<HomeUiState> = combine(
        listsWithDue,
        repository.tags,
        options,
        repository.repairSummary,
        uiFilters
    ) { (lists, due, extras), tags, options, repair, ui ->
        buildState(lists, due, extras, tags, options, repair, ui)
    }.flowOn(Dispatchers.Default)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER_MS), HomeUiState())

    /** Search and the first-run tips take the whole window, so the navigation bar hides. */
    val hidesNavBar: StateFlow<Boolean> = combine(searchOn, uiState) { on, state -> on || state.showTips }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_AFTER_MS), false)

    fun finishTips() {
        tipsSeen.value = true
        viewModelScope.launch(Dispatchers.IO) { FirstRunTips.markSeen(getApplication()) }
    }

    fun setSearchActive(active: Boolean) {
        searchOn.value = active
        if (!active) query.value = ""
    }

    fun setQuery(value: String) {
        query.value = value
    }

    fun clearSearch() {
        searchOn.value = false
        query.value = ""
    }

    fun selectHolder(holderId: String?) {
        filters.update { it.withHolder(holderId) }
    }

    fun applyFilters(value: CardFilters) {
        filters.value = value
        statusFilters.value = emptySet()
        reviewOnly.value = false
    }

    fun applyFilters(value: CardFilters, status: Set<String>, review: Boolean) {
        filters.value = value
        statusFilters.value = status
        reviewOnly.value = review
    }

    fun removeFilter(group: FilterGroup, id: String) {
        filters.update { it.without(group, id) }
    }

    fun removeStatusFilter(id: String) {
        statusFilters.update { it - id }
    }

    fun removeReviewFilter() {
        reviewOnly.value = false
    }

    /** Empty-note action: turn on the Expired status filter. */
    fun showExpired() {
        statusFilters.update { it + CardsStatusFilter.EXPIRED }
        searchOn.value = false
        query.value = ""
    }

    /** Repair banner Review: clear everything else, show cards needing a check. */
    fun showNeedsCheck() {
        filters.value = CardFilters()
        statusFilters.value = emptySet()
        reviewOnly.value = true
        searchOn.value = false
        query.value = ""
        viewModelScope.launch { repository.dismissRepairSummary() }
    }

    fun dismissRepairSummary() {
        viewModelScope.launch { repository.dismissRepairSummary() }
    }

    private fun buildState(
        lists: Lists,
        due: DueInputs,
        extras: Extras,
        tags: List<Tag>,
        options: HomeOptions,
        repair: RepairSummary?,
        ui: UiFilters
    ): HomeUiState {
        val mapper = HomeCardMapper(lists.networks, lists.holders, lists.banks, lists.types, tags, bankFallback)
        val today = due.day
        val mapped = lists.cards.map { card ->
            val dueDate = if (due.enabled && !card.archived) ReminderRules.dueReminderDate(card.dueDay, today) else null
            val dueKey = dueDate?.let { ReminderRules.dueKey(card.id, it) }
            mapper.map(card, today, options.expiresSoonMonths).copy(
                dueInDays = dueDate?.takeIf { dueKey !in due.paid }
                    ?.let { ChronoUnit.DAYS.between(today, it).toInt() },
                dueKey = dueKey
            )
        }
        val allCards = CardUseStatus.withBestToday(mapped, today)
        val cards = allCards.filterNot { it.card.archived }
        val recent = CardUseStatus.recentCards(cards, extras.usage)
        val entries = cards.map { it.entry }
        val holderRefs = lists.holders.map { NamedRef(it.id, it.name) }
        val bankRefs = lists.banks.map { NamedRef(it.id, it.name) }
        val networkRefs = lists.networks.map { NamedRef(it.id, it.name) }
        val typeRefs = lists.types.map { NamedRef(it.id, it.name) }
        val tagRefs = tags.map { NamedRef(it.id, it.name) }

        val activeQuery = if (ui.search.on) ui.search.query else ""
        val searching = ui.search.on && ui.search.query.isNotBlank()
        val visibleCards = CardsListState.visibleCards(
            cards = allCards,
            filters = ui.cardFilters,
            query = activeQuery,
            options = CardsListOptions(
                placement = options.expiredCards,
                searching = searching,
                status = CardsStatusFilter.states(ui.status),
                reviewOnly = ui.reviewOnly
            )
        )

        return HomeUiState(
            loading = false,
            hasAnyCards = allCards.isNotEmpty(),
            allCards = allCards,
            cards = cards,
            visibleCards = visibleCards,
            holderChips = CardQuery.holdersWithCards(holderRefs, entries),
            filterGroups = CardQuery.filterGroups(entries, holderRefs, bankRefs, networkRefs, typeRefs, tagRefs),
            statusOptions = CardsListState.statusOptions(allCards, expiredLabel, soonLabel),
            filters = ui.cardFilters,
            statusFilter = ui.status,
            reviewOnly = ui.reviewOnly,
            needsCheckCount = CardsListState.needsCheckCount(allCards),
            emptyNote = CardsListState.emptyNote(
                cards = allCards,
                placement = options.expiredCards,
                searching = ui.search.on,
                filtersActive = !ui.cardFilters.isEmpty ||
                    ui.status.isNotEmpty() ||
                    ui.reviewOnly
            ),
            repairSummary = repair,
            recentCards = recent,
            lastUsedAt = recent.firstOrNull()?.let { extras.usage[it.id] },
            today = today,
            showTips = !extras.tipsSeen && allCards.isEmpty(),
            searchOn = ui.search.on,
            query = ui.search.query,
            options = options
        )
    }

    private companion object {
        /** Long enough to ride out a rotation; after that the list stops decrypting in the background. */
        const val STOP_AFTER_MS = 5_000L
    }
}
