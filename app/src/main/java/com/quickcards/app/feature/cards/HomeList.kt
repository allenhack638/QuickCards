package com.quickcards.app.feature.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.layout.FoldingFeature
import com.quickcards.app.R
import com.quickcards.app.core.data.repository.RepairSummary
import com.quickcards.app.core.ui.HomeCard
import com.quickcards.app.core.ui.components.PassCard
import com.quickcards.app.core.ui.components.PassField
import com.quickcards.app.core.ui.rememberSeparatingFold
import com.quickcards.app.core.ui.sharedPass
import com.quickcards.app.core.ui.theme.QcDimens

@Composable
internal fun CardList(
    cards: List<HomeCard>,
    options: HomeOptions,
    repairSummary: RepairSummary?,
    bottomInset: Dp,
    modifier: Modifier,
    onCopy: suspend (HomeCard, PassField) -> Boolean,
    onReveal: suspend (PassField) -> Boolean,
    onCopyNote: suspend (HomeCard) -> Boolean,
    onReviewRepair: () -> Unit,
    onDismissRepair: () -> Unit,
    onActions: (HomeCard) -> Unit
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val fold = rememberSeparatingFold()
    var originInWindow by remember { mutableStateOf<Offset?>(null) }

    BoxWithConstraints(
        modifier = modifier.onGloballyPositioned {
            originInWindow = it.positionInWindow()
        }
    ) {
        val contentWidthPx = constraints.maxWidth.toFloat()
        val origin = originInWindow
        val verticalFold = fold?.takeIf {
            it.orientation == FoldingFeature.Orientation.VERTICAL && it.isSeparating
        }
        val minPanePx = with(density) { QcDimens.CardMinPane.toPx() }
        val foldLeftPx = if (verticalFold != null && origin != null) {
            verticalFold.bounds.left - origin.x
        } else {
            0f
        }
        val foldRightPx = if (verticalFold != null && origin != null) {
            verticalFold.bounds.right - origin.x
        } else {
            0f
        }
        val splitGrid = verticalFold != null && origin != null &&
            foldLeftPx >= minPanePx && foldRightPx <= contentWidthPx - minPanePx

        if (splitGrid) {
            val half = (cards.size + 1) / 2
            Row(modifier = Modifier.fillMaxSize()) {
                Box(
                    modifier = Modifier
                        .width(with(density) { foldLeftPx.toDp() })
                        .fillMaxHeight()
                ) {
                    CardGrid(
                        cards = cards.subList(0, half),
                        options = options,
                        repairSummary = repairSummary,
                        columns = columnsFor(with(density) { foldLeftPx.toDp() }),
                        bottomInset = bottomInset,
                        onCopy = onCopy,
                        onReveal = onReveal,
                        onCopyNote = onCopyNote,
                        onReviewRepair = onReviewRepair,
                        onDismissRepair = onDismissRepair,
                        onActions = onActions
                    )
                }
                Spacer(
                    modifier = Modifier.width(
                        with(density) { (foldRightPx - foldLeftPx).coerceAtLeast(0f).toDp() }
                    )
                )
                Box(
                    modifier = Modifier
                        .width(
                            with(density) {
                                (contentWidthPx - foldRightPx).coerceAtLeast(0f).toDp()
                            }
                        )
                        .fillMaxHeight()
                ) {
                    CardGrid(
                        cards = cards.drop(half),
                        options = options,
                        repairSummary = null,
                        columns = columnsFor(with(density) { (contentWidthPx - foldRightPx).toDp() }),
                        bottomInset = bottomInset,
                        onCopy = onCopy,
                        onReveal = onReveal,
                        onCopyNote = onCopyNote,
                        onReviewRepair = onReviewRepair,
                        onDismissRepair = onDismissRepair,
                        onActions = onActions
                    )
                }
            }
        } else {
            val columns = when {
                configuration.screenWidthDp.dp >= QcDimens.GridBreakpointWide -> 3
                configuration.screenWidthDp.dp >= QcDimens.GridBreakpoint -> 2
                else -> 1
            }
            CardGrid(
                cards = cards,
                options = options,
                repairSummary = repairSummary,
                columns = columns,
                bottomInset = bottomInset,
                onCopy = onCopy,
                onReveal = onReveal,
                onCopyNote = onCopyNote,
                onReviewRepair = onReviewRepair,
                onDismissRepair = onDismissRepair,
                onActions = onActions
            )
        }
    }
}

private fun columnsFor(paneWidth: Dp): Int = when {
    paneWidth >= QcDimens.GridBreakpointWide -> 3
    paneWidth >= QcDimens.GridBreakpoint -> 2
    else -> 1
}

@Composable
private fun CardGrid(
    cards: List<HomeCard>,
    options: HomeOptions,
    repairSummary: RepairSummary?,
    columns: Int,
    bottomInset: Dp,
    onCopy: suspend (HomeCard, PassField) -> Boolean,
    onReveal: suspend (PassField) -> Boolean,
    onCopyNote: suspend (HomeCard) -> Boolean,
    onReviewRepair: () -> Unit,
    onDismissRepair: () -> Unit,
    onActions: (HomeCard) -> Unit
) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = QcDimens.Gutter,
            end = QcDimens.Gutter,
            bottom = QcDimens.Section + bottomInset
        ),
        horizontalArrangement = Arrangement.spacedBy(QcDimens.ListPassGap),
        verticalArrangement = Arrangement.spacedBy(QcDimens.ListPassGap)
    ) {
        if (repairSummary != null) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                RepairBanner(
                    summary = repairSummary,
                    onReview = onReviewRepair,
                    onDismiss = onDismissRepair
                )
            }
        }
        items(cards, key = { it.id }) { card ->
            Box(
                modifier = Modifier.fillMaxWidth().animateItem(),
                contentAlignment = Alignment.TopCenter
            ) {
                PassCard(
                    card = card,
                    maskNumbers = options.maskNumbers,
                    showCardType = options.showCardType,
                    tagsShow = options.tagsShow,
                    modifier = if (columns > 1) {
                        Modifier.widthIn(max = QcDimens.CardMaxWidth)
                    } else {
                        Modifier
                    }.sharedPass(card.id),
                    expiryState = card.expiryState,
                    needsReview = card.card.needsReview.isNotEmpty(),
                    archivedLabel = archivedLabel(card),
                    note = card.card.note,
                    showNote = options.showNotes,
                    onCopy = { field -> onCopy(card, field) },
                    onReveal = onReveal,
                    onCopyNote = { onCopyNote(card) },
                    onActions = { onActions(card) }
                )
            }
        }
    }
}

@Composable
private fun archivedLabel(card: HomeCard): String? {
    if (!card.card.archived) return null
    val reason = card.card.archiveReason
    return if (reason.isNullOrBlank()) {
        stringResource(R.string.cards_status_archived)
    } else {
        stringResource(R.string.cards_status_archived_reason, reason)
    }
}
