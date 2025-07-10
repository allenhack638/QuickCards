package com.quickcards.app.utils

import java.util.*

/**
 * Comprehensive payment input formatting and validation utility
 * Handles card numbers, expiration dates, and CVV formatting with real-time validation
 */
object PaymentInputFormatter {
    
    /**
     * Card type detection based on card number patterns
     */
    enum class CardType(
        val displayName: String,
        val maxLength: Int,
        val cvvLength: Int,
        val formatPattern: List<Int>
    ) {
        VISA("Visa", 16, 3, listOf(4, 4, 4, 4)),
        MASTERCARD("Mastercard", 16, 3, listOf(4, 4, 4, 4)),
        AMERICAN_EXPRESS("American Express", 15, 4, listOf(4, 6, 5)),
        DISCOVER("Discover", 16, 3, listOf(4, 4, 4, 4)),
        DINERS_CLUB("Diners Club", 14, 3, listOf(4, 6, 4)),
        UNKNOWN("Unknown", 19, 4, listOf(4, 4, 4, 4, 3)) // Fallback for unknown cards
    }
    
    /**
     * Detect card type from card number
     */
    fun detectCardType(cardNumber: String): CardType {
        val cleanNumber = cardNumber.replace("\\D".toRegex(), "")
        
        return when {
            cleanNumber.matches("^4.*".toRegex()) -> CardType.VISA
            cleanNumber.matches("^5[1-5].*".toRegex()) || 
            cleanNumber.matches("^2[2-7].*".toRegex()) -> CardType.MASTERCARD
            cleanNumber.matches("^3[47].*".toRegex()) -> CardType.AMERICAN_EXPRESS
            cleanNumber.matches("^6(?:011|5).*".toRegex()) -> CardType.DISCOVER
            cleanNumber.matches("^3[0689].*".toRegex()) -> CardType.DINERS_CLUB
            else -> CardType.UNKNOWN
        }
    }
    
    /**
     * Format card number with appropriate spacing based on card type
     * Returns formatted string and maintains cursor position
     */
    data class CardNumberFormatResult(
        val formattedNumber: String,
        val cleanNumber: String,
        val cursorPosition: Int,
        val cardType: CardType,
        val isValid: Boolean
    )
    
    fun formatCardNumber(input: String, cursorPosition: Int): CardNumberFormatResult {
        // Remove all non-digit characters
        val cleanNumber = input.replace("\\D".toRegex(), "")
        val cardType = detectCardType(cleanNumber)
        
        // Limit input to maximum 16 digits regardless of card type
        val limitedCleanNumber = if (cleanNumber.length > 16) {
            cleanNumber.substring(0, 16)
        } else {
            cleanNumber
        }
        
        // Format according to card type pattern
        val formatted = StringBuilder()
        var digitIndex = 0
        var newCursorPosition = cursorPosition
        
        for (groupSize in cardType.formatPattern) {
            if (digitIndex >= limitedCleanNumber.length) break
            
            val groupEnd = minOf(digitIndex + groupSize, limitedCleanNumber.length)
            val group = limitedCleanNumber.substring(digitIndex, groupEnd)
            
            if (formatted.isNotEmpty()) {
                formatted.append("-")
                // Adjust cursor position for added separators
                if (digitIndex < cursorPosition) {
                    newCursorPosition++
                }
            }
            
            formatted.append(group)
            digitIndex = groupEnd
        }
        
        // Validate card number length (16 digits maximum)
        val isValid = limitedCleanNumber.length == 16 && 
                     isValidLuhnChecksum(limitedCleanNumber)
        
        return CardNumberFormatResult(
            formattedNumber = formatted.toString(),
            cleanNumber = limitedCleanNumber,
            cursorPosition = newCursorPosition.coerceAtMost(formatted.length),
            cardType = cardType,
            isValid = isValid
        )
    }
    
    /**
     * Luhn algorithm for card number validation
     */
    private fun isValidLuhnChecksum(cardNumber: String): Boolean {
        if (cardNumber.length < 13) return false
        
        var sum = 0
        var alternate = false
        
        for (i in cardNumber.length - 1 downTo 0) {
            val char = cardNumber[i]
            if (!char.isDigit()) return false // Safety check for non-digit characters
            
            var digit = char.digitToInt()
            
            if (alternate) {
                digit *= 2
                if (digit > 9) {
                    digit = (digit % 10) + 1
                }
            }
            
            sum += digit
            alternate = !alternate
        }
        
        return sum % 10 == 0
    }
    
    /**
     * Format expiration date as MM/YY with validation
     */
    data class ExpirationFormatResult(
        val formattedDate: String,
        val cleanDate: String,
        val cursorPosition: Int,
        val isValid: Boolean,
        val errorMessage: String?
    )
    
    fun formatExpirationDate(input: String, cursorPosition: Int): ExpirationFormatResult {
        // Remove all non-digit characters
        val cleanInput = input.replace("\\D".toRegex(), "")
        
        // Limit to 4 digits (MMYY)
        val limitedInput = if (cleanInput.length > 4) {
            cleanInput.substring(0, 4)
        } else {
            cleanInput
        }
        
        // Format with slash
        val formatted = when (limitedInput.length) {
            0, 1 -> limitedInput
            2 -> limitedInput
            3, 4 -> "${limitedInput.substring(0, 2)}/${limitedInput.substring(2)}"
            else -> limitedInput
        }
        
        // Calculate new cursor position
        var newCursorPosition = cursorPosition
        if (limitedInput.length >= 2 && cursorPosition > 2) {
            newCursorPosition = cursorPosition + 1 // Account for slash
        }
        newCursorPosition = newCursorPosition.coerceAtMost(formatted.length)
        
        // Validate month and year
        val (isValid, errorMessage) = validateExpirationDate(limitedInput)
        
        return ExpirationFormatResult(
            formattedDate = formatted,
            cleanDate = limitedInput,
            cursorPosition = newCursorPosition,
            isValid = isValid,
            errorMessage = errorMessage
        )
    }
    
    /**
     * Validate expiration date
     */
    private fun validateExpirationDate(cleanDate: String): Pair<Boolean, String?> {
        if (cleanDate.length < 4) {
            return Pair(false, null) // Incomplete, but no error yet
        }
        
        val month = cleanDate.substring(0, 2).toIntOrNull()
        val year = cleanDate.substring(2, 4).toIntOrNull()
        
        if (month == null || year == null) {
            return Pair(false, "Invalid date format")
        }
        
        // Validate month (01-12)
        if (month < 1 || month > 12) {
            return Pair(false, "Invalid month")
        }
        
        // Validate year (current year and future)
        val currentYear = Calendar.getInstance().get(Calendar.YEAR) % 100
        val currentMonth = Calendar.getInstance().get(Calendar.MONTH) + 1
        
        when {
            year < currentYear -> {
                return Pair(false, "Card has expired")
            }
            year == currentYear && month < currentMonth -> {
                return Pair(false, "Card has expired")
            }
            year > currentYear + 15 -> {
                return Pair(false, "Invalid expiration year")
            }
            else -> {
                return Pair(true, null)
            }
        }
    }
    
    /**
     * Format and validate CVV input
     */
    data class CVVFormatResult(
        val formattedCVV: String,
        val cleanCVV: String,
        val cursorPosition: Int,
        val isValid: Boolean,
        val maxLength: Int
    )
    
    fun formatCVV(input: String, cursorPosition: Int, cardType: CardType = CardType.UNKNOWN): CVVFormatResult {
        // Remove all non-digit characters
        val cleanCVV = input.replace("\\D".toRegex(), "")
        
        // Limit to maximum 4 digits for all card types
        val maxLength = 4
        val limitedCVV = if (cleanCVV.length > maxLength) {
            cleanCVV.substring(0, maxLength)
        } else {
            cleanCVV
        }
        
        // CVV doesn't need formatting, just validation
        val isValid = limitedCVV.length == maxLength
        val newCursorPosition = cursorPosition.coerceAtMost(limitedCVV.length)
        
        return CVVFormatResult(
            formattedCVV = limitedCVV,
            cleanCVV = limitedCVV,
            cursorPosition = newCursorPosition,
            isValid = isValid,
            maxLength = maxLength
        )
    }
    
    /**
     * Comprehensive validation for all payment fields
     */
    data class PaymentValidationResult(
        val isCardNumberValid: Boolean,
        val isExpirationValid: Boolean,
        val isCVVValid: Boolean,
        val cardType: CardType,
        val errors: List<String>
    )
    
    fun validatePaymentFields(
        cardNumber: String,
        expirationDate: String,
        cvv: String
    ): PaymentValidationResult {
        val errors = mutableListOf<String>()
        
        // Validate card number
        val cardResult = formatCardNumber(cardNumber, 0)
        val isCardNumberValid = cardResult.isValid
        if (!isCardNumberValid) {
            errors.add("Invalid card number")
        }
        
        // Validate expiration date
        val expirationResult = formatExpirationDate(expirationDate, 0)
        val isExpirationValid = expirationResult.isValid
        if (!isExpirationValid && expirationResult.errorMessage != null) {
            errors.add(expirationResult.errorMessage!!)
        }
        
        // Validate CVV
        val cvvResult = formatCVV(cvv, 0, cardResult.cardType)
        val isCVVValid = cvvResult.isValid
        if (!isCVVValid) {
            errors.add("Invalid CVV")
        }
        
        return PaymentValidationResult(
            isCardNumberValid = isCardNumberValid,
            isExpirationValid = isExpirationValid,
            isCVVValid = isCVVValid,
            cardType = cardResult.cardType,
            errors = errors
        )
    }
}