package com.quickcards.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quickcards.app.utils.ResponsiveDimensions
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.quickcards.app.data.model.Card
import com.quickcards.app.data.model.CardFilter
import com.quickcards.app.security.CardOperationAuthManager
import com.quickcards.app.ui.components.CardItem
import com.quickcards.app.ui.components.FilterChipsRow
import com.quickcards.app.ui.components.FilterDialog
import com.quickcards.app.ui.components.SearchBar
import com.quickcards.app.utils.FilterManager
import com.quickcards.app.viewmodel.CardViewModel
import com.quickcards.app.utils.KeyboardVisibilityHandler
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CardsScreen(
    navController: NavController,
    cardViewModel: CardViewModel = viewModel()
) {
    val context = LocalContext.current
    val cardAuthManager = CardOperationAuthManager.getInstance(context)
    val filterManager = FilterManager.getInstance(context)
    val scope = rememberCoroutineScope()
    val cards by cardViewModel.allCards.observeAsState(emptyList())
    val searchQuery by cardViewModel.searchQuery.observeAsState("")
    val searchResults by cardViewModel.searchResults.observeAsState(emptyList())
    val isLoading by cardViewModel.isLoading.observeAsState(false)
    val filterState by filterManager.filterState.collectAsState()
    
    // Detect keyboard visibility
    val isKeyboardVisible by KeyboardVisibilityHandler.rememberKeyboardVisibilityState()
    
    // Scroll-to-top functionality
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    var showSearchBar by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }
    var isSelectionMode by remember { mutableStateOf(false) }
    var selectedCards by remember { mutableStateOf<Set<String>>(emptySet()) }
    var showLongPressMenu by remember { mutableStateOf(false) }
    var longPressedCard by remember { mutableStateOf<Card?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var cardToDelete by remember { mutableStateOf<Card?>(null) }
    
    // Track card count for scroll-to-top
    var previousCardCount by remember { mutableStateOf(0) }
    
    // Refresh cards when screen becomes active
    LaunchedEffect(Unit) {
        cardViewModel.forceRefreshCards()
    }
    
    // Update filter options when cards change
    LaunchedEffect(cards) {
        filterManager.updateAvailableOptions(cards)
    }
    
    // Scroll to top when new cards are added
    LaunchedEffect(cards.size) {
        if (cards.size > previousCardCount && previousCardCount > 0) {
            // New card added, scroll to top smoothly
            coroutineScope.launch {
                listState.animateScrollToItem(0)
            }
        }
        previousCardCount = cards.size
    }
    
    // Exit selection mode
    fun exitSelectionMode() {
        isSelectionMode = false
        selectedCards = emptySet()
    }
    
    // Handle card selection
    fun toggleCardSelection(cardId: String) {
        selectedCards = if (selectedCards.contains(cardId)) {
            selectedCards - cardId
        } else {
            selectedCards + cardId
        }
        
        // Exit selection mode if no cards are selected
        if (selectedCards.isEmpty()) {
            exitSelectionMode()
        }
    }
    
    // Handle long press menu actions
    fun handleLongPressMenuAction(action: String, card: Card) {
        showLongPressMenu = false
        longPressedCard = null
        
        when (action) {
            "edit" -> {
                navController.navigate("edit_card/${card.id}")
            }
            "delete" -> {
                scope.launch {
                    // Require authentication for deleting
                    val isAuthenticated = cardAuthManager.authenticateForCardDelete(context as androidx.fragment.app.FragmentActivity)
                    if (isAuthenticated) {
                        cardToDelete = card
                        showDeleteDialog = true
                    } else {
                        // Show user feedback for authentication failure
                        android.widget.Toast.makeText(context, "Authentication required to delete card", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
            }
            "select" -> {
                isSelectionMode = true
                selectedCards = setOf(card.id)
            }
        }
    }
    
    // Handle multiple card deletion
    fun deleteSelectedCards() {
        selectedCards.forEach { cardId ->
            cardViewModel.deleteCardById(cardId)
        }
        exitSelectionMode()
        showDeleteDialog = false
    }
    
    Scaffold(
        topBar = {
            if (isSelectionMode) {
                TopAppBar(
                    title = { Text("${selectedCards.size} selected") },
                    navigationIcon = {
                        IconButton(onClick = { exitSelectionMode() }) {
                            Icon(Icons.Default.Close, contentDescription = "Exit Selection")
                        }
                    },
                    actions = {
                        // Edit button (only show if one card is selected)
                        if (selectedCards.size == 1) {
                            IconButton(
                                onClick = {
                                    val cardId = selectedCards.first()
                                    navController.navigate("edit_card/$cardId")
                                    exitSelectionMode()
                                }
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit")
                            }
                        }
                        
                        // Delete button
                        IconButton(
                            onClick = {
                                scope.launch {
                                    // Require authentication for deleting
                                    val isAuthenticated = cardAuthManager.authenticateForCardDelete(context as androidx.fragment.app.FragmentActivity)
                                    if (isAuthenticated) {
                                        showDeleteDialog = true
                                    } else {
                                        // Show user feedback for authentication failure
                                        android.widget.Toast.makeText(context, "Authentication required to delete cards", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete")
                        }
                                }
        )
    }
    
    // Filter Dialog
    if (showFilterDialog) {
        FilterDialog(
            filterState = filterState,
            onFilterSelected = { filter ->
                filterManager.addFilter(filter)
                // Show brief feedback
                android.widget.Toast.makeText(context, "Filter applied", android.widget.Toast.LENGTH_SHORT).show()
            },
            onFilterDeselected = { filter ->
                filterManager.removeFilter(filter)
                // Show brief feedback
                android.widget.Toast.makeText(context, "Filter removed", android.widget.Toast.LENGTH_SHORT).show()
            },
            onDismiss = { showFilterDialog = false }
        )
    }
}
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(ResponsiveDimensions.getResponsivePadding().horizontal)
        ) {
            if (!isSelectionMode) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "My Cards",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Row {
                        IconButton(onClick = { showSearchBar = !showSearchBar }) {
                            Icon(
                                Icons.Default.Search,
                                contentDescription = "Search Cards"
                            )
                        }
                        IconButton(
                            onClick = { showFilterDialog = true }
                        ) {
                            Icon(
                                Icons.Default.FilterList,
                                contentDescription = "Filter Cards",
                                tint = if (filterState.hasActiveFilters()) 
                                    MaterialTheme.colorScheme.primary 
                                else 
                                    MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(ResponsiveDimensions.getResponsiveSpacing().medium))
                
                // Filter Chips
                FilterChipsRow(
                    activeFilters = filterState.activeFilters,
                    onRemoveFilter = { filter ->
                        filterManager.removeFilter(filter)
                    },
                    onClearAll = {
                        filterManager.clearAllFilters()
                    }
                )
                
                // Search Bar
                if (showSearchBar) {
                    SearchBar(
                        query = searchQuery,
                        onQueryChange = { cardViewModel.searchCards(it) },
                        onClearQuery = { cardViewModel.searchCards("") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(ResponsiveDimensions.getResponsiveSpacing().medium))
                }
            }
            
            // Cards List (optimized with derivedStateOf)
            val displayCards by remember(filterState) {
                derivedStateOf {
                    val filteredCards = filterManager.applyFilters(cards)
                    if (searchQuery.isNotBlank()) {
                        // Apply search to filtered cards
                        filteredCards.filter { card ->
                            card.bankName.contains(searchQuery, ignoreCase = true) ||
                            card.cardIssuer.contains(searchQuery, ignoreCase = true) ||
                            card.cardType.contains(searchQuery, ignoreCase = true) ||
                            card.owner.contains(searchQuery, ignoreCase = true) ||
                            card.description.contains(searchQuery, ignoreCase = true)
                        }
                    } else {
                        filteredCards
                    }
                }
            }
            
            if (isLoading) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator()
                }
            } else if (displayCards.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = if (searchQuery.isNotBlank()) "No cards found" else "No cards added yet",
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Spacer(modifier = Modifier.height(ResponsiveDimensions.getResponsiveSpacing().small))
                        Text(
                            text = if (searchQuery.isNotBlank()) "Try a different search term" else "Tap the + button to add your first card",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(ResponsiveDimensions.getResponsiveSpacing().extraLarge), // ✅ Responsive spacing for larger cards
                    contentPadding = PaddingValues(
                        bottom = if (isKeyboardVisible) ResponsiveDimensions.getResponsiveSpacing().medium else (ResponsiveDimensions.getResponsiveSpacing().large * 5) // ✅ Dynamic responsive padding based on keyboard visibility
                    ),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = displayCards,
                        key = { card -> card.id } // ✅ Add key for better performance
                    ) { card ->
                        // Use cached decrypted card data
                        CardItem(
                            card = card,
                            onClick = { 
                                if (isSelectionMode) {
                                    toggleCardSelection(card.id)
                                } else {
                                    // Navigate to card detail only if card has valid ID
                                    if (card.id.isNotBlank()) {
                                        navController.navigate("card_detail/${card.id}")
                                    }
                                }
                            },
                            onLongClick = {
                                if (!isSelectionMode) {
                                    longPressedCard = card
                                    showLongPressMenu = true
                                }
                            },
                            isSelected = selectedCards.contains(card.id),
                            isSelectionMode = isSelectionMode
                        )
                    }
                }
            }
        }
        
        // Floating Action Button (only show when not in selection mode and keyboard is hidden)
        if (!isSelectionMode && !isKeyboardVisible) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomEnd
            ) {
                FloatingActionButton(
                    onClick = { 
                        // Navigate to add card screen
                        navController.navigate("add_card")
                    },
                    modifier = Modifier.padding(ResponsiveDimensions.getResponsivePadding().horizontal)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add Card")
                }
            }
        }
    }
    
    // Long Press Menu Dialog
    if (showLongPressMenu && longPressedCard != null) {
        Dialog(
            onDismissRequest = { 
                showLongPressMenu = false
                longPressedCard = null
            },
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.8f)
                    .clip(RoundedCornerShape(ResponsiveDimensions.getResponsiveCardDimensions().cornerRadius)),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = ResponsiveDimensions.getResponsiveCardDimensions().elevation
            ) {
                Column(
                    modifier = Modifier.padding(ResponsiveDimensions.getResponsivePadding().horizontal)
                ) {
                    Text(
                        text = "Card Actions",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = ResponsiveDimensions.getResponsiveSpacing().medium)
                    )
                    
                    // Edit Option
                    TextButton(
                        onClick = { 
                            handleLongPressMenuAction("edit", longPressedCard!!)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier.padding(end = ResponsiveDimensions.getResponsiveSpacing().small)
                        )
                        Text("Edit Card")
                    }
                    
                    // Delete Option
                    TextButton(
                        onClick = { 
                            handleLongPressMenuAction("delete", longPressedCard!!)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier.padding(end = ResponsiveDimensions.getResponsiveSpacing().small)
                        )
                        Text("Delete Card")
                    }
                    
                    // Select Option (Start Multi-Selection)
                    TextButton(
                        onClick = { 
                            handleLongPressMenuAction("select", longPressedCard!!)
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "Select",
                            modifier = Modifier.padding(end = ResponsiveDimensions.getResponsiveSpacing().small)
                        )
                        Text("Select Cards")
                    }
                    
                    Spacer(modifier = Modifier.height(ResponsiveDimensions.getResponsiveSpacing().small))
                    
                    // Cancel Button
                    OutlinedButton(
                        onClick = { 
                            showLongPressMenu = false
                            longPressedCard = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Cancel")
                    }
                }
            }
        }
    }
    
    // Delete Confirmation Dialog
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { 
                Text(
                    if (isSelectionMode) 
                        "Delete ${selectedCards.size} cards?" 
                    else 
                        "Delete card?"
                ) 
            },
            text = { 
                Text(
                    if (isSelectionMode) 
                        "Are you sure you want to delete the selected cards? This action cannot be undone." 
                    else 
                        "Are you sure you want to delete this card? This action cannot be undone."
                ) 
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            if (isSelectionMode) {
                                // For multiple deletion, authenticate once
                                val isAuthenticated = cardAuthManager.authenticateForCardDelete(context as androidx.fragment.app.FragmentActivity)
                                if (isAuthenticated) {
                                    deleteSelectedCards()
                                } else {
                                    // Show user feedback for authentication failure
                                    android.widget.Toast.makeText(context, "Authentication required to delete cards", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            } else {
                                // For single deletion, authenticate
                                val isAuthenticated = cardAuthManager.authenticateForCardDelete(context as androidx.fragment.app.FragmentActivity)
                                if (isAuthenticated) {
                                    cardToDelete?.let { card ->
                                        cardViewModel.deleteCardById(card.id)
                                    }
                                    showDeleteDialog = false
                                    cardToDelete = null
                                } else {
                                    // Show user feedback for authentication failure
                                    android.widget.Toast.makeText(context, "Authentication required to delete card", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { 
                        showDeleteDialog = false
                        cardToDelete = null
                    }
                ) {
                    Text("Cancel")
                }
            }
        )
    }
}