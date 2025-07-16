package com.quickcards.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.quickcards.app.utils.ResponsiveDimensions

@Composable
fun ColorPalette(
    selectedColor: String,
    onColorSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    
    // Predefined colors
    val predefinedColors = listOf(
        "#2196F3", // Blue (default)
        "#4CAF50", // Green
        "#FF9800", // Orange
        "#9C27B0", // Purple
        "#F44336"  // Red
    )
    
    var showColorPicker by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(ResponsiveDimensions.getResponsiveSpacing().small)
    ) {
        // Label
        Text(
            text = "Card Theme",
            style = MaterialTheme.typography.labelLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        // Color options
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(ResponsiveDimensions.getResponsiveSpacing().small)
        ) {
            // Predefined colors
            predefinedColors.forEach { color ->
                ColorOption(
                    color = color,
                    isSelected = selectedColor == color,
                    onClick = { onColorSelected(color) },
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Custom color picker
            ColorOption(
                color = if (selectedColor !in predefinedColors) selectedColor else "#E0E0E0",
                isSelected = selectedColor !in predefinedColors,
                onClick = { showColorPicker = true },
                isCustom = true,
                modifier = Modifier.weight(1f)
            )
        }
    }
    
    // Color picker dialog
    if (showColorPicker) {
        ColorPickerDialog(
            initialColor = selectedColor,
            onColorSelected = { color ->
                onColorSelected(color)
                showColorPicker = false
            },
            onDismiss = { showColorPicker = false }
        )
    }
}

@Composable
private fun ColorOption(
    color: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    isCustom: Boolean = false,
    modifier: Modifier = Modifier
) {
    val colorValue = try {
        Color(android.graphics.Color.parseColor(color))
    } catch (e: Exception) {
        Color.Gray
    }
    
    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(colorValue)
            .border(
                width = if (isSelected) 3.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(6.dp)
            )
            .clickable { onClick() },
        contentAlignment = Alignment.Center
    ) {
        if (isCustom) {
            Icon(
                imageVector = Icons.Default.Palette,
                contentDescription = "Custom Color",
                tint = if (colorValue.red * 0.299f + colorValue.green * 0.587f + colorValue.blue * 0.114f > 0.5f) Color.Black else Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun ColorPickerDialog(
    initialColor: String,
    onColorSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedColor by remember { mutableStateOf(initialColor) }
    
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(ResponsiveDimensions.getResponsiveCardDimensions().cornerRadius)),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = ResponsiveDimensions.getResponsiveCardDimensions().elevation
        ) {
            Column(
                modifier = Modifier.padding(ResponsiveDimensions.getResponsivePadding().horizontal),
                verticalArrangement = Arrangement.spacedBy(ResponsiveDimensions.getResponsiveSpacing().medium)
            ) {
                // Header
                Text(
                    text = "Choose Custom Color",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = ResponsiveDimensions.getResponsiveSpacing().medium)
                )
                
                // Color preview
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(
                            try {
                                Color(android.graphics.Color.parseColor(selectedColor))
                            } catch (e: Exception) {
                                Color.Gray
                            }
                        )
                        .border(
                            width = 1.dp,
                            color = MaterialTheme.colorScheme.outline,
                            shape = RoundedCornerShape(12.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = selectedColor,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                // Color input field
                OutlinedTextField(
                    value = selectedColor,
                    onValueChange = { 
                        if (it.startsWith("#") && it.length <= 7) {
                            selectedColor = it
                        }
                    },
                    label = { Text("Hex Color Code") },
                    placeholder = { Text("#RRGGBB") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                // Quick color buttons
                Text(
                    text = "Quick Colors",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Medium
                )
                
                val quickColors = listOf(
                    "#FF6B6B", "#4ECDC4", "#45B7D1", "#96CEB4", "#FFEAA7",
                    "#DDA0DD", "#98D8C8", "#F7DC6F", "#BB8FCE", "#85C1E9"
                )
                
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(ResponsiveDimensions.getResponsiveSpacing().small)
                ) {
                    items(quickColors) { color ->
                        ColorOption(
                            color = color,
                            isSelected = selectedColor == color,
                            onClick = { selectedColor = color },
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ResponsiveDimensions.getResponsiveSpacing().small)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = { onColorSelected(selectedColor) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Apply")
                    }
                }
                
                Spacer(modifier = Modifier.height(ResponsiveDimensions.getResponsiveSpacing().medium))
            }
        }
    }
} 