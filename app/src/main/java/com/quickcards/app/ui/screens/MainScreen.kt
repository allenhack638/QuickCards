package com.quickcards.app.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.quickcards.app.utils.KeyboardVisibilityHandler

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen() {
    val navController = rememberNavController()
    val items = listOf(
        BottomNavItem.Cards,
        BottomNavItem.Settings
    )
    
    // Detect keyboard visibility
    val isKeyboardVisible by KeyboardVisibilityHandler.rememberKeyboardVisibilityState()
    
    Scaffold(
        bottomBar = {
            // Hide bottom navigation when keyboard is visible
            if (!isKeyboardVisible) {
                NavigationBar {
                    val navBackStackEntry by navController.currentBackStackEntryAsState()
                    val currentDestination = navBackStackEntry?.destination
                    
                    items.forEach { item ->
                        NavigationBarItem(
                            icon = { Icon(item.icon, contentDescription = item.title) },
                            label = { Text(item.title) },
                            selected = currentDestination?.hierarchy?.any { it.route == item.route } == true,
                            onClick = {
                                navController.navigate(item.route) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = true
                                    }
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            }
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = BottomNavItem.Cards.route,
            modifier = Modifier.padding(innerPadding)
        ) {
            composable(BottomNavItem.Cards.route) {
                CardsScreen(navController = navController)
            }
            composable(BottomNavItem.Settings.route) {
                SettingsScreen(navController = navController)
            }
            composable("add_card") {
                AddEditCardScreen(navController = navController)
            }
            composable("edit_card/{cardId}") { backStackEntry ->
                val cardId = backStackEntry.arguments?.getString("cardId")
                AddEditCardScreen(navController = navController, cardId = cardId)
            }
            composable("card_detail/{cardId}") { backStackEntry ->
                val cardId = backStackEntry.arguments?.getString("cardId")
                if (cardId != null && cardId.isNotBlank()) {
                    CardDetailScreen(navController = navController, cardId = cardId)
                } else {
                    // Navigate back if invalid cardId
                    LaunchedEffect(Unit) {
                        navController.popBackStack()
                    }
                }
            }
            composable("manage_banks") {
                ManageBanksScreen(navController = navController)
            }
            composable("manage_tags") {
                ManageTagsScreen(navController = navController)
            }
        }
    }
}

sealed class BottomNavItem(val route: String, val title: String, val icon: androidx.compose.ui.graphics.vector.ImageVector) {
    object Cards : BottomNavItem("cards", "Cards", Icons.Default.CreditCard)
    object Settings : BottomNavItem("settings", "Settings", Icons.Default.Settings)
}