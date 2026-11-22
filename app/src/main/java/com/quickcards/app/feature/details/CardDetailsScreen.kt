package com.quickcards.app.feature.details

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import com.quickcards.app.core.ui.adaptive.scaledFrom
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
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
import com.quickcards.app.core.autofill.CardUsage
import com.quickcards.app.core.data.settings.AppSettings
import com.quickcards.app.core.notifications.rememberCardNotifications
import com.quickcards.app.core.security.ClipboardHelper
import com.quickcards.app.core.security.UnlockSession
import com.quickcards.app.core.ui.HomeCard
import com.quickcards.app.core.ui.components.CardHero
import com.quickcards.app.core.ui.LocalAppLocked
import com.quickcards.app.core.ui.components.QcGroup
import com.quickcards.app.core.ui.components.QcIconButton
import com.quickcards.app.core.ui.components.QcNestedTopBar
import com.quickcards.app.core.ui.theme.PassColors
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTheme
import com.quickcards.domain.CardNumberRules
import com.quickcards.domain.CardStatusRules
import com.quickcards.domain.ExpiryState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Card details: the pass, then Number, Expiry, CVV, Holder and Note, each with its own Copy.
 * Holding Number or CVV reveals it for five seconds. [card] null after it was shown (deleted or
 * archived) goes back.
 */
@Composable
fun CardDetailsScreen(
    card: HomeCard?,
    bottomInset: Dp,
    onBack: () -> Unit,
    onEdit: (cardId: String) -> Unit,
    onActions: (cardId: String) -> Unit,
    layout: DetailsLayout = DetailsLayout.STACKED,
    showBack: Boolean = true
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    // Keyed by card so a reveal or Copied mark never carries over when a pane switches cards.
    val viewModel: CardDetailsViewModel = viewModel(key = card?.id) {
        CardDetailsViewModel(ClipboardCopier(context.applicationContext))
    }
    var seen by remember { mutableStateOf(false) }
    LaunchedEffect(card == null) {
        if (card != null) seen = true else if (seen) onBack()
    }
    val shown = card ?: return

    val copied by viewModel.copied.collectAsState()
    val revealed by viewModel.revealed.collectAsState()
    val locked = LocalAppLocked.current
    val scope = rememberCoroutineScope()
    val notifications = rememberCardNotifications()
    var lastUsedId by remember { mutableStateOf(CardStatusRules.lastUsedId(CardUsage.lastUsed(context))) }
    var announcement by remember { mutableStateOf("") }
    var drain by remember { mutableFloatStateOf(1f) }

    val unlock = Unlock { reason ->
        val host = activity ?: return@Unlock false
        UnlockSession.requireUnlock(host, reason.prompt(context))
    }

    LaunchedEffect(locked) { if (locked) viewModel.hide() }
    LaunchedEffect(copied) {
        val field = copied ?: return@LaunchedEffect
        delay(COPIED_MS)
        viewModel.clearCopied(field)
    }
    // Frame-time based so the 5 s reveal ignores the animator duration scale.
    LaunchedEffect(revealed) {
        if (revealed == null) return@LaunchedEffect
        drain = 1f
        val start = withFrameMillis { it }
        do {
            val elapsed = withFrameMillis { it } - start
            drain = (1f - elapsed / QcDimens.RevealDurationMillis).coerceIn(0f, 1f)
        } while (drain > 0f)
        viewModel.hide()
    }

    fun copy(field: DetailsField) {
        scope.launch {
            if (!viewModel.copy(shown, field, unlock)) return@launch
            lastUsedId = shown.id
            if (field == DetailsField.NUMBER) notifications.numberCopied(shown)
            announcement = context.getString(field.copiedRes())
        }
    }

    fun reveal(field: DetailsField) {
        scope.launch { viewModel.reveal(shown, field, unlock) }
    }

    fun edit() {
        scope.launch {
            val host = activity ?: return@launch
            if (UnlockSession.requireUnlock(host, context.getString(R.string.cards_unlock_edit))) onEdit(shown.id)
        }
    }

    val status = CardStatusRules.of(
        archived = shown.card.archived,
        expiry = shown.expiryState,
        needsCheck = shown.card.needsReview.isNotEmpty(),
        lastUsed = shown.id == lastUsedId
    )
    val colors = QcTheme.colors
    val hue = PassColors.hue(shown.card.bandColor, shown.expiryState == ExpiryState.EXPIRED)
    val hueInk = if (colors.dark) lerp(hue.c2, PassColors.OnArt, HUE_INK_LIFT) else hue.c2
    val copiedWord = stringResource(R.string.cards_pass_copied)
    val rows = detailsRows(shown, revealed, ::copy, ::reveal)

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
    val hero: @Composable (Modifier) -> Unit = { modifier -> CardHero(card = shown, status = status, modifier = modifier) }
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
                    hero(Modifier.scaledFrom(QcDimens.HeroDesign))
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
    revealed: DetailsField?,
    onCopy: (DetailsField) -> Unit,
    onReveal: (DetailsField) -> Unit
): List<Pair<DetailsField, DetailsRowSpec>> {
    val dotsStyle = SpanStyle(color = QcTheme.colors.ink2)
    val last4 = card.card.number.takeLast(4)
    val numberShown = revealed == DetailsField.NUMBER
    val cvv = card.card.cvv
    val cvvShown = revealed == DetailsField.CVV
    val numberLabel = stringResource(R.string.details_row_number)
    val expiryLabel = stringResource(R.string.cards_field_expiry)
    val cvvLabel = stringResource(R.string.cards_field_cvv)
    val holderLabel = stringResource(R.string.details_row_holder)
    val noteLabel = stringResource(R.string.details_row_note)
    return buildList {
        val number = CardNumberRules.format(card.card.number, card.network, masked = false)
        add(
            DetailsField.NUMBER to DetailsRowSpec(
                label = numberLabel,
                value = if (numberShown) AnnotatedString(number) else masked(dotsStyle, NUMBER_MASK, last4),
                mono = true,
                spokenValue = if (numberShown) {
                    stringResource(R.string.cards_a11y_field_value, numberLabel, number)
                } else {
                    stringResource(R.string.details_number_hidden, last4)
                },
                copyLabel = stringResource(R.string.details_copy_number),
                copiedLabel = stringResource(R.string.details_copied_number),
                onCopy = { onCopy(DetailsField.NUMBER) },
                revealLabel = stringResource(R.string.cards_reveal_number),
                onReveal = { onReveal(DetailsField.NUMBER) }
            )
        )
        add(
            DetailsField.EXPIRY to DetailsRowSpec(
                label = expiryLabel,
                value = AnnotatedString(card.card.expiry),
                mono = true,
                spokenValue = stringResource(R.string.cards_a11y_field_value, expiryLabel, card.card.expiry),
                copyLabel = stringResource(R.string.details_copy_expiry),
                copiedLabel = stringResource(R.string.details_copied_expiry),
                onCopy = { onCopy(DetailsField.EXPIRY) }.takeIf { card.card.expiry.isNotBlank() }
            )
        )
        add(
            DetailsField.CVV to if (cvv.isEmpty()) {
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
                    onCopy = { onCopy(DetailsField.CVV) },
                    revealLabel = stringResource(R.string.cards_reveal_cvv),
                    onReveal = { onReveal(DetailsField.CVV) }
                )
            }
        )
        if (card.holderName.isNotBlank()) {
            add(
                DetailsField.HOLDER to DetailsRowSpec(
                    label = holderLabel,
                    value = AnnotatedString(card.holderName),
                    mono = false,
                    spokenValue = stringResource(R.string.cards_a11y_field_value, holderLabel, card.holderName),
                    copyLabel = stringResource(R.string.details_copy_holder),
                    copiedLabel = stringResource(R.string.details_copied_name),
                    onCopy = { onCopy(DetailsField.HOLDER) }
                )
            )
        }
        if (card.card.note.isNotBlank()) {
            add(
                DetailsField.NOTE to DetailsRowSpec(
                    label = noteLabel,
                    value = AnnotatedString(card.card.note),
                    mono = false,
                    spokenValue = stringResource(R.string.cards_a11y_field_value, noteLabel, card.card.note),
                    copyLabel = stringResource(R.string.cards_a11y_copy_note),
                    copiedLabel = stringResource(R.string.cards_note_copied),
                    onCopy = { onCopy(DetailsField.NOTE) }
                )
            )
        }
    }
}

private fun masked(dots: SpanStyle, mask: String, tail: String): AnnotatedString = buildAnnotatedString {
    withStyle(dots.copy(letterSpacing = QcText.MonoDotsF3.letterSpacing)) { append(mask) }
    if (tail.isNotEmpty()) append(" $tail")
}

private fun DetailsField.copiedRes(): Int = when (this) {
    DetailsField.NUMBER -> R.string.details_copied_number
    DetailsField.EXPIRY -> R.string.details_copied_expiry
    DetailsField.CVV -> R.string.details_copied_cvv
    DetailsField.HOLDER -> R.string.details_copied_name
    DetailsField.NOTE -> R.string.cards_note_copied
}

private fun UnlockReason.prompt(context: Context): String? = when (this) {
    UnlockReason.COPY -> null
    UnlockReason.COPY_NOTE -> context.getString(R.string.cards_unlock_note)
    UnlockReason.SHOW_NUMBER -> context.getString(R.string.cards_reveal_number)
    UnlockReason.SHOW_CVV -> context.getString(R.string.cards_reveal_cvv)
}

/** Sensitive, timed clipboard copy (Settings › Clear clipboard) that also marks the card as used. */
private class ClipboardCopier(private val context: Context) : DetailsCopier {
    override fun copy(card: HomeCard, field: DetailsField, value: String) {
        val clearSec = AppSettings.get(context).clipClearSec.value
        val label = context.getString(
            when (field) {
                DetailsField.NUMBER -> R.string.details_row_number
                DetailsField.EXPIRY -> R.string.cards_field_expiry
                DetailsField.CVV -> R.string.cards_field_cvv
                DetailsField.HOLDER -> R.string.cards_field_name
                DetailsField.NOTE -> R.string.cards_clipboard_note
            }
        )
        ClipboardHelper.copySensitive(
            context,
            context.getString(R.string.cards_clipboard_field, card.title, label),
            value,
            if (clearSec > 0) clearSec * 1000L else null
        )
        CardUsage.record(context, card.id)
    }
}

private const val NUMBER_MASK = "••••"
private const val COPIED_MS = 1_600L
private const val HUE_INK_LIFT = .6f
