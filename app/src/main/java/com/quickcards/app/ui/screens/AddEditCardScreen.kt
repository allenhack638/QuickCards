package com.quickcards.app.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickcards.app.utils.ResponsiveDimensions
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.autofill.AutofillNode
import androidx.compose.ui.autofill.AutofillType
import androidx.compose.ui.composed
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalAutofill
import androidx.compose.ui.platform.LocalAutofillTree
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quickcards.app.data.model.Card
import com.quickcards.app.security.CardOperationAuthManager
import com.quickcards.app.ui.components.payment.FormattedCardNumberField
import com.quickcards.app.ui.components.payment.FormattedExpirationField
import com.quickcards.app.ui.components.payment.FormattedCVVField
import com.quickcards.app.utils.PaymentInputFormatter
import com.quickcards.app.viewmodel.BankViewModel
import com.quickcards.app.viewmodel.CardViewModel
import kotlinx.coroutines.launch

// Extension function to add autofill support
@ExperimentalComposeUiApi
fun Modifier.autofill(
    autofillTypes: List<AutofillType>,
    onFill: ((String) -> Unit),
) = composed {
    val autofill = LocalAutofill.current
    val autofillNode = AutofillNode(onFill = onFill, autofillTypes = autofillTypes)
    LocalAutofillTree.current += autofillNode

    this
        .onGloballyPositioned {
            autofillNode.boundingBox = it.boundsInWindow()
        }
        .onFocusChanged { focusState ->
            autofill?.run {
                if (focusState.isFocused) {
                    requestAutofillForNode(autofillNode)
                } else {
                    cancelAutofillForNode(autofillNode)
                }
            }
        }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalComposeUiApi::class)
@Composable
fun AddEditCardScreen(
    navController: NavController,
    cardId: String? = null,
    cardViewModel: CardViewModel = viewModel(),
    bankViewModel: BankViewModel = viewModel()
) {
    val context = LocalContext.current
    val isEditing = cardId != null
    val cardAuthManager = CardOperationAuthManager.getInstance(context)
    val scope = rememberCoroutineScope()
    
    // State variables
    var cardNumber by remember { mutableStateOf("") }
    var owner by remember { mutableStateOf("") }
    var expiryDate by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var showCvv by remember { mutableStateOf(false) }
    var description by remember { mutableStateOf("") }
    var selectedBank by remember { mutableStateOf("") }
    var selectedCardType by remember { mutableStateOf("Credit") }
    var selectedCardIssuer by remember { mutableStateOf("Visa") }
    var selectedCardVariant by remember { mutableStateOf("Standard") }
    var cardType by remember { mutableStateOf(PaymentInputFormatter.CardType.UNKNOWN) }
    var isLoading by remember { mutableStateOf(false) }
    
    // Observe data
    val banks by bankViewModel.allBanks.observeAsState(emptyList())
    
    // Card variant options
    val cardVariants = listOf("Standard", "Gold", "Platinum", "Black", "World", "World Elite", "Infinite")
    var isAddingNewVariant by remember { mutableStateOf(false) }
    var newVariantName by remember { mutableStateOf("") }
    
    // Card Type options (updated to include Gift Card)
    val cardTypes = listOf("Credit", "Debit", "Prepaid", "Gift Card")
    
    // Card Issuer options (10 prominent issuers as requested)
    val cardIssuers = listOf(
        "Visa", 
        "Mastercard", 
        "RuPay", 
        "American Express", 
        "Discover", 
        "Diners Club", 
        "JCB", 
        "UnionPay",
        "Bajaj Finserv", // Popular in India
        "HDFC Bank" // Popular in India
    )
    
    // Helper function to detect card issuer from card number
    fun detectCardIssuer(cardNumber: String): String {
        val cleanNumber = cardNumber.replace("[^0-9]".toRegex(), "")
        
        return when {
            cleanNumber.startsWith("4") -> "Visa"
            cleanNumber.matches("^5[1-5].*".toRegex()) || 
            cleanNumber.matches("^2[2-7].*".toRegex()) -> "Mastercard"
            cleanNumber.matches("^3[47].*".toRegex()) -> "American Express"
            cleanNumber.matches("^6(?:011|5).*".toRegex()) -> "Discover"
            cleanNumber.matches("^3[0689].*".toRegex()) -> "Diners Club"
            cleanNumber.matches("^35.*".toRegex()) -> "JCB"
            cleanNumber.matches("^62.*".toRegex()) -> "UnionPay"
            // RuPay patterns - starts with 60, 65, 81, 82, 508, 353, 356
            cleanNumber.matches("^(60|65|81|82|508|353|356).*".toRegex()) -> "RuPay"
            else -> selectedCardIssuer // Keep current selection if no match
        }
    }
    
    // Load card data if editing
    LaunchedEffect(cardId) {
        if (cardId != null) {
            try {
                val card = cardViewModel.getCardById(cardId)
                card?.let {
                    // Load clean values - the formatted fields will handle formatting
                    cardNumber = it.cardNumber
                    owner = it.owner
                    expiryDate = it.expiryDate
                    cvv = it.cvv
                    description = it.description
                    selectedBank = it.bankName
                    selectedCardType = it.cardType
                    selectedCardIssuer = it.cardIssuer
                    selectedCardVariant = it.cardVariant
                }
            } catch (e: Exception) {
                // Handle error
            }
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isEditing) "Edit Card" else "Add Card") },
                navigationIcon = {
                    IconButton(onClick = { navController.navigateUp() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Save/Update Button - Elegant and clean in top right with proper margin
                    TextButton(
                        onClick = {
                            scope.launch {
                                if (isEditing) {
                                    // Require authentication for editing
                                    val isAuthenticated = cardAuthManager.authenticateForCardEdit(context as androidx.fragment.app.FragmentActivity)
                                    if (!isAuthenticated) {
                                        isLoading = false
                                        // Show user feedback for authentication failure
                                        android.widget.Toast.makeText(context, "Authentication required to edit card", android.widget.Toast.LENGTH_SHORT).show()
                                        return@launch
                                    }
                                }
                                
                                isLoading = true
                                val card = Card(
                                    id = cardId ?: "",
                                    cardNumber = cardNumber,
                                    owner = owner,
                                    expiryDate = expiryDate,
                                    cvv = cvv,
                                    description = description,
                                    bankName = selectedBank,
                                    cardType = selectedCardType,
                                    cardIssuer = selectedCardIssuer,
                                    cardVariant = selectedCardVariant,
                                    tags = emptyList() // Remove tags functionality
                                )
                                
                                if (isEditing) {
                                    cardViewModel.updateCard(card) {
                                        isLoading = false
                                        // Navigate after successful update
                                        navController.navigate("cards") {
                                            popUpTo("cards") { inclusive = false }
                                            launchSingleTop = true
                                        }
                                    }
                                } else {
                                    cardViewModel.insertCard(card) {
                                        isLoading = false
                                        // Navigate after successful insert
                                navController.navigate("cards") {
                                    popUpTo("cards") { inclusive = false }
                                    launchSingleTop = true
                                        }
                                    }
                                }
                            }
                        },
                        // Updated enabled condition to include selectedBank check
                        enabled = !isLoading && cardNumber.isNotBlank() && owner.isNotBlank() && 
                                expiryDate.isNotBlank() && cvv.isNotBlank() && selectedBank.isNotBlank(),
                        modifier = Modifier.padding(end = ResponsiveDimensions.getResponsiveSpacing().medium), // Responsive right margin
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (cardNumber.isNotBlank() && owner.isNotBlank() && 
                                               expiryDate.isNotBlank() && cvv.isNotBlank() && selectedBank.isNotBlank()) {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.08f)
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.04f)
                            },
                            contentColor = if (cardNumber.isNotBlank() && owner.isNotBlank() && 
                                             expiryDate.isNotBlank() && cvv.isNotBlank() && selectedBank.isNotBlank()) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                            }
                        ),
                        shape = RoundedCornerShape(18.dp),
                        elevation = ButtonDefaults.buttonElevation(
                            defaultElevation = 0.dp,
                            pressedElevation = 0.dp,
                            disabledElevation = 0.dp
                        )
                    ) {
                        Text(
                            text = if (isEditing) "Update" else "Save",
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 2.dp, vertical = 1.dp)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(ResponsiveDimensions.getResponsivePadding().horizontal)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(ResponsiveDimensions.getResponsiveSpacing().medium)
        ) {
            // Card Number with autofill support
            FormattedCardNumberField(
                value = cardNumber,
                onValueChange = { newValue ->
                    cardNumber = newValue
                    // Auto-detect and update card issuer
                    val detectedIssuer = detectCardIssuer(newValue)
                    if (detectedIssuer != selectedCardIssuer) {
                        selectedCardIssuer = detectedIssuer
                    }
                },
                onCardTypeDetected = { detectedType ->
                    cardType = detectedType
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .autofill(
                        autofillTypes = listOf(AutofillType.CreditCardNumber),
                        onFill = { filledValue ->
                            cardNumber = filledValue
                            // Auto-detect and update card issuer when autofilled
                            val detectedIssuer = detectCardIssuer(filledValue)
                            if (detectedIssuer != selectedCardIssuer) {
                                selectedCardIssuer = detectedIssuer
                            }
                        }
                    )
            )
            
            // Owner Name with auto-capitalization, profile icon, and autofill
            OutlinedTextField(
                value = owner,
                onValueChange = { newValue ->
                    // Auto-capitalize each word
                    owner = newValue.split(" ").joinToString(" ") { word ->
                        word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                    }
                },
                label = { Text("Cardholder Name") },
                leadingIcon = {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = "Cardholder Name",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .autofill(
                        autofillTypes = listOf(AutofillType.PersonFullName),
                        onFill = { filledValue ->
                            // Auto-capitalize each word when autofilled
                            owner = filledValue.split(" ").joinToString(" ") { word ->
                                word.lowercase().replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                            }
                        }
                    )
                    .semantics {
                        contentDescription = "Credit card holder name"
                    },
                singleLine = true,
                placeholder = { Text("John Doe") },
                keyboardOptions = KeyboardOptions(
                    keyboardType = KeyboardType.Text
                )
            )
            
            // Card Issuer Selection (NEW)
            var cardIssuerExpanded by remember { mutableStateOf(false) }
            
            ExposedDropdownMenuBox(
                expanded = cardIssuerExpanded,
                onExpandedChange = { cardIssuerExpanded = !cardIssuerExpanded }
            ) {
                OutlinedTextField(
                    value = selectedCardIssuer,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Card Issuer") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cardIssuerExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = cardIssuerExpanded,
                    onDismissRequest = { cardIssuerExpanded = false }
                ) {
                    cardIssuers.forEach { issuer ->
                        DropdownMenuItem(
                            text = { Text(issuer) },
                            onClick = {
                                selectedCardIssuer = issuer
                                cardIssuerExpanded = false
                            }
                        )
                    }
                }
            }
            
            // Card Variant Selection
            var cardVariantExpanded by remember { mutableStateOf(false) }
            
            ExposedDropdownMenuBox(
                expanded = cardVariantExpanded,
                onExpandedChange = { cardVariantExpanded = !cardVariantExpanded }
            ) {
                OutlinedTextField(
                    value = selectedCardVariant,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Card Variant") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cardVariantExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = cardVariantExpanded,
                    onDismissRequest = { cardVariantExpanded = false }
                ) {
                    cardVariants.forEach { variant ->
                        DropdownMenuItem(
                            text = { Text(variant) },
                            onClick = {
                                selectedCardVariant = variant
                                cardVariantExpanded = false
                            }
                        )
                    }
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("+ Add New Variant") },
                        onClick = {
                            isAddingNewVariant = true
                            cardVariantExpanded = false
                        }
                    )
                }
            }
            
            // Add New Variant Dialog
            if (isAddingNewVariant) {
                AlertDialog(
                    onDismissRequest = { 
                        isAddingNewVariant = false
                        newVariantName = ""
                    },
                    title = { Text("Add New Card Variant") },
                    text = {
                        OutlinedTextField(
                            value = newVariantName,
                            onValueChange = { newVariantName = it },
                            label = { Text("Variant Name") },
                            placeholder = { Text("e.g., Premium, Elite") },
                            singleLine = true
                        )
                    },
                    confirmButton = {
                        TextButton(
                            onClick = {
                                if (newVariantName.isNotBlank()) {
                                    selectedCardVariant = newVariantName.trim()
                                    isAddingNewVariant = false
                                    newVariantName = ""
                                }
                            },
                            enabled = newVariantName.isNotBlank()
                        ) {
                            Text("Add")
                        }
                    },
                    dismissButton = {
                        TextButton(
                            onClick = { 
                                isAddingNewVariant = false
                                newVariantName = ""
                            }
                        ) {
                            Text("Cancel")
                        }
                    }
                )
            }
            
            // Expiry and CVV Row with autofill support
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(ResponsiveDimensions.getResponsiveSpacing().medium)
            ) {
                FormattedExpirationField(
                    value = expiryDate,
                    onValueChange = { expiryDate = it },
                    modifier = Modifier
                        .weight(1f)
                        .autofill(
                            autofillTypes = listOf(AutofillType.CreditCardExpirationDate),
                            onFill = { filledValue ->
                                expiryDate = filledValue
                            }
                        )
                )
                
                FormattedCVVField(
                    value = cvv,
                    onValueChange = { cvv = it },
                    cardType = cardType,
                    showValue = showCvv,
                    modifier = Modifier
                        .weight(1f)
                        .autofill(
                            autofillTypes = listOf(AutofillType.CreditCardSecurityCode),
                            onFill = { filledValue ->
                                cvv = filledValue
                            }
                        )
                )
            }
            
            // Bank Selection
            var bankExpanded by remember { mutableStateOf(false) }
            
            ExposedDropdownMenuBox(
                expanded = bankExpanded,
                onExpandedChange = { bankExpanded = !bankExpanded }
            ) {
                OutlinedTextField(
                    value = selectedBank,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Bank") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = bankExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = bankExpanded,
                    onDismissRequest = { bankExpanded = false }
                ) {
                    banks.forEach { bank ->
                        DropdownMenuItem(
                            text = { Text(bank.name) },
                            onClick = {
                                selectedBank = bank.name
                                bankExpanded = false
                            }
                        )
                    }
                }
            }
            
            // Card Type Selection (Repositioned above Description as requested)
            var cardTypeExpanded by remember { mutableStateOf(false) }
            
            ExposedDropdownMenuBox(
                expanded = cardTypeExpanded,
                onExpandedChange = { cardTypeExpanded = !cardTypeExpanded }
            ) {
                OutlinedTextField(
                    value = selectedCardType,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Card Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = cardTypeExpanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = cardTypeExpanded,
                    onDismissRequest = { cardTypeExpanded = false }
                ) {
                    cardTypes.forEach { cardType ->
                        DropdownMenuItem(
                            text = { Text(cardType) },
                            onClick = {
                                selectedCardType = cardType
                                cardTypeExpanded = false
                            }
                        )
                    }
                }
            }
            
            // Description (Now positioned at the bottom as requested)
            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text("Description (Optional)") },
                modifier = Modifier.fillMaxWidth(),
                maxLines = 3
            )
            
            // Add space at bottom for better scrolling
            Spacer(modifier = Modifier.height(ResponsiveDimensions.getResponsiveSpacing().medium))
        }
    }
}