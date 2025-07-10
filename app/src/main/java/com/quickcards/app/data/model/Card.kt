package com.quickcards.app.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "cards")
data class Card(
    @PrimaryKey
    val id: String = UUID.randomUUID().toString(),
    val cardNumber: String, // Will be encrypted
    val expiryDate: String, // Will be encrypted
    val cvv: String, // Will be encrypted
    val bankName: String,
    val cardType: String, // Credit, Debit, Prepaid, Gift Card
    val cardIssuer: String = "Visa", // New field for card issuer
    val cardVariant: String = "Standard", // New field for card variants like Standard, Gold, Platinum, etc.
    val owner: String = "me", // Card owner name
    val tags: List<String> = emptyList(),
    val description: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    companion object {
        // Helper function to determine card type from number
        fun getCardTypeFromNumber(number: String): String {
            return when {
                number.startsWith("4") -> "Visa"
                number.startsWith("5") || number.startsWith("2") -> "Mastercard"
                number.startsWith("3") -> "American Express"
                number.startsWith("6") -> "Discover"
                else -> "Unknown"
            }
        }
    }
    
    // Helper function to get last 4 digits for display (legacy support)
    fun getLastFourDigits(): String {
        return if (cardNumber.length >= 4) {
            "**** **** **** ${cardNumber.takeLast(4)}"
        } else {
            "****"
        }
    }
    
    // New improved masking function: first 4 and last 4 digits
    fun getMaskedCardNumber(): String {
        val cleanNumber = cardNumber.replace("[^0-9]".toRegex(), "")
        
        return when {
            cleanNumber.length >= 12 -> {
                // Standard case: 1234 **** **** 5678
                "${cleanNumber.take(4)} **** **** ${cleanNumber.takeLast(4)}"
            }
            cleanNumber.length >= 8 -> {
                // Medium case: 1234 **** 5678
                "${cleanNumber.take(4)} **** ${cleanNumber.takeLast(4)}"
            }
            cleanNumber.length >= 6 -> {
                // Short case: 1234 **
                "${cleanNumber.take(4)} **"
            }
            cleanNumber.length >= 4 -> {
                // Very short: show first 4 only
                cleanNumber.take(4)
            }
            else -> {
                // Too short: mask all
                "****"
            }
        }
    }
    
    // Helper to get formatted expiry date for display
    fun getFormattedExpiryDate(): String {
        return if (expiryDate.length >= 4) {
            "${expiryDate.take(2)}/${expiryDate.drop(2)}"
        } else {
            expiryDate
        }
    }
    
    // Helper to get card network from number
    fun getCardNetwork(): String {
        return getCardTypeFromNumber(cardNumber)
    }
}