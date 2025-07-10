package com.quickcards.app.ui.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quickcards.app.data.model.Card
import com.quickcards.app.security.BiometricAuthHelper
import com.quickcards.app.viewmodel.CardViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardDetailScreen(
    navController: NavController,
    cardId: String,
    cardViewModel: CardViewModel = viewModel()
) {
    val context = LocalContext.current
    val biometricAuthHelper = BiometricAuthHelper(context)
    
    var card by remember { mutableStateOf<Card?>(null) }
    var decryptedCard by remember { mutableStateOf<Card?>(null) }
    var showCvv by remember { mutableStateOf(false) }
    var cvvVisibilityTimer by remember { mutableStateOf(0) }
    var isLoading by remember { mutableStateOf(true) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    
    // Auto-hide CVV after 10 seconds
    LaunchedEffect(showCvv) {
        if (showCvv) {
            cvvVisibilityTimer = 10
            while (cvvVisibilityTimer > 0) {
                delay(1000)
                cvvVisibilityTimer--
            }
            showCvv = false
        }
    }
    
    // Load card data
    LaunchedEffect(cardId) {
        if (cardId.isBlank() || cardId.isEmpty()) {
            isLoading = false
            Toast.makeText(context, "Invalid card ID", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
            return@LaunchedEffect
        }
        
        try {
            val loadedCard = cardViewModel.getCardById(cardId)
            if (loadedCard != null) {
                card = loadedCard
                decryptedCard = loadedCard // getCardById already returns decrypted card
            } else {
                Toast.makeText(context, "Card not found", Toast.LENGTH_SHORT).show()
                navController.popBackStack()
            }
            isLoading = false
        } catch (e: Exception) {
            isLoading = false
            Toast.makeText(context, "Error loading card details: ${e.message}", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
    }
    
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete Card") },
            text = { Text("Are you sure you want to delete this card? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        card?.let { cardViewModel.deleteCard(it) }
                        showDeleteDialog = false
                        navController.popBackStack()
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Card Details") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { navController.navigate("edit_card/$cardId") }) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        } else if (decryptedCard == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Card not found",
                    style = MaterialTheme.typography.headlineSmall,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            val cardData = decryptedCard!! // Safe to use !! here since we checked for null above
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Card Visual
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = cardData.bankName,
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = MaterialTheme.colorScheme.surface
                            ) {
                                Text(
                                    text = Card.getCardTypeFromNumber(cardData.cardNumber),
                                    style = MaterialTheme.typography.labelMedium,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(32.dp))
                        
                        Text(
                            text = formatCardNumber(cardData.cardNumber),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = "VALID THRU",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = cardData.expiryDate,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                            }
                            
                            Column {
                                Text(
                                    text = "CVV",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (showCvv) cardData.cvv else "***",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    if (showCvv) {
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "${cvvVisibilityTimer}s",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                
                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { copyToClipboard(context, "Card Number", cardData.cardNumber) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Card Number")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy Number")
                    }
                    
                    OutlinedButton(
                        onClick = { copyToClipboard(context, "Expiry Date", cardData.expiryDate) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy Expiry")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Copy Expiry")
                    }
                }
                
                OutlinedButton(
                    onClick = {
                        if (showCvv) {
                            showCvv = false
                        } else {
                            biometricAuthHelper.authenticateForCVV(
                                context as androidx.fragment.app.FragmentActivity,
                                object : BiometricAuthHelper.AuthenticationCallback {
                                    override fun onAuthenticationSuccess() {
                                        showCvv = true
                                    }
                                    
                                    override fun onAuthenticationError(errorCode: Int, errorMessage: String) {
                                        Toast.makeText(context, "Authentication failed: $errorMessage", Toast.LENGTH_SHORT).show()
                                    }
                                    
                                    override fun onAuthenticationFailed() {
                                        Toast.makeText(context, "Authentication failed", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        if (showCvv) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = if (showCvv) "Hide CVV" else "Show CVV"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (showCvv) "Hide CVV" else "Show CVV")
                }
                
                // Card Information
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Card Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        
                        Spacer(modifier = Modifier.height(12.dp))
                        
                        InfoRow("Bank", cardData.bankName)
                        InfoRow("Card Type", Card.getCardTypeFromNumber(cardData.cardNumber))
                        
                        if (cardData.description.isNotBlank()) {
                            InfoRow("Description", cardData.description)
                        }
                        
                        if (cardData.tags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Tags",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                cardData.tags.forEach { tag ->
                                    Surface(
                                        shape = RoundedCornerShape(4.dp),
                                        color = MaterialTheme.colorScheme.primaryContainer
                                    ) {
                                        Text(
                                            text = tag,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Column(
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

private fun formatCardNumber(cardNumber: String): String {
    return cardNumber.chunked(4).joinToString(" ")
}

private fun copyToClipboard(context: Context, label: String, text: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    val clip = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(clip)
    Toast.makeText(context, "$label copied to clipboard", Toast.LENGTH_SHORT).show()
}