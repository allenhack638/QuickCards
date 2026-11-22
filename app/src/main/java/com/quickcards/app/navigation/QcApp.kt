package com.quickcards.app.navigation

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavType
import androidx.navigation.compose.LocalOwnersProvider
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.quickcards.app.core.security.AppLockController
import com.quickcards.app.core.ui.LocalAppLocked
import com.quickcards.app.core.ui.LocalNavAnimatedScope
import com.quickcards.app.core.ui.LocalSharedTransitionScope
import com.quickcards.app.core.ui.adaptive.FormPanel
import com.quickcards.app.core.ui.adaptive.QcWindow
import com.quickcards.app.core.ui.adaptive.rememberHinge
import com.quickcards.app.core.ui.collectWhileStarted
import com.quickcards.app.core.ui.components.HelpTopic
import com.quickcards.app.core.ui.components.LocalSnackbar
import com.quickcards.app.core.ui.components.QcSnackbarHost
import com.quickcards.app.core.ui.components.SnackbarController
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.update.AppUpdatePrompts
import com.quickcards.app.core.update.AppUpdates
import com.quickcards.app.feature.archive.ArchivedCardsScreen
import com.quickcards.app.feature.backup.BackupHost
import com.quickcards.app.feature.backup.rememberBackupHostState
import com.quickcards.app.feature.cardform.CardFormScreen
import com.quickcards.app.feature.cards.HomeScreen
import com.quickcards.app.feature.cards.HomeViewModel
import com.quickcards.app.feature.details.CardActionsHost
import com.quickcards.app.feature.details.CardDetailsScreen
import com.quickcards.app.feature.details.DetailsLayout
import com.quickcards.app.feature.details.rememberCardActionsState
import com.quickcards.app.feature.manage.ManageEditorScreen
import com.quickcards.app.feature.manage.ManageListScreen
import com.quickcards.app.feature.manage.ManageScreen
import com.quickcards.app.feature.manage.ManageViewModel
import com.quickcards.app.feature.manage.entityKindFromRoute
import com.quickcards.app.feature.manage.routeKey
import com.quickcards.app.feature.settings.AppLockScreen
import com.quickcards.app.feature.settings.PrivacyScreen
import com.quickcards.app.feature.settings.SettingsScreen
import com.quickcards.domain.CardFilters
import com.quickcards.domain.EntityKind

private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

/** The navigation graph, the scaffold with its bar or rail, and the hosts drawn over every screen. */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun QcApp() {
    val navController = rememberNavController()
    val openDuplicate: (String) -> Unit = { cardId ->
        navController.popBackStack()
        navController.navigate(detailsRoute(cardId))
    }
    val openEdit: (String) -> Unit = { id -> navController.navigate(editRoute(id)) }
    val homeViewModel: HomeViewModel = viewModel()
    val homeState by homeViewModel.uiState.collectWhileStarted()
    val homeFullWindow by homeViewModel.hidesNavBar.collectWhileStarted()
    var selectedCardId by rememberSaveable { mutableStateOf<String?>(null) }
    val formHolder = rememberSaveableStateHolder()
    val cardActions = rememberCardActionsState()
    val manageViewModel: ManageViewModel = viewModel()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route

    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    val snackbarController = remember(snackbarHostState, snackbarScope) {
        SnackbarController(snackbarHostState, snackbarScope)
    }
    val systemBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val locked by AppLockController.isLocked.collectAsState()
    val sharedAxisOffset = with(LocalDensity.current) { QcDimens.SharedAxisOffset.roundToPx() }

    val updateLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { }
    AppUpdatePrompts(locked = locked, launcher = updateLauncher, snackbar = snackbarController)
    val updateState by AppUpdates.state.collectAsState()

    CompositionLocalProvider(
        LocalSnackbar provides snackbarController,
        LocalAppLocked provides locked
    ) {
        var autofillRequested by remember { mutableStateOf(false) }
        val backupHostState = rememberBackupHostState()
        // One handler for the Help sheet, whether it opens from Settings or the spreadsheet import.
        val openHelpTopic: (HelpTopic) -> Unit = { topic ->
            when (topic) {
                HelpTopic.INSTANT_COPY -> navController.navigate(ROUTE_APP_LOCK)
                HelpTopic.AUTOFILL -> {
                    autofillRequested = true
                    if (route != ROUTE_SETTINGS) navController.openTab(ROUTE_SETTINGS)
                }
                HelpTopic.SPREADSHEET -> backupHostState.requestImport()
                HelpTopic.CHECK -> {
                    homeViewModel.showNeedsCheck()
                    navController.openTab(ROUTE_CARDS, save = true, restore = false)
                }
            }
        }
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
                val hinge = rememberHinge()
                val window = remember(maxWidth, maxHeight, hinge) { QcWindow.of(maxWidth, maxHeight, hinge) }
                OpenCardRequestEffect(
                    locked = locked,
                    home = homeState,
                    onHome = { navController.openTab(ROUTE_CARDS, save = false) },
                    onOpen = { cardId ->
                        if (window.twoPane) selectedCardId = cardId else navController.navigate(detailsRoute(cardId))
                    }
                )
                val useRail = window.rail
                val formOverlay = window.formDialog && isForm(route)
                val detailPane = window.detailBesideList && route == ROUTE_CARD_DETAILS
                val showBar = (isRoot(route) && !homeFullWindow) || (useRail && (formOverlay || detailPane))
                val bottomBarVisible = showBar && !useRail
                val contentBottomInset = if (bottomBarVisible) 0.dp else systemBarBottom
                val selectTab: (String) -> Unit = { target ->
                    if (target != route) {
                        homeViewModel.clearSearch()
                        navController.openTab(target)
                    }
                }
                Row(modifier = Modifier.fillMaxSize().then(if (formOverlay) Modifier.clearAndSetSemantics {} else Modifier)) {
                    if (showBar && useRail) {
                        QcNavigationRail(selectedRoute = route?.takeIf(::isRoot) ?: ROUTE_CARDS, onSelect = selectTab)
                    }
                    Scaffold(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight(),
                        containerColor = MaterialTheme.colorScheme.background,
                        contentWindowInsets = WindowInsets(0, 0, 0, 0),
                        snackbarHost = {
                            QcSnackbarHost(
                                hostState = snackbarHostState,
                                modifier = if (bottomBarVisible) {
                                    Modifier.padding(bottom = QcDimens.NavBarHeight + QcDimens.Divider + systemBarBottom)
                                } else {
                                    Modifier.navigationBarsPadding()
                                }
                            )
                        }
                    ) { padding ->
                        // Tabs reserve the bar height themselves so centred content does not jump
                        // when a pushed screen hides the bar.
                        val tabBarSpace = if (useRail || homeFullWindow) 0.dp else QcDimens.NavBarHeight + QcDimens.Divider + systemBarBottom
                        val tabBottomInset = if (useRail || homeFullWindow) systemBarBottom else 0.dp
                        val tabModifier = Modifier.padding(bottom = tabBarSpace)
                        Box(modifier = Modifier.fillMaxSize()) {
                            SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
                                CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                                    NavHost(
                                        navController = navController,
                                        startDestination = ROUTE_CARDS,
                                        modifier = Modifier
                                            .padding(top = padding.calculateTopPadding())
                                            .windowInsetsPadding(WindowInsets.statusBars.only(WindowInsetsSides.Top))
                                            .clipToBounds(),
                                        contentAlignment = Alignment.TopStart,
                                        // Material fade-through: the old screen is gone (90 ms) before the new one
                                        // fades in, so text from two screens never overlaps.
                                        enterTransition = {
                                            if (window.formDialog && isForm(targetState.destination.route)) {
                                                EnterTransition.None
                                            } else if (
                                                (isRoot(initialState.destination.route) && isRoot(targetState.destination.route)) ||
                                                (window.detailBesideList && targetState.destination.route == ROUTE_CARD_DETAILS)
                                            ) {
                                                fadeThrough()
                                            } else {
                                                fadeThrough() + slideInHorizontally(tween(240, easing = EmphasizedDecelerate)) { sharedAxisOffset }
                                            }
                                        },
                                        exitTransition = {
                                            if (window.formDialog && isForm(targetState.destination.route)) ExitTransition.None else fadeOut(tween(50))
                                        },
                                        popEnterTransition = {
                                            if (window.formDialog && isForm(initialState.destination.route)) {
                                                EnterTransition.None
                                            } else if (window.detailBesideList && initialState.destination.route == ROUTE_CARD_DETAILS) {
                                                fadeThrough()
                                            } else {
                                                fadeThrough() + slideInHorizontally(tween(240, easing = EmphasizedDecelerate)) { -sharedAxisOffset }
                                            }
                                        },
                                        popExitTransition = {
                                            if (window.formDialog && isForm(initialState.destination.route)) ExitTransition.None else fadeOut(tween(50))
                                        }
                                    ) {
                                        val paneActions = CardsPaneActions(
                                            home = { selectedId, onOpen ->
                                                HomeScreen(
                                                    navController = navController,
                                                    viewModel = homeViewModel,
                                                    bottomInset = tabBottomInset,
                                                    onImport = { backupHostState.requestImport() },
                                                    onOpenArchived = { navController.navigate(ROUTE_ARCHIVED) },
                                                    onOpenCard = onOpen,
                                                    onCardActions = cardActions::open,
                                                    selectedId = selectedId,
                                                    chipsInBar = window.compactHeight
                                                )
                                            },
                                            onEdit = openEdit,
                                            onActions = cardActions::open
                                        )
                                        val cardsWithDetail: @Composable (String?, (String) -> Unit, () -> Unit) -> Unit = { selectedId, onOpen, onGone ->
                                            CardsWithDetail(window, homeState, selectedId, contentBottomInset, paneActions, onOpen, onGone)
                                        }
                                        val cardsRoot: @Composable () -> Unit = {
                                            if (window.twoPane) {
                                                cardsWithDetail(selectedCardId, { selectedCardId = it }, { selectedCardId = null })
                                            } else {
                                                paneActions.home(null) { cardId -> navController.navigate(detailsRoute(cardId)) }
                                            }
                                        }
                                        val archived: @Composable () -> Unit = {
                                            ArchivedCardsScreen(
                                                onBack = { navController.popBackStack() },
                                                onCardActions = cardActions::open,
                                                bottomInset = contentBottomInset
                                            )
                                        }
                                        composable(ROUTE_CARDS) {
                                            CompositionLocalProvider(LocalNavAnimatedScope provides this) {
                                                Box(modifier = tabModifier) { cardsRoot() }
                                            }
                                        }
                                        composable(ROUTE_MANAGE) {
                                            Box(modifier = tabModifier) {
                                                ManageScreen(
                                                    viewModel = manageViewModel,
                                                    bottomInset = tabBottomInset,
                                                    onOpenKind = { kind -> navController.navigate("manage/${kind.routeKey}") },
                                                    onOpenArchived = { navController.navigate(ROUTE_ARCHIVED) }
                                                )
                                            }
                                        }
                                        composable(
                                            route = ROUTE_MANAGE_LIST,
                                            arguments = listOf(navArgument("kind") { type = NavType.StringType })
                                        ) { entry ->
                                            val kind = entityKindFromRoute(entry.arguments?.getString("kind")) ?: EntityKind.HOLDER
                                            ManageListScreen(
                                                kind = kind,
                                                viewModel = manageViewModel,
                                                onBack = { navController.popBackStack() },
                                                onAdd = { navController.navigate("manage/${kind.routeKey}/edit") },
                                                onEdit = { id -> navController.navigate("manage/${kind.routeKey}/edit?id=$id") }
                                            )
                                        }
                                        composable(
                                            route = ROUTE_MANAGE_EDIT,
                                            arguments = listOf(
                                                navArgument("kind") { type = NavType.StringType },
                                                navArgument("id") {
                                                    type = NavType.StringType
                                                    nullable = true
                                                }
                                            )
                                        ) { entry ->
                                            val kind = entityKindFromRoute(entry.arguments?.getString("kind")) ?: EntityKind.HOLDER
                                            ManageEditorScreen(
                                                kind = kind,
                                                editingId = entry.arguments?.getString("id"),
                                                viewModel = manageViewModel,
                                                onBack = { navController.popBackStack() },
                                                onShowCards = { listKind, id ->
                                                    homeViewModel.applyFilters(filtersFor(listKind, id))
                                                    navController.openTab(ROUTE_CARDS, save = false)
                                                }
                                            )
                                        }
                                        composable(ROUTE_ARCHIVED) { archived() }
                                        composable(ROUTE_APP_LOCK) {
                                            AppLockScreen(onBack = { navController.popBackStack() })
                                        }
                                        composable(ROUTE_PRIVACY) {
                                            PrivacyScreen(onBack = { navController.popBackStack() })
                                        }
                                        composable(ROUTE_SETTINGS) {
                                            Box(modifier = tabModifier) {
                                                SettingsScreen(
                                                    bottomInset = tabBottomInset,
                                                    onExport = backupHostState::requestExport,
                                                    onImport = backupHostState::requestImport,
                                                    onErase = backupHostState::requestErase,
                                                    onPrivacy = { navController.navigate(ROUTE_PRIVACY) },
                                                    onAppLock = { navController.navigate(ROUTE_APP_LOCK) },
                                                    onHelpTopic = openHelpTopic,
                                                    showAutofill = autofillRequested,
                                                    onAutofillShown = { autofillRequested = false },
                                                    updateState = updateState,
                                                    onUpdate = { AppUpdates.startFromSettings(updateLauncher) },
                                                    onCompleteUpdate = AppUpdates::completeUpdate,
                                                    twoPane = window.settingsTwoPane
                                                )
                                            }
                                        }
                                        composable(
                                            route = ROUTE_CARD_DETAILS,
                                            arguments = listOf(navArgument("cardId") { type = NavType.StringType })
                                        ) { entry ->
                                            val cardId = entry.arguments?.getString("cardId")
                                            if (window.detailBesideList) {
                                                LaunchedEffect(cardId) { if (window.twoPane) selectedCardId = cardId }
                                                val swapDetails: (String) -> Unit = { id ->
                                                    navController.navigate(detailsRoute(id)) {
                                                        popUpTo(ROUTE_CARD_DETAILS) { inclusive = true }
                                                    }
                                                }
                                                cardsWithDetail(cardId, swapDetails) { navController.popBackStack() }
                                            } else {
                                                CardDetailsScreen(
                                                    card = homeState.allCards.firstOrNull { it.id == cardId },
                                                    loading = homeState.loading,
                                                    lastUsedId = homeState.lastUsedId,
                                                    bottomInset = contentBottomInset,
                                                    onBack = { navController.popBackStack() },
                                                    onEdit = openEdit,
                                                    onActions = cardActions::open,
                                                    layout = if (window.sideBySideDetails) DetailsLayout.SIDE else DetailsLayout.STACKED
                                                )
                                            }
                                        }
                                        // On medium and expanded windows the form is a dialog (drawn below, over the rail);
                                        // the destination then keeps the screen it opened from underneath.
                                        val formUnderlay: @Composable (NavBackStackEntry) -> Unit = { entry ->
                                            val below = remember(entry) { navController.previousBackStackEntry }
                                            Box(modifier = Modifier.fillMaxSize().clearAndSetSemantics {}) {
                                                when (below?.destination?.route) {
                                                    ROUTE_ARCHIVED -> archived()
                                                    ROUTE_CARD_DETAILS -> cardsWithDetail(below.arguments?.getString("cardId"), {}, {})
                                                    else -> cardsRoot()
                                                }
                                            }
                                        }
                                        composable(ROUTE_CARD_ADD) { entry ->
                                            if (window.formDialog) {
                                                formUnderlay(entry)
                                            } else {
                                                CardFormScreen(navController = navController, onOpenCard = openDuplicate)
                                            }
                                        }
                                        composable(
                                            route = ROUTE_CARD_EDIT,
                                            arguments = listOf(navArgument("cardId") { type = NavType.StringType })
                                        ) { entry ->
                                            if (window.formDialog) {
                                                formUnderlay(entry)
                                            } else {
                                                CompositionLocalProvider(LocalNavAnimatedScope provides this) {
                                                    CardFormScreen(navController = navController, onOpenCard = openDuplicate)
                                                }
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
                                        onSelect = { target -> if (bottomBarVisible) selectTab(target) }
                                    )
                                }
                            }
                        }
                    }
                }
                val formEntry = backStackEntry?.takeIf { formOverlay }
                if (formEntry != null) {
                    FormPanel {
                        formEntry.LocalOwnersProvider(formHolder) {
                            CardFormScreen(navController = navController, onOpenCard = openDuplicate, asDialog = true)
                        }
                    }
                }
            }
            BackupHost(state = backupHostState, onHelpTopic = openHelpTopic)
            CardActionsHost(state = cardActions, cards = homeState.allCards, onEdit = openEdit)
            AppLockHost(locked = locked)
        }
    }
}

private fun fadeThrough() = fadeIn(tween(160, delayMillis = 50, easing = EmphasizedDecelerate))

private fun filtersFor(kind: EntityKind, id: String): CardFilters = when (kind) {
    EntityKind.HOLDER -> CardFilters(holders = setOf(id))
    EntityKind.BANK -> CardFilters(banks = setOf(id))
    EntityKind.NETWORK -> CardFilters(networks = setOf(id))
    EntityKind.TYPE -> CardFilters(types = setOf(id))
    EntityKind.TAG -> CardFilters(tags = setOf(id))
}
