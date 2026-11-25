package com.quickcards.app.feature.cardform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import com.quickcards.app.core.ui.components.metaLine
import com.quickcards.app.R
import com.quickcards.app.core.ui.art.CardArt
import com.quickcards.app.core.ui.components.MASK_GROUP
import com.quickcards.app.core.ui.components.MarkFit
import com.quickcards.app.core.ui.components.NetworkMark
import com.quickcards.app.core.ui.components.NetworkMarks
import com.quickcards.app.core.ui.components.QcStatusChip
import com.quickcards.app.core.ui.components.markTone
import com.quickcards.app.core.ui.sharedPass
import com.quickcards.app.core.ui.theme.PassColors
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.domain.ExpiryState
import com.quickcards.domain.ExpiryStatus
import java.time.LocalDate

private const val DOTS_ALPHA = 0.72f

/**
 * The live pass above the form: graphite "New card •••• 0000 MM/YY" while empty, then the bank,
 * type · holder, the detected network's mark, the last four and the expiry as they are typed.
 * A past expiry turns only the preview graphite with the Expired chip.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun FormPreview(session: CardFormSession, bankName: String?, holderName: String?, typeName: String?) {
    val form = session.form
    val network = session.network(session.lists.networks)
    val expired = ExpiryStatus.expiryState(form.expiry, LocalDate.now(), session.options.expiresSoonMonths) == ExpiryState.EXPIRED
    val meta = metaLine(typeName, holderName)
    CardArt(
        band = form.bandColor,
        expired = expired,
        modifier = Modifier
            .fillMaxWidth()
            .sharedPass(form.editingId)
            .heightIn(min = QcSpacing.s16 * 2 + QcSpacing.s2)
            .clearAndSetSemantics { }
    ) {
        Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(QcSpacing.s6)) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(QcSpacing.s1)) {
                    Text(
                        text = bankName ?: stringResource(R.string.form_preview_new),
                        style = QcText.F3Semibold,
                        color = PassColors.OnArt,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (meta.isNotEmpty()) {
                        Text(text = meta, style = QcText.F2, color = PassColors.OnArtSecondary, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(QcSpacing.s1)) {
                    network?.takeIf { NetworkMarks.drawable(it.id) != null }?.let { net ->
                        NetworkMark(
                            networkId = net.id,
                            contentDescription = null,
                            tone = markTone(net.id, onArt = true, dark = false),
                            fit = MarkFit.HEAD
                        )
                    }
                    if (expired) QcStatusChip(text = stringResource(R.string.cards_status_expired))
                }
            }
            FlowRow(horizontalArrangement = Arrangement.spacedBy(QcSpacing.s3)) {
                Row(modifier = Modifier.align(Alignment.Bottom), verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = MASK_GROUP,
                        style = QcText.MonoDotsF4,
                        color = PassColors.OnArt.copy(alpha = DOTS_ALPHA),
                        modifier = Modifier.padding(end = QcSpacing.s2)
                    )
                    Text(
                        text = form.number.takeLast(4).ifEmpty { stringResource(R.string.form_preview_digits) },
                        style = QcText.MonoF4,
                        color = if (form.number.isEmpty()) PassColors.OnArtSecondary else PassColors.OnArt
                    )
                }
                Text(
                    text = form.expiry.ifEmpty { stringResource(R.string.form_helper_expiry) },
                    style = QcText.MonoF3.copy(fontSize = QcText.F2.fontSize),
                    modifier = Modifier.align(Alignment.Bottom).padding(bottom = QcSpacing.s1),
                    color = when {
                        expired -> PassColors.OnArtDanger
                        form.expiry.isEmpty() -> PassColors.OnArtSecondary
                        else -> PassColors.OnArt
                    }
                )
            }
        }
    }
}
