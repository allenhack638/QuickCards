package com.quickcards.app.feature.cards

import android.content.pm.PackageManager
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import com.quickcards.app.R
import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.app.core.ui.HomeCard
import com.quickcards.app.core.ui.components.CardHero
import com.quickcards.app.core.ui.components.QcEmptyMessage
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcFilledButton
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTheme
import com.quickcards.app.core.ui.theme.QcTonalButton

/** Search or filters matched nothing: the search disc, "No matches" and, for a search, Clear search. */
@Composable
internal fun NoResult(modifier: Modifier, onClearSearch: (() -> Unit)?) {
    QcEmptyMessage(
        modifier = modifier,
        icon = QcIcons.search,
        title = stringResource(R.string.cards_no_result_title),
        body = stringResource(R.string.cards_no_result_body),
        actionLabel = onClearSearch?.let { stringResource(R.string.cards_no_result_clear) },
        compact = true,
        onAction = onClearSearch
    )
}

/** Everything archived or hidden-expired: the same frame as [NoResult], plus the way back. */
@Composable
internal fun EmptyNote(
    modifier: Modifier,
    note: CardsEmptyNote,
    onShowExpired: () -> Unit,
    onOpenArchived: () -> Unit
) {
    when (note) {
        CardsEmptyNote.HIDDEN_EXPIRED -> QcEmptyMessage(
            modifier = modifier,
            icon = QcIcons.eventBusy,
            title = stringResource(R.string.cards_empty_hidden_expired_title),
            actionLabel = stringResource(R.string.cards_empty_hidden_expired_action),
            compact = true,
            onAction = onShowExpired
        )
        CardsEmptyNote.ONLY_ARCHIVED -> QcEmptyMessage(
            modifier = modifier,
            icon = QcIcons.inventory2,
            title = stringResource(R.string.cards_empty_archived_title),
            actionLabel = stringResource(R.string.cards_empty_archived_action),
            compact = true,
            onAction = onOpenArchived
        )
        CardsEmptyNote.NONE -> NoResult(modifier = modifier, onClearSearch = null)
    }
}

/**
 * The empty vault: a faded sample card, "Add your first card" and the trust line, then the one
 * filled Add card and equal Import and Tap a card (NFC phones only) at the bottom.
 */
@Composable
internal fun EmptyState(
    modifier: Modifier,
    bottomInset: Dp,
    onAdd: () -> Unit,
    onImport: () -> Unit,
    onTapCard: () -> Unit
) {
    val colors = QcTheme.colors
    val context = LocalContext.current
    val hasNfc = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC) }
    BoxWithConstraints(modifier = modifier) {
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .widthIn(max = EmptyMaxWidth)
                .verticalScroll(rememberScrollState())
                .heightIn(min = maxHeight)
                .padding(start = QcSpacing.s4, end = QcSpacing.s4, top = QcSpacing.s2, bottom = bottomInset)
        ) {
            SampleCard()
            Text(
                text = stringResource(R.string.cards_empty_title),
                style = QcText.F4,
                color = colors.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = QcSpacing.s8)
                    .semantics { heading() }
            )
            Text(
                text = stringResource(R.string.cards_empty_body),
                style = QcText.F2,
                color = colors.ink2,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = QcSpacing.s2)
            )
            Spacer(Modifier.weight(1f).heightIn(min = QcSpacing.s4))
            Column(
                modifier = Modifier.padding(vertical = QcSpacing.s4),
                verticalArrangement = Arrangement.spacedBy(QcSpacing.s3)
            ) {
                QcFilledButton(
                    text = stringResource(R.string.cards_add),
                    onClick = onAdd,
                    large = true,
                    icon = QcIcons.add,
                    modifier = Modifier.fillMaxWidth()
                )
                Row(horizontalArrangement = Arrangement.spacedBy(QcSpacing.s3)) {
                    QcTonalButton(
                        text = stringResource(R.string.cards_import),
                        onClick = onImport,
                        large = true,
                        icon = QcIcons.download,
                        modifier = Modifier.weight(1f)
                    )
                    if (hasNfc) {
                        QcTonalButton(
                            text = stringResource(R.string.cards_empty_tap),
                            onClick = onTapCard,
                            large = true,
                            icon = QcIcons.contactless,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}

/** A real jewel card, desaturated to 60% and faded to 72%: decoration only. */
@Composable
private fun SampleCard() {
    val bank = stringResource(R.string.cards_empty_sample_bank)
    val expiry = stringResource(R.string.cards_empty_sample_expiry)
    val sample = remember(bank, expiry) {
        HomeCard(
            card = VaultCard(number = SAMPLE_NUMBER, expiry = expiry),
            bankName = bank,
            holderName = "",
            typeName = null,
            network = null,
            networkId = null,
            tagNames = emptyList()
        )
    }
    val paint = remember {
        Paint().apply {
            alpha = SAMPLE_ALPHA
            colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(SAMPLE_SATURATION) })
        }
    }
    CardHero(
        card = sample,
        status = null,
        mask = SAMPLE_MASK,
        modifier = Modifier
            .clearAndSetSemantics { }
            .drawWithContent {
                drawIntoCanvas { canvas ->
                    canvas.saveLayer(Rect(0f, 0f, size.width, size.height), paint)
                    drawContent()
                    canvas.restore()
                }
            }
    )
}

/** On a wide window the empty vault keeps a phone's column: the card at its widest plus the gutters. */
private val EmptyMaxWidth = QcDimens.CardMaxWidth + QcSpacing.s4 * 2

private const val SAMPLE_NUMBER = "0000"
private const val SAMPLE_MASK = "••"
private const val SAMPLE_ALPHA = 0.72f
private const val SAMPLE_SATURATION = 0.6f
