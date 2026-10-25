package com.quickcards.app.navigation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.lifecycle.withResumed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.quickcards.domain.CardFilters
import com.quickcards.domain.EntityKind
import com.quickcards.app.core.security.UnlockSession
import com.quickcards.app.core.ui.LocalNavAnimatedScope
import com.quickcards.app.core.ui.LocalSharedTransitionScope
import com.quickcards.app.feature.archive.ArchivedCardsScreen
import com.quickcards.app.feature.backup.BackupHost
import com.quickcards.app.feature.backup.rememberBackupHostState
import com.quickcards.app.feature.cardform.CardFormScreen
import com.quickcards.app.core.ui.components.LocalSnackbar
import com.quickcards.app.core.ui.components.SnackbarController
import com.quickcards.app.feature.cards.HomeScreen
import com.quickcards.app.feature.cards.HomeViewModel
import com.quickcards.app.core.security.AppLockController
import com.quickcards.app.feature.lock.LockScreen
import com.quickcards.app.feature.manage.ManageListScreen
import com.quickcards.app.feature.manage.ManageScreen
import com.quickcards.app.feature.manage.ManageViewModel
import com.quickcards.app.feature.manage.entityKindFromRoute
import com.quickcards.app.feature.manage.routeKey
import com.quickcards.app.feature.settings.SettingsScreen
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcType
import kotlinx.coroutines.launch

private const val ROUTE_CARDS = "cards"
private const val ROUTE_MANAGE = "manage"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_CARD_ADD = "card/add"
private const val ROUTE_CARD_EDIT = "card/edit/{cardId}"
private const val ROUTE_ARCHIVED = "archived"

private val RootRoutes = setOf(ROUTE_CARDS, ROUTE_MANAGE, ROUTE_SETTINGS)
private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

private data class NavItem(
    val route: String,
    val label: String,
    val icon: ImageVector,
    val selectedIcon: ImageVector
)

private val NavigationItems = listOf(
    NavItem(ROUTE_CARDS, "Cards", QcIcons.card, QcIcons.Selected.card),
    NavItem(ROUTE_MANAGE, "Manage", QcIcons.manage, QcIcons.Selected.manage),
    NavItem(ROUTE_SETTINGS, "Settings", QcIcons.settings, QcIcons.Selected.settings)
)

private fun isRoot(route: String?): Boolean = route in RootRoutes

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun QcApp() {
    val navController = rememberNavController()
    val homeViewModel: HomeViewModel = viewModel()
    val manageViewModel: ManageViewModel = viewModel()
    val searchActive by homeViewModel.searchActive.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route
    val showBar = isRoot(route) && !searchActive

    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val snackbarController = remember(snackbarHostState, snackbarScope) {
        SnackbarController(snackbarHostState, snackbarScope)
    }
    val systemBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val locked by AppLockController.isLocked.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = androidx.compose.ui.platform.LocalLifecycleOwner.current
    val unlockScope = rememberCoroutineScope()
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
    val density = LocalDensity.current
    val sharedAxisOffset = with(density) { 32.dp.roundToPx() }

    androidx.compose.runtime.LaunchedEffect(locked) {
        if (locked) focusManager.clearFocus(force = true)
    }

    CompositionLocalProvider(
        LocalSnackbar provides snackbarController,
        com.quickcards.app.core.ui.LocalAppLocked provides locked
    ) {
        val backupHostState = rememberBackupHostState()
        Box(modifier = Modifier.fillMaxSize()) {
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .windowInsetsPadding(
                    WindowInsets.systemBars
                        .union(WindowInsets.displayCutout)
                        .only(WindowInsetsSides.Horizontal)
                )
                .then(if (locked) Modifier.clearAndSetSemantics {} else Modifier)
        ) {
        val useRail = maxWidth >= 600.dp
        val bottomBarVisible = showBar && !useRail
        val contentBottomInset = if (bottomBarVisible) 0.dp else systemBarBottom
        Row(modifier = Modifier.fillMaxSize()) {
            if (showBar && useRail) {
                QcNavigationRail(
                    selectedRoute = route ?: ROUTE_CARDS,
                    onSelect = { target ->
                        if (target != route) {
                            homeViewModel.clearSearch()
                            navController.navigate(target) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                    }
                )
            }
            Scaffold(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                containerColor = MaterialTheme.colorScheme.background,
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                snackbarHost = {
                    SnackbarHost(
                        hostState = snackbarHostState,
                        modifier = if (bottomBarVisible) {
                            Modifier.padding(
                                bottom = QcDimens.NavBarHeight + QcDimens.Divider + systemBarBottom
                            )
                        } else {
                            Modifier.navigationBarsPadding()
                        }
                    ) { data ->
                        androidx.compose.material3.Snackbar(
                            snackbarData = data,
                            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                        )
                    }
                }
            ) { padding ->
                // Tabs reserve the bar height themselves so centred content does not jump
                // when a pushed screen hides the bar.
                val tabBarSpace = if (useRail || searchActive) {
                    0.dp
                } else {
                    QcDimens.NavBarHeight + QcDimens.Divider + systemBarBottom
                }
                val tabBottomInset = if (useRail || searchActive) systemBarBottom else 0.dp
                val tabModifier = Modifier.padding(bottom = tabBarSpace)
                Box(modifier = Modifier.fillMaxSize()) {
                    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
                    CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                    NavHost(
                        navController = navController,
                        startDestination = ROUTE_CARDS,
                        modifier = Modifier
                            .padding(top = padding.calculateTopPadding())
                            .windowInsetsPadding(
                                WindowInsets.statusBars.only(WindowInsetsSides.Top)
                            )
                            .clipToBounds(),
                        contentAlignment = Alignment.TopStart,
                        // Material fade-through: the old screen is gone (90 ms) before the new one
                        // fades in, so text from two screens never overlaps.
                        enterTransition = {
                            if (isRoot(initialState.destination.route) && isRoot(targetState.destination.route)) {
                                fadeIn(tween(160, delayMillis = 50, easing = EmphasizedDecelerate))
                            } else {
                                fadeIn(tween(160, delayMillis = 50, easing = EmphasizedDecelerate)) +
                                    slideInHorizontally(
                                        animationSpec = tween(240, easing = EmphasizedDecelerate)
                                    ) { sharedAxisOffset }
                            }
                        },
                        exitTransition = { fadeOut(tween(50)) },
                        popEnterTransition = {
                            fadeIn(tween(160, delayMillis = 50, easing = EmphasizedDecelerate)) +
                                slideInHorizontally(
                                    animationSpec = tween(240, easing = EmphasizedDecelerate)
                                ) { -sharedAxisOffset }
                        },
                        popExitTransition = { fadeOut(tween(50)) }
                    ) {
                    composable(ROUTE_CARDS) {
                        CompositionLocalProvider(LocalNavAnimatedScope provides this) {
                            Box(modifier = tabModifier) {
                                HomeScreen(
                                    navController = navController,
                                    viewModel = homeViewModel,
                                    bottomInset = tabBottomInset,
                                    onImport = { backupHostState.requestImport() },
                                    onOpenArchived = { navController.navigate(ROUTE_ARCHIVED) }
                                )
                            }
                        }
                    }
                    composable(ROUTE_MANAGE) {
                        Box(modifier = tabModifier) {
                            ManageScreen(
                                viewModel = manageViewModel,
                                bottomInset = tabBottomInset,
                                onOpenKind = { kind ->
                                    navController.navigate("manage/${kind.routeKey}")
                                },
                                onOpenArchived = { navController.navigate(ROUTE_ARCHIVED) }
                            )
                        }
                    }
                    composable(
                        route = "manage/{kind}",
                        arguments = listOf(navArgument("kind") { type = NavType.StringType })
                    ) { entry ->
                        val kind = entityKindFromRoute(entry.arguments?.getString("kind"))
                            ?: EntityKind.HOLDER
                        ManageListScreen(
                            kind = kind,
                            viewModel = manageViewModel,
                            onBack = { navController.popBackStack() },
                            onShowCards = { listKind, id ->
                                homeViewModel.applyFilters(
                                    when (listKind) {
                                        EntityKind.HOLDER -> CardFilters(holders = setOf(id))
                                        EntityKind.BANK -> CardFilters(banks = setOf(id))
                                        EntityKind.NETWORK -> CardFilters(networks = setOf(id))
                                        EntityKind.TYPE -> CardFilters(types = setOf(id))
                                        EntityKind.TAG -> CardFilters(tags = setOf(id))
                                    }
                                )
                                navController.navigate(ROUTE_CARDS) {
                                    popUpTo(navController.graph.findStartDestination().id) {
                                        saveState = false
                                    }
                                    launchSingleTop = true
                                }
                            }
                        )
                    }
                    composable(ROUTE_ARCHIVED) {
                        ArchivedCardsScreen(
                            onBack = { navController.popBackStack() },
                            onEdit = { cardId -> navController.navigate("card/edit/$cardId") },
                            bottomInset = contentBottomInset
                        )
                    }
                    composable(ROUTE_SETTINGS) {
                        Box(modifier = tabModifier) {
                            SettingsScreen(
                                bottomInset = tabBottomInset,
                                onExport = backupHostState::requestExport,
                                onImport = backupHostState::requestImport,
                                onErase = backupHostState::requestErase
                            )
                        }
                    }
                    composable(ROUTE_CARD_ADD) {
                        CardFormScreen(navController = navController)
                    }
                    composable(
                        route = ROUTE_CARD_EDIT,
                        arguments = listOf(navArgument("cardId") { type = NavType.StringType })
                    ) {
                        CompositionLocalProvider(LocalNavAnimatedScope provides this) {
                            CardFormScreen(navController = navController)
                        }
                    }
                    }
                    }
                    }
                    // Drawn over the NavHost and animated in the draw phase only (offset +
                    // alpha), so showing or hiding it never re-measures the screens.
                    val barProgress by animateFloatAsState(
                        targetValue = if (bottomBarVisible) 1f else 0f,
                        animationSpec = tween(
                            durationMillis = if (bottomBarVisible) 240 else 100,
                            delayMillis = if (bottomBarVisible) 50 else 0,
                            easing = EmphasizedDecelerate
                        ),
                        label = "navBar"
                    )
                    if (barProgress > 0f) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .graphicsLayer {
                                    alpha = barProgress
                                    translationY = (1f - barProgress) * size.height
                                }
                        ) {
                            QcNavigationBar(
                                selectedRoute = route ?: ROUTE_CARDS,
                                onSelect = { target ->
                                    if (bottomBarVisible && target != route) {
                                        homeViewModel.clearSearch()
                                        navController.navigate(target) {
                                            popUpTo(navController.graph.findStartDestination().id) {
                                                saveState = true
                                            }
                                            launchSingleTop = true
                                            restoreState = true
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
        }
        BackupHost(state = backupHostState)
        fun promptUnlock() {
            val host = context as? FragmentActivity ?: return
            unlockScope.launch {
                if (UnlockSession.requireUnlock(host, "Unlock to see your cards")) {
                    AppLockController.unlock()
                }
            }
        }
        // Ask once per lock, as soon as the app is in the foreground; a cancel leaves
        // the lock screen with its Unlock button rather than re-prompting.
        var autoPrompted by remember { mutableStateOf(false) }
        androidx.compose.runtime.LaunchedEffect(locked) {
            if (!locked) {
                autoPrompted = false
                return@LaunchedEffect
            }
            if (autoPrompted) return@LaunchedEffect
            if (!AppLockController.autoPromptOnLock && com.quickcards.app.BuildConfig.DEBUG) return@LaunchedEffect
            lifecycleOwner.lifecycle.withResumed { }
            autoPrompted = true
            promptUnlock()
        }
        if (locked) {
            LockScreen(onUnlock = ::promptUnlock)
        }
        }
    }
}

@Composable
private fun QcNavigationRail(
    selectedRoute: String,
    onSelect: (String) -> Unit
) {
    Row(modifier = Modifier.fillMaxHeight()) {
        NavigationRail(
            containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
            modifier = Modifier
                .fillMaxHeight()
                .width(80.dp)
        ) {
            NavigationItems.forEach { item ->
                val selected = item.route == selectedRoute
                NavigationRailItem(
                    selected = selected,
                    onClick = { onSelect(item.route) },
                    icon = {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.icon,
                            contentDescription = null
                        )
                    },
                    label = {
                        Text(
                            text = item.label,
                            style = QcType.Label.copy(
                                fontWeight = if (selected) FontWeight(700) else FontWeight(500)
                            )
                        )
                    },
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
        Box(
            modifier = Modifier
                .width(1.dp)
                .fillMaxHeight()
                .background(MaterialTheme.colorScheme.outlineVariant)
        )
    }
}

@Composable
private fun QcNavigationBar(
    selectedRoute: String,
    onSelect: (String) -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerLow)) {
        HorizontalDivider(
            thickness = 1.dp,
            color = MaterialTheme.colorScheme.outlineVariant
        )
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
                    icon = {
                        Icon(
                            imageVector = if (selected) item.selectedIcon else item.icon,
                            contentDescription = null
                        )
                    },
                    label = {
                        Text(
                            text = item.label,
                            style = QcType.Label.copy(
                                fontWeight = if (selected) FontWeight(700) else FontWeight(500)
                            )
                        )
                    },
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
