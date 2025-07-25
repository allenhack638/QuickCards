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
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.rememberCoroutineScope
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
import com.quickcards.app.utils.ResponsiveDimensions
import androidx.compose.ui.graphics.Brush
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.quickcards.app.data.model.Card
import com.quickcards.app.security.BiometricAuthHelper
import com.quickcards.app.security.CVVVisibilityManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

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
    var isAuthenticating by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()
    
    // Get CVV visibility manager
    val cvvManager = CVVVisibilityManager.getInstance()
    val visibleCardId by cvvManager.visibleCardId.observeAsState(null)
    val cvvTimer by cvvManager.cvvTimer.observeAsState(0)
    
    // Check if this card's CVV is currently visible
    val showCvv = visibleCardId == card.id
    
    // Handle lifecycle changes to hide CVV when app goes to background
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> {
                    cvvManager.hideCVV()
                }
                else -> {}
            }
        }
        
        lifecycleOwner.lifecycle.addObserver(observer)
        
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }
    
    // Define gradient colors based on card's custom color
    val cardGradient = try {
        val baseColor = Color(android.graphics.Color.parseColor(card.cardColor))
        val darkerColor = baseColor.copy(alpha = 0.8f)
        Brush.linearGradient(
            colors = listOf(baseColor, darkerColor)
        )
    } catch (e: Exception) {
        // Fallback to default blue gradient if color parsing fails
        Brush.linearGradient(
            colors = listOf(
                Color(0xFF2196F3), // Blue
                Color(0xFF1976D2)  // Darker blue
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
                        cvvManager.showCVV(card.id)
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
    
    // Handle card tap with authentication, then copy and show CVV
    fun authenticateAndCopyCardDetails() {
        if (activity != null && !isAuthenticating) {
            isAuthenticating = true
            val biometricHelper = BiometricAuthHelper(context)
            
            biometricHelper.authenticateForCVV(
                activity,
                object : BiometricAuthHelper.AuthenticationCallback {
                    override fun onAuthenticationSuccess() {
                        isAuthenticating = false
                        
                        // Copy card number to clipboard
                        val cleanCardNumber = card.cardNumber.replace("[^0-9]".toRegex(), "")
                        clipboardManager.setText(AnnotatedString(cleanCardNumber))
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        
                        // Show CVV after authentication
                        cvvManager.showCVV(card.id)
                        
                        // Show toast notification
                        android.widget.Toast.makeText(
                            context,
                            "Card number copied • CVV visible",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                        
                        // Auto-clear clipboard after 30 seconds for security
                        coroutineScope.launch {
                            delay(30000) // 30 seconds
                            clipboardManager.setText(AnnotatedString(""))
                            android.widget.Toast.makeText(
                                context,
                                "Clipboard cleared for security",
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                        }
                    }
                    
                    override fun onAuthenticationError(errorCode: Int, errorMessage: String) {
                        isAuthenticating = false
                        android.widget.Toast.makeText(
                            context,
                            "Authentication failed: $errorMessage",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                    
                    override fun onAuthenticationFailed() {
                        isAuthenticating = false
                        android.widget.Toast.makeText(
                            context,
                            "Authentication failed",
                            android.widget.Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            )
        }
    }
    
    // Handle card number copy only (for legacy use)
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
        
        // Auto-clear clipboard after 30 seconds for security
        coroutineScope.launch {
            delay(30000) // 30 seconds
            clipboardManager.setText(AnnotatedString(""))
            android.widget.Toast.makeText(
                context,
                "Clipboard cleared for security",
                android.widget.Toast.LENGTH_SHORT
            ).show()
        }
    }
    
    // Single Card Design with all content
    val responsiveCardDimensions = ResponsiveDimensions.getResponsiveCardDimensions()
    
    Card(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(responsiveCardDimensions.cornerRadius))
            .combinedClickable(
                onClick = {
                    if (isSelectionMode) {
                        onClick()
                    } else {
                        // Authenticate first, then copy card details and show CVV
                        authenticateAndCopyCardDetails()
                    }
                },
                onLongClick = {
                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                    onLongClick()
                }
            ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) responsiveCardDimensions.elevation else (responsiveCardDimensions.elevation * 0.75f)
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
                .padding(responsiveCardDimensions.padding)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(ResponsiveDimensions.getResponsiveSpacing().large)
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
                        
                        Spacer(modifier = Modifier.width(ResponsiveDimensions.getResponsiveSpacing().medium))
                        
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
                            modifier = Modifier.padding(top = ResponsiveDimensions.getResponsiveSpacing().small)
                        ) {
                            Text(
                                text = card.cardType,
                                color = Color.White.copy(alpha = 0.95f),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.5.sp,
                                modifier = Modifier.padding(horizontal = ResponsiveDimensions.getResponsiveSpacing().medium, vertical = ResponsiveDimensions.getResponsiveSpacing().small)
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
                    Spacer(modifier = Modifier.width(ResponsiveDimensions.getResponsiveSpacing().small))
                    Text(
                        text = if (showCvv) card.cvv else "***",
                        color = Color.White,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.width(ResponsiveDimensions.getResponsiveSpacing().small))
                    IconButton(
                        onClick = { 
                            if (showCvv) {
                                cvvManager.hideCVV()
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
                        Spacer(modifier = Modifier.height(ResponsiveDimensions.getResponsiveSpacing().small))
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
                        horizontalArrangement = Arrangement.spacedBy(ResponsiveDimensions.getResponsiveSpacing().small),
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