package com.quickcards.app.feature.cardform

import android.app.Application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.quickcards.app.QuickCardsApplication
import com.quickcards.app.R
import com.quickcards.app.core.data.model.Bank
import com.quickcards.app.core.data.model.CardNetwork
import com.quickcards.app.core.data.model.CardRepair
import com.quickcards.app.core.data.model.CardType
import com.quickcards.app.core.data.model.Holder
import com.quickcards.app.core.data.model.Tag
import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.app.core.data.model.toDomain
import com.quickcards.app.core.data.repository.DuplicateCardException
import com.quickcards.app.core.data.settings.AppSettingsDefaults
import com.quickcards.domain.CardError
import com.quickcards.domain.CardField
import com.quickcards.domain.EntityKind
import com.quickcards.domain.ExistingCardRef
import com.quickcards.domain.Field
import com.quickcards.domain.NamedRef
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Locale

sealed interface CardFormEvent {
    data class FocusField(val field: Field) : CardFormEvent
    data class Saved(
        val cardId: String,
        val isEdit: Boolean,
        val networkName: String?
    ) : CardFormEvent

    data object SaveFailed : CardFormEvent
}

/** Adapts Room and settings to the pure [CardFormSession], which holds every editing rule. */
class CardFormViewModel(
    application: Application,
    savedStateHandle: SavedStateHandle
) : AndroidViewModel(application) {

    private val repository = (application as QuickCardsApplication).repository
    private val settings = (application as QuickCardsApplication).settings

    val cardId: String? = savedStateHandle.get<String>("cardId")

    private val _session = MutableStateFlow<CardFormSession?>(null)
    val session: StateFlow<CardFormSession?> = _session.asStateFlow()

    private val _saving = MutableStateFlow(false)
    val saving: StateFlow<Boolean> = _saving.asStateFlow()

    /** Automatic repairs still inside their 30-day undo window, keyed by field. */
    private val _repairs = MutableStateFlow<Map<CardField, CardRepair>>(emptyMap())
    val repairs: StateFlow<Map<CardField, CardRepair>> = _repairs.asStateFlow()

    private val _events = MutableSharedFlow<CardFormEvent>(extraBufferCapacity = 8)
    val events: SharedFlow<CardFormEvent> = _events.asSharedFlow()

    private var initialized = false
    private var editingCard: VaultCard? = null

    private data class Lists(
        val cards: List<VaultCard> = emptyList(),
        val holders: List<Holder> = emptyList(),
        val banks: List<Bank> = emptyList(),
        val networks: List<CardNetwork> = emptyList(),
        val types: List<CardType> = emptyList(),
        val tags: List<Tag> = emptyList()
    )

    private data class Options(
        val tagsShow: Boolean,
        val showNotes: Boolean,
        val expiresSoonMonths: Int
    )

    private val listsFlow = combine(
        repository.cards,
        repository.holders,
        repository.banks,
        repository.networks,
        repository.cardTypes
    ) { cards, holders, banks, networks, types ->
        Lists(cards, holders, banks, networks, types)
    }.combine(repository.tags) { lists, tags -> lists.copy(tags = tags) }

    private val optionsFlow = combine(
        settings.tagsShow,
        settings.showNotes,
        settings.expiresSoonMonths
    ) { tagsShow, showNotes, expiresSoonMonths ->
        Options(tagsShow, showNotes, expiresSoonMonths)
    }

    init {
        viewModelScope.launch {
            combine(listsFlow, optionsFlow) { lists, options -> lists to options }
                .flowOn(Dispatchers.Default)
                .collect { (lists, options) ->
                    val build = buildLists(lists, options)
                    val current = _session.value
                    if (!initialized) {
                        val card = cardId?.let { id -> lists.cards.firstOrNull { it.id == id } }
                        if (cardId != null && card == null) return@collect
                        editingCard = card
                        _session.value = if (card != null) {
                            CardFormSession.forEdit(card, build.lists, build.options)
                                .showReviewErrors(card.needsReview)
                        } else {
                            CardFormSession.forAdd(build.lists, build.options)
                        }
                        initialized = true
                    } else if (current != null) {
                        _session.value = current.withLists(build.lists, build.options)
                    }
                }
        }

        if (cardId != null) {
            viewModelScope.launch {
                _repairs.value = repository.repairsFor(cardId).associateBy { it.field }
            }
        }
    }

    private data class BuiltLists(
        val lists: CardFormLists,
        val options: CardFormOptions
    )

    private fun buildLists(lists: Lists, options: Options): BuiltLists {
        val domainNetworks = lists.networks.map { it.toDomain() }
        val holdersById = lists.holders.associateBy { it.id }
        val banksById = lists.banks.associateBy { it.id }

        val duplicates = lists.cards
            .filterNot { it.id == cardId }
            .associate { card ->
                card.number to ExistingCardRef(
                    title = cardTitle(card, lists.banks),
                    holderName = card.holderId?.let { holdersById[it]?.name }
                )
            }

        val tagRefs = lists.tags
            .sortedWith(compareBy({ it.createdAt }, { it.name.lowercase(Locale.ROOT) }))
            .map { NamedRef(it.id, it.name) }

        return BuiltLists(
            lists = CardFormLists(
                holders = lists.holders.map { NamedRef(it.id, it.name) },
                banks = lists.banks
                    .map { NamedRef(it.id, it.name) }
                    .sortedBy { it.name.lowercase(Locale.ROOT) },
                networks = domainNetworks,
                types = lists.types
                    .sortedWith(compareBy({ it.sortOrder }, { it.name.lowercase(Locale.ROOT) }))
                    .map { NamedRef(it.id, it.name) },
                tags = tagRefs,
                tagLimit = AppSettingsDefaults.TAGS_MAX,
                tagMaxLen = AppSettingsDefaults.TAGS_MAX_LEN,
                duplicates = duplicates
            ),
            options = CardFormOptions(
                tagsShow = options.tagsShow,
                showNotes = options.showNotes,
                expiresSoonMonths = options.expiresSoonMonths
            )
        )
    }

    private fun cardTitle(card: VaultCard, banks: List<Bank>): String =
        card.bankId?.let { id -> banks.firstOrNull { it.id == id }?.name }
            ?: getApplication<Application>().getString(R.string.form_field_bank)

    private fun update(block: (CardFormSession) -> CardFormSession) {
        _session.update { current -> current?.let(block) }
    }

    fun updateNumber(text: String) = update { it.updateNumber(text) }
    fun numberBlur() = update { it.numberBlur() }
    fun updateExpiry(text: String) = update { it.updateExpiry(text) }
    fun expiryBlur() = update { it.expiryBlur() }
    fun updateCvv(text: String) = update { it.updateCvv(text) }
    fun cvvBlur() = update { it.cvvBlur() }
    fun updateNote(text: String) = update { it.updateNote(text) }
    fun updateDueDay(text: String) = update { it.updateDueDay(text) }

    /** Reverts the latest repair in vault and form; the restored legacy value is validated at once. */
    fun undoRepair(field: CardField) {
        val id = cardId ?: return
        val repair = _repairs.value[field] ?: return
        viewModelScope.launch {
            if (repository.undoRepair(id, field)) {
                _repairs.update { repairs -> repairs - field }
                update { session -> session.restoreRepair(field, repair.before) }
            }
        }
    }

    fun selectHolder(id: String) = update { it.selectHolder(id) }
    fun selectBank(id: String) = update { it.selectBank(id) }
    fun selectType(id: String?) = update { it.selectType(id) }
    fun selectBand(key: String) = update { it.selectBand(key) }
    fun toggleTag(id: String) = update { it.toggleTag(id) }
    fun removeTag(id: String) = update { it.removeTag(id) }

    fun save() {
        val current = _session.value ?: return
        if (_saving.value) return
        val attempted = current.saveAttempt()
        _session.value = attempted
        val invalid = attempted.firstInvalid()
        if (invalid != null) {
            _events.tryEmit(CardFormEvent.FocusField(invalid))
            return
        }

        val form = attempted.form
        val network = form.network(attempted.lists.networks)
        val base = editingCard
        val holderName = attempted.lists.holderName(form.holderId).orEmpty()
        // Editing another field must not silently rewrite the imported network override or a
        // name-on-card that differs from the holder's display name.
        val holderChanged = base != null && base.holderId != form.holderId
        // A number edit re-detects the network; otherwise an imported override survives.
        val numberChanged = base != null && base.number != form.number
        val nameOnCard = if (base != null && !holderChanged && base.nameOnCard.isNotBlank()) {
            base.nameOnCard
        } else {
            holderName.trim().uppercase(Locale.ROOT)
        }
        val card = VaultCard(
            id = base?.id ?: java.util.UUID.randomUUID().toString(),
            number = form.number,
            expiry = form.expiry,
            cvv = form.cvv,
            holderId = form.holderId,
            bankId = form.bankId,
            networkId = if (numberChanged) null else base?.networkId,
            networkAuto = if (numberChanged) true else base?.networkAuto ?: true,
            typeId = form.typeId,
            nameOnCard = nameOnCard,
            cardName = base?.cardName.orEmpty(),
            note = form.note.trim(),
            dueDay = form.dueDay.toIntOrNull()?.takeIf { it in 1..31 },
            bandColor = form.bandColor,
            sortOrder = base?.sortOrder ?: 0,
            createdAt = base?.createdAt ?: System.currentTimeMillis(),
            updatedAt = System.currentTimeMillis(),
            tagIds = form.tagIds
        )

        viewModelScope.launch {
            _saving.value = true
            val result = if (base != null) {
                repository.updateCard(card)
            } else {
                repository.insertCard(card)
            }
            _saving.value = false
            result.fold(
                onSuccess = { saved ->
                    _events.tryEmit(
                        CardFormEvent.Saved(
                            cardId = saved.id,
                            isEdit = base != null,
                            networkName = network?.name
                        )
                    )
                },
                onFailure = { error -> handleSaveFailure(error) }
            )
        }
    }

    private suspend fun handleSaveFailure(error: Throwable) {
        val session = _session.value ?: return
        val duplicate = when (error) {
            is DuplicateCardException -> {
                error.existingCardId?.let { repository.getCard(it) }
                    ?: repository.findDuplicate(session.form.number)
            }
            else -> null
        }
        if (duplicate != null) {
            val bank = duplicate.bankId
                ?.let { id -> session.lists.banks.firstOrNull { it.id == id }?.name }
                ?: getApplication<Application>().getString(R.string.form_field_bank)
            val holder = session.lists.holderName(duplicate.holderId)
            update { it.withError(Field.NUMBER, CardError.Duplicate(bank, holder)) }
            _events.tryEmit(CardFormEvent.FocusField(Field.NUMBER))
            return
        }
        _events.tryEmit(CardFormEvent.SaveFailed)
    }

    suspend fun createTag(name: String): Tag? =
        runCatching { repository.createTag(name) }.getOrNull()

    fun onTagCreated(tag: Tag) = update { it.copy(lists = it.lists.withTag(tag.id, tag.name)).addTag(tag.id) }

    /** The dialog already persisted the item; wait for the flow to expose it before selecting. */
    fun onEntitySaved(kind: EntityKind, id: String) {
        viewModelScope.launch {
            when (kind) {
                EntityKind.HOLDER -> {
                    val holder = repository.holders
                        .first { list -> list.any { it.id == id } }
                        .first { it.id == id }
                    update {
                        it.copy(lists = it.lists.withHolder(holder.id, holder.name))
                            .selectHolder(holder.id)
                    }
                }

                EntityKind.BANK -> {
                    val bank = repository.banks
                        .first { list -> list.any { it.id == id } }
                        .first { it.id == id }
                    update {
                        it.copy(lists = it.lists.withBank(bank.id, bank.name))
                            .selectBank(bank.id)
                    }
                }

                EntityKind.TYPE -> {
                    val type = repository.cardTypes
                        .first { list -> list.any { it.id == id } }
                        .first { it.id == id }
                    update {
                        it.copy(lists = it.lists.withType(type.id, type.name))
                            .selectType(type.id)
                    }
                }

                EntityKind.NETWORK -> {
                    val network = repository.networks
                        .first { list -> list.any { it.id == id } }
                        .first { it.id == id }
                        .toDomain()
                    update { it.copy(lists = it.lists.withNetwork(network)) }
                }

                EntityKind.TAG -> Unit
            }
        }
    }
}
