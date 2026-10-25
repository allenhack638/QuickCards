package com.quickcards.domain

/**
 * Pure port of visibleCards, passes, filterCount and the filter-sheet groups
 * from prototype/v2.html. No Android dependencies.
 */
data class CardQueryEntry(
    val id: String,
    val number: String,
    val bankId: String?,
    val bankName: String,
    val holderId: String?,
    val holderName: String,
    val typeId: String?,
    val typeName: String,
    val networkId: String?,
    val networkName: String,
    val networkCode: String,
    val note: String,
    val tagIds: List<String>,
    val tagNames: List<String>
) {
    val lastFour: String get() = number.takeLast(4)

    val title: String
        get() = if (typeName.isBlank()) bankName else "$bankName $typeName"
}

enum class FilterGroup(val label: String) {
    HOLDER("Card holder"),
    BANK("Bank"),
    NETWORK("Network"),
    TYPE("Type"),
    TAG("Tag")
}

data class CardFilters(
    val holders: Set<String> = emptySet(),
    val banks: Set<String> = emptySet(),
    val networks: Set<String> = emptySet(),
    val types: Set<String> = emptySet(),
    val tags: Set<String> = emptySet()
) {
    val activeCount: Int
        get() = banks.size + networks.size + types.size + tags.size

    val isEmpty: Boolean
        get() = holders.isEmpty() && activeCount == 0

    fun withHolder(holderId: String?): CardFilters =
        copy(holders = if (holderId == null) emptySet() else setOf(holderId))

    fun without(group: FilterGroup, id: String): CardFilters = when (group) {
        FilterGroup.HOLDER -> copy(holders = holders - id)
        FilterGroup.BANK -> copy(banks = banks - id)
        FilterGroup.NETWORK -> copy(networks = networks - id)
        FilterGroup.TYPE -> copy(types = types - id)
        FilterGroup.TAG -> copy(tags = tags - id)
    }

    fun toggle(group: FilterGroup, id: String): CardFilters = when (group) {
        FilterGroup.HOLDER -> copy(holders = holders.toggled(id))
        FilterGroup.BANK -> copy(banks = banks.toggled(id))
        FilterGroup.NETWORK -> copy(networks = networks.toggled(id))
        FilterGroup.TYPE -> copy(types = types.toggled(id))
        FilterGroup.TAG -> copy(tags = tags.toggled(id))
    }

    fun ids(group: FilterGroup): Set<String> = when (group) {
        FilterGroup.HOLDER -> holders
        FilterGroup.BANK -> banks
        FilterGroup.NETWORK -> networks
        FilterGroup.TYPE -> types
        FilterGroup.TAG -> tags
    }

    private fun Set<String>.toggled(id: String): Set<String> =
        if (contains(id)) this - id else this + id
}

data class NamedRef(val id: String, val name: String)

data class FilterOption(val id: String, val label: String, val count: Int)

data class FilterGroupOptions(val group: FilterGroup, val options: List<FilterOption>)

object CardQuery {

    fun passes(entry: CardQueryEntry, filters: CardFilters): Boolean {
        if (filters.holders.isNotEmpty() && entry.holderId !in filters.holders) return false
        if (filters.banks.isNotEmpty() && entry.bankId !in filters.banks) return false
        if (filters.networks.isNotEmpty() && entry.networkId !in filters.networks) return false
        if (filters.types.isNotEmpty() && entry.typeId !in filters.types) return false
        if (filters.tags.isNotEmpty() && entry.tagIds.none { it in filters.tags }) return false
        return true
    }

    fun visibleCards(
        cards: List<CardQueryEntry>,
        filters: CardFilters,
        query: String,
        tagsSearch: Boolean
    ): List<CardQueryEntry> {
        val text = query.trim().lowercase()
        val digits = text.replace(Regex("\\s+"), "")
        return cards.filter { entry ->
            if (!passes(entry, filters)) return@filter false
            if (text.isEmpty()) return@filter true
            if (digits.isNotEmpty() && digits.all { it in '0'..'9' } && entry.number.contains(digits)) {
                return@filter true
            }
            val fields = mutableListOf(
                entry.bankName,
                entry.holderName,
                entry.note,
                entry.typeName,
                entry.networkName,
                entry.networkCode
            )
            if (tagsSearch) fields.addAll(entry.tagNames)
            fields.any { it.lowercase().contains(text) }
        }
    }

    fun filterCount(filters: CardFilters): Int = filters.activeCount

    fun holdersWithCards(holders: List<NamedRef>, cards: List<CardQueryEntry>): List<NamedRef> =
        holders.filter { holder -> cards.any { it.holderId == holder.id } }

    fun holderFirstName(name: String): String = name.trim().substringBefore(' ')

    fun filterGroups(
        cards: List<CardQueryEntry>,
        holders: List<NamedRef>,
        banks: List<NamedRef>,
        networks: List<NamedRef>,
        types: List<NamedRef>,
        tags: List<NamedRef>
    ): List<FilterGroupOptions> {
        val groups = listOf(
            FilterGroupOptions(
                FilterGroup.HOLDER,
                usedOptions(holders, cards) { entry, id -> entry.holderId == id }
            ),
            FilterGroupOptions(
                FilterGroup.BANK,
                usedOptions(banks, cards) { entry, id -> entry.bankId == id }
            ),
            FilterGroupOptions(
                FilterGroup.NETWORK,
                usedOptions(networks, cards) { entry, id -> entry.networkId == id }
            ),
            FilterGroupOptions(
                FilterGroup.TYPE,
                usedOptions(types, cards) { entry, id -> entry.typeId == id }
            ),
            FilterGroupOptions(
                FilterGroup.TAG,
                usedOptions(tags, cards) { entry, id -> id in entry.tagIds }
            )
        )
        return groups.filter { it.options.isNotEmpty() }
    }

    private fun usedOptions(
        refs: List<NamedRef>,
        cards: List<CardQueryEntry>,
        matches: (CardQueryEntry, String) -> Boolean
    ): List<FilterOption> = refs.mapNotNull { ref ->
        val count = cards.count { matches(it, ref.id) }
        if (count == 0) null else FilterOption(ref.id, ref.name, count)
    }
}

object TimeText {

    /** secText from prototype/v2.html. */
    fun seconds(value: Int): String = when {
        value <= 0 -> "Off"
        value < 60 -> "$value seconds"
        value == 60 -> "1 minute"
        else -> "${value / 60} minutes"
    }

    /** Short clipboard wording: 30s, 1 min, 2 min. */
    fun clipText(value: Int): String = seconds(value)
        .replace(" seconds", "s")
        .replace(" minutes", " min")
        .replace(" minute", " min")
}
