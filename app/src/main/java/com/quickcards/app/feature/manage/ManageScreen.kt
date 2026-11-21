package com.quickcards.app.feature.manage

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quickcards.app.R
import com.quickcards.app.core.ui.components.QcChevron
import com.quickcards.app.core.ui.components.QcGroup
import com.quickcards.app.core.ui.components.QcListRow
import com.quickcards.app.core.ui.components.QcScreenBar
import com.quickcards.app.core.ui.entity.titleRes
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTheme
import com.quickcards.domain.EntityKind

private val Destinations: List<Pair<EntityKind, ImageVector>> = listOf(
    EntityKind.HOLDER to QcIcons.person,
    EntityKind.BANK to QcIcons.bank,
    EntityKind.NETWORK to QcIcons.network,
    EntityKind.TYPE to QcIcons.type,
    EntityKind.TAG to QcIcons.tag
)

/** Manage: the five lists with their counts, then Archived on its own. */
@Composable
fun ManageScreen(
    onOpenKind: (EntityKind) -> Unit,
    onOpenArchived: () -> Unit,
    bottomInset: Dp = 0.dp,
    viewModel: ManageViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val counts = state.counts()
    val loaded = !state.loading

    Column(modifier = Modifier.fillMaxSize()) {
        QcScreenBar(title = stringResource(R.string.manage_title))
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = QcSpacing.s4)
                .padding(top = QcSpacing.s2, bottom = QcSpacing.s6 + bottomInset),
            verticalArrangement = Arrangement.spacedBy(QcSpacing.s6)
        ) {
            QcGroup {
                Destinations.forEach { (kind, icon) ->
                    ManageRow(
                        icon = icon,
                        title = stringResource(kind.titleRes),
                        count = if (loaded) counts.count(kind) else null,
                        onClick = { onOpenKind(kind) }
                    )
                }
            }
            QcGroup {
                ManageRow(
                    icon = QcIcons.inventory2,
                    title = stringResource(R.string.manage_list_archived),
                    count = if (loaded) state.archivedCount else null,
                    onClick = onOpenArchived
                )
            }
        }
    }
}

@Composable
private fun ManageRow(icon: ImageVector, title: String, count: Int?, onClick: () -> Unit) {
    QcListRow(
        title = title,
        icon = icon,
        onClick = onClick,
        trailing = {
            if (count != null) {
                Text(text = count.toString(), style = QcText.F3.copy(fontFeatureSettings = "tnum"), color = QcTheme.colors.ink)
            }
            QcChevron()
        }
    )
}
