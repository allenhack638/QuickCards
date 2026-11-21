package com.quickcards.app.core.ui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.quickcards.app.core.ui.theme.QcSize
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTheme

/** Top-level bar (64): `f5` title on the 16 edge as the level-1 heading, then 48 icon actions. */
@Composable
fun QcScreenBar(title: String, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = QcSize.bar)
            .padding(start = QcSpacing.s4, end = QcSpacing.s1),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            style = QcText.F5,
            color = QcTheme.colors.ink,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() }
        )
        actions()
    }
}
