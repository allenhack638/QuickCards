package com.quickcards.app.core.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcSize
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTheme

/** A 48dp icon target with a 24dp glyph; [label] is its TalkBack name. */
@Composable
fun QcIconButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = QcTheme.colors.ink
) {
    IconButton(onClick = onClick, modifier = modifier.size(QcSize.hit)) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(QcSize.iconLg))
    }
}

/**
 * Nested screen bar (64): back, an optional `f4` title, trailing icon actions. Without a visible
 * title, [paneTitle] names the screen to TalkBack as its level-1 heading.
 */
@Composable
fun QcNestedTopBar(
    onBack: () -> Unit,
    backLabel: String,
    modifier: Modifier = Modifier,
    title: String? = null,
    paneTitle: String? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = QcSize.bar)
            .padding(horizontal = QcSpacing.s1),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(QcSpacing.s1)
    ) {
        QcIconButton(QcIcons.back, backLabel, onBack)
        val name = title ?: paneTitle
        Text(
            text = title.orEmpty(),
            style = QcText.F4,
            color = QcTheme.colors.ink,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .weight(1f)
                .then(
                    if (name != null) {
                        Modifier.semantics {
                            heading()
                            this.paneTitle = name
                            if (title == null) contentDescription = name
                        }
                    } else {
                        Modifier
                    }
                )
        )
        actions()
    }
}
