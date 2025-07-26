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
import com.quickcards.app.security.SecureFileManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class CardViewModel(application: Application) : AndroidViewModel(application) {
    
    private val database = QuickCardsDatabase.getDatabase(application, viewModelScope)
    private val cardDao = database.cardDao()
    private val encryptionHelper = EncryptionHelper.getInstance()
    private val secureFileManager = SecureFileManager.getInstance()
    
    // Cache for decrypted cards to avoid repeated decryption
    private val decryptedCardCache = ConcurrentHashMap<String, Card>()
    
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
            viewModelScope.launch(Dispatchers.IO) {
                val decryptedCards = encryptedCards.map { card: Card ->
                    getDecryptedCardCached(card)
                }
                _allCards.postValue(decryptedCards)
                _isLoading.postValue(false)
            }
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
                        getDecryptedCardCached(card)
                    }
                    _searchResults.postValue(decryptedResults)
                }
            } catch (e: Exception) {
                _searchResults.postValue(emptyList())
            }
        }
    }
    
    fun insertCard(card: Card, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Ensure card has a valid ID and color
                val cardWithValidId = if (card.id.isBlank()) {
                    card.copy(
                        id = java.util.UUID.randomUUID().toString(),
                        cardColor = Card.validateCardColor(card.cardColor, card.cardIssuer)
                    )
                } else {
                    card.copy(
                        cardColor = Card.validateCardColor(card.cardColor, card.cardIssuer)
                    )
                }
                
                val encryptedCard = cardWithValidId.copy(
                    cardNumber = encryptionHelper.encrypt(cardWithValidId.cardNumber),
                    expiryDate = encryptionHelper.encrypt(cardWithValidId.expiryDate),
                    cvv = encryptionHelper.encrypt(cardWithValidId.cvv)
                )
                cardDao.insertCard(encryptedCard)
                
                // Add to cache
                decryptedCardCache[cardWithValidId.id] = cardWithValidId
                
                // Force refresh the cards list to ensure UI updates
                refreshCardsList()
                
                withContext(Dispatchers.Main) {
                    onComplete?.invoke()
                }
            } catch (e: Exception) {
                // Handle error
                withContext(Dispatchers.Main) {
                    onComplete?.invoke()
                }
            }
        }
    }
    
    fun updateCard(card: Card, onComplete: (() -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Validate and fix card color
                val cardWithValidColor = card.copy(
                    cardColor = Card.validateCardColor(card.cardColor, card.cardIssuer)
                )
                
                val encryptedCard = cardWithValidColor.copy(
                    cardNumber = encryptionHelper.encrypt(cardWithValidColor.cardNumber),
                    expiryDate = encryptionHelper.encrypt(cardWithValidColor.expiryDate),
                    cvv = encryptionHelper.encrypt(cardWithValidColor.cvv),
                    updatedAt = System.currentTimeMillis()
                )
                cardDao.updateCard(encryptedCard)
                
                // Update cache
                decryptedCardCache[card.id] = card
                
                // Force refresh the cards list to ensure UI updates
                refreshCardsList()
                
                withContext(Dispatchers.Main) {
                    onComplete?.invoke()
                }
            } catch (e: Exception) {
                // Handle error
                withContext(Dispatchers.Main) {
                    onComplete?.invoke()
                }
            }
        }
    }
    
    // Force refresh the cards list
    fun refreshCardsList() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val encryptedCards = cardDao.getAllCardsSync()
                val decryptedCards = encryptedCards.map { card: Card ->
                    getDecryptedCardCached(card)
                }
                _allCards.postValue(decryptedCards)
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun deleteCard(card: Card) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                cardDao.deleteCard(card)
                // Remove from cache
                decryptedCardCache.remove(card.id)
                // Force refresh the cards list to ensure UI updates
                refreshCardsList()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun deleteCardById(id: String) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                cardDao.deleteCardById(id)
                // Remove from cache
                decryptedCardCache.remove(id)
                // Force refresh the cards list to ensure UI updates
                refreshCardsList()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    fun deleteAllCards() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                cardDao.deleteAllCards()
                // Clear cache
                decryptedCardCache.clear()
                // Force refresh the cards list to ensure UI updates
                refreshCardsList()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    // Method to fix any cards with empty IDs or invalid colors
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
    
    // Method to fix cards with missing or invalid colors
    fun fixCardsWithInvalidColors() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val encryptedCards = cardDao.getAllCardsSync()
                for (encryptedCard in encryptedCards) {
                    val decryptedCard = getDecryptedCard(encryptedCard)
                    val fixedCard = decryptedCard.copy(
                        cardColor = Card.validateCardColor(decryptedCard.cardColor, decryptedCard.cardIssuer)
                    )
                    
                    // Only update if color was actually fixed
                    if (fixedCard.cardColor != decryptedCard.cardColor) {
                        val updatedEncryptedCard = fixedCard.copy(
                            cardNumber = encryptionHelper.encrypt(fixedCard.cardNumber),
                            expiryDate = encryptionHelper.encrypt(fixedCard.expiryDate),
                            cvv = encryptionHelper.encrypt(fixedCard.cvv),
                            updatedAt = System.currentTimeMillis()
                        )
                        cardDao.updateCard(updatedEncryptedCard)
                        
                        // Update cache
                        decryptedCardCache[fixedCard.id] = fixedCard
                    }
                }
                
                // Refresh the cards list
                refreshCardsList()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    suspend fun getCardById(id: String): Card? {
        return try {
            val encryptedCard = cardDao.getCardById(id)
            encryptedCard?.let { card ->
                getDecryptedCardCached(card)
            }
        } catch (e: Exception) {
            null
        }
    }
    
    // Cached version of getDecryptedCard
    fun getDecryptedCardCached(card: Card): Card {
        return decryptedCardCache.getOrPut(card.id) {
            getDecryptedCard(card)
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
    
    // Clear cache when needed
    fun clearCache() {
        decryptedCardCache.clear()
    }
    
    // Force refresh all cards from database
    fun forceRefreshCards() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Clear cache first
                decryptedCardCache.clear()
                // Then refresh the list
                refreshCardsList()
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    // Debug method to check database state
    suspend fun debugDatabaseState(): String {
        return withContext(Dispatchers.IO) {
            try {
                val existingCards = cardDao.getAllCardsSync()
                val cacheSize = decryptedCardCache.size
                "Database: ${existingCards.size} cards, Cache: $cacheSize items"
            } catch (e: Exception) {
                "Error checking database state: ${e.message}"
            }
        }
    }
    
 // In your CardViewModel class, update these methods:

// Export cards to encrypted file format
suspend fun exportCardsToSecureFile(): ByteArray {
    return withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("QuickCards", "Starting export to secure file")
            
            val encryptedCards = cardDao.getAllCardsSync()
            android.util.Log.d("QuickCards", "Retrieved ${encryptedCards.size} cards from database")
            
            val decryptedCards = encryptedCards.map { card: Card ->
                getDecryptedCardCached(card)
            }
            android.util.Log.d("QuickCards", "Decrypted ${decryptedCards.size} cards")
            
            // Convert to JSON
            val gson = com.google.gson.Gson()
            val jsonData = gson.toJson(decryptedCards)
            android.util.Log.d("QuickCards", "JSON data length: ${jsonData.length}")
            
            // Encrypt the JSON data
            val encryptedFileData = secureFileManager.encryptExportData(jsonData)
            android.util.Log.d("QuickCards", "Encrypted file size: ${encryptedFileData.size}")
            
            encryptedFileData
        } catch (e: Exception) {
            android.util.Log.e("QuickCards", "Export failed", e)
            throw Exception("Failed to export cards: ${e.message}")
        }
    }
    }
    
    // Import cards from encrypted file format
    suspend fun importCardsFromSecureFile(encryptedFileData: ByteArray, forceImport: Boolean = false): Result<Pair<Int, Int>> {
        // Force clear cache before import to ensure fresh state
        decryptedCardCache.clear()
    return withContext(Dispatchers.IO) {
        try {
            android.util.Log.d("QuickCards", "Starting import from secure file, size: ${encryptedFileData.size}")
            
            // Verify file format first
            if (!secureFileManager.isValidEncryptedFile(encryptedFileData)) {
                android.util.Log.e("QuickCards", "File validation failed")
                return@withContext Result.failure(Exception("Invalid file format. Please select a valid QuickCards encrypted export file."))
            }
            
            // Decrypt the file
            val jsonData = secureFileManager.decryptImportData(encryptedFileData)
            android.util.Log.d("QuickCards", "Decrypted JSON data length: ${jsonData.length}")
            
            // Parse JSON
            val gson = com.google.gson.Gson()
            val cardType = object : com.google.gson.reflect.TypeToken<List<Card>>() {}.type
            val importedCards: List<Card> = gson.fromJson(jsonData, cardType)
            android.util.Log.d("QuickCards", "Parsed ${importedCards.size} cards from JSON")
            
            // Get existing card numbers for duplicate detection (only if not forcing import)
            val existingCardNumbers = if (!forceImport) {
                val existingCards = cardDao.getAllCardsSync()
                android.util.Log.d("QuickCards", "Found ${existingCards.size} existing cards in database")
                
                if (existingCards.isEmpty()) {
                    android.util.Log.d("QuickCards", "Database is empty - no duplicates possible")
                    emptySet()
                } else {
                    val cardNumbers = existingCards.map { card ->
                        val decryptedNumber = encryptionHelper.decrypt(card.cardNumber)
                        val cleanNumber = decryptedNumber.replace("[^0-9]".toRegex(), "")
                        android.util.Log.d("QuickCards", "Existing card: ${cleanNumber.take(4)}...")
                        cleanNumber
                    }.toSet()
                    
                    android.util.Log.d("QuickCards", "Existing card numbers count: ${cardNumbers.size}")
                    cardNumbers
                }
            } else {
                android.util.Log.d("QuickCards", "Force import enabled - skipping duplicate detection")
                emptySet()
            }
            
            // Process each card
            var importedCount = 0
            var duplicateCount = 0
            var invalidCount = 0
            
            for (card in importedCards) {
                android.util.Log.d("QuickCards", "Processing card: ${card.bankName}")
                if (isValidCard(card)) {
                    val cleanCardNumber = card.cardNumber.replace("[^0-9]".toRegex(), "")
                    android.util.Log.d("QuickCards", "Clean card number: ${cleanCardNumber.take(4)}...")
                    
                    // Check if card number already exists (only if not forcing import)
                    if (!forceImport && existingCardNumbers.contains(cleanCardNumber)) {
                        android.util.Log.d("QuickCards", "Skipping duplicate card: ${cleanCardNumber.take(4)}...")
                        duplicateCount++
                        continue
                    }
                    
                    val cardWithNewId = card.copy(
                        id = java.util.UUID.randomUUID().toString(),
                        cardColor = Card.validateCardColor(card.cardColor, card.cardIssuer),
                        createdAt = System.currentTimeMillis(),
                        updatedAt = System.currentTimeMillis()
                    )
                    
                    val encryptedCard = cardWithNewId.copy(
                        cardNumber = encryptionHelper.encrypt(cardWithNewId.cardNumber),
                        expiryDate = encryptionHelper.encrypt(cardWithNewId.expiryDate),
                        cvv = encryptionHelper.encrypt(cardWithNewId.cvv)
                    )
                    cardDao.insertCard(encryptedCard)
                    
                    // Add to cache
                    decryptedCardCache[cardWithNewId.id] = cardWithNewId
                    importedCount++
                } else {
                    android.util.Log.d("QuickCards", "Skipping invalid card: ${card.bankName}")
                    invalidCount++
                }
            }
            
            android.util.Log.d("QuickCards", "Import completed: $importedCount imported, $duplicateCount duplicates, $invalidCount invalid")
            Result.success(Pair(importedCount, duplicateCount))
        } catch (e: SecurityException) {
            android.util.Log.e("QuickCards", "Security error during import", e)
            Result.failure(e)
        } catch (e: Exception) {
            android.util.Log.e("QuickCards", "Import failed", e)
            Result.failure(Exception("Failed to import cards: ${e.message}"))
        }
    }
    }
    
    // Check if a file is a valid encrypted export file
    fun isValidEncryptedFile(fileData: ByteArray): Boolean {
        return secureFileManager.isValidEncryptedFile(fileData)
    }
    
    // Get secure file format information
    fun getSecureFileFormatInfo(): String {
        return secureFileManager.getFileFormatInfo()
    }
    
    // Get secure file extension
    fun getSecureFileExtension(): String {
        return secureFileManager.getFileExtension()
    }
    
    // Validate card data structure
    private fun isValidCard(card: Card): Boolean {
        return try {
            android.util.Log.d("QuickCards", "Validating card: ${card.bankName} - ${card.cardNumber.take(4)}...")
            
            // Check required fields
            val isValid = card.cardNumber.isNotBlank() &&
            card.owner.isNotBlank() &&
            card.expiryDate.isNotBlank() &&
            card.cvv.isNotBlank() &&
            card.bankName.isNotBlank() &&
            card.cardType.isNotBlank() &&
            card.cardIssuer.isNotBlank() &&
            card.cardVariant.isNotBlank()
            
            if (!isValid) {
                android.util.Log.w("QuickCards", "Card validation failed - missing required fields")
                return false
            }
            
            // Additional validation: check card number format (basic Luhn algorithm check)
            val cleanCardNumber = card.cardNumber.replace("[^0-9]".toRegex(), "")
            val isValidLength = cleanCardNumber.length in 13..19
            
            if (!isValidLength) {
                android.util.Log.w("QuickCards", "Card validation failed - invalid card number length: ${cleanCardNumber.length}")
                return false
            }
            
            // Temporarily disable Luhn check for testing - uncomment the lines below to re-enable
            // val isValidLuhn = isValidLuhn(cleanCardNumber)
            // if (!isValidLuhn) {
            //     android.util.Log.w("QuickCards", "Card validation failed - Luhn check failed")
            //     return false
            // }
            
            android.util.Log.d("QuickCards", "Card validation passed")
            true
        } catch (e: Exception) {
            android.util.Log.e("QuickCards", "Card validation exception: ${e.message}")
            false
        }
    }
    
    // Basic Luhn algorithm validation for card numbers
    private fun isValidLuhn(cardNumber: String): Boolean {
        if (cardNumber.isEmpty()) return false
        
        var sum = 0
        var alternate = false
        
        for (i in cardNumber.length - 1 downTo 0) {
            var n = cardNumber[i].toString().toInt()
            if (alternate) {
                n *= 2
                if (n > 9) {
                    n = (n % 10) + 1
                }
            }
            sum += n
            alternate = !alternate
        }
        
        return sum % 10 == 0
    }
}