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
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.window.layout.FoldingFeature
import com.quickcards.app.R
import com.quickcards.app.core.data.repository.RepairSummary
import com.quickcards.app.core.ui.HomeCard
import com.quickcards.app.core.ui.art.ArtKind
import com.quickcards.app.core.ui.art.CardArt
import com.quickcards.app.core.ui.components.MarkFit
import com.quickcards.app.core.ui.components.NetworkMark
import com.quickcards.app.core.ui.components.NetworkMarks
import com.quickcards.app.core.ui.components.PassCard
import com.quickcards.app.core.ui.components.QcGroup
import com.quickcards.app.core.ui.components.QcGroupLabel
import com.quickcards.app.core.ui.components.markTone
import com.quickcards.app.core.ui.rememberSeparatingFold
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
    val copyEnabled: Boolean
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
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val fold = rememberSeparatingFold()
    var originInWindow by remember { mutableStateOf<Offset?>(null) }

    BoxWithConstraints(modifier = modifier.onGloballyPositioned { originInWindow = it.positionInWindow() }) {
        val contentWidthPx = constraints.maxWidth.toFloat()
        val origin = originInWindow
        val verticalFold = fold?.takeIf { it.orientation == FoldingFeature.Orientation.VERTICAL && it.isSeparating }
        val minPanePx = with(density) { QcDimens.CardMinPane.toPx() }
        val foldLeftPx = if (verticalFold != null && origin != null) verticalFold.bounds.left - origin.x else 0f
        val foldRightPx = if (verticalFold != null && origin != null) verticalFold.bounds.right - origin.x else 0f
        val splitGrid = verticalFold != null && origin != null &&
            foldLeftPx >= minPanePx && foldRightPx <= contentWidthPx - minPanePx

        if (splitGrid) {
            val half = (cards.size + 1) / 2
            Row(modifier = Modifier.fillMaxSize()) {
                val left = with(density) { foldLeftPx.toDp() }
                val right = with(density) { (contentWidthPx - foldRightPx).coerceAtLeast(0f).toDp() }
                Box(modifier = Modifier.width(left).fillMaxHeight()) {
                    CardGrid(cards.subList(0, half), tile, header, actions, columnsFor(left), bottomInset)
                }
                Spacer(modifier = Modifier.width(with(density) { (foldRightPx - foldLeftPx).coerceAtLeast(0f).toDp() }))
                Box(modifier = Modifier.width(right).fillMaxHeight()) {
                    CardGrid(cards.drop(half), tile, null, actions, columnsFor(right), bottomInset)
                }
            }
        } else {
            CardGrid(cards, tile, header, actions, columnsFor(configuration.screenWidthDp.dp), bottomInset)
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
    tile: TileState,
    header: ListHeader?,
    actions: TileActions,
    columns: Int,
    bottomInset: Dp
) {
    val gridState = rememberLazyGridState()
    val showsRecent = header?.recent?.isNotEmpty() == true
    // Items are keyed, so a new top item would otherwise open below the fold.
    var shownRecent by remember { mutableStateOf(showsRecent) }
    LaunchedEffect(showsRecent) {
        if (showsRecent != shownRecent) gridState.scrollToItem(0)
        shownRecent = showsRecent
    }
    LazyVerticalGrid(
        columns = GridCells.Fixed(columns),
        state = gridState,
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = QcSpacing.s4,
            end = QcSpacing.s4,
            top = if (header?.repair != null && !header.noLock) 0.dp else QcSpacing.s2,
            bottom = QcSpacing.s8 + bottomInset
        ),
        horizontalArrangement = Arrangement.spacedBy(QcSpacing.s3),
        verticalArrangement = Arrangement.spacedBy(QcSpacing.s3)
    ) {
        if (header != null) {
            if (header.noLock) {
                item(span = { GridItemSpan(maxLineSpan) }, contentType = "banner") {
                    NoLockBanner(onSettings = header.onScreenLock)
                }
            }
            header.repair?.let { summary ->
                item(span = { GridItemSpan(maxLineSpan) }, contentType = "banner") {
                    RepairBanner(
                        summary = summary,
                        onReview = header.onReviewRepair,
                        onDismiss = header.onDismissRepair,
                        modifier = Modifier.padding(bottom = QcSpacing.s2)
                    )
                }
            }
            if (header.recent.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }, contentType = "recent") {
                    RecentGroup(header.recent, tile, actions)
                }
            }
        }
        items(cards, key = { it.id }) { card ->
            Box(modifier = Modifier.fillMaxWidth().animateItem(), contentAlignment = Alignment.TopCenter) {
                HomeTile(
                    card = card,
                    tile = tile,
                    actions = actions,
                    modifier = if (columns > 1) Modifier.widthIn(max = QcDimens.CardMaxWidth) else Modifier
                )
            }
        }
    }
}

@Composable
internal fun HomeTile(card: HomeCard, tile: TileState, actions: TileActions, modifier: Modifier = Modifier) {
    val status = CardsListState.status(card, tile.lastUsedId)
    PassCard(
        card = card,
        modifier = modifier.sharedPass(card.id),
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
        MiniArt(card)
        Column(modifier = Modifier.weight(1f).clearAndSetSemantics { }) {
            Text(
                text = buildAnnotatedString {
                    append(card.bankName)
                    append(' ')
                    withStyle(SpanStyle(fontFamily = QcText.MonoDotsF3.fontFamily, letterSpacing = QcText.MonoDotsF3.letterSpacing)) {
                        append("••")
                    }
                    append(' ')
                    withStyle(SpanStyle(fontFamily = QcText.MonoF3.fontFamily, fontWeight = QcText.MonoF3.fontWeight, letterSpacing = QcText.MonoF3.letterSpacing)) {
                        append(last4)
                    }
                },
                style = QcText.F3,
                color = colors.ink,
                maxLines = 1
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

@Composable
private fun MiniArt(card: HomeCard) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val networkId = card.network?.id
    CardArt(band = card.card.bandColor, kind = ArtKind.MINI, expired = card.expiryState == ExpiryState.EXPIRED) {
        if (networkId != null && NetworkMarks.drawable(networkId) != null) {
            NetworkMark(
                networkId = networkId,
                contentDescription = null,
                tone = markTone(networkId, onArt = true, dark = dark),
                fit = MarkFit.MINI,
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(end = QcSpacing.s1, bottom = QcSpacing.s1 / 2)
            )
        }
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
