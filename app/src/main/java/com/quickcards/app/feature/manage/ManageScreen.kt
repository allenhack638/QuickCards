package com.quickcards.app.feature.manage

import androidx.compose.foundation.clickable
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quickcards.app.R
import com.quickcards.app.core.ui.entity.subtitleRes
import com.quickcards.app.core.ui.entity.titleRes
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcType
import com.quickcards.domain.EntityKind

private data class ManageDestination(
    val kind: EntityKind,
    val icon: ImageVector
)

private val Destinations = listOf(
    ManageDestination(EntityKind.HOLDER, QcIcons.person),
    ManageDestination(EntityKind.BANK, QcIcons.bank),
    ManageDestination(EntityKind.NETWORK, QcIcons.network),
    ManageDestination(EntityKind.TYPE, QcIcons.type),
    ManageDestination(EntityKind.TAG, QcIcons.tag)
)

@Composable
fun ManageScreen(
    onOpenKind: (EntityKind) -> Unit,
    onOpenArchived: () -> Unit,
    bottomInset: Dp = 0.dp,
    viewModel: ManageViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val counts = state.counts()
    val countsLoaded = !state.loading

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(QcDimens.AppBar)
                .padding(start = QcDimens.Gutter, end = QcDimens.Half),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.manage_title),
                style = QcType.ScreenTitle,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            Destinations.forEach { destination ->
                ManageRow(
                    icon = destination.icon,
                    title = stringResource(destination.kind.titleRes),
                    subtitle = stringResource(destination.kind.subtitleRes),
                    count = if (countsLoaded) counts.count(destination.kind) else null,
                    onClick = { onOpenKind(destination.kind) }
                )
            }
            ManageRow(
                icon = QcIcons.inventory2,
                title = stringResource(R.string.manage_list_archived),
                subtitle = stringResource(R.string.manage_archived_subtitle),
                count = if (countsLoaded) state.archivedCount else null,
                onClick = onOpenArchived
            )
            Spacer(modifier = Modifier.height(QcDimens.Section + bottomInset))
        }
    }
}

@Composable
private fun ManageRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    count: Int?,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = QcDimens.ListRowTwoLine)
            .clickable(onClick = onClick)
            .padding(horizontal = QcDimens.Gutter, vertical = QcDimens.Base),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(QcDimens.Gutter)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(QcDimens.Icon)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = QcType.BodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = QcType.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (count != null) {
            Text(
                text = count.toString(),
                style = QcType.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Icon(
            imageVector = QcIcons.chevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(QcDimens.Icon)
        )
    }
}
