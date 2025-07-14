package com.quickcards.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.FragmentActivity
import com.quickcards.app.security.BiometricAuthHelper

@Composable
fun LockScreen(
    onUnlock: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    var isAuthenticating by remember { mutableStateOf(false) }
    var authenticationError by remember { mutableStateOf<String?>(null) }

    // Show error as a Toast instead of a card
    LaunchedEffect(authenticationError) {
        authenticationError?.let { errorMsg ->
            if (errorMsg.isNotBlank()) {
                android.widget.Toast.makeText(context, errorMsg, android.widget.Toast.LENGTH_SHORT).show()
                authenticationError = null
            }
        }
    }

    fun authenticateUser() {
        if (activity != null && !isAuthenticating) {
            isAuthenticating = true
            authenticationError = null

            val biometricHelper = BiometricAuthHelper(context)

            biometricHelper.authenticateUser(
                activity,
                object : BiometricAuthHelper.AuthenticationCallback {
                    override fun onAuthenticationSuccess() {
                        isAuthenticating = false
                        onUnlock()
                    }
                    override fun onAuthenticationError(errorCode: Int, errorMessage: String) {
                        isAuthenticating = false
                        authenticationError = "Authentication error: $errorMessage"
                    }
                    override fun onAuthenticationFailed() {
                        isAuthenticating = false
                        authenticationError = "Authentication failed. Please try again."
                    }
                }
            )
        }
    }

    LaunchedEffect(Unit) {
        if (activity != null) {
            authenticateUser()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            // App Icon/Logo
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(120.dp)
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = "QuickCards",
                        tint = Color.White,
                        modifier = Modifier.size(60.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // App Name
            Text(
                text = "QuickCards",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Lock Status
            Text(
                text = "App Locked",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Lock Icon
            Icon(
                Icons.Default.Lock,
                contentDescription = "Locked",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(48.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            // Instructions
            Text(
                text = "Use your fingerprint, face, or device lock to unlock the app",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Manual Unlock Button (only show when not authenticating)
            if (!isAuthenticating) {
                Button(
                    onClick = { authenticateUser() },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        Icons.Default.Security,
                        contentDescription = "Unlock",
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Unlock App")
                }
            } else {
                // Show loading indicator when authenticating
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            "Authenticating...",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Timeout Info
            Text(
                text = "App will auto-lock after 30 seconds of inactivity",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
        }
    }
} 