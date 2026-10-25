package com.quickcards.app.feature.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quickcards.app.R
import com.quickcards.app.core.data.repository.RepairSummary
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcType

/** The one-time legacy repair summary above the Cards list. */
@Composable
internal fun RepairBanner(
    summary: RepairSummary,
    onReview: () -> Unit,
    onDismiss: () -> Unit
) {
    val text = buildString {
        append(
            pluralStringResource(
                R.plurals.cards_repair_fixed,
                summary.fixedCards,
                summary.fixedCards
            )
        )
        if (summary.needsCheck > 0) {
            append(' ')
            append(
                pluralStringResource(
                    R.plurals.cards_repair_check,
                    summary.needsCheck,
                    summary.needsCheck
                )
            )
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(QcDimens.CornerMd))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = QcIcons.autoFixHigh,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .offset(y = (-2).dp)
                .size(QcDimens.Icon)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = text,
                style = QcType.Body,
                color = MaterialTheme.colorScheme.onSurface
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .offset(x = 12.dp)
                    .padding(top = QcDimens.Half),
                horizontalArrangement = Arrangement.spacedBy(QcDimens.Half, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (summary.needsCheck > 0) {
                    TextButton(
                        onClick = onReview,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(stringResource(R.string.cards_repair_review))
                    }
                }
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(stringResource(R.string.cards_repair_dismiss))
                }
            }
        }
    }
}
