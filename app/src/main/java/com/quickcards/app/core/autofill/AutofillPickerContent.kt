package com.quickcards.app.core.autofill

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import com.quickcards.app.R
import com.quickcards.app.core.ui.components.MiniCard
import com.quickcards.app.core.ui.components.QcEmptyMessage
import com.quickcards.app.core.ui.components.QcGroup
import com.quickcards.app.core.ui.components.QcGroupLabel
import com.quickcards.app.core.ui.components.QcIconButton
import com.quickcards.app.core.ui.components.QcNestedTopBar
import com.quickcards.app.core.ui.components.SearchTopBar
import com.quickcards.app.core.ui.components.maskedNumberText
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcSize
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTheme
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
    val tagNames: List<String>,
    val networkId: String?,
    val expired: Boolean,
    val band: String,
    val networkName: String = "",
    val networkCode: String = "",
    val note: String = ""
) {
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
 * "Choose card": every card, searchable the same way as Home (digits anywhere in the number,
 * bank, holder, network, tag, note). Shown only after the unlock; tapping a card fills the form.
 */
@Composable
fun AutofillPickerContent(
    cards: List<PickerCard>,
    requester: String?,
    onPick: (String) -> Unit,
    onCancel: () -> Unit
) {
    var searchOn by rememberSaveable { mutableStateOf(false) }
    var query by remember { mutableStateOf("") }
    val byId = remember(cards) { cards.associateBy { it.id } }
    val entries = remember(cards) { cards.map { it.entry() } }
    val visible = remember(entries, query) {
        CardQuery.visibleCards(entries, CardFilters(), query).mapNotNull { byId[it.id] }
    }
    fun closeSearch() {
        searchOn = false
        query = ""
    }
    BackHandler(enabled = searchOn) { closeSearch() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .navigationBarsPadding()
            .imePadding()
    ) {
        if (searchOn) {
            SearchTopBar(query = query, onQueryChange = { query = it }, onBack = ::closeSearch)
        } else {
            QcNestedTopBar(
                onBack = onCancel,
                backLabel = stringResource(R.string.manage_a11y_back),
                title = stringResource(R.string.autofill_picker_title)
            ) {
                QcIconButton(QcIcons.search, stringResource(R.string.cards_search_placeholder), { searchOn = true })
            }
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(start = QcSpacing.s4, end = QcSpacing.s4, bottom = QcSpacing.s6)
        ) {
            if (!requester.isNullOrBlank()) {
                item { QcGroupLabel(stringResource(R.string.autofill_picker_for, requester)) }
            }
            if (visible.isEmpty()) {
                item {
                    QcEmptyMessage(
                        icon = QcIcons.search,
                        title = stringResource(R.string.cards_no_result_title),
                        body = stringResource(R.string.cards_no_result_body),
                        compact = true,
                        actionLabel = stringResource(R.string.cards_no_result_clear),
                        onAction = { query = "" }
                    )
                }
            } else {
                item {
                    QcGroup(modifier = if (requester.isNullOrBlank()) Modifier.padding(top = QcSpacing.s2) else Modifier) {
                        visible.forEach { card -> PickerRow(card = card, onClick = { onPick(card.id) }) }
                    }
                }
            }
        }
    }
}

@Composable
private fun PickerRow(card: PickerCard, onClick: () -> Unit) {
    val colors = QcTheme.colors
    val meta = listOf(card.typeName, card.holderName).filter { it.isNotBlank() }.joinToString(" · ")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag(PICKER_ROW)
            .clickable(onClick = onClick)
            .heightIn(min = QcSize.row2)
            .padding(horizontal = QcSpacing.s4, vertical = QcSpacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(QcSpacing.s4)
    ) {
        MiniCard(band = card.band, networkId = card.networkId, expired = card.expired)
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = maskedNumberText(stringResource(R.string.autofill_suggestion, card.bankName, card.number.takeLast(4))),
                style = QcText.F3,
                color = colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (meta.isNotEmpty()) {
                Text(text = meta, style = QcText.F2, color = colors.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
