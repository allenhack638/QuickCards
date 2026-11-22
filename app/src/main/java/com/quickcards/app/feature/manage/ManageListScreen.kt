package com.quickcards.app.feature.manage

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.quickcards.app.R
import com.quickcards.app.core.data.model.BandColor
import com.quickcards.app.core.data.repository.BankColourRules
import com.quickcards.app.core.ui.theme.QcArtDimens
import com.quickcards.app.core.ui.art.ArtKind
import com.quickcards.app.core.ui.art.CardArt
import com.quickcards.app.core.ui.components.NetworkMark
import com.quickcards.app.core.ui.components.NetworkMarks
import com.quickcards.app.core.ui.components.QcGroup
import com.quickcards.app.core.ui.components.QcIconButton
import com.quickcards.app.core.ui.components.QcNestedTopBar
import com.quickcards.app.core.ui.components.markTone
import com.quickcards.app.core.ui.components.networkLeadWidth
import com.quickcards.app.core.ui.entity.emptyTitleRes
import com.quickcards.app.core.ui.entity.oneRes
import com.quickcards.app.core.ui.entity.titleRes
import com.quickcards.app.core.ui.theme.QcFilledButton
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcShapes
import com.quickcards.app.core.ui.theme.QcSize
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTheme
import com.quickcards.domain.CardNumberRules
import com.quickcards.domain.EntityKind
import com.quickcards.domain.Monogram

/** One Manage list: rows open the editor; + adds. */
@Composable
fun ManageListScreen(
    kind: EntityKind,
    viewModel: ManageViewModel,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (id: String) -> Unit
) {
    val state by viewModel.state.collectAsState()
    val entities = state.list(kind)
    val one = stringResource(kind.oneRes)
    val leadWidth = if (kind == EntityKind.NETWORK) networkLeadSlot(entities) else QcSpacing.s10

    Column(modifier = Modifier.fillMaxSize()) {
        QcNestedTopBar(
            onBack = onBack,
            backLabel = stringResource(R.string.manage_a11y_back),
            title = stringResource(kind.titleRes)
        ) {
            QcIconButton(QcIcons.add, stringResource(R.string.manage_a11y_add, one), onAdd)
        }
        when {
            state.loading -> Spacer(Modifier.weight(1f))
            entities.isEmpty() -> EmptyList(kind, one, onAdd)
            else -> Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = QcSpacing.s4)
                    .padding(top = QcSpacing.s2, bottom = QcSpacing.s6)
                    .navigationBarsPadding()
            ) {
                QcGroup {
                    entities.forEach { entity ->
                        EntityRow(kind, entity, leadWidth) { onEdit(entity.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun EmptyList(kind: EntityKind, one: String, onAdd: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = QcSpacing.s8, vertical = QcSpacing.s16),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(QcSpacing.s4)
    ) {
        Text(
            text = stringResource(kind.emptyTitleRes),
            style = QcText.F4,
            color = QcTheme.colors.ink,
            textAlign = TextAlign.Center
        )
        QcFilledButton(stringResource(R.string.manage_dialog_new, one), onAdd, icon = QcIcons.add)
    }
}

/** Every network row shares one lead width: the 40 mark slot, wider only for a long custom label. */
@Composable
private fun networkLeadSlot(entities: List<ManagedEntity>): Dp {
    val density = LocalDensity.current
    val measurer = rememberTextMeasurer()
    val widths = entities.map { entity ->
        if (NetworkMarks.drawable(entity.network?.id) != null) {
            QcArtDimens.SlotWidth
        } else {
            with(density) {
                measurer.measure(entity.network?.code.orEmpty(), QcText.F1Semibold).size.width.toDp() + QcSpacing.s2 * 2
            }
        }
    }
    return networkLeadWidth(widths, QcArtDimens.SlotWidth, QcSize.row2)
}

@Composable
private fun EntityRow(kind: EntityKind, entity: ManagedEntity, leadWidth: Dp, onClick: () -> Unit) {
    val colors = QcTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = QcSize.row2)
            .clickable(onClick = onClick)
            .padding(horizontal = QcSpacing.s4, vertical = QcSpacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(QcSpacing.s4)
    ) {
        Box(modifier = Modifier.width(leadWidth), contentAlignment = Alignment.Center) {
            EntityLead(kind, entity)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entity.name,
                style = QcText.F3,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(text = usageSubtitle(kind, entity), style = QcText.F2, color = colors.ink2)
        }
    }
}

@Composable
private fun usageSubtitle(kind: EntityKind, entity: ManagedEntity): String {
    val usage = if (entity.usageCount > 0) {
        pluralStringResource(R.plurals.manage_usage_cards, entity.usageCount, entity.usageCount)
    } else {
        stringResource(R.string.manage_usage_not_used)
    }
    val network = entity.network
    if (kind != EntityKind.NETWORK || network == null) return usage
    return stringResource(
        if (network.cvvOptional) R.string.manage_usage_network_cvv_optional else R.string.manage_usage_network,
        CardNumberRules.lengthsText(network.lengths),
        network.cvvLength,
        usage
    )
}

@Composable
private fun EntityLead(kind: EntityKind, entity: ManagedEntity) {
    val colors = QcTheme.colors
    when (kind) {
        EntityKind.HOLDER -> Box(
            modifier = Modifier
                .size(QcSpacing.s10)
                .background(colors.tonal, QcShapes.full),
            contentAlignment = Alignment.Center
        ) {
            Text(text = Monogram.initials(entity.name), style = QcText.F2Semibold, color = colors.ink, maxLines = 1)
        }

        EntityKind.BANK -> CardArt(
            band = entity.bank?.let(BankColourRules::colourOf) ?: BandColor.GRAPHITE,
            kind = ArtKind.MINI,
            modifier = Modifier.size(QcSize.ctl, QcSize.statusChip)
        )

        EntityKind.NETWORK -> {
            val id = entity.network?.id
            if (id != null && NetworkMarks.drawable(id) != null) {
                NetworkMark(networkId = id, contentDescription = null, tone = markTone(id, onArt = false, dark = colors.dark))
            } else {
                Box(
                    modifier = Modifier
                        .widthIn(min = QcArtDimens.SlotWidth)
                        .heightIn(min = QcArtDimens.SlotHeight)
                        .border(QcSize.hairline, colors.outline, QcShapes.xs)
                        .padding(horizontal = QcSpacing.s2),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = entity.network?.code.orEmpty(),
                        style = QcText.F1Semibold,
                        color = colors.ink,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        EntityKind.TYPE, EntityKind.TAG -> Icon(
            imageVector = if (kind == EntityKind.TAG) QcIcons.tag else QcIcons.type,
            contentDescription = null,
            tint = colors.ink2,
            modifier = Modifier.size(QcSize.iconLg)
        )
    }
}
