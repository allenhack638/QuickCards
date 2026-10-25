package com.quickcards.app.core.autofill

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.quickcards.app.R
import com.quickcards.app.core.ui.components.NetworkMark
import com.quickcards.app.core.ui.components.NetworkMarks
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcType
import com.quickcards.app.core.ui.components.SearchTopBar
import com.quickcards.domain.CardFilters
import com.quickcards.domain.CardQuery
import com.quickcards.domain.CardQueryEntry

internal const val PICKER_ROW = "autofill_picker_row"

/** One card as the picker lists it; the number stays in memory only for search. */
data class PickerCard(
    val id: String,
    val bankName: String,
    val typeName: String,
    val holderName: String,
    val number: String,
    val expiry: String,
    val tagNames: List<String>,
    val networkId: String?,
    val expired: Boolean,
    val networkName: String = "",
    val networkCode: String = "",
    val note: String = ""
) {
    val title: String get() = if (typeName.isBlank()) bankName else "$bankName $typeName"

    internal fun entry() = CardQueryEntry(
        id = id,
        number = number,
        bankId = null,
        bankName = bankName,
        holderId = null,
        holderName = holderName,
        typeId = null,
        typeName = typeName,
        networkId = networkId,
        networkName = networkName,
        networkCode = networkCode,
        note = note,
        tagIds = emptyList(),
        tagNames = tagNames
    )
}

/**
 * Every card, searchable the same way as Home (digits anywhere in the number, bank, holder,
 * network, tag, note). Shown only after the unlock; tapping a card fills the form.
 */
@Composable
fun AutofillPickerContent(
    cards: List<PickerCard>,
    requester: String?,
    onPick: (String) -> Unit,
    onCancel: () -> Unit
) {
    var query by remember { mutableStateOf("") }
    val byId = remember(cards) { cards.associateBy { it.id } }
    val entries = remember(cards) { cards.map { it.entry() } }
    val visible = remember(entries, query) {
        CardQuery.visibleCards(entries, CardFilters(), query).mapNotNull { byId[it.id] }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        SearchTopBar(query = query, onQueryChange = { query = it }, onBack = onCancel)
        Text(
            text = if (requester.isNullOrBlank()) {
                stringResource(R.string.autofill_picker_title)
            } else {
                stringResource(R.string.autofill_picker_for, requester)
            },
            style = QcType.Label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = QcDimens.Gutter, vertical = QcDimens.Base)
        )
        if (visible.isEmpty()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = QcDimens.Gutter, vertical = QcDimens.Section),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.autofill_picker_empty),
                    style = QcType.Title,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.height(QcDimens.Base))
                Text(
                    text = stringResource(R.string.autofill_picker_empty_hint),
                    style = QcType.Body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )
            }
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(visible, key = { it.id }) { card ->
                    PickerRow(card = card, onClick = { onPick(card.id) })
                }
            }
        }
    }
}

@Composable
private fun PickerRow(card: PickerCard, onClick: () -> Unit) {
    val subtitle = buildList {
        card.holderName.takeIf { it.isNotBlank() }?.let(::add)
        add(stringResource(R.string.autofill_picker_last_four, card.number.takeLast(4)))
        if (card.tagNames.isNotEmpty()) add(card.tagNames.joinToString(", "))
    }.joinToString(" · ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(PICKER_ROW)
            .clickable(onClick = onClick)
            .heightIn(min = QcDimens.ListRowTwoLine)
            .padding(horizontal = QcDimens.Gutter, vertical = QcDimens.Base),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(QcDimens.Gutter)
    ) {
        Box(modifier = Modifier.width(QcDimens.NetworkBadgeMinWidth), contentAlignment = Alignment.CenterStart) {
            val mark = NetworkMarks.drawable(card.networkId)
            if (mark != null) {
                NetworkMark(resId = mark, contentDescription = card.networkName.ifBlank { null })
            } else if (card.networkCode.isNotBlank()) {
                Box(
                    modifier = Modifier
                        .height(QcDimens.NetworkBadgeHeight)
                        .border(QcDimens.Divider, MaterialTheme.colorScheme.outline, RoundedCornerShape(QcDimens.CornerBadge))
                        .padding(horizontal = QcDimens.Half),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = card.networkCode,
                        style = QcType.Label,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = card.title,
                style = QcType.BodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = subtitle,
                style = QcType.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (card.expired) {
            Text(
                text = stringResource(R.string.autofill_picker_expired).uppercase(),
                style = QcType.FieldLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
