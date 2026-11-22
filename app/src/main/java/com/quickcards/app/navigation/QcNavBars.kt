package com.quickcards.app.navigation

import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.quickcards.app.R
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcShapes
import com.quickcards.app.core.ui.theme.QcSize
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTheme

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

/** One destination: a 56 × 32 indicator (tonal-2 when selected) around the icon, then the `f1` label. */
@Composable
private fun NavDestination(item: NavItem, selected: Boolean, onSelect: (String) -> Unit, modifier: Modifier) {
    val colors = QcTheme.colors
    val ink = if (selected) colors.ink else colors.ink2
    Column(
        modifier = modifier.selectable(selected = selected, role = Role.Tab, onClick = { onSelect(item.route) }),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(QcSpacing.s1, Alignment.CenterVertically)
    ) {
        Box(
            modifier = Modifier
                .size(width = QcSize.navIndicator, height = QcSize.ctl)
                .background(if (selected) colors.tonal2 else Color.Transparent, QcShapes.full),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (selected) item.selectedIcon else item.icon,
                contentDescription = null,
                tint = ink,
                modifier = Modifier.size(QcSize.iconLg)
            )
        }
        Text(
            text = stringResource(item.label),
            style = if (selected) QcText.F1Semibold else QcText.F1,
            color = ink,
            maxLines = 1
        )
    }
}

@Composable
internal fun QcNavigationRail(selectedRoute: String, onSelect: (String) -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(QcDimens.Rail)
            .background(QcTheme.colors.bg)
            .windowInsetsPadding(WindowInsets.systemBars.only(WindowInsetsSides.Vertical + WindowInsetsSides.Start))
            .padding(vertical = QcSpacing.s2)
            .selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(QcSpacing.s1)
    ) {
        NavigationItems.forEach { item ->
            NavDestination(item, item.route == selectedRoute, onSelect, Modifier.fillMaxWidth().heightIn(min = QcSpacing.s16))
        }
    }
}

@Composable
internal fun QcNavigationBar(selectedRoute: String, onSelect: (String) -> Unit) {
    val colors = QcTheme.colors
    Column(modifier = Modifier.fillMaxWidth().background(colors.bg)) {
        Box(modifier = Modifier.fillMaxWidth().height(QcDimens.Divider).background(colors.line))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .height(QcDimens.NavBarHeight)
                .padding(horizontal = QcSpacing.s2)
                .selectableGroup()
        ) {
            NavigationItems.forEach { item ->
                NavDestination(item, item.route == selectedRoute, onSelect, Modifier.weight(1f).fillMaxHeight())
            }
        }
    }
}
