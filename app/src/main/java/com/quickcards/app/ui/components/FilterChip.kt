package com.quickcards.app.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quickcards.app.data.model.CardFilter
import com.quickcards.app.utils.ResponsiveDimensions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveFilterChip(
    filter: CardFilter,
    onRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val filterLabel = when (filter.type) {
        CardFilter.FilterType.BANK -> "Bank: ${filter.value}"
        CardFilter.FilterType.CARD_ISSUER -> "Issuer: ${filter.value}"
        CardFilter.FilterType.CARD_TYPE -> "Type: ${filter.value}"
    }
    
    FilterChip(
        selected = true,
        onClick = { onRemove() },
        label = {
            Text(
                text = filterLabel,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        },
        trailingIcon = {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = "Remove filter",
                modifier = Modifier.size(16.dp)
            )
        },
        colors = FilterChipDefaults.filterChipColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
            iconColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = modifier
    )
}

@Composable
fun FilterChipsRow(
    activeFilters: Set<CardFilter>,
    onRemoveFilter: (CardFilter) -> Unit,
    onClearAll: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (activeFilters.isEmpty()) return
    
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(ResponsiveDimensions.getResponsiveSpacing().small)
    ) {
        // Active filters chips
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(ResponsiveDimensions.getResponsiveSpacing().small),
            contentPadding = PaddingValues(horizontal = ResponsiveDimensions.getResponsivePadding().horizontal)
        ) {
            items(activeFilters.toList()) { filter ->
                ActiveFilterChip(
                    filter = filter,
                    onRemove = { onRemoveFilter(filter) }
                )
            }
        }
        
        // Clear all button
        TextButton(
            onClick = onClearAll,
            modifier = Modifier.padding(horizontal = ResponsiveDimensions.getResponsivePadding().horizontal)
        ) {
            Text(
                text = "Clear All Filters",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
} 