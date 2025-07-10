package com.quickcards.app.ui.components.payment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.ExperimentalComposeUiApi
import com.quickcards.app.utils.PaymentInputFormatter

/**
 * Formatted Card Number Input Field with real-time validation and formatting
 * Uses professional-grade state management for smooth UX
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun FormattedCardNumberField(
    value: String,
    onValueChange: (String) -> Unit,
    onCardTypeDetected: (PaymentInputFormatter.CardType) -> Unit = {},
    modifier: Modifier = Modifier,
    label: String = "Card Number",
    isError: Boolean = false,
    supportingText: String? = null
) {
    var cardType by remember { mutableStateOf(PaymentInputFormatter.CardType.UNKNOWN) }
    
    // Format the current value for display (optimized)
    val formattedValue = remember(value) {
        PaymentInputFormatter.formatCardNumber(value, value.length).formattedNumber
    }
    
    // Detect card type separately to avoid recomposition loops
    LaunchedEffect(value) {
        val result = PaymentInputFormatter.formatCardNumber(value, value.length)
        if (result.cardType != cardType) {
            cardType = result.cardType
            onCardTypeDetected(result.cardType)
        }
    }
    
    var textFieldValue by remember { mutableStateOf(TextFieldValue(formattedValue)) }
    
    // Update textFieldValue when external value changes (for editing existing cards)
    LaunchedEffect(value) {
        val result = PaymentInputFormatter.formatCardNumber(value, value.length)
        if (result.formattedNumber != textFieldValue.text) {
            textFieldValue = TextFieldValue(result.formattedNumber, TextRange(result.formattedNumber.length))
        }
    }
    
    OutlinedTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            // Format the input
            val result = PaymentInputFormatter.formatCardNumber(
                newValue.text,
                newValue.selection.start
            )
            
            // Update card type if changed
            if (result.cardType != cardType) {
                cardType = result.cardType
                onCardTypeDetected(result.cardType)
            }
            
            // Always update with formatted value for smooth UX
            textFieldValue = TextFieldValue(
                text = result.formattedNumber,
                selection = TextRange(result.cursorPosition)
            )
            
            // Notify parent with clean number only if it changed
            if (result.cleanNumber != value) {
                onValueChange(result.cleanNumber)
            }
        },
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        leadingIcon = {
            Icon(
                Icons.Default.CreditCard,
                contentDescription = "Card Number",
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        },
        trailingIcon = null,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        singleLine = true
    )
}

/**
 * Formatted Expiration Date Input Field (MM/YY) with validation
 * Uses professional-grade state management for smooth UX
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun FormattedExpirationField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String = "MM/YY",
    isError: Boolean = false,
    supportingText: String? = null
) {
    var validationResult by remember { 
        mutableStateOf(PaymentInputFormatter.formatExpirationDate("", 0)) 
    }
    
    // Format the current value for display (optimized)
    val formattedValue = remember(value) {
        PaymentInputFormatter.formatExpirationDate(value, value.length).formattedDate
    }
    
    // Update validation separately to avoid recomposition loops
    LaunchedEffect(value) {
        validationResult = PaymentInputFormatter.formatExpirationDate(value, value.length)
    }
    
    var textFieldValue by remember { mutableStateOf(TextFieldValue(formattedValue)) }
    
    // Update textFieldValue when external value changes (for editing existing cards)
    LaunchedEffect(value) {
        val result = PaymentInputFormatter.formatExpirationDate(value, value.length)
        if (result.formattedDate != textFieldValue.text) {
            textFieldValue = TextFieldValue(result.formattedDate, TextRange(result.formattedDate.length))
            validationResult = result
        }
    }
    
    OutlinedTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            // Format the input
            val result = PaymentInputFormatter.formatExpirationDate(
                newValue.text,
                newValue.selection.start
            )
            
            validationResult = result
            
            // Always update with formatted value for smooth UX
            textFieldValue = TextFieldValue(
                text = result.formattedDate,
                selection = TextRange(result.cursorPosition)
            )
            
            // Notify parent with clean date only if it changed
            if (result.cleanDate != value) {
                onValueChange(result.cleanDate)
            }
        },
        modifier = modifier.width(120.dp),
        label = { Text(label) },
        leadingIcon = {
            Icon(
                Icons.Default.DateRange,
                contentDescription = "Expiration Date"
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        isError = isError || (validationResult.errorMessage != null && validationResult.cleanDate.length == 4),
        supportingText = if (validationResult.errorMessage != null && validationResult.cleanDate.length == 4) {
            { Text(validationResult.errorMessage!!, color = MaterialTheme.colorScheme.error) }
        } else supportingText?.let { { Text(it) } },
        singleLine = true
    )
}

/**
 * Formatted CVV Input Field with dynamic length based on card type
 * Uses professional-grade state management for smooth UX
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun FormattedCVVField(
    value: String,
    onValueChange: (String) -> Unit,
    cardType: PaymentInputFormatter.CardType = PaymentInputFormatter.CardType.UNKNOWN,
    modifier: Modifier = Modifier,
    label: String = "CVV",
    isError: Boolean = false,
    supportingText: String? = null,
    showValue: Boolean = false
) {
    var validationResult by remember { 
        mutableStateOf(PaymentInputFormatter.formatCVV("", 0, cardType)) 
    }
    
    // Format the current value for display (optimized)
    val formattedValue = remember(value, cardType) {
        PaymentInputFormatter.formatCVV(value, value.length, cardType).formattedCVV
    }
    
    // Update validation separately to avoid recomposition loops
    LaunchedEffect(value, cardType) {
        validationResult = PaymentInputFormatter.formatCVV(value, value.length, cardType)
    }
    
    var textFieldValue by remember { mutableStateOf(TextFieldValue(formattedValue)) }
    
    // Update textFieldValue when external value changes (for editing existing cards)
    LaunchedEffect(value, cardType) {
        val result = PaymentInputFormatter.formatCVV(value, value.length, cardType)
        if (result.formattedCVV != textFieldValue.text) {
            textFieldValue = TextFieldValue(result.formattedCVV, TextRange(result.formattedCVV.length))
            validationResult = result
        }
    }
    
    OutlinedTextField(
        value = textFieldValue,
        onValueChange = { newValue ->
            // Format the input
            val result = PaymentInputFormatter.formatCVV(
                newValue.text,
                newValue.selection.start,
                cardType
            )
            
            validationResult = result
            
            // Always update with formatted value for smooth UX
            textFieldValue = TextFieldValue(
                text = result.formattedCVV,
                selection = TextRange(result.cursorPosition)
            )
            
            // Notify parent with clean CVV only if it changed
            if (result.cleanCVV != value) {
                onValueChange(result.cleanCVV)
            }
        },
        modifier = modifier.width(100.dp),
        label = { Text(label) },
        leadingIcon = {
            Icon(
                Icons.Default.Security,
                contentDescription = "CVV"
            )
        },
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        visualTransformation = if (showValue) {
            VisualTransformation.None
        } else {
            androidx.compose.ui.text.input.PasswordVisualTransformation()
        },
        isError = isError,
        supportingText = supportingText?.let { { Text(it) } },
        singleLine = true
    )
}

/**
 * Combined Row for Expiration Date and CVV (common layout pattern)
 */
@Composable
fun ExpirationAndCVVRow(
    expirationValue: String,
    onExpirationChange: (String) -> Unit,
    cvvValue: String,
    onCVVChange: (String) -> Unit,
    cardType: PaymentInputFormatter.CardType = PaymentInputFormatter.CardType.UNKNOWN,
    modifier: Modifier = Modifier,
    showCVV: Boolean = false,
    isExpirationError: Boolean = false,
    isCVVError: Boolean = false,
    expirationSupportingText: String? = null,
    cvvSupportingText: String? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FormattedExpirationField(
            value = expirationValue,
            onValueChange = onExpirationChange,
            isError = isExpirationError,
            supportingText = expirationSupportingText
        )
        
        FormattedCVVField(
            value = cvvValue,
            onValueChange = onCVVChange,
            cardType = cardType,
            showValue = showCVV,
            isError = isCVVError,
            supportingText = cvvSupportingText
        )
    }
}

/**
 * Complete Payment Form with all fields and validation
 */
@Composable
fun PaymentForm(
    cardNumber: String,
    onCardNumberChange: (String) -> Unit,
    expirationDate: String,
    onExpirationDateChange: (String) -> Unit,
    cvv: String,
    onCVVChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    showCVV: Boolean = false,
    onValidationChange: (PaymentInputFormatter.PaymentValidationResult) -> Unit = {}
) {
    var cardType by remember { mutableStateOf(PaymentInputFormatter.CardType.UNKNOWN) }
    
    // Perform validation whenever any field changes
    LaunchedEffect(cardNumber, expirationDate, cvv) {
        val validationResult = PaymentInputFormatter.validatePaymentFields(
            cardNumber, expirationDate, cvv
        )
        onValidationChange(validationResult)
    }
    
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        FormattedCardNumberField(
            value = cardNumber,
            onValueChange = onCardNumberChange,
            onCardTypeDetected = { detectedType ->
                cardType = detectedType
            }
        )
        
        ExpirationAndCVVRow(
            expirationValue = expirationDate,
            onExpirationChange = onExpirationDateChange,
            cvvValue = cvv,
            onCVVChange = onCVVChange,
            cardType = cardType,
            showCVV = showCVV
        )
    }
}