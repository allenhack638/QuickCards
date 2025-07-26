package com.quickcards.app.data.model

import androidx.compose.runtime.Stable

@Stable
data class CardFilter(
    val type: FilterType,
    val value: String
) {
    enum class FilterType {
        BANK,
        CARD_ISSUER,
        CARD_TYPE
    }
    
    companion object {
        fun bank(value: String) = CardFilter(FilterType.BANK, value)
        fun cardIssuer(value: String) = CardFilter(FilterType.CARD_ISSUER, value)
        fun cardType(value: String) = CardFilter(FilterType.CARD_TYPE, value)
    }
}

@Stable
data class FilterState(
    val activeFilters: Set<CardFilter> = emptySet(),
    val availableBanks: Set<String> = emptySet(),
    val availableCardIssuers: Set<String> = emptySet(),
    val availableCardTypes: Set<String> = emptySet()
) {
    fun hasActiveFilters(): Boolean = activeFilters.isNotEmpty()
    
    fun getActiveFiltersByType(type: CardFilter.FilterType): Set<String> {
        return activeFilters
            .filter { it.type == type }
            .map { it.value }
            .toSet()
    }
    
    fun addFilter(filter: CardFilter): FilterState {
        return copy(activeFilters = activeFilters + filter)
    }
    
    fun removeFilter(filter: CardFilter): FilterState {
        return copy(activeFilters = activeFilters - filter)
    }
    
    fun clearAllFilters(): FilterState {
        return copy(activeFilters = emptySet())
    }
    
    fun updateAvailableOptions(cards: List<Card>): FilterState {
        val banks = cards.map { it.bankName }.distinct().toSet()
        val issuers = cards.map { it.cardIssuer }.distinct().toSet()
        val types = cards.map { it.cardType }.distinct().toSet()
        
        return copy(
            availableBanks = banks,
            availableCardIssuers = issuers,
            availableCardTypes = types
        )
    }
} 