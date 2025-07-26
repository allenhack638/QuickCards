package com.quickcards.app.utils

import android.content.Context
import android.content.SharedPreferences
import com.quickcards.app.data.model.Card
import com.quickcards.app.data.model.CardFilter
import com.quickcards.app.data.model.FilterState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FilterManager private constructor(context: Context) {
    
    companion object {
        private const val PREFS_NAME = "card_filters_prefs"
        private const val KEY_ACTIVE_FILTERS = "active_filters"
        
        @Volatile
        private var INSTANCE: FilterManager? = null
        
        fun getInstance(context: Context): FilterManager {
            return INSTANCE ?: synchronized(this) {
                val instance = FilterManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
    
    private val prefs: SharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()
    
    init {
        loadFilters()
    }
    
    private fun loadFilters() {
        val filterStrings = prefs.getStringSet(KEY_ACTIVE_FILTERS, emptySet()) ?: emptySet()
        val activeFilters = filterStrings.mapNotNull { filterString ->
            try {
                val parts = filterString.split(":")
                if (parts.size == 2) {
                    val type = CardFilter.FilterType.valueOf(parts[0])
                    val value = parts[1]
                    CardFilter(type, value)
                } else null
            } catch (e: Exception) {
                null
            }
        }.toSet()
        
        _filterState.value = _filterState.value.copy(activeFilters = activeFilters)
    }
    
    private fun saveFilters() {
        val filterStrings = _filterState.value.activeFilters.map { "${it.type}:${it.value}" }.toSet()
        prefs.edit().putStringSet(KEY_ACTIVE_FILTERS, filterStrings).apply()
    }
    
    fun addFilter(filter: CardFilter) {
        val newState = _filterState.value.addFilter(filter)
        _filterState.value = newState
        saveFilters()
    }
    
    fun removeFilter(filter: CardFilter) {
        val newState = _filterState.value.removeFilter(filter)
        _filterState.value = newState
        saveFilters()
    }
    
    fun clearAllFilters() {
        val newState = _filterState.value.clearAllFilters()
        _filterState.value = newState
        saveFilters()
    }
    
    fun updateAvailableOptions(cards: List<Card>) {
        val newState = _filterState.value.updateAvailableOptions(cards)
        _filterState.value = newState
    }
    
    fun applyFilters(cards: List<Card>): List<Card> {
        if (!_filterState.value.hasActiveFilters()) {
            return cards
        }
        
        return cards.filter { card ->
            val activeBanks = _filterState.value.getActiveFiltersByType(CardFilter.FilterType.BANK)
            val activeIssuers = _filterState.value.getActiveFiltersByType(CardFilter.FilterType.CARD_ISSUER)
            val activeTypes = _filterState.value.getActiveFiltersByType(CardFilter.FilterType.CARD_TYPE)
            
            val bankMatches = activeBanks.isEmpty() || activeBanks.contains(card.bankName)
            val issuerMatches = activeIssuers.isEmpty() || activeIssuers.contains(card.cardIssuer)
            val typeMatches = activeTypes.isEmpty() || activeTypes.contains(card.cardType)
            
            bankMatches && issuerMatches && typeMatches
        }
    }
} 