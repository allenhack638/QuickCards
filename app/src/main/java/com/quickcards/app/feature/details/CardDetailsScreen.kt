package com.quickcards.app.feature.details

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quickcards.app.R
import com.quickcards.app.core.security.AppLockController
import com.quickcards.app.core.security.UnlockSession
import com.quickcards.app.core.ui.CardCopyField
import com.quickcards.app.core.ui.CardUseStatus
import com.quickcards.app.core.ui.HomeCard
import com.quickcards.app.core.ui.adaptive.scaledFrom
import com.quickcards.app.core.ui.components.CardHero
import com.quickcards.app.core.ui.components.LocalSnackbar
import com.quickcards.app.core.ui.components.MASK_GROUP
import com.quickcards.app.core.ui.components.QcGroup
import com.quickcards.app.core.ui.components.QcIconButton
import com.quickcards.app.core.ui.components.QcNestedTopBar
import com.quickcards.app.core.ui.rememberCardCopier
import com.quickcards.app.core.ui.theme.PassColors
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTheme
import com.quickcards.domain.CardNumberRules
import com.quickcards.domain.ExpiryState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Card details: the pass, then Number, Expiry, CVV, Holder and Note, each with its own Copy.
 * Holding Number or CVV reveals it for five seconds; with [maskNumber] off (Settings › Hide card
 * numbers) the number is shown in full on the card and its row. A [card] that disappears after it was shown
 * (deleted) goes back; one that never shows once [loading] is over (a stale link or reminder)
 * says so and goes back.
 */
@Composable
fun CardDetailsScreen(
    card: HomeCard?,
    loading: Boolean,
    lastUsedId: String?,
    bottomInset: Dp,
    onBack: () -> Unit,
    onEdit: (cardId: String) -> Unit,
    onActions: (cardId: String) -> Unit,
    onStatus: (cardId: String) -> Unit,
    layout: DetailsLayout = DetailsLayout.STACKED,
    showBack: Boolean = true,
    maskNumber: Boolean = true
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    val snackbar = LocalSnackbar.current
    // One per pane: the marks it holds are keyed to a card and cleared on a switch, a lock or leaving.
    val viewModel: CardDetailsViewModel = viewModel { CardDetailsViewModel(AppLockController.isLocked) }
    DisposableEffect(viewModel) { onDispose { viewModel.clear() } }
    var seen by remember { mutableStateOf(false) }
    LaunchedEffect(card == null, loading) {
        when {
            card != null -> seen = true
            seen -> onBack()
            !loading -> {
                snackbar.show(context.getString(R.string.details_card_not_found))
                onBack()
            }
        }
    }
    if (card == null) {
        QcNestedTopBar(onBack = onBack.takeIf { showBack }, backLabel = stringResource(R.string.details_back))
        return
    }
    val shown: HomeCard = card
    LaunchedEffect(shown.id) { viewModel.show(shown.id) }

    val copiedMark by viewModel.copied.collectAsState()
    val revealedMark by viewModel.revealed.collectAsState()
    // Read against the card on screen, so a mark from another card never draws for a frame.
    val copied = copiedMark?.takeIf { it.cardId == shown.id }?.field
    val revealed = revealedMark?.takeIf { it.cardId == shown.id }?.field
    val scope = rememberCoroutineScope()
    val copier = rememberCardCopier()
    var announcement by remember(shown.id) { mutableStateOf("") }
    var drain by remember(shown.id) { mutableFloatStateOf(1f) }

    val unlock = Unlock { reason ->
        val host = activity ?: return@Unlock false
        UnlockSession.requireUnlock(host, reason.prompt(context))
    }

    LaunchedEffect(copiedMark) {
        val mark = copiedMark ?: return@LaunchedEffect
        delay(QcDimens.CopiedMillis)
        viewModel.clearCopied(mark)
    }
    // Frame-time based so the 5 s reveal ignores the animator duration scale.
    LaunchedEffect(revealedMark) {
        if (revealedMark == null) return@LaunchedEffect
        drain = 1f
        val start = withFrameMillis { it }
        do {
            val elapsed = withFrameMillis { it } - start
            drain = (1f - elapsed / QcDimens.RevealDurationMillis).coerceIn(0f, 1f)
        } while (drain > 0f)
        viewModel.hide()
    }

    fun copy(field: CardCopyField) {
        scope.launch {
            viewModel.copy(shown, field, unlock, copier)?.let { announcement = it }
        }
    }

    fun reveal(field: CardCopyField) {
        scope.launch { viewModel.reveal(shown, field, unlock) }
    }

    fun edit() {
        scope.launch {
            val host = activity ?: return@launch
            if (UnlockSession.requireUnlock(host, context.getString(R.string.cards_unlock_edit))) onEdit(shown.id)
        }
    }

    val status = CardUseStatus.of(shown, lastUsedId)
    val colors = QcTheme.colors
    val hue = PassColors.hue(shown.card.bandColor, shown.expiryState == ExpiryState.EXPIRED)
    val hueInk = if (colors.dark) lerp(hue.c2, PassColors.OnArt, HUE_INK_LIFT) else hue.c2
    val copiedWord = stringResource(R.string.cards_pass_copied)
    val rows = detailsRows(shown, revealed, maskNumber, ::copy, ::reveal)

    val topBar: @Composable () -> Unit = {
        QcNestedTopBar(
            onBack = onBack.takeIf { showBack },
            backLabel = stringResource(R.string.details_back),
            paneTitle = stringResource(R.string.details_pane_title, shown.bankName, shown.card.number.takeLast(4))
        ) {
            QcIconButton(QcIcons.edit, stringResource(R.string.details_edit), ::edit)
            QcIconButton(QcIcons.more, stringResource(R.string.details_more), { onActions(shown.id) })
        }
    }
    val hero: @Composable (Modifier) -> Unit = { modifier -> CardHero(
        card = shown,
        status = status,
        modifier = modifier,
        maskNumber = maskNumber,
        onStatus = { onStatus(shown.id) }
    ) }
    val keysStacked = rememberKeysStacked(rows.map { it.second.label })
    val group: @Composable (Modifier) -> Unit = { modifier ->
        QcGroup(modifier = modifier) {
            rows.forEach { (field, spec) ->
                DetailsRow(
                    spec = spec,
                    copied = copied == field,
                    hueInk = hueInk,
                    hueFill = hue.c2,
                    copiedWord = copiedWord,
                    drain = drain.takeIf { revealed == field },
                    keyStacked = keysStacked
                )
            }
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        when (layout) {
            DetailsLayout.STACKED -> {
                topBar()
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(start = QcSpacing.s4, end = QcSpacing.s4, bottom = QcSpacing.s8 + bottomInset)
                        .widthIn(max = QcDimens.PageMax),
                    verticalArrangement = Arrangement.spacedBy(QcSpacing.s6)
                ) {
                    // Full content width, so the card shares the rows' edges; the ISO ratio sets its height.
                    hero(Modifier.scaledFrom(QcDimens.HeroDesign).aspectRatio(QcDimens.HeroRatio))
                    group(Modifier)
                }
            }
            DetailsLayout.SPLIT -> {
                topBar()
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .verticalScroll(rememberScrollState())
                        .padding(start = QcSpacing.s6, end = QcSpacing.s6, top = QcSpacing.s2, bottom = QcSpacing.s8 + bottomInset)
                        .widthIn(max = QcDimens.DetailMax),
                    horizontalArrangement = Arrangement.spacedBy(QcSpacing.s6)
                ) {
                    hero(Modifier.width(QcDimens.HeroDesign))
                    group(Modifier.weight(1f))
                }
            }
            DetailsLayout.SIDE -> Row(modifier = Modifier.fillMaxSize()) {
                Column(modifier = Modifier.width(QcDimens.ListPane).fillMaxHeight()) {
                    topBar()
                    hero(Modifier.padding(horizontal = QcSpacing.s4))
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .verticalScroll(rememberScrollState())
                        .padding(start = QcSpacing.s4, end = QcSpacing.s4, top = QcSpacing.s4, bottom = QcSpacing.s4 + bottomInset)
                ) {
                    group(Modifier)
                }
            }
        }
        Box(
            modifier = Modifier.semantics {
                liveRegion = LiveRegionMode.Polite
                contentDescription = announcement
            }
        )
    }
}

/** Stacked (phone, narrow pane), card left of the rows under one bar (wide pane), or card and bar left of the rows (landscape). */
enum class DetailsLayout { STACKED, SPLIT, SIDE }

@Composable
private fun detailsRows(
    card: HomeCard,
    revealed: CardCopyField?,
    maskNumber: Boolean,
    onCopy: (CardCopyField) -> Unit,
    onReveal: (CardCopyField) -> Unit
): List<Pair<CardCopyField, DetailsRowSpec>> {
    val dotsStyle = SpanStyle(color = QcTheme.colors.ink2)
    val last4 = card.card.number.takeLast(4)
    val numberShown = !maskNumber || revealed == CardCopyField.NUMBER
    val cvv = card.card.cvv
    val cvvShown = revealed == CardCopyField.CVV
    val numberLabel = stringResource(R.string.details_row_number)
    val expiryLabel = stringResource(R.string.cards_field_expiry)
    val cvvLabel = stringResource(R.string.cards_field_cvv)
    val holderLabel = stringResource(R.string.details_row_holder)
    val noteLabel = stringResource(R.string.details_row_note)
    return buildList {
        val number = CardNumberRules.format(card.card.number, card.network, masked = false)
        add(
            CardCopyField.NUMBER to DetailsRowSpec(
                label = numberLabel,
                value = if (numberShown) AnnotatedString(number) else masked(dotsStyle, MASK_GROUP, last4),
                mono = true,
                spokenValue = if (numberShown) {
                    stringResource(R.string.cards_a11y_field_value, numberLabel, number)
                } else {
                    stringResource(R.string.details_number_hidden, last4)
                },
                copyLabel = stringResource(R.string.details_copy_number),
                copiedLabel = stringResource(R.string.details_copied_number),
                onCopy = { onCopy(CardCopyField.NUMBER) },
                revealLabel = stringResource(R.string.cards_reveal_number).takeIf { maskNumber },
                onReveal = { onReveal(CardCopyField.NUMBER) }.takeIf { maskNumber }
            )
        )
        add(
            CardCopyField.EXPIRY to DetailsRowSpec(
                label = expiryLabel,
                value = AnnotatedString(card.card.expiry),
                mono = true,
                spokenValue = stringResource(R.string.cards_a11y_field_value, expiryLabel, card.card.expiry),
                copyLabel = stringResource(R.string.details_copy_expiry),
                copiedLabel = stringResource(R.string.details_copied_expiry),
                onCopy = { onCopy(CardCopyField.EXPIRY) }.takeIf { card.card.expiry.isNotBlank() }
            )
        )
        add(
            CardCopyField.CVV to if (cvv.isEmpty()) {
                val none = stringResource(R.string.cards_cvv_none)
                DetailsRowSpec(
                    label = cvvLabel,
                    value = AnnotatedString(none),
                    mono = false,
                    spokenValue = stringResource(R.string.cards_a11y_field_value, cvvLabel, none),
                    copyLabel = "",
                    copiedLabel = "",
                    onCopy = null,
                    muted = true
                )
            } else {
                DetailsRowSpec(
                    label = cvvLabel,
                    value = if (cvvShown) AnnotatedString(cvv) else masked(dotsStyle, "•".repeat(cvv.length), ""),
                    mono = true,
                    spokenValue = if (cvvShown) {
                        stringResource(R.string.cards_a11y_field_value, cvvLabel, cvv)
                    } else {
                        stringResource(R.string.details_cvv_hidden)
                    },
                    copyLabel = stringResource(R.string.details_copy_cvv),
                    copiedLabel = stringResource(R.string.details_copied_cvv),
                    onCopy = { onCopy(CardCopyField.CVV) },
                    revealLabel = stringResource(R.string.cards_reveal_cvv),
                    onReveal = { onReveal(CardCopyField.CVV) }
                )
            }
        )
        if (card.holderName.isNotBlank()) {
            add(
                CardCopyField.HOLDER to DetailsRowSpec(
                    label = holderLabel,
                    value = AnnotatedString(card.holderName),
                    mono = false,
                    spokenValue = stringResource(R.string.cards_a11y_field_value, holderLabel, card.holderName),
                    copyLabel = stringResource(R.string.details_copy_holder),
                    copiedLabel = stringResource(R.string.details_copied_name),
                    onCopy = { onCopy(CardCopyField.HOLDER) }
                )
            )
        }
        if (card.card.note.isNotBlank()) {
            add(
                CardCopyField.NOTE to DetailsRowSpec(
                    label = noteLabel,
                    value = AnnotatedString(card.card.note),
                    mono = false,
                    spokenValue = stringResource(R.string.cards_a11y_field_value, noteLabel, card.card.note),
                    copyLabel = stringResource(R.string.cards_a11y_copy_note),
                    copiedLabel = stringResource(R.string.cards_note_copied),
                    onCopy = { onCopy(CardCopyField.NOTE) }
                )
            )
        }
    }
}

private fun masked(dots: SpanStyle, mask: String, tail: String): AnnotatedString = buildAnnotatedString {
    withStyle(dots.copy(letterSpacing = QcText.MonoDotsF3.letterSpacing)) { append(mask) }
    if (tail.isNotEmpty()) append(" $tail")
}

private fun UnlockReason.prompt(context: Context): String? = when (this) {
    UnlockReason.COPY -> null
    UnlockReason.COPY_NOTE -> context.getString(R.string.cards_unlock_note)
    UnlockReason.SHOW_NUMBER -> context.getString(R.string.cards_reveal_number)
    UnlockReason.SHOW_CVV -> context.getString(R.string.cards_reveal_cvv)
}

private const val HUE_INK_LIFT = .6f
