package com.quickcards.app.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.disabled
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.em
import com.quickcards.app.R
import com.quickcards.app.core.ui.HomeCard
import com.quickcards.app.core.ui.art.ArtKind
import com.quickcards.app.core.ui.art.CardArt
import com.quickcards.app.core.ui.theme.PassColors
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcMotion
import com.quickcards.app.core.ui.theme.QcShapes
import com.quickcards.app.core.ui.theme.QcSize
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.domain.CardStatus
import com.quickcards.domain.ExpiryState

/**
 * The Home tile: card art with bank (+ pin), "Type · Holder" and the one status chip, an
 * optional two-line note and tags, then "•• last4  MM/YY" with the Copy pill on its centre line
 * and an optional bill-due strip; with [maskNumber] off the full grouped number replaces "•• last4".
 * The body opens the card ([onOpen]) and a hold opens its actions ([onActions]); Copy, the note
 * and Mark paid sit above the body as their own targets.
 * Null callbacks make a static illustration.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun PassCard(
    card: HomeCard,
    modifier: Modifier = Modifier,
    maskNumber: Boolean = true,
    status: CardStatus? = null,
    archiveReason: String? = null,
    inArchive: Boolean = false,
    note: String? = null,
    tags: List<String> = emptyList(),
    dueText: String? = null,
    copied: Boolean = false,
    copyEnabled: Boolean = true,
    onOpen: (() -> Unit)? = null,
    onActions: (() -> Unit)? = null,
    onCopy: (() -> Unit)? = null,
    onCopyNote: (() -> Unit)? = null,
    onMarkPaid: (() -> Unit)? = null
) {
    val expired = card.expiryState == ExpiryState.EXPIRED
    val last4 = card.card.number.takeLast(4)
    val hue = remember(card.card.bandColor, expired) { PassColors.hue(card.card.bandColor, expired) }
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val openLabel = buildString {
        append(
            if (status == null) {
                stringResource(R.string.cards_a11y_tile, card.bankName, last4)
            } else {
                stringResource(R.string.cards_a11y_tile_status, card.bankName, last4, spokenStatus(status, archiveReason))
            }
        )
        if (card.card.pinned && !card.card.archived) append(", ").append(stringResource(R.string.cards_pass_pinned))
        if (dueText != null) append(", ").append(dueText)
        if (onActions != null) append(". ").append(stringResource(R.string.cards_a11y_hold_hint))
    }
    val actionsLabel = stringResource(R.string.cards_a11y_card_actions)

    CardArt(band = card.card.bandColor, modifier = modifier.fillMaxWidth(), kind = ArtKind.TILE, expired = expired) {
        if (onOpen != null) {
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .outset(ArtPad)
                    .combinedClickable(
                        interactionSource = interaction,
                        indication = null,
                        onLongClickLabel = actionsLabel,
                        onClick = onOpen,
                        onLongClick = onActions
                    )
                    .background(if (pressed) PassColors.OnArtPressed else Color.Transparent)
                    .clearAndSetSemantics {
                        contentDescription = openLabel
                        role = Role.Button
                        onClick { onOpen(); true }
                        if (onActions != null) {
                            onLongClick(actionsLabel) { onActions(); true }
                            customActions = listOf(CustomAccessibilityAction(actionsLabel) { onActions(); true })
                        }
                    }
            )
        }
        Column(modifier = Modifier.fillMaxWidth()) {
            PassHead(card = card, status = status, archiveReason = archiveReason, inArchive = inArchive)
            if (note != null) {
                PassNote(note = note, onOpen = onOpen, onCopyNote = onCopyNote)
            }
            if (tags.isNotEmpty()) {
                FlowRow(
                    modifier = Modifier.padding(top = QcSpacing.s2),
                    horizontalArrangement = Arrangement.spacedBy(QcSpacing.s2),
                    verticalArrangement = Arrangement.spacedBy(QcSpacing.s2)
                ) {
                    tags.forEach { PassTag(it) }
                }
            }
            Spacer(Modifier.size(if (note != null || tags.isNotEmpty()) QcSpacing.s4 else QcSpacing.s6))
            val fullNumber = shownNumber(card, maskNumber)
            DigitsAndCopy(fullNumber = fullNumber) {
                PassDigits(
                    last4 = last4,
                    fullNumber = fullNumber,
                    expiry = card.card.expiry,
                    expired = expired
                )
                CopyPill(
                    bank = card.bankName,
                    last4 = last4,
                    copied = copied,
                    enabled = copyEnabled && onCopy != null,
                    tint = hue.c2,
                    onCopy = onCopy ?: {}
                )
            }
            if (dueText != null) {
                DueStrip(text = dueText, onMarkPaid = onMarkPaid)
            }
        }
    }
}

private val ArtPad: Dp = QcSpacing.s4

@Composable
private fun PassHead(card: HomeCard, status: CardStatus?, archiveReason: String?, inArchive: Boolean) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val networkId = card.network?.id
    val hasMark = networkId != null && NetworkMarks.drawable(networkId) != null
    val meta = metaLine(card.typeName, card.holderName)
    Column(
        modifier = Modifier.clearAndSetSemantics { },
        verticalArrangement = Arrangement.spacedBy(QcSpacing.s1)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(QcSpacing.s3)) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(QcSpacing.s1)
            ) {
                Text(
                    text = card.bankName,
                    style = QcText.F3Semibold.copy(letterSpacing = (-0.01f).em),
                    color = PassColors.OnArt,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                if (card.card.pinned && !card.card.archived) {
                    Icon(
                        imageVector = QcIcons.pinFilled,
                        contentDescription = null,
                        tint = PassColors.OnArt,
                        modifier = Modifier
                            .size(QcSize.iconXs)
                            .alpha(PIN_ALPHA)
                    )
                }
            }
            if (hasMark && networkId != null) {
                NetworkMark(
                    networkId = networkId,
                    contentDescription = card.network?.name,
                    tone = markTone(networkId, onArt = true, dark = dark),
                    fit = MarkFit.HEAD,
                    modifier = Modifier.bleedVertical(QcSpacing.s1)
                )
            } else if (!card.network?.code.isNullOrBlank()) {
                Text(
                    text = card.network?.code.orEmpty(),
                    style = QcText.F1Semibold,
                    color = PassColors.OnArt,
                    maxLines = 1,
                    modifier = Modifier.widthIn(max = QcSize.row * 2)
                )
            }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(QcSpacing.s3)) {
            Text(
                text = meta,
                style = QcText.F2,
                color = PassColors.OnArtSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (status != null) {
                CardStatusChip(status = status, archiveReason = archiveReason, inArchive = inArchive)
            }
        }
    }
}

@Composable
private fun spokenStatus(status: CardStatus, archiveReason: String?): String = when (status) {
    CardStatus.ARCHIVED -> if (archiveReason.isNullOrBlank()) {
        stringResource(R.string.cards_status_archived)
    } else {
        stringResource(R.string.cards_status_archived_reason, archiveReason)
    }
    CardStatus.EXPIRED -> stringResource(R.string.cards_status_expired)
    CardStatus.CHECK -> stringResource(R.string.cards_a11y_status_check)
    CardStatus.EXPIRES_SOON -> stringResource(R.string.cards_status_expires_soon)
    CardStatus.LAST_USED -> stringResource(R.string.cards_last_used)
}

/** Lets a mark overhang its row by [inset] top and bottom, like the prototype's -4 margins. */
private fun Modifier.bleedVertical(inset: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val px = inset.roundToPx()
    layout(placeable.width, (placeable.height - 2 * px).coerceAtLeast(0)) {
        placeable.place(0, -px)
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PassNote(note: String, onOpen: (() -> Unit)?, onCopyNote: (() -> Unit)?) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val holdLabel = stringResource(R.string.cards_a11y_hold_copy_note)
    Text(
        text = note,
        style = QcText.F2,
        color = PassColors.OnArtSecondary,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier
            .padding(top = QcSpacing.s2)
            .fillMaxWidth()
            .clip(QcShapes.xs)
            .background(if (pressed) PassColors.OnArtPressed else Color.Transparent)
            .then(
                if (onCopyNote != null) {
                    Modifier
                        .combinedClickable(
                            interactionSource = interaction,
                            indication = null,
                            onLongClickLabel = holdLabel,
                            onClick = { onOpen?.invoke() },
                            onLongClick = onCopyNote
                        )
                        .semantics(mergeDescendants = true) {
                            contentDescription = note
                        }
                } else {
                    Modifier
                }
            )
    )
}

@Composable
private fun PassTag(name: String) {
    Box(
        modifier = Modifier
            .heightIn(min = QcSize.ctlSm)
            .background(PassColors.OnArt.copy(alpha = TAG_FILL), QcShapes.sm)
            .padding(horizontal = QcSpacing.s2),
        contentAlignment = Alignment.Center
    ) {
        Text(text = name, style = QcText.F1Semibold, color = PassColors.OnArt, maxLines = 1)
    }
}

/**
 * The digits beside Copy, centred in a `ctl`-tall row. When large text would wrap the full number
 * past [BESIDE_COPY_LINES] lines there, the number takes the whole width and Copy drops below it,
 * right-aligned.
 */
@Composable
private fun DigitsAndCopy(fullNumber: String?, content: @Composable () -> Unit) {
    val measurer = rememberTextMeasurer()
    Layout(content = content, modifier = Modifier.fillMaxWidth()) { measurables, constraints ->
        val (digits, pill) = measurables
        val width = constraints.maxWidth
        val loose = constraints.copy(minWidth = 0, minHeight = 0)
        val pillPlaceable = pill.measure(loose)
        val beside = (width - pillPlaceable.width - QcSpacing.s3.roundToPx()).coerceAtLeast(0)
        val fits = fullNumber == null ||
            measurer.measure(fullNumber, QcText.MonoF3, constraints = Constraints(maxWidth = beside)).lineCount <= BESIDE_COPY_LINES
        if (fits) {
            val digitsPlaceable = digits.measure(loose.copy(minWidth = beside, maxWidth = beside))
            val height = maxOf(digitsPlaceable.height, pillPlaceable.height, QcSize.ctl.roundToPx())
            layout(width, height) {
                digitsPlaceable.place(0, (height - digitsPlaceable.height) / 2)
                pillPlaceable.place(width - pillPlaceable.width, (height - pillPlaceable.height) / 2)
            }
        } else {
            val digitsPlaceable = digits.measure(loose.copy(minWidth = width, maxWidth = width))
            layout(width, digitsPlaceable.height + pillPlaceable.height) {
                digitsPlaceable.place(0, 0)
                pillPlaceable.place(width - pillPlaceable.width, digitsPlaceable.height)
            }
        }
    }
}

/**
 * "•• 1234  12/28", or "4111 1111 1111 1234  12/28" in f3 so it fits beside Copy; when larger
 * text leaves no room the expiry drops below the number.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PassDigits(last4: String, fullNumber: String?, expiry: String, expired: Boolean) {
    FlowRow(
        modifier = Modifier.clearAndSetSemantics { },
        horizontalArrangement = Arrangement.spacedBy(QcSpacing.s3),
        verticalArrangement = Arrangement.Center
    ) {
        if (fullNumber != null) {
            Text(text = fullNumber, style = QcText.MonoF3, color = PassColors.OnArt, modifier = Modifier.alignByBaseline())
        } else {
            Row(modifier = Modifier.alignByBaseline()) {
                Text(
                    text = DOTS,
                    style = QcText.MonoDotsF4,
                    color = PassColors.OnArt.copy(alpha = DOTS_ALPHA),
                    maxLines = 1,
                    modifier = Modifier.alignByBaseline()
                )
                Spacer(Modifier.width(QcSpacing.s2))
                Text(text = last4, style = QcText.MonoF4, color = PassColors.OnArt, maxLines = 1, modifier = Modifier.alignByBaseline())
            }
        }
        Text(
            text = expiry,
            style = QcText.MonoF3.copy(fontSize = QcText.F2.fontSize),
            color = if (expired) PassColors.OnArtDanger else PassColors.OnArt,
            maxLines = 1,
            modifier = Modifier.alignByBaseline()
        )
    }
}

/** 32 visible, 48 hit. Copied turns white with the card's light stop as ink, in 150 ms. */
@Composable
private fun CopyPill(
    bank: String,
    last4: String,
    copied: Boolean,
    enabled: Boolean,
    tint: Color,
    onCopy: () -> Unit
) {
    val fill by animateColorAsState(
        when {
            copied -> PassColors.OnArt
            enabled -> PassColors.OnArtFill
            else -> PassColors.OnArt.copy(alpha = DISABLED_FILL)
        },
        tween(QcMotion.STATE_MS)
    )
    val ink by animateColorAsState(
        when {
            copied -> tint
            enabled -> PassColors.OnArt
            else -> PassColors.OnArt.copy(alpha = DISABLED_INK)
        },
        tween(QcMotion.STATE_MS)
    )
    val ring = when {
        copied -> Color.Transparent
        enabled -> PassColors.OnArtFillRing
        else -> PassColors.OnArt.copy(alpha = DISABLED_RING)
    }
    val label = if (copied) {
        stringResource(R.string.cards_a11y_number_copied, bank)
    } else {
        stringResource(R.string.cards_a11y_copy_number, bank, last4)
    }
    Box(
        modifier = Modifier
            .heightIn(min = QcSize.hit)
            .clickable(
                enabled = enabled,
                interactionSource = null,
                indication = null,
                role = Role.Button,
                onClick = onCopy
            )
            .clearAndSetSemantics {
                contentDescription = label
                role = Role.Button
                if (enabled) onClick { onCopy(); true } else disabled()
            },
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .widthIn(min = QcSpacing.s16 + QcSpacing.s6)
                .heightIn(min = QcSize.ctl)
                .then(if (copied) Modifier.shadow(QcSpacing.s1, QcShapes.full) else Modifier)
                .background(fill, QcShapes.full)
                .border(QcSize.hairline, ring, QcShapes.full)
                .padding(start = QcSpacing.s2, end = QcSpacing.s3, top = QcSpacing.s1, bottom = QcSpacing.s1),
            horizontalArrangement = Arrangement.spacedBy(QcSpacing.s1, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (copied) QcIcons.check else QcIcons.copyOutlined,
                contentDescription = null,
                tint = ink,
                modifier = Modifier.size(QcSize.iconSm)
            )
            Text(
                text = stringResource(if (copied) R.string.cards_pass_copied else R.string.cards_copy),
                style = QcText.F2Medium,
                color = ink,
                maxLines = 1
            )
        }
    }
}

/** "Due in 2 days" + Mark paid, inset 8 from the card edge so Mark paid ends on the Copy pill's edge. */
@Composable
private fun DueStrip(text: String, onMarkPaid: (() -> Unit)?) {
    val markLabel = stringResource(R.string.cards_due_mark_paid_a11y)
    Row(
        modifier = Modifier
            .padding(top = QcSpacing.s6)
            .bleed(QcSpacing.s2)
            .fillMaxWidth()
            .heightIn(min = QcSize.hit)
            .background(PassColors.OnArt.copy(alpha = DUE_FILL), QcShapes.md)
            .border(QcSize.hairline, PassColors.OnArt.copy(alpha = DUE_RING), QcShapes.md)
            .padding(horizontal = QcSpacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(QcSpacing.s2)
    ) {
        Icon(
            imageVector = QcIcons.paymentsOutlined,
            contentDescription = null,
            tint = PassColors.OnArt,
            modifier = Modifier.size(QcSize.iconSm)
        )
        Text(
            text = text,
            style = QcText.F2Medium,
            color = PassColors.OnArt,
            maxLines = 1,
            modifier = Modifier.weight(1f)
        )
        if (onMarkPaid != null) {
            Box(
                modifier = Modifier
                    .heightIn(min = QcSize.hit)
                    .clickable(onClickLabel = markLabel, role = Role.Button, onClick = onMarkPaid)
                    .semantics { contentDescription = markLabel },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.cards_due_mark_paid),
                    style = QcText.F2Semibold,
                    color = PassColors.OnArt,
                    modifier = Modifier
                        .heightIn(min = QcSize.ctl)
                        .background(PassColors.OnArtFill, QcShapes.full)
                        .border(QcSize.hairline, PassColors.OnArtFillRing, QcShapes.full)
                        .padding(horizontal = QcSpacing.s3, vertical = QcSpacing.s1 + QcSpacing.s1 / 2)
                )
            }
        }
    }
}

/** Grows the element by [inset] on every side without changing its reported size. */
private fun Modifier.outset(inset: Dp): Modifier = layout { measurable, constraints ->
    val px = inset.roundToPx()
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = constraints.minWidth + 2 * px,
            maxWidth = constraints.maxWidth + 2 * px,
            minHeight = constraints.minHeight + 2 * px,
            maxHeight = constraints.maxHeight + 2 * px
        )
    )
    layout(constraints.minWidth, constraints.minHeight) { placeable.place(-px, -px) }
}

/** Grows the element by [inset] left, right and bottom, like the strip's negative margins. */
private fun Modifier.bleed(inset: Dp): Modifier = layout { measurable, constraints ->
    val px = inset.roundToPx()
    val wide = constraints.copy(
        minWidth = constraints.minWidth + 2 * px,
        maxWidth = if (constraints.hasBoundedWidth) constraints.maxWidth + 2 * px else constraints.maxWidth
    )
    val placeable = measurable.measure(wide)
    layout(placeable.width - 2 * px, (placeable.height - px).coerceAtLeast(0)) {
        placeable.place(-px, 0)
    }
}

private const val BESIDE_COPY_LINES = 2
private const val DOTS = "••"
private const val DOTS_ALPHA = 0.72f
private const val PIN_ALPHA = 0.9f
private const val TAG_FILL = 0.16f
private const val DUE_FILL = 0.12f
private const val DUE_RING = 0.20f
private const val DISABLED_FILL = 0.06f
private const val DISABLED_INK = 0.5f
private const val DISABLED_RING = 0.14f
