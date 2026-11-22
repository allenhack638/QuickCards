package com.quickcards.app.navigation

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.lifecycle.withResumed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
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
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
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
import com.quickcards.app.R
import com.quickcards.app.core.reminders.OpenCardRequest
import com.quickcards.app.core.security.UnlockSession
import com.quickcards.app.core.ui.LocalNavAnimatedScope
import com.quickcards.app.core.ui.LocalSharedTransitionScope
import com.quickcards.app.feature.archive.ArchivedCardsScreen
import com.quickcards.app.feature.backup.BackupHost
import com.quickcards.app.feature.backup.BackupHostState
import com.quickcards.app.feature.backup.rememberBackupHostState
import com.quickcards.app.core.ui.components.HelpTopic
import com.quickcards.app.feature.cardform.CardFormScreen
import com.quickcards.app.core.ui.components.LocalSnackbar
import com.quickcards.app.core.update.AppUpdatePrompts
import com.quickcards.app.core.update.AppUpdates
import com.quickcards.app.core.ui.components.QcSnackbarHost
import com.quickcards.app.core.ui.components.SnackbarController
import com.quickcards.app.feature.cards.HomeScreen
import com.quickcards.app.feature.details.CardActionsHost
import com.quickcards.app.feature.details.CardDetailsScreen
import com.quickcards.app.feature.details.rememberCardActionsState
import com.quickcards.app.feature.cards.HomeViewModel
import com.quickcards.app.core.security.AppLockController
import com.quickcards.app.feature.lock.LockScreen
import com.quickcards.app.feature.manage.ManageEditorScreen
import com.quickcards.app.feature.manage.ManageListScreen
import com.quickcards.app.feature.manage.ManageScreen
import com.quickcards.app.feature.manage.ManageViewModel
import com.quickcards.app.feature.manage.entityKindFromRoute
import com.quickcards.app.feature.manage.routeKey
import com.quickcards.app.feature.settings.PrivacyScreen
import com.quickcards.app.feature.settings.AppLockScreen
import com.quickcards.app.feature.settings.SettingsScreen
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcType
import androidx.annotation.StringRes
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavBackStackEntry
import androidx.navigation.compose.LocalOwnersProvider
import com.quickcards.app.core.ui.adaptive.FormPanel
import com.quickcards.app.core.ui.adaptive.ListDetailPanes
import com.quickcards.app.core.ui.adaptive.QcWindow
import com.quickcards.app.core.ui.adaptive.WidthClass
import com.quickcards.app.core.ui.adaptive.rememberHinge
import com.quickcards.app.feature.archive.ArchivedCardsViewModel
import com.quickcards.app.feature.details.DetailsLayout
import kotlinx.coroutines.launch

private const val ROUTE_CARDS = "cards"
private const val ROUTE_MANAGE = "manage"
private const val ROUTE_SETTINGS = "settings"
private const val ROUTE_CARD_ADD = "card/add"
private const val ROUTE_CARD_EDIT = "card/edit/{cardId}"
private const val ROUTE_CARD_DETAILS = "card/details/{cardId}"
private const val ROUTE_ARCHIVED = "archived"
private const val ROUTE_PRIVACY = "privacy"
private const val ROUTE_APP_LOCK = "settings/lock"
private const val ROUTE_MANAGE_EDIT = "manage/{kind}/edit?id={id}"

private val RootRoutes = setOf(ROUTE_CARDS, ROUTE_MANAGE, ROUTE_SETTINGS)

/** Card and rows side by side only when the rows get at least the card's width. */
private val SplitDetailMin = QcDimens.HeroDesign * 2 + QcSpacing.s6 * 3
private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

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

private fun isRoot(route: String?): Boolean = route in RootRoutes

private fun isForm(route: String?): Boolean = route == ROUTE_CARD_ADD || route == ROUTE_CARD_EDIT

@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun QcApp() {
    val navController = rememberNavController()
    val openDuplicate: (String) -> Unit = { cardId ->
        navController.popBackStack()
        navController.navigate("card/details/$cardId")
    }
    val homeViewModel: HomeViewModel = viewModel()
    val homeState by homeViewModel.uiState.collectAsState()
    val archivedViewModel: ArchivedCardsViewModel = viewModel()
    val archivedState by archivedViewModel.uiState.collectAsState()
    val openableCards = remember(homeState, archivedState) {
        (homeState.cards + homeState.visibleCards + archivedState.cards.map { it.card }).distinctBy { it.id }
    }
    var selectedCardId by rememberSaveable { mutableStateOf<String?>(null) }
    val formHolder = rememberSaveableStateHolder()
    val cardActions = rememberCardActionsState()
    val manageViewModel: ManageViewModel = viewModel()
    val homeFullWindow by homeViewModel.hidesNavBar.collectAsState()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val route = backStackEntry?.destination?.route

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

    val updateLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.StartIntentSenderForResult()
    ) { }
    AppUpdatePrompts(locked = locked, launcher = updateLauncher, snackbar = snackbarController)
    val updateState by AppUpdates.state.collectAsState()

    CompositionLocalProvider(
        LocalSnackbar provides snackbarController,
        com.quickcards.app.core.ui.LocalAppLocked provides locked
    ) {
        var autofillRequested by remember { mutableStateOf(false) }
        lateinit var backupHostState: BackupHostState
        val openSettingsRoot = {
            navController.navigate(ROUTE_SETTINGS) {
                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                launchSingleTop = true
                restoreState = true
            }
        }
        // One handler for the Help sheet, whether it opens from Settings or the spreadsheet import.
        val openHelpTopic: (HelpTopic) -> Unit = { topic ->
            when (topic) {
                HelpTopic.INSTANT_COPY -> navController.navigate(ROUTE_APP_LOCK)
                HelpTopic.AUTOFILL -> {
                    autofillRequested = true
                    if (route != ROUTE_SETTINGS) openSettingsRoot()
                }
                HelpTopic.SPREADSHEET -> backupHostState.requestImport()
                HelpTopic.CHECK -> {
                    homeViewModel.showNeedsCheck()
                    navController.navigate(ROUTE_CARDS) {
                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                        launchSingleTop = true
                    }
                }
            }
        }
        backupHostState = rememberBackupHostState(onHelpTopic = openHelpTopic)
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
        val requestedCard by OpenCardRequest.cardId.collectAsState()
        LaunchedEffect(requestedCard, locked) {
            val cardId = requestedCard ?: return@LaunchedEffect
            if (locked) return@LaunchedEffect
            lifecycleOwner.lifecycle.withResumed { }
            if (AppLockController.isLocked.value) return@LaunchedEffect
            OpenCardRequest.consume(cardId)
            navController.navigate(ROUTE_CARDS) {
                popUpTo(navController.graph.findStartDestination().id)
                launchSingleTop = true
            }
            if (window.twoPane) selectedCardId = cardId else navController.navigate("card/details/$cardId")
        }
        val useRail = window.rail
        val formOverlay = window.formDialog && isForm(route)
        val detailPane = window.detailBesideList && route == ROUTE_CARD_DETAILS
        val showBar = (isRoot(route) && !homeFullWindow) || (useRail && (formOverlay || detailPane))
        val bottomBarVisible = showBar && !useRail
        val contentBottomInset = if (bottomBarVisible) 0.dp else systemBarBottom
        Row(modifier = Modifier.fillMaxSize().then(if (formOverlay) Modifier.clearAndSetSemantics {} else Modifier)) {
            if (showBar && useRail) {
                QcNavigationRail(
                    selectedRoute = route?.takeIf(::isRoot) ?: ROUTE_CARDS,
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
                    QcSnackbarHost(
                        hostState = snackbarHostState,
                        modifier = if (bottomBarVisible) {
                            Modifier.padding(
                                bottom = QcDimens.NavBarHeight + QcDimens.Divider + systemBarBottom
                            )
                        } else {
                            Modifier.navigationBarsPadding()
                        }
                    )
                }
            ) { padding ->
                // Tabs reserve the bar height themselves so centred content does not jump
                // when a pushed screen hides the bar.
                val tabBarSpace = if (useRail || homeFullWindow) {
                    0.dp
                } else {
                    QcDimens.NavBarHeight + QcDimens.Divider + systemBarBottom
                }
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
                            .windowInsetsPadding(
                                WindowInsets.statusBars.only(WindowInsetsSides.Top)
                            )
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
                                fadeIn(tween(160, delayMillis = 50, easing = EmphasizedDecelerate))
                            } else {
                                fadeIn(tween(160, delayMillis = 50, easing = EmphasizedDecelerate)) +
                                    slideInHorizontally(
                                        animationSpec = tween(240, easing = EmphasizedDecelerate)
                                    ) { sharedAxisOffset }
                            }
                        },
                        exitTransition = {
                            if (window.formDialog && isForm(targetState.destination.route)) ExitTransition.None else fadeOut(tween(50))
                        },
                        popEnterTransition = {
                            if (window.formDialog && isForm(initialState.destination.route)) {
                                EnterTransition.None
                            } else if (window.detailBesideList && initialState.destination.route == ROUTE_CARD_DETAILS) {
                                fadeIn(tween(160, delayMillis = 50, easing = EmphasizedDecelerate))
                            } else {
                                fadeIn(tween(160, delayMillis = 50, easing = EmphasizedDecelerate)) +
                                    slideInHorizontally(
                                        animationSpec = tween(240, easing = EmphasizedDecelerate)
                                    ) { -sharedAxisOffset }
                            }
                        },
                        popExitTransition = {
                            if (window.formDialog && isForm(initialState.destination.route)) ExitTransition.None else fadeOut(tween(50))
                        }
                    ) {
                    val home: @Composable (selectedId: String?, onOpen: (String) -> Unit) -> Unit = { selectedId, onOpen ->
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
                    }
                    val openDetails: (String) -> Unit = { cardId -> navController.navigate("card/details/$cardId") }
                    val swapDetails: (String) -> Unit = { cardId ->
                        navController.navigate("card/details/$cardId") {
                            popUpTo(ROUTE_CARD_DETAILS) { inclusive = true }
                        }
                    }
                    // Cards list | the open card. Without a card yet (loading, tips, empty vault) Home keeps the window.
                    val cardsWithDetail: @Composable (selectedId: String?, onOpen: (String) -> Unit, onGone: () -> Unit) -> Unit =
                        { selectedId, onOpen, onGone ->
                            val card = openableCards.firstOrNull { it.id == selectedId }
                                ?: homeState.visibleCards.firstOrNull().takeIf { selectedId == null }
                            if (card == null || homeState.showTips) {
                                home(null, onOpen)
                            } else {
                                ListDetailPanes(
                                    listWidth = if (window.width == WidthClass.EXPANDED) QcDimens.ListPane else QcDimens.ListPaneMedium,
                                    hinge = window.hinge,
                                    list = { home(card.id, onOpen) },
                                    detail = { paneWidth ->
                                        CardDetailsScreen(
                                            card = card,
                                            bottomInset = contentBottomInset,
                                            onBack = onGone,
                                            onEdit = { id -> navController.navigate("card/edit/$id") },
                                            onActions = cardActions::open,
                                            layout = if (paneWidth >= SplitDetailMin) DetailsLayout.SPLIT else DetailsLayout.STACKED,
                                            showBack = false
                                        )
                                    }
                                )
                            }
                        }
                    val cardsRoot: @Composable () -> Unit = {
                        if (window.twoPane) {
                            cardsWithDetail(selectedCardId, { selectedCardId = it }, { selectedCardId = null })
                        } else {
                            home(null, openDetails)
                        }
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
                        val kind = entityKindFromRoute(entry.arguments?.getString("kind"))
                            ?: EntityKind.HOLDER
                        ManageEditorScreen(
                            kind = kind,
                            editingId = entry.arguments?.getString("id"),
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
                    val archived: @Composable () -> Unit = {
                        ArchivedCardsScreen(
                            onBack = { navController.popBackStack() },
                            onCardActions = cardActions::open,
                            bottomInset = contentBottomInset,
                            viewModel = archivedViewModel
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
                            cardsWithDetail(cardId, swapDetails) { navController.popBackStack() }
                        } else {
                            CardDetailsScreen(
                                card = openableCards.firstOrNull { it.id == cardId },
                                bottomInset = contentBottomInset,
                                onBack = { navController.popBackStack() },
                                onEdit = { id -> navController.navigate("card/edit/$id") },
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
        val formEntry = backStackEntry?.takeIf { formOverlay }
        if (formEntry != null) {
            FormPanel {
                formEntry.LocalOwnersProvider(formHolder) {
                    CardFormScreen(navController = navController, onOpenCard = openDuplicate, asDialog = true)
                }
            }
        }
        }
        BackupHost(state = backupHostState)
        CardActionsHost(
            state = cardActions,
            cards = openableCards,
            onEdit = { id -> navController.navigate("card/edit/$id") }
        )
        fun promptUnlock() {
            val host = context as? FragmentActivity ?: return
            unlockScope.launch {
                if (UnlockSession.requireUnlock(host)) {
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
                icon = {
                    Icon(
                        imageVector = if (selected) item.selectedIcon else item.icon,
                        contentDescription = null
                    )
                },
                label = {
                    Text(
                        text = stringResource(item.label),
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
                            text = stringResource(item.label),
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
