package com.quickcards.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.quickcards.app.data.model.CardFilter
import com.quickcards.app.data.model.FilterState
import com.quickcards.app.utils.ResponsiveDimensions
import androidx.compose.foundation.clickable

@Composable
fun FilterDialog(
    filterState: FilterState,
    onFilterSelected: (CardFilter) -> Unit,
    onFilterDeselected: (CardFilter) -> Unit,
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .fillMaxHeight(0.8f),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(ResponsiveDimensions.getResponsivePadding().horizontal),
                verticalArrangement = Arrangement.spacedBy(ResponsiveDimensions.getResponsiveSpacing().medium)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Filter Cards",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Close"
                        )
                    }
                }
                
                HorizontalDivider()
                
                // Filter Categories
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(ResponsiveDimensions.getResponsiveSpacing().large),
                    modifier = Modifier.weight(1f)
                ) {
                    // Banks
                    if (filterState.availableBanks.isNotEmpty()) {
                        item {
                            FilterCategory(
                                title = "Banks",
                                options = filterState.availableBanks.toList(),
                                activeFilters = filterState.getActiveFiltersByType(CardFilter.FilterType.BANK),
                                onFilterSelected = { bank ->
                                    onFilterSelected(CardFilter.bank(bank))
                                },
                                onFilterDeselected = { bank ->
                                    onFilterDeselected(CardFilter.bank(bank))
                                }
                            )
                        }
                    }
                    
                    // Card Issuers
                    if (filterState.availableCardIssuers.isNotEmpty()) {
                        item {
                            FilterCategory(
                                title = "Card Issuers",
                                options = filterState.availableCardIssuers.toList(),
                                activeFilters = filterState.getActiveFiltersByType(CardFilter.FilterType.CARD_ISSUER),
                                onFilterSelected = { issuer ->
                                    onFilterSelected(CardFilter.cardIssuer(issuer))
                                },
                                onFilterDeselected = { issuer ->
                                    onFilterDeselected(CardFilter.cardIssuer(issuer))
                                }
                            )
                        }
                    }
                    
                    // Card Types
                    if (filterState.availableCardTypes.isNotEmpty()) {
                        item {
                            FilterCategory(
                                title = "Card Types",
                                options = filterState.availableCardTypes.toList(),
                                activeFilters = filterState.getActiveFiltersByType(CardFilter.FilterType.CARD_TYPE),
                                onFilterSelected = { type ->
                                    onFilterSelected(CardFilter.cardType(type))
                                },
                                onFilterDeselected = { type ->
                                    onFilterDeselected(CardFilter.cardType(type))
                                }
                            )
                        }
                    }
                }
                
                // Action buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(ResponsiveDimensions.getResponsiveSpacing().medium)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    
                    Button(
                        onClick = onDismiss,
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

@Composable
private fun FilterCategory(
    title: String,
    options: List<String>,
    activeFilters: Set<String>,
    onFilterSelected: (String) -> Unit,
    onFilterDeselected: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(ResponsiveDimensions.getResponsiveSpacing().small)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface
        )
        
        Column(
            verticalArrangement = Arrangement.spacedBy(ResponsiveDimensions.getResponsiveSpacing().small)
        ) {
            options.sorted().forEach { option ->
                FilterOption(
                    option = option,
                    isSelected = activeFilters.contains(option),
                    onSelectionChanged = { isSelected ->
                        if (isSelected) {
                            onFilterSelected(option)
                        } else {
                            onFilterDeselected(option)
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun FilterOption(
    option: String,
    isSelected: Boolean,
    onSelectionChanged: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelectionChanged(!isSelected) },
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = onSelectionChanged
        )
        
        Spacer(modifier = Modifier.width(ResponsiveDimensions.getResponsiveSpacing().small))
        
        Text(
            text = option,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
} 