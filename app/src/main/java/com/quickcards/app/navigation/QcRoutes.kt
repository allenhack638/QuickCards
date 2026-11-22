package com.quickcards.app.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

internal const val ROUTE_CARDS = "cards"
internal const val ROUTE_MANAGE = "manage"
internal const val ROUTE_SETTINGS = "settings"
internal const val ROUTE_CARD_ADD = "card/add"
internal const val ROUTE_CARD_EDIT = "card/edit/{cardId}"
internal const val ROUTE_CARD_DETAILS = "card/details/{cardId}"
internal const val ROUTE_ARCHIVED = "archived"
internal const val ROUTE_PRIVACY = "privacy"
internal const val ROUTE_APP_LOCK = "settings/lock"
internal const val ROUTE_MANAGE_LIST = "manage/{kind}"
internal const val ROUTE_MANAGE_EDIT = "manage/{kind}/edit?id={id}"

private val RootRoutes = setOf(ROUTE_CARDS, ROUTE_MANAGE, ROUTE_SETTINGS)

internal fun isRoot(route: String?): Boolean = route in RootRoutes

internal fun isForm(route: String?): Boolean = route == ROUTE_CARD_ADD || route == ROUTE_CARD_EDIT

internal fun detailsRoute(cardId: String) = "card/details/$cardId"

internal fun editRoute(cardId: String) = "card/edit/$cardId"

/** Goes to a tab root; [save] and [restore] keep each tab's own back stack. */
internal fun NavController.openTab(route: String, save: Boolean = true, restore: Boolean = save) = navigate(route) {
    popUpTo(graph.findStartDestination().id) { saveState = save }
    launchSingleTop = true
    restoreState = restore
}
