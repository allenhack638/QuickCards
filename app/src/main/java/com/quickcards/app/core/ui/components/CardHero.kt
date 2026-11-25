package com.quickcards.app.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.em
import com.quickcards.app.core.ui.HomeCard
import com.quickcards.app.core.ui.theme.QcArtDimens
import com.quickcards.app.core.ui.art.ArtKind
import com.quickcards.app.core.ui.art.CardArt
import com.quickcards.app.core.ui.art.EmvChip
import com.quickcards.app.core.ui.theme.PassColors
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcSize
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.domain.CardStatus
import com.quickcards.domain.ExpiryState

/**
 * The details pass: bank (+ pin) | mark, type | status chip, EMV chip, •••• last 4, holder | expiry.
 * With [maskNumber] off the full grouped number shows in f4, the size that fits the card's width.
 * [onStatus] opens the status chip's explanation.
 */
@Composable
fun CardHero(
    card: HomeCard,
    status: CardStatus?,
    modifier: Modifier = Modifier,
    mask: String = MASK_GROUP,
    maskNumber: Boolean = true,
    onStatus: (() -> Unit)? = null
) {
    val expired = card.expiryState == ExpiryState.EXPIRED
    CardArt(
        band = card.card.bandColor,
        kind = ArtKind.HERO,
        expired = expired,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = QcArtDimens.HeroMinHeight - QcArtDimens.Padding * 2),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            HeroHead(card, status, onStatus)
            EmvChip(Modifier.clearAndSetSemantics { })
            Column(modifier = Modifier.clearAndSetSemantics { }) {
                val fullNumber = shownNumber(card, maskNumber)
                if (fullNumber == null) {
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(letterSpacing = QcText.MonoDotsF5.letterSpacing, color = PassColors.OnArt.copy(alpha = DOTS_ALPHA))) {
                                append(mask)
                            }
                            append(HERO_DOT_GAP)
                            append(card.card.number.takeLast(4))
                        },
                        style = QcText.MonoF5.copy(letterSpacing = HERO_TRACKING),
                        color = PassColors.OnArt,
                        maxLines = 1
                    )
                } else {
                    Text(
                        text = fullNumber,
                        style = QcText.MonoF4.copy(letterSpacing = HERO_TRACKING),
                        color = PassColors.OnArt,
                        maxLines = 2
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(QcSpacing.s3),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = card.holderName,
                        style = QcText.F2,
                        color = PassColors.OnArtSecondary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = card.card.expiry,
                        style = QcText.MonoF3.copy(fontSize = QcText.F2.fontSize),
                        color = if (expired) PassColors.OnArtDanger else PassColors.OnArt
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroHead(card: HomeCard, status: CardStatus?, onStatus: (() -> Unit)?) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val networkId = card.network?.id?.takeIf { NetworkMarks.drawable(it) != null }
    Column(verticalArrangement = Arrangement.spacedBy(QcSpacing.s1)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(QcSpacing.s3)) {
            Row(
                modifier = Modifier.weight(1f).clearAndSetSemantics { },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(QcSpacing.s1)
            ) {
                Text(
                    text = card.bankName,
                    style = QcText.F3Semibold.copy(letterSpacing = BANK_TRACKING),
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
                        modifier = Modifier.size(QcSize.iconXs).alpha(PIN_ALPHA)
                    )
                }
            }
            if (networkId != null) {
                NetworkMark(
                    networkId = networkId,
                    contentDescription = card.network?.name,
                    tone = markTone(networkId, onArt = true, dark = dark),
                    fit = MarkFit.HEAD,
                    modifier = Modifier.offset(x = QcSpacing.s1, y = -QcSpacing.s1)
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
                text = card.typeName.orEmpty(),
                style = QcText.F2,
                color = PassColors.OnArtSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).clearAndSetSemantics { }
            )
            if (status != null) {
                CardStatusChip(
                    status = status,
                    archiveReason = card.card.archiveReason,
                    bestDays = card.bestDays,
                    onClick = onStatus
                )
            }
        }
    }
}

private const val HERO_DOT_GAP = " "
private const val DOTS_ALPHA = .72f
private const val PIN_ALPHA = .9f
private val HERO_TRACKING = 0.06f.em
private val BANK_TRACKING = (-0.01f).em
