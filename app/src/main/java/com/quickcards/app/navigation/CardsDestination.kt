package com.quickcards.app.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.withResumed
import com.quickcards.app.R
import com.quickcards.app.core.reminders.OpenCardRequest
import com.quickcards.app.core.security.AppLockController
import com.quickcards.app.core.ui.LocalLeaveGuard
import com.quickcards.app.core.ui.adaptive.ListDetailPanes
import com.quickcards.app.core.ui.adaptive.QcWindow
import com.quickcards.app.core.ui.adaptive.WidthClass
import com.quickcards.app.core.ui.components.LocalSnackbar
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.feature.cards.HomeUiState
import com.quickcards.app.feature.details.CardDetailsScreen
import com.quickcards.app.feature.details.DetailsLayout

/** Card and rows side by side only when the rows get at least the card's width. */
private val SplitDetailMin = QcDimens.HeroDesign * 2 + QcSpacing.s6 * 3

/** What the cards list and the details beside it need from the navigation graph. */
internal class CardsPaneActions(
    val home: @Composable (selectedId: String?, onOpen: (String) -> Unit) -> Unit,
    val onEdit: (cardId: String) -> Unit,
    val onActions: (cardId: String) -> Unit,
    val onStatus: (cardId: String) -> Unit
)

/**
 * Cards list | the open card. Without a card yet (loading, tips, empty vault) Home keeps the
 * window. A selected card that is gone once the list has loaded calls [onGone].
 */
@Composable
internal fun CardsWithDetail(
    window: QcWindow,
    home: HomeUiState,
    selectedId: String?,
    bottomInset: Dp,
    actions: CardsPaneActions,
    onOpen: (String) -> Unit,
    onGone: () -> Unit
) {
    val selected = home.allCards.firstOrNull { it.id == selectedId }
    val missing = selectedId != null && selected == null && !home.loading
    LaunchedEffect(missing) { if (missing) onGone() }
    val card = selected ?: home.visibleCards.firstOrNull().takeIf { selectedId == null }
    if (card == null || home.showTips) {
        actions.home(null, onOpen)
        return
    }
    ListDetailPanes(
        listWidth = if (window.width == WidthClass.EXPANDED) QcDimens.ListPane else QcDimens.ListPaneMedium,
        hinge = window.hinge,
        list = { actions.home(card.id, onOpen) },
        detail = { paneWidth ->
            CardDetailsScreen(
                card = card,
                loading = home.loading,
                lastUsedId = home.lastUsedId,
                bottomInset = bottomInset,
                onBack = onGone,
                onEdit = actions.onEdit,
                onActions = actions.onActions,
                onStatus = actions.onStatus,
                layout = if (paneWidth >= SplitDetailMin) DetailsLayout.SPLIT else DetailsLayout.STACKED,
                showBack = false,
                maskNumber = home.options.maskNumbers
            )
        }
    )
}

/**
 * Opens the card a reminder asked for once the app is unlocked, resumed and the list has loaded.
 * An open form with unsaved changes asks "Discard changes?" first; Keep editing drops the request.
 * Either way it goes to Home first ([onHome]); a card deleted since the reminder was posted then
 * gets "Card not found" instead of a details screen.
 */
@Composable
internal fun OpenCardRequestEffect(
    locked: Boolean,
    home: HomeUiState,
    onHome: () -> Unit,
    onOpen: (cardId: String) -> Unit
) {
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val leaveGuard = LocalLeaveGuard.current
    val requested by OpenCardRequest.cardId.collectAsState()
    LaunchedEffect(requested, locked, home.loading) {
        val cardId = requested ?: return@LaunchedEffect
        if (locked || home.loading) return@LaunchedEffect
        lifecycleOwner.lifecycle.withResumed { }
        if (AppLockController.isLocked.value) return@LaunchedEffect
        val leave = leaveGuard.confirmLeave()
        OpenCardRequest.consume(cardId)
        if (!leave) return@LaunchedEffect
        onHome()
        if (home.allCards.any { it.id == cardId }) {
            onOpen(cardId)
        } else {
            snackbar.show(context.getString(R.string.details_card_not_found))
        }
    }
}
