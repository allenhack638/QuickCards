package com.quickcards.app.feature.cards

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.quickcards.app.R
import com.quickcards.app.core.data.model.BandColor
import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.app.core.ui.HomeCard
import com.quickcards.app.core.ui.components.CardHero
import com.quickcards.app.core.ui.art.ArtDimens
import com.quickcards.app.core.ui.components.PassCard
import com.quickcards.app.core.ui.theme.QcElevation
import com.quickcards.app.core.ui.theme.QcFilledButton
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcMotion
import com.quickcards.app.core.ui.theme.QcShapes
import com.quickcards.app.core.ui.theme.QcSize
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTextButton
import com.quickcards.app.core.ui.theme.QcTextTone
import com.quickcards.app.core.ui.theme.QcTheme
import com.quickcards.app.core.ui.theme.QcTonalButton
import com.quickcards.domain.CardNetwork

/**
 * First run, shown once before the empty vault: three one-line tips over real card art, with
 * Skip on every page, Back to the previous page, and Add card on the last.
 */
@Composable
internal fun FirstRunTipsScreen(
    bottomInset: Dp,
    onSkip: () -> Unit,
    onAdd: () -> Unit,
    modifier: Modifier = Modifier
) {
    var page by rememberSaveable { mutableIntStateOf(0) }
    val samples = rememberSamples()
    BackHandler(enabled = page > 0) { page-- }
    Column(modifier = modifier.fillMaxSize().padding(bottom = bottomInset)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(QcSize.bar)
                .padding(horizontal = QcSpacing.s1),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            QcTextButton(text = stringResource(R.string.tips_skip), onClick = onSkip, tone = QcTextTone.Neutral)
        }
        AnimatedContent(
            targetState = page,
            transitionSpec = { fadeIn(tween(QcMotion.NAV_MS)) togetherWith fadeOut(tween(QcMotion.NAV_MS)) },
            modifier = Modifier.weight(1f)
        ) { shown ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = QcSpacing.s4, end = QcSpacing.s4, bottom = QcSpacing.s8),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(modifier = Modifier.fillMaxWidth().clearAndSetSemantics { }) {
                    when (shown) {
                        0 -> PassCard(card = samples.icici, copied = true)
                        1 -> SealedHero(samples.hdfc)
                        else -> Wallet(samples)
                    }
                }
                Text(
                    text = stringResource(
                        when (shown) {
                            0 -> R.string.tips_copy
                            1 -> R.string.tips_encrypted
                            else -> R.string.cards_empty_title
                        }
                    ),
                    style = QcText.F4,
                    color = QcTheme.colors.ink,
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .padding(top = QcSpacing.s12)
                        .semantics { heading() }
                )
            }
        }
        PageDots(page = page, count = TIP_COUNT)
        Box(modifier = Modifier.padding(start = QcSpacing.s4, end = QcSpacing.s4, top = QcSpacing.s5, bottom = QcSpacing.s4)) {
            if (page < TIP_COUNT - 1) {
                QcTonalButton(
                    text = stringResource(R.string.tips_next),
                    onClick = { page++ },
                    large = true,
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                QcFilledButton(
                    text = stringResource(R.string.cards_add),
                    onClick = onAdd,
                    large = true,
                    icon = QcIcons.add,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun PageDots(page: Int, count: Int) {
    val colors = QcTheme.colors
    val label = stringResource(R.string.tips_a11y_page, page + 1, count)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = QcSpacing.s4)
            .semantics { contentDescription = label },
        horizontalArrangement = Arrangement.spacedBy(QcSpacing.s2, Alignment.CenterHorizontally)
    ) {
        repeat(count) { index ->
            Box(
                modifier = Modifier
                    .size(width = if (index == page) QcSpacing.s6 else QcSpacing.s2, height = QcSpacing.s2)
                    .background(if (index == page) colors.ink else colors.tonal2, QcShapes.full)
            )
        }
    }
}

/** Tip 2: a lock seal sits on the card's lower edge. */
@Composable
private fun SealedHero(card: HomeCard) {
    val colors = QcTheme.colors
    Box(modifier = Modifier.fillMaxWidth()) {
        CardHero(card = card, status = null)
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .layout { measurable, constraints ->
                    val placeable = measurable.measure(constraints)
                    layout(placeable.width, placeable.height) { placeable.place(0, QcSpacing.s6.roundToPx()) }
                }
                .size(QcSpacing.s12)
                .shadow(QcElevation.e2, QcShapes.full, ambientColor = colors.shadow, spotColor = colors.shadow)
                .background(colors.surfaceHi, QcShapes.full),
            contentAlignment = Alignment.Center
        ) {
            Icon(QcIcons.lock, contentDescription = null, tint = colors.ink, modifier = Modifier.size(QcSize.iconLg))
        }
    }
}

/** Tip 3: a wallet of three cards; the back two show only their top edge. */
@Composable
private fun Wallet(samples: TipSamples) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ArtDimens.HeroMinHeight + QcSpacing.s10 * 2),
        contentAlignment = Alignment.TopCenter
    ) {
        listOf(samples.icici to WALLET_BACK, samples.axis to WALLET_MIDDLE, samples.hdfc to 1f)
            .forEachIndexed { index, (card, width) ->
                CardHero(
                    card = card,
                    status = null,
                    modifier = Modifier
                        .padding(top = QcSpacing.s10 * index)
                        .fillMaxWidth(width)
                )
            }
    }
}

private class TipSamples(val icici: HomeCard, val hdfc: HomeCard, val axis: HomeCard)

/** Made-up cards for the illustrations; nothing here is stored. */
@Composable
private fun rememberSamples(): TipSamples {
    val holder = stringResource(R.string.tips_sample_holder)
    val credit = stringResource(R.string.tips_sample_credit)
    val debit = stringResource(R.string.tips_sample_debit)
    val icici = stringResource(R.string.tips_sample_icici)
    val hdfc = stringResource(R.string.tips_sample_hdfc)
    val axis = stringResource(R.string.tips_sample_axis)
    return remember(holder, credit, debit, icici, hdfc, axis) {
        fun sample(bank: String, type: String, last4: String, expiry: String, band: String, network: CardNetwork, pinned: Boolean = false) =
            HomeCard(
                card = VaultCard(number = last4, expiry = expiry, bandColor = band, pinned = pinned),
                bankName = bank,
                holderName = holder,
                typeName = type,
                network = network,
                networkId = network.id,
                tagNames = emptyList()
            )
        TipSamples(
            icici = sample(icici, debit, "4417", "08/27", BandColor.RUST, SampleNetworks.mastercard),
            hdfc = sample(hdfc, credit, "0366", "12/29", BandColor.NAVY, SampleNetworks.visa, pinned = true),
            axis = sample(axis, credit, "9021", "03/28", BandColor.WINE, SampleNetworks.rupay)
        )
    }
}

private object SampleNetworks {
    val visa = CardNetwork("visa", "Visa", "VISA", "", "", 3, true)
    val mastercard = CardNetwork("mc", "Mastercard", "MC", "", "", 3, true)
    val rupay = CardNetwork("rupay", "RuPay", "RUPAY", "", "", 3, true)
}

private const val TIP_COUNT = 3
private const val WALLET_BACK = 0.84f
private const val WALLET_MIDDLE = 0.92f
