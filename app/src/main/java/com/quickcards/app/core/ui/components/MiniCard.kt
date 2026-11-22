package com.quickcards.app.core.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.luminance
import com.quickcards.app.core.ui.art.ArtKind
import com.quickcards.app.core.ui.art.CardArt
import com.quickcards.app.core.ui.theme.QcSpacing

/** The 40x24 card art thumbnail of list rows, with the network mark (white) at its bottom right. */
@Composable
fun MiniCard(band: String, networkId: String?, expired: Boolean, modifier: Modifier = Modifier) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    CardArt(band = band, modifier = modifier, kind = ArtKind.MINI, expired = expired) {
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
