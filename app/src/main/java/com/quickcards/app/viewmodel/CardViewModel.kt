package com.quickcards.app.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.asLiveData
import androidx.lifecycle.viewModelScope
import com.quickcards.app.data.database.QuickCardsDatabase
import com.quickcards.app.data.model.Card
import com.quickcards.app.security.EncryptionHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

class CardViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = QuickCardsDatabase.getDatabase(application, viewModelScope)
    private val cardDao = database.cardDao()
    private val encryptionHelper = EncryptionHelper.getInstance()
    
    private val _isLoading = MutableLiveData<Boolean>(true) // Start with loading true
    val isLoading: LiveData<Boolean> = _isLoading
    
    private val _allCards = MutableLiveData<List<Card>>()
    val allCards: LiveData<List<Card>> = _allCards
    
    init {
        observeCards()
    }
    
    private fun observeCards() {
        _isLoading.postValue(true)
        cardDao.getAllCards().asLiveData().observeForever { encryptedCards ->
            val decryptedCards = encryptedCards.map { card: Card ->
                getDecryptedCard(card)
            }
            _allCards.postValue(decryptedCards)
            _isLoading.postValue(false)
        }
    }
    
    private val _searchQuery = MutableLiveData<String>()
    val searchQuery: LiveData<String> = _searchQuery
    
    private val _searchResults = MutableLiveData<List<Card>>()
    val searchResults: LiveData<List<Card>> = _searchResults
    
    fun searchCards(query: String) {
        _searchQuery.value = query
        if (query.isBlank()) {
            _searchResults.value = emptyList()
            return
        }
        
        viewModelScope.launch(Dispatchers.IO) {
            try {
                cardDao.searchCards(query).collect { encryptedCards ->
                    val decryptedResults = encryptedCards.map { card: Card ->
                        getDecryptedCard(card)
                    }
                    _searchResults.postValue(decryptedResults)
                }
            } catch (e: Exception) {
                _searchResults.postValue(emptyList())
            }
        }
    }
    
    fun insertCard(card: Card) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Ensure card has a valid ID
                val cardWithValidId = if (card.id.isBlank()) {
                    card.copy(id = java.util.UUID.randomUUID().toString())
                } else {
                    card
                }
                
                val encryptedCard = cardWithValidId.copy(
                    cardNumber = encryptionHelper.encrypt(cardWithValidId.cardNumber),
                    expiryDate = encryptionHelper.encrypt(cardWithValidId.expiryDate),
                    cvv = encryptionHelper.encrypt(cardWithValidId.cvv)
                )
                cardDao.insertCard(encryptedCard)
                // LiveData will automatically update
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun updateCard(card: Card) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val encryptedCard = card.copy(
                    cardNumber = encryptionHelper.encrypt(card.cardNumber),
                    expiryDate = encryptionHelper.encrypt(card.expiryDate),
                    cvv = encryptionHelper.encrypt(card.cvv),
                    updatedAt = System.currentTimeMillis()
                )
                cardDao.updateCard(encryptedCard)
                // LiveData will automatically update
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun deleteCard(card: Card) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                cardDao.deleteCard(card)
                // LiveData will automatically update
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun deleteCardById(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                cardDao.deleteCardById(id)
                // LiveData will automatically update
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun deleteAllCards() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                cardDao.deleteAllCards()
                // LiveData will automatically update
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    // Method to fix any cards with empty IDs
    fun fixCardsWithEmptyIds() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // This would need to be implemented in DAO if needed
                // For now, clearing all data and restarting should fix the issue
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    suspend fun getCardById(id: String): Card? {
        return try {
            val encryptedCard = cardDao.getCardById(id)
            encryptedCard?.let { card ->
                card.copy(
                    cardNumber = encryptionHelper.decrypt(card.cardNumber),
                    expiryDate = encryptionHelper.decrypt(card.expiryDate),
                    cvv = encryptionHelper.decrypt(card.cvv)
                )
            }
        } catch (e: Exception) {
            null
        }
    }
    
    fun getDecryptedCard(card: Card): Card {
        return try {
            card.copy(
                cardNumber = encryptionHelper.decrypt(card.cardNumber),
                expiryDate = encryptionHelper.decrypt(card.expiryDate),
                cvv = encryptionHelper.decrypt(card.cvv)
            )
        } catch (e: Exception) {
            card
        }
    }
    
    // Export cards to JSON
    suspend fun exportCardsToJson(): String {
        return try {
            val encryptedCards = cardDao.getAllCardsSync()
            val decryptedCards = encryptedCards.map { card: Card ->
                getDecryptedCard(card)
            }
            // Convert to JSON using Gson or similar
            val gson = com.google.gson.Gson()
            gson.toJson(decryptedCards)
        } catch (e: Exception) {
            throw Exception("Failed to export cards: ${e.message}")
        }
    }
    
    // Import cards from JSON
    suspend fun importCardsFromJson(jsonData: String): Result<Int> {
        return try {
            val gson = com.google.gson.Gson()
            val cardType = object : com.google.gson.reflect.TypeToken<List<Card>>() {}.type
            val importedCards: List<Card> = gson.fromJson(jsonData, cardType)
            
            // Validate each card
            var importedCount = 0
            for (card in importedCards) {
                if (isValidCard(card)) {
                    val cardWithNewId = card.copy(
                        id = java.util.UUID.randomUUID().toString(),
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    
                    val encryptedCard = cardWithNewId.copy(
                        cardNumber = encryptionHelper.encrypt(cardWithNewId.cardNumber),
                        expiryDate = encryptionHelper.encrypt(cardWithNewId.expiryDate),
                        cvv = encryptionHelper.encrypt(cardWithNewId.cvv)
                    )
                    cardDao.insertCard(encryptedCard)
                    importedCount++
                }
            }
            
            // LiveData will automatically update
            Result.success(importedCount)
        } catch (e: Exception) {
            Result.failure(Exception("Failed to import cards: ${e.message}"))
        }
    }
    
    // Validate card data structure
    private fun isValidCard(card: Card): Boolean {
        return try {
            // Check required fields
            card.cardNumber.isNotBlank() &&
            card.owner.isNotBlank() &&
            card.expiryDate.isNotBlank() &&
            card.cvv.isNotBlank() &&
            card.bankName.isNotBlank() &&
            card.cardType.isNotBlank() &&
            card.cardIssuer.isNotBlank() &&
            card.cardVariant.isNotBlank()
        } catch (e: Exception) {
            false
        }
    }
}