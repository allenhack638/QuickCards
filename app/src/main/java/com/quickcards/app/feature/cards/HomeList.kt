package com.quickcards.app.feature.cards

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.staggeredgrid.LazyVerticalStaggeredGrid
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridCells
import androidx.compose.foundation.lazy.staggeredgrid.StaggeredGridItemSpan
import androidx.compose.foundation.lazy.staggeredgrid.items
import androidx.compose.foundation.lazy.staggeredgrid.rememberLazyStaggeredGridState
import androidx.compose.ui.semantics.selected
import com.quickcards.app.core.ui.adaptive.selectionRing
import com.quickcards.app.core.ui.art.ArtDimens
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quickcards.app.R
import com.quickcards.app.core.data.repository.RepairSummary
import com.quickcards.app.core.ui.HomeCard
import com.quickcards.app.core.ui.components.MASK_DOTS
import com.quickcards.app.core.ui.components.MiniCard
import com.quickcards.app.core.ui.components.PassCard
import com.quickcards.app.core.ui.components.maskedNumberText
import androidx.compose.ui.text.style.TextOverflow
import com.quickcards.app.core.ui.components.QcGroup
import com.quickcards.app.core.ui.components.QcGroupLabel
import com.quickcards.app.core.ui.components.QcSkeleton
import com.quickcards.app.core.ui.sharedPass
import com.quickcards.app.core.ui.theme.PassColors
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcMotion
import com.quickcards.app.core.ui.theme.QcShapes
import com.quickcards.app.core.ui.theme.QcSize
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTheme
import com.quickcards.domain.ExpiryState

/** What every tile and Recent row can do; the screen owns the behaviour. */
internal class TileActions(
    val open: (HomeCard) -> Unit,
    val actions: (HomeCard) -> Unit,
    val copy: (HomeCard) -> Unit,
    val copyNote: (HomeCard) -> Unit,
    val markPaid: (HomeCard) -> Unit
)

/** The frame-wide tile state: which card shows Copied and whether copying can work at all. */
internal data class TileState(
    val options: HomeOptions,
    val lastUsedId: String?,
    val copiedId: String?,
    val copyEnabled: Boolean,
    val selectedId: String? = null
)

/** Banners and Recent sit above the tiles, spanning every lane. */
internal class ListHeader(
    val noLock: Boolean,
    val repair: RepairSummary?,
    val recent: List<HomeCard>,
    val onScreenLock: () -> Unit,
    val onReviewRepair: () -> Unit,
    val onDismissRepair: () -> Unit
)

@Composable
internal fun CardList(
    cards: List<HomeCard>,
    tile: TileState,
    header: ListHeader,
    actions: TileActions,
    bottomInset: Dp,
    modifier: Modifier
) {
    BoxWithConstraints(modifier = modifier) {
        CardGrid(cards, tile, header, actions, lanes = if (maxWidth >= QcDimens.MediumWidth) 2 else 1, bottomInset)
    }
}

/** One column, or two staggered lanes from 600 wide: each tile goes to the shorter lane, list order kept. */
@Composable
private fun CardGrid(
    cards: List<HomeCard>,
    tile: TileState,
    header: ListHeader,
    actions: TileActions,
    lanes: Int,
    bottomInset: Dp
) {
    val gridState = rememberLazyStaggeredGridState()
    val showsRecent = header.recent.isNotEmpty()
    // Items are keyed, so a new top item would otherwise open below the fold.
    var shownRecent by remember { mutableStateOf(showsRecent) }
    LaunchedEffect(showsRecent) {
        if (showsRecent != shownRecent) gridState.scrollToItem(0)
        shownRecent = showsRecent
    }
    LazyVerticalStaggeredGrid(
        columns = StaggeredGridCells.Fixed(lanes),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = QcSpacing.s4,
            end = QcSpacing.s4,
            top = if (header.repair != null && !header.noLock) 0.dp else QcSpacing.s2,
            bottom = QcSpacing.s8 + bottomInset
        ),
        horizontalArrangement = Arrangement.spacedBy(QcSpacing.s3),
        verticalItemSpacing = QcSpacing.s3
    ) {
        if (header.noLock) {
            item(span = StaggeredGridItemSpan.FullLine, contentType = "banner") {
                NoLockBanner(onSettings = header.onScreenLock)
            }
        }
        header.repair?.let { summary ->
            item(span = StaggeredGridItemSpan.FullLine, contentType = "banner") {
                RepairBanner(
                    summary = summary,
                    onReview = header.onReviewRepair,
                    onDismiss = header.onDismissRepair,
                    modifier = Modifier.padding(bottom = QcSpacing.s2)
                )
            }
        }
        if (header.recent.isNotEmpty()) {
            item(span = StaggeredGridItemSpan.FullLine, contentType = "recent") {
                RecentGroup(header.recent, tile, actions)
            }
        }
        items(cards, key = { it.id }) { card ->
            Box(modifier = Modifier.fillMaxWidth().animateItem(), contentAlignment = Alignment.TopCenter) {
                HomeTile(
                    card = card,
                    tile = tile,
                    actions = actions,
                    modifier = if (lanes > 1) Modifier.widthIn(max = QcDimens.CardMaxWidth) else Modifier
                )
            }
        }
    }
}

@Composable
internal fun HomeTile(card: HomeCard, tile: TileState, actions: TileActions, modifier: Modifier = Modifier) {
    val status = CardsListState.status(card, tile.lastUsedId)
    val selected = tile.selectedId == card.id
    PassCard(
        card = card,
        modifier = modifier
            .selectionRing(selected, ArtDimens.CornerTile, QcTheme.colors.ink)
            .semantics { this.selected = selected }
            .sharedPass(card.id),
        status = status,
        archiveReason = card.card.archiveReason,
        note = card.card.note.takeIf { tile.options.showNotes && it.isNotBlank() },
        tags = if (tile.options.tagsShow) card.tagNames else emptyList(),
        dueText = card.dueInDays?.let { dueText(it) },
        copied = tile.copiedId == card.id,
        copyEnabled = tile.copyEnabled,
        onOpen = { actions.open(card) },
        onActions = { actions.actions(card) },
        onCopy = { actions.copy(card) },
        onCopyNote = { actions.copyNote(card) },
        onMarkPaid = { actions.markPaid(card) }
    )
}

@Composable
private fun dueText(days: Int): String = when (days) {
    0 -> stringResource(R.string.cards_due_today)
    1 -> stringResource(R.string.cards_due_tomorrow)
    else -> pluralStringResource(R.plurals.cards_due_in_days, days, days)
}

/** Search opens on the last three copied cards: a row opens the card, the disc copies its number. */
@Composable
private fun RecentGroup(cards: List<HomeCard>, tile: TileState, actions: TileActions) {
    Column(modifier = Modifier.padding(bottom = QcSpacing.s2)) {
        QcGroupLabel(text = stringResource(R.string.cards_recent))
        QcGroup {
            cards.forEach { card ->
                RecentRow(card, copied = tile.copiedId == card.id, enabled = tile.copyEnabled, actions = actions)
            }
        }
    }
}

@Composable
private fun RecentRow(card: HomeCard, copied: Boolean, enabled: Boolean, actions: TileActions) {
    val colors = QcTheme.colors
    val last4 = card.card.number.takeLast(4)
    val hue = remember(card.card.bandColor) { PassColors.hue(card.card.bandColor) }
    val openLabel = stringResource(R.string.cards_a11y_tile, card.bankName, last4)
    val meta = listOfNotNull(card.typeName?.takeIf { it.isNotBlank() }, card.holderName.takeIf { it.isNotBlank() })
        .joinToString(" · ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = QcSize.row2)
            .clickable(role = Role.Button) { actions.open(card) }
            .semantics(mergeDescendants = false) { contentDescription = openLabel }
            .padding(start = QcSpacing.s4, end = QcSpacing.s2, top = QcSpacing.s2, bottom = QcSpacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(QcSpacing.s3)
    ) {
        MiniCard(
            band = card.card.bandColor,
            networkId = card.network?.id,
            expired = card.expiryState == ExpiryState.EXPIRED
        )
        Column(modifier = Modifier.weight(1f).clearAndSetSemantics { }) {
            Text(
                text = maskedNumberText("${card.bankName} $MASK_DOTS $last4"),
                style = QcText.F3,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (meta.isNotEmpty()) {
                Text(text = meta, style = QcText.F2, color = colors.ink2, maxLines = 1)
            }
        }
        if (copied) {
            Text(text = stringResource(R.string.cards_pass_copied), style = QcText.F1, color = hue.c2)
        }
        CopyDisc(card, last4, copied, enabled, hue.c2) { actions.copy(card) }
    }
}

/** A 36 tonal disc in a 48 hit; copied, it fills with the card's colour and shows a check. */
@Composable
private fun CopyDisc(card: HomeCard, last4: String, copied: Boolean, enabled: Boolean, tint: Color, onCopy: () -> Unit) {
    val colors = QcTheme.colors
    val fill by animateColorAsState(if (copied) tint else colors.tonal, tween(QcMotion.STATE_MS))
    val ink by animateColorAsState(
        when {
            copied -> PassColors.OnArt
            enabled -> colors.ink
            else -> colors.ink3
        },
        tween(QcMotion.STATE_MS)
    )
    val label = if (copied) {
        stringResource(R.string.cards_a11y_number_copied, card.bankName)
    } else {
        stringResource(R.string.cards_a11y_copy_number, card.bankName, last4)
    }
    Box(
        modifier = Modifier
            .size(QcSize.hit)
            .clickable(enabled = enabled, interactionSource = null, indication = null, onClick = onCopy)
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
                if (enabled) onClick { onCopy(); true } else disabled()
            },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(QcSize.ctlMd)
                .background(fill, QcShapes.full),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = if (copied) QcIcons.check else QcIcons.copyOutlined,
                contentDescription = null,
                tint = ink,
                modifier = Modifier.size(QcSize.iconSm)
            )
        }
    }
}

/** Cold start: three static tonal tiles in the tile shape until the vault has loaded. */
@Composable
internal fun CardListSkeleton(modifier: Modifier) {
    Column(
        modifier = modifier
            .padding(start = QcSpacing.s4, end = QcSpacing.s4, top = QcSpacing.s2)
            .clearAndSetSemantics { },
        verticalArrangement = Arrangement.spacedBy(QcSpacing.s3)
    ) {
        repeat(SKELETON_TILES) {
            QcSkeleton(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(QcDimens.SkeletonTile),
                shape = QcShapes.lg
            )
        }
    }
}

private const val SKELETON_TILES = 3
