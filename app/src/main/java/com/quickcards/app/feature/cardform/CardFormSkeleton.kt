package com.quickcards.app.feature.cardform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import com.quickcards.app.R
import com.quickcards.app.core.ui.components.QcSkeleton
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcShapes
import com.quickcards.app.core.ui.theme.QcSize
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTheme

/**
 * The form's chrome while the session loads: preview and fields as static tonal blocks under
 * their real labels, and the Save footer, still off. Hidden from accessibility until it is real.
 */
@Composable
internal fun CardFormSkeleton(mode: CardFormMode, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxSize().clearAndSetSemantics { }) {
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(start = QcSpacing.s4, end = QcSpacing.s4, top = QcSpacing.s2, bottom = QcSpacing.s4),
            verticalArrangement = Arrangement.spacedBy(QcSpacing.s3)
        ) {
            QcSkeleton(modifier = Modifier.fillMaxWidth().height(QcDimens.SkeletonTile), shape = QcShapes.lg)
            GhostField(stringResource(R.string.form_field_holder))
            GhostField(stringResource(R.string.form_field_number))
            Row(horizontalArrangement = Arrangement.spacedBy(QcSpacing.s3)) {
                GhostField(stringResource(R.string.form_field_expiry), Modifier.weight(1f))
                GhostField(stringResource(R.string.form_field_cvv), Modifier.weight(1f))
            }
            GhostField(stringResource(R.string.form_field_bank))
        }
        FormFooter(
            label = stringResource(if (mode == CardFormMode.EDIT) R.string.form_save_changes else R.string.form_save_card),
            enabled = false,
            note = null,
            noteIsError = false,
            onClick = {}
        )
    }
}

@Composable
private fun GhostField(label: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(QcSpacing.s1)) {
        Text(text = label, style = QcText.F2Medium, color = QcTheme.colors.ink2, maxLines = 1)
        QcSkeleton(modifier = Modifier.fillMaxWidth().height(QcSize.field))
    }
}
