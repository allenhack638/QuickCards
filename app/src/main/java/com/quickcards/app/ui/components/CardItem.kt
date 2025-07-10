package com.quickcards.app.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Brush
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.quickcards.app.data.model.Card
import com.quickcards.app.security.BiometricAuthHelper
import kotlinx.coroutines.delay

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CardItem(
    card: Card,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier,
    isSelected: Boolean = false,
    isSelectionMode: Boolean = false
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val clipboardManager = LocalClipboardManager.current
    val hapticFeedback = LocalHapticFeedback.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var showCvv by remember { mutableStateOf(false) }
    var isAuthenticating by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // CVV timeout functionality
    LaunchedEffect(showCvv) {
        if (showCvv) {
            // Hide CVV after 10 seconds
            delay(10000)
            showCvv = false
        }
    }
    
    // Handle lifecycle changes to hide CVV when app goes to background
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> {
                    showCvv = false
                }
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Define gradient colors based on card issuer
    val cardGradient = when (card.cardIssuer) {
        "Visa" -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF4A90E2), // Blue
                Color(0xFF357ABD)  // Darker blue
            )
        )
        "Mastercard" -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF2C3E50), // Dark blue-gray
                Color(0xFF34495E)  // Lighter dark
            )
        )
        "American Express" -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF00B894), // Green
                Color(0xFF00A085)  // Darker green
            )
        )
        "Discover" -> Brush.linearGradient(
            colors = listOf(
                Color(0xFFE17055), // Orange
                Color(0xFFD63031)  // Red-orange
            )
        )
        "RuPay" -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF9B59B6), // Purple
                Color(0xFF8E44AD)  // Darker purple
            )
        )
        "Diners Club" -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF34495E), // Dark gray
                Color(0xFF2C3E50)  // Darker gray
            )
        )
        "JCB" -> Brush.linearGradient(
            colors = listOf(
                Color(0xFFE74C3C), // Red
                Color(0xFFC0392B)  // Darker red
            )
        )
        "UnionPay" -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF3498DB), // Light blue
                Color(0xFF2980B9)  // Darker blue
            )
        )
        "Bajaj Finserv" -> Brush.linearGradient(
            colors = listOf(
                Color(0xFFFF7675), // Light red
                Color(0xFFE84393)  // Pink
            )
        )
        "HDFC Bank" -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF00B894), // Teal
                Color(0xFF00A085)  // Darker teal
            )
        )
        else -> Brush.linearGradient(
            colors = listOf(
                Color(0xFF636E72), // Gray
                Color(0xFF2D3436)  // Darker gray
            )
        )
    }
    
    // Handle CVV authentication
    fun authenticateAndShowCvv() {
        if (activity != null && !isAuthenticating) {
            isAuthenticating = true
            val biometricHelper = BiometricAuthHelper(context)
            
            biometricHelper.authenticateForCVV(
                activity,
                object : BiometricAuthHelper.AuthenticationCallback {
                    override fun onAuthenticationSuccess() {
                        isAuthenticating = false
                        showCvv = true
                    }
                    
                    override fun onAuthenticationError(errorCode: Int, errorMessage: String) {
                        isAuthenticating = false
                        // Handle authentication error
                    }
                    
                    override fun onAuthenticationFailed() {
                        isAuthenticating = false
                        // Handle authentication failure
                    }
                }
            )
        }
    }
    
    // Handle card number copy
    fun copyCardNumberToClipboard() {
        val cleanCardNumber = card.cardNumber.replace("[^0-9]".toRegex(), "")
        clipboardManager.setText(AnnotatedString(cleanCardNumber))
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        
        // Show toast notification
        android.widget.Toast.makeText(
            context,
            "Card number copied to clipboard",
            android.widget.Toast.LENGTH_SHORT
        ).show()
    }
    
    // Single Card Design with all content
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onClick()
                    } else {
                        // Copy card number when clicking upper part
                        copyCardNumberToClipboard()
                    }
                },
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 16.dp else 12.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    if (isSelected) 
                        Brush.linearGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.05f)
                            )
                        )
                    else 
                        cardGradient
                )
                .padding(24.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Top Row: Bank Name and Card Issuer
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Bank Name with single clear card icon
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.CreditCard,
                            contentDescription = "Credit Card",
                            tint = Color.White.copy(alpha = 0.9f),
                            modifier = Modifier.size(32.dp)
                        )
                        
                        Spacer(modifier = Modifier.width(12.dp))
                        
                        Column {
                            Text(
                                text = card.bankName,
                                color = Color.White,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                            if (card.cardVariant != "Standard") {
                                Text(
                                    text = card.cardVariant,
                                    color = Color.White.copy(alpha = 0.8f),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                    
                    // Card Issuer and Type (Right side)
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        // Card Issuer (plain text, no background)
                        Text(
                            text = card.cardIssuer,
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        
                        // Card Type Badge (elegant and subtle)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = Color.White.copy(alpha = 0.15f),
                            modifier = Modifier.padding(top = 6.dp)
                        ) {
                            Text(
                                text = card.cardType,
                                color = Color.White.copy(alpha = 0.95f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                
                // Card Number
                Text(
                    text = card.getMaskedCardNumber(),
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 3.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                
                // Bottom Row: Valid Thru and Card Holder
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    // Valid Thru
                    Column {
                        Text(
                            text = "VALID THRU",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = card.getFormattedExpiryDate(),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    // Card Holder
                    Column(
                        horizontalAlignment = Alignment.End
                    ) {
                        Text(
                            text = "CARD HOLDER",
                            color = Color.White.copy(alpha = 0.7f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = 1.sp
                        )
                        Text(
                            text = card.owner.uppercase(),
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // CVV Section with Authentication
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "CVV:",
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (showCvv) card.cvv else "***",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { 
                            if (showCvv) {
                                showCvv = false
                            } else {
                                authenticateAndShowCvv()
                            }
                        },
                        modifier = Modifier.size(24.dp),
                        enabled = !isAuthenticating
                    ) {
                        if (isAuthenticating) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = Color.White.copy(alpha = 0.8f),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = if (showCvv) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                contentDescription = if (showCvv) "Hide CVV" else "Show CVV",
                                tint = Color.White.copy(alpha = 0.8f),
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
                
                // Note Section (only show if description is not empty)
                if (card.description.isNotBlank()) {
                    Column(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Note:",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = card.description,
                            color = Color.White.copy(alpha = 0.8f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Normal,
                            lineHeight = 18.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                
                // Tags (if exist) - compact display
                if (card.tags.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        card.tags.take(3).forEach { tag ->
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = tag,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        if (card.tags.size > 3) {
                            Text(
                                text = "+${card.tags.size - 3}",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White.copy(alpha = 0.7f),
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}