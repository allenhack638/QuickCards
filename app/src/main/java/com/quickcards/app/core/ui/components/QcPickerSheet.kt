package com.quickcards.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcShapes
import com.quickcards.app.core.ui.theme.QcSize
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTheme
import com.quickcards.domain.Monogram

enum class QcPickerKind { HOLDER, BANK, TYPE }

/** [colour] leads a bank row with that bank's card colour instead of its monogram. */
data class QcPickerEntry(val id: String, val name: String, val colour: String? = null)

/**
 * The one picker for holder, bank and type: title, an always-visible search field, radio rows
 * with a check on the choice, then "New …". Picking applies and closes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QcPickerSheet(
    kind: QcPickerKind,
    title: String,
    searchLabel: String,
    searchDescription: String,
    entries: List<QcPickerEntry>,
    selectedId: String?,
    addLabel: String,
    onDismiss: () -> Unit,
    onPick: (String?) -> Unit,
    onAdd: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    val filtered = if (query.isBlank()) {
        entries
    } else {
        entries.filter { it.name.contains(query.trim(), ignoreCase = true) }
    }

    QcModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight * QcDimens.SheetMaxHeightFraction)
                    .padding(bottom = QcSpacing.s6)
            ) {
                Text(
                    text = title,
                    style = QcText.F4,
                    color = QcTheme.colors.ink,
                    modifier = Modifier
                        .padding(
                            start = QcSpacing.s6,
                            end = QcSpacing.s6,
                            top = QcSpacing.s2,
                            bottom = QcSpacing.s3
                        )
                        .semantics { heading() }
                )
                QcSearchField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = searchLabel,
                    description = searchDescription,
                    modifier = Modifier.padding(start = QcSpacing.s6, end = QcSpacing.s6, bottom = QcSpacing.s2)
                )
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(filtered, key = { it.id }) { entry ->
                        PickerRow(
                            kind = kind,
                            name = entry.name,
                            colour = entry.colour,
                            selected = entry.id == selectedId,
                            onClick = { onPick(entry.id) }
                        )
                    }
                    item(key = "__add") {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = QcSpacing.s6, vertical = QcSpacing.s2)
                                .fillMaxWidth()
                                .height(QcSize.hairline)
                                .background(QcTheme.colors.line)
                        )
                        SheetRow(
                            title = addLabel,
                            onClick = onAdd,
                            lead = { PickerLeadSlot { RowIcon() } }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun RowIcon() {
    Icon(
        imageVector = QcIcons.add,
        contentDescription = null,
        tint = QcTheme.colors.ink2,
        modifier = Modifier.size(QcSize.iconLg)
    )
}

@Composable
private fun PickerRow(
    kind: QcPickerKind,
    name: String,
    colour: String?,
    selected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier.selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
    ) {
        SheetRow(
            title = name,
            lead = when (kind) {
                QcPickerKind.HOLDER -> { { PickerLeadSlot { HolderAvatar(name) } } }
                QcPickerKind.BANK -> if (colour != null) {
                    { PickerLeadSlot { ColourChip(colour) } }
                } else {
                    { PickerLeadSlot { MonogramTile(Monogram.monoFor(name)) } }
                }
                QcPickerKind.TYPE -> null
            },
            trailing = if (selected) {
                {
                    Icon(
                        imageVector = QcIcons.check,
                        contentDescription = null,
                        tint = QcTheme.colors.ink,
                        modifier = Modifier.size(QcSize.iconLg)
                    )
                }
            } else {
                null
            }
        )
    }
}

/** 40 wide, so text lines up across avatar, monogram and icon rows. */
@Composable
private fun PickerLeadSlot(content: @Composable () -> Unit) {
    Box(modifier = Modifier.size(QcSpacing.s10), contentAlignment = Alignment.Center) { content() }
}

@Composable
private fun HolderAvatar(name: String) {
    val colors = QcTheme.colors
    Box(
        modifier = Modifier
            .size(QcSpacing.s10)
            .background(colors.tonalHi, QcShapes.full),
        contentAlignment = Alignment.Center
    ) {
        Text(text = Monogram.initials(name), style = QcText.F2Semibold, color = colors.ink, maxLines = 1)
    }
}
