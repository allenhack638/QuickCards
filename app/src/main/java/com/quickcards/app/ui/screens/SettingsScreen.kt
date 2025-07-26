package com.quickcards.app.ui.screens

import android.content.Context
import android.content.Intent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quickcards.app.utils.ResponsiveDimensions
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quickcards.app.security.BiometricAuthHelper
import com.quickcards.app.security.SecureFileManager
import com.quickcards.app.security.SecurityManager
import androidx.fragment.app.FragmentActivity
import com.quickcards.app.viewmodel.BankViewModel
import com.quickcards.app.viewmodel.CardViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    navController: NavController,
    cardViewModel: CardViewModel = viewModel(),
    bankViewModel: BankViewModel = viewModel()
) {
    val context = LocalContext.current
    val biometricAuthHelper = BiometricAuthHelper(context)
    val secureFileManager = SecureFileManager.getInstance()
    val coroutineScope = rememberCoroutineScope()
    
    val cards by cardViewModel.allCards.observeAsState(emptyList())
    val banks by bankViewModel.allBanks.observeAsState(emptyList())
    
    var showClearDataDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }
    var showDuplicateDialog by remember { mutableStateOf(false) }
    var pendingImportData by remember { mutableStateOf<ByteArray?>(null) }
    
    // Export launcher
    val exportLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/octet-stream")
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                isExporting = true
                try {
                    val encryptedData = cardViewModel.exportCardsToSecureFile()
                    context.contentResolver.openOutputStream(uri)?.use { outputStream ->
                        outputStream.write(encryptedData)
                    }
                    Toast.makeText(context, "Cards exported successfully to encrypted file", Toast.LENGTH_SHORT).show()
                } catch (e: Exception) {
                    Toast.makeText(context, "Export failed: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isExporting = false
                }
            }
        }
    }
    
    // Function to handle export with authentication
    fun handleExportWithAuth() {
        if (biometricAuthHelper.isAuthenticationAvailable()) {
            biometricAuthHelper.authenticateUser(
                context as androidx.fragment.app.FragmentActivity,
                object : BiometricAuthHelper.AuthenticationCallback {
                    override fun onAuthenticationSuccess() {
                        exportLauncher.launch("QuickCards_Backup_${System.currentTimeMillis()}.qcex")
                    }
                    
                    override fun onAuthenticationError(errorCode: Int, errorMessage: String) {
                        Toast.makeText(context, "Authentication error: ${biometricAuthHelper.getErrorMessage(errorCode)}", Toast.LENGTH_LONG).show()
                    }
                    
                    override fun onAuthenticationFailed() {
                        Toast.makeText(context, "Authentication failed. Please try again.", Toast.LENGTH_SHORT).show()
                    }
                },
                "Export Cards",
                "Authenticate to export your cards",
                "Your card data will be encrypted and exported securely"
            )
        } else {
            // Fallback: export without authentication if no auth is available
            exportLauncher.launch("QuickCards_Backup_${System.currentTimeMillis()}.qcex")
        }
    }
    

    
    // Import launcher
    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                isImporting = true
                try {
                    val fileData = context.contentResolver.openInputStream(uri)?.use { inputStream ->
                        inputStream.readBytes()
                    }
                    
                    if (fileData != null) {
                        // Debug: Log file size and first few bytes
                        android.util.Log.d("QuickCards", "Import file size: ${fileData.size} bytes")
                        if (fileData.size >= 4) {
                            val header = String(fileData.sliceArray(0..3), Charsets.UTF_8)
                            android.util.Log.d("QuickCards", "File header: '$header'")
                        }
                        
                        // Try to import as encrypted file first
                        if (cardViewModel.isValidEncryptedFile(fileData)) {
                            android.util.Log.d("QuickCards", "File appears to be valid encrypted format")
                            val result = cardViewModel.importCardsFromSecureFile(fileData)
                            if (result.isSuccess) {
                                val (importedCount, skippedCount) = result.getOrNull() ?: Pair(0, 0)
                                if (skippedCount > 0) {
                                    // Show dialog asking user if they want to force import
                                    pendingImportData = fileData
                                    showDuplicateDialog = true
                                } else {
                                val message = when {
                                        importedCount > 0 -> "Successfully imported $importedCount cards"
                                    else -> "No cards imported"
                                }
                                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
                                }
                            } else {
                                val errorMessage = result.exceptionOrNull()?.message ?: "Unknown error"
                                android.util.Log.e("QuickCards", "Import failed: $errorMessage")
                                Toast.makeText(context, "Import failed: $errorMessage", Toast.LENGTH_LONG).show()
                            }
                        } else {
                            android.util.Log.d("QuickCards", "File is not valid encrypted format, trying as plain JSON")
                            // Try as plain JSON format by creating a simple encrypted wrapper
                            try {
                                val jsonData = String(fileData, Charsets.UTF_8)
                                // Create a simple encrypted wrapper for plain JSON
                                val encryptedWrapper = secureFileManager.encryptExportData(jsonData)
                                val result = cardViewModel.importCardsFromSecureFile(encryptedWrapper)
                                if (result.isSuccess) {
                                    val (importedCount, skippedCount) = result.getOrNull() ?: Pair(0, 0)
                                    Toast.makeText(context, "Successfully imported $importedCount cards from JSON (${skippedCount} skipped)", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, "Import failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_SHORT).show()
                                }
                            } catch (e: Exception) {
                                android.util.Log.e("QuickCards", "JSON import failed: ${e.message}")
                                Toast.makeText(context, "Invalid file format. Please select a valid QuickCards export file.", Toast.LENGTH_LONG).show()
                            }
                        }
                    } else {
                        Toast.makeText(context, "Failed to read file data", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Import failed: ${e.message}", Toast.LENGTH_SHORT).show()
                } finally {
                    isImporting = false
                }
            }
        }
    }
    
    if (showClearDataDialog) {
        AlertDialog(
            onDismissRequest = { showClearDataDialog = false },
            title = { Text("Clear All Data") },
            text = { 
                Text("This will permanently delete all your cards and banks. This action cannot be undone. Please authenticate to continue.") 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        try {
                            if (biometricAuthHelper.isAuthenticationAvailable()) {
                                biometricAuthHelper.authenticateUser(
                                    activity = context as androidx.fragment.app.FragmentActivity,
                                    callback = object : BiometricAuthHelper.AuthenticationCallback {
                                        override fun onAuthenticationSuccess() {
                                            isLoading = true
                                            cardViewModel.deleteAllCards()
                                            bankViewModel.deleteAllBanks()
                                            showClearDataDialog = false
                                            isLoading = false
                                            Toast.makeText(context, "All data cleared successfully", Toast.LENGTH_SHORT).show()
                                        }
                                        
                                        override fun onAuthenticationError(errorCode: Int, errorMessage: String) {
                                            android.util.Log.e("QuickCards", "Clear data auth error: $errorCode - $errorMessage")
                                            showClearDataDialog = false
                                            Toast.makeText(context, "Authentication failed: ${biometricAuthHelper.getErrorMessage(errorCode)}", Toast.LENGTH_LONG).show()
                                        }
                                        
                                        override fun onAuthenticationFailed() {
                                            android.util.Log.w("QuickCards", "Clear data authentication failed")
                                            showClearDataDialog = false
                                            Toast.makeText(context, "Authentication failed", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            } else {
                                // Fallback: clear data without authentication
                                isLoading = true
                                cardViewModel.deleteAllCards()
                                bankViewModel.deleteAllBanks()
                                showClearDataDialog = false
                                isLoading = false
                                Toast.makeText(context, "All data cleared successfully", Toast.LENGTH_SHORT).show()
                            }
                        } catch (e: Exception) {
                            android.util.Log.e("QuickCards", "Clear data auth setup error: ${e.message}")
                            showClearDataDialog = false
                            Toast.makeText(context, "Authentication error: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                ) {
                    Text("Clear Data", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showClearDataDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
    

    
    // Duplicate Detection Dialog
    if (showDuplicateDialog) {
        AlertDialog(
            onDismissRequest = { 
                showDuplicateDialog = false 
                pendingImportData = null
            },
            title = { Text("Duplicate Cards Detected") },
            text = { 
                Text("Some cards in the import file already exist in your device. Would you like to force import all cards (this may create duplicates) or skip the duplicates?") 
            },
            confirmButton = {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    TextButton(
                        onClick = {
                            showDuplicateDialog = false
                            pendingImportData?.let { fileData ->
                                coroutineScope.launch {
                                    isImporting = true
                                    try {
                                        val result = cardViewModel.importCardsFromSecureFile(fileData, forceImport = true)
                                        if (result.isSuccess) {
                                            val (importedCount, skippedCount) = result.getOrNull() ?: Pair(0, 0)
                                            Toast.makeText(context, "Force imported $importedCount cards", Toast.LENGTH_LONG).show()
                                        } else {
                                            Toast.makeText(context, "Force import failed: ${result.exceptionOrNull()?.message}", Toast.LENGTH_LONG).show()
                                        }
                                    } catch (e: Exception) {
                                        Toast.makeText(context, "Force import failed: ${e.message}", Toast.LENGTH_LONG).show()
                                    } finally {
                                        isImporting = false
                                        pendingImportData = null
                                    }
                                }
                            }
                        }
                    ) {
                        Text("Force Import All")
                    }
                    
                    TextButton(
                        onClick = {
                            showDuplicateDialog = false
                            pendingImportData = null
                            Toast.makeText(context, "Import cancelled", Toast.LENGTH_SHORT).show()
                        }
                    ) {
                        Text("Skip Duplicates")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showDuplicateDialog = false 
                        pendingImportData = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(ResponsiveDimensions.getResponsivePadding().horizontal)
            .verticalScroll(rememberScrollState()),
        verticalArrangement = Arrangement.spacedBy(ResponsiveDimensions.getResponsiveSpacing().medium)
    ) {
        // Header with Animation
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(600)) + slideInVertically(
                initialOffsetY = { -40 },
                animationSpec = tween(600)
            )
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = "Settings",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(ResponsiveDimensions.getResponsiveSpacing().medium))
                Text(
                    text = "Settings",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        
        // Data Management Section
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(700, delayMillis = 100)) + slideInVertically(
                initialOffsetY = { 40 },
                animationSpec = tween(700, delayMillis = 100)
            )
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = ResponsiveDimensions.getResponsiveCardDimensions().elevation * 0.5f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(ResponsiveDimensions.getResponsivePadding().horizontal)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Storage,
                            contentDescription = "Data Management",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(ResponsiveDimensions.getResponsiveSpacing().small))
                        Text(
                            text = "Data Management",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    // Statistics (removed Tags)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        StatCard("Cards", cards.size.toString())
                        StatCard("Banks", banks.size.toString())
                        StatCard("Security", "Protected")
                    }
                    
                    Spacer(modifier = Modifier.height(ResponsiveDimensions.getResponsiveSpacing().medium))
                    
                    // Security Information
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(ResponsiveDimensions.getResponsiveSpacing().medium)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(ResponsiveDimensions.getResponsiveSpacing().small)
                            ) {
                                Icon(
                                    Icons.Default.Security,
                                    contentDescription = "Security",
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = "Security Features",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            Text(
                                text = "• Screenshots and screen recording blocked",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "• App excluded from recent apps preview",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "• All sensitive data encrypted with AES-256-GCM",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "• Biometric authentication required for sensitive operations",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(ResponsiveDimensions.getResponsiveSpacing().medium))
                    
                    // Export/Import Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(ResponsiveDimensions.getResponsiveSpacing().small)
                    ) {
                        OutlinedButton(
                            onClick = {
                                if (cards.isEmpty()) {
                                    Toast.makeText(context, "No cards to export", Toast.LENGTH_SHORT).show()
                                    return@OutlinedButton
                                }
                                
                                // Require authentication before export
                                try {
                                    if (biometricAuthHelper.isAuthenticationAvailable()) {
                                        biometricAuthHelper.authenticateUser(
                                            activity = context as FragmentActivity,
                                            callback = object : BiometricAuthHelper.AuthenticationCallback {
                                                override fun onAuthenticationSuccess() {
                                                    val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
                                                    val fileExtension = cardViewModel.getSecureFileExtension()
                                                    exportLauncher.launch("quickcards_export_$timestamp.$fileExtension")
                                                }
                                                
                                                override fun onAuthenticationError(errorCode: Int, errorMessage: String) {
                                                    android.util.Log.e("QuickCards", "Biometric auth error: $errorCode - $errorMessage")
                                                    Toast.makeText(context, "Authentication failed: ${biometricAuthHelper.getErrorMessage(errorCode)}", Toast.LENGTH_LONG).show()
                                                }
                                                
                                                override fun onAuthenticationFailed() {
                                                    android.util.Log.w("QuickCards", "Biometric authentication failed")
                                                    Toast.makeText(context, "Authentication failed. Please try again.", Toast.LENGTH_SHORT).show()
                                                }
                                            },
                                            title = "Authenticate for Export",
                                            subtitle = "Verify your identity to export cards",
                                            description = "Use your fingerprint, face, or device lock to continue"
                                        )
                                    } else {
                                        // Fallback: export without authentication
                                        val timestamp = SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(Date())
                                        val fileExtension = cardViewModel.getSecureFileExtension()
                                        exportLauncher.launch("quickcards_export_$timestamp.$fileExtension")
                                    }
                                } catch (e: Exception) {
                                    android.util.Log.e("QuickCards", "Authentication setup error: ${e.message}")
                                    Toast.makeText(context, "Authentication error: ${e.message}", Toast.LENGTH_SHORT).show()
                                }
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isExporting && !isImporting,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            )
                        ) {
                            if (isExporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.FileUpload, contentDescription = "Export")
                            }
                            Spacer(modifier = Modifier.width(ResponsiveDimensions.getResponsiveSpacing().small))
                            Text(if (isExporting) "Exporting..." else "Export Cards")
                        }
                        
                        OutlinedButton(
                            onClick = {
                                importLauncher.launch(arrayOf("application/octet-stream", "application/json", "*/*"))
                            },
                            modifier = Modifier.weight(1f),
                            enabled = !isExporting && !isImporting,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.secondary
                            )
                        ) {
                            if (isImporting) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(16.dp),
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Icon(Icons.Default.FileDownload, contentDescription = "Import")
                            }
                            Spacer(modifier = Modifier.width(ResponsiveDimensions.getResponsiveSpacing().small))
                            Text(if (isImporting) "Importing..." else "Import Cards")
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Clear All Data Button
                    OutlinedButton(
                        onClick = { showClearDataDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = MaterialTheme.colorScheme.error
                        )
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear Data")
                        Spacer(modifier = Modifier.width(ResponsiveDimensions.getResponsiveSpacing().small))
                        Text("Clear All Data")
                    }
                }
            }
        }
        
        // Manage Banks Section
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(700, delayMillis = 200)) + slideInVertically(
                initialOffsetY = { 40 },
                animationSpec = tween(700, delayMillis = 200)
            )
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = ResponsiveDimensions.getResponsiveCardDimensions().elevation * 0.5f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(ResponsiveDimensions.getResponsivePadding().horizontal)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Default.AccountBalance,
                                contentDescription = "Banks",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(ResponsiveDimensions.getResponsiveSpacing().small))
                            Text(
                                text = "Manage Banks",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        
                        FilledTonalButton(
                            onClick = { navController.navigate("manage_banks") },
                            colors = ButtonDefaults.filledTonalButtonColors(
                                containerColor = MaterialTheme.colorScheme.primaryContainer
                            )
                        ) {
                            Text("View All")
                            Spacer(modifier = Modifier.width(ResponsiveDimensions.getResponsiveSpacing().small))
                            Icon(
                                Icons.Default.ArrowForward,
                                contentDescription = "View Banks",
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(ResponsiveDimensions.getResponsiveSpacing().small))
                    
                    Text(
                        text = "Add, edit, or remove banks from your dropdown list",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // Card Management Section (for Card Types and Issuers)
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(700, delayMillis = 300)) + slideInVertically(
                initialOffsetY = { 40 },
                animationSpec = tween(700, delayMillis = 300)
            )
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = ResponsiveDimensions.getResponsiveCardDimensions().elevation * 0.5f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(ResponsiveDimensions.getResponsivePadding().horizontal)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CreditCard,
                            contentDescription = "Card Management",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(ResponsiveDimensions.getResponsiveSpacing().small))
                        Text(
                            text = "Card Management",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(ResponsiveDimensions.getResponsiveSpacing().medium))
                    
                    // Card Types and Issuers info
                    Text(
                        text = "Card Types: Credit, Debit, Prepaid, Gift Card",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(ResponsiveDimensions.getResponsiveSpacing().small))
                    
                    Text(
                        text = "Card Issuers: Visa, Mastercard, RuPay, American Express, Discover, Diners Club, JCB, UnionPay, Bajaj Finserv, HDFC Bank",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    
                    Spacer(modifier = Modifier.height(ResponsiveDimensions.getResponsiveSpacing().small))
                    
                    Text(
                        text = "Card types and issuers are automatically detected when adding cards",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
        
        // App Information Section
        AnimatedVisibility(
            visible = true,
            enter = fadeIn(animationSpec = tween(700, delayMillis = 400)) + slideInVertically(
                initialOffsetY = { 40 },
                animationSpec = tween(700, delayMillis = 400)
            )
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = ResponsiveDimensions.getResponsiveCardDimensions().elevation * 0.5f),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(ResponsiveDimensions.getResponsivePadding().horizontal)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Info,
                            contentDescription = "App Information",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(ResponsiveDimensions.getResponsiveSpacing().small))
                        Text(
                            text = "App Information",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    InfoRow("Version", "1.0.0")
                    InfoRow("Build", "1")
                    InfoRow("Data Storage", "Local Only")
                    InfoRow("Encryption", "AES-256 GCM")
                    InfoRow("Authentication", if (biometricAuthHelper.isAuthenticationAvailable()) "Enabled" else "Not Available")
                }
            }
        }
        
        if (isLoading) {
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator()
            }
        }
    }
}

@Composable
private fun StatCard(title: String, value: String) {
    Card(
        elevation = CardDefaults.cardElevation(defaultElevation = ResponsiveDimensions.getResponsiveCardDimensions().elevation * 0.25f),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(ResponsiveDimensions.getResponsiveCardDimensions().cornerRadius))
    ) {
        Column(
            modifier = Modifier.padding(ResponsiveDimensions.getResponsivePadding().horizontal),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(ResponsiveDimensions.getResponsiveSpacing().small))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium
        )
    }
}