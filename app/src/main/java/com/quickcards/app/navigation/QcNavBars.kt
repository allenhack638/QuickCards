package com.quickcards.app.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.NavigationRailItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.quickcards.app.R
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcType

private data class NavItem(
    val route: String,
    @StringRes val label: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

private val NavigationItems = listOf(
    NavItem(ROUTE_CARDS, R.string.cards_title, QcIcons.card, QcIcons.Selected.card),
    NavItem(ROUTE_MANAGE, R.string.manage_title, QcIcons.manage, QcIcons.Selected.manage),
    NavItem(ROUTE_SETTINGS, R.string.settings_title, QcIcons.settings, QcIcons.Selected.settings)
)

@Composable
private fun NavLabel(item: NavItem, selected: Boolean) {
    Text(
        text = stringResource(item.label),
        style = QcType.Label.copy(fontWeight = if (selected) FontWeight(700) else FontWeight(500))
    )
}

@Composable
private fun NavIcon(item: NavItem, selected: Boolean) {
    Icon(imageVector = if (selected) item.selectedIcon else item.icon, contentDescription = null)
}

@Composable
internal fun QcNavigationRail(selectedRoute: String, onSelect: (String) -> Unit) {
    NavigationRail(
        containerColor = MaterialTheme.colorScheme.background,
        modifier = Modifier
            .fillMaxHeight()
            .width(QcDimens.Rail)
    ) {
        NavigationItems.forEach { item ->
            val selected = item.route == selectedRoute
            NavigationRailItem(
                selected = selected,
                onClick = { onSelect(item.route) },
                icon = { NavIcon(item, selected) },
                label = { NavLabel(item, selected) },
                alwaysShowLabel = true,
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.onSurface,
                    selectedTextColor = MaterialTheme.colorScheme.onSurface,
                    indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                    unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
internal fun QcNavigationBar(selectedRoute: String, onSelect: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow)) {
        HorizontalDivider(thickness = QcDimens.Divider, color = MaterialTheme.colorScheme.outlineVariant)
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            tonalElevation = 0.dp,
            windowInsets = NavigationBarDefaults.windowInsets
        ) {
            NavigationItems.forEach { item ->
                val selected = item.route == selectedRoute
                NavigationBarItem(
                    selected = selected,
                    onClick = { onSelect(item.route) },
                    icon = { NavIcon(item, selected) },
                    label = { NavLabel(item, selected) },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.onSurface,
                        selectedTextColor = MaterialTheme.colorScheme.onSurface,
                        indicatorColor = MaterialTheme.colorScheme.secondaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                )
            }
        }
    }
}
