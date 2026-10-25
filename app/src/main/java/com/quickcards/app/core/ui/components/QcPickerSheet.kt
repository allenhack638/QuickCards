package com.quickcards.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickcards.app.core.ui.components.FormTextField
import com.quickcards.app.core.ui.components.MonogramTile
import com.quickcards.app.core.ui.components.SheetRow
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcType
import com.quickcards.domain.Monogram
import java.util.Locale

enum class QcPickerKind { HOLDER, BANK, TYPE }

data class QcPickerEntry(val id: String, val name: String)

/** Picker for holder, bank and type. Type lists start with "None"; search shows above seven items. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QcPickerSheet(
    kind: QcPickerKind,
    title: String,
    searchLabel: String,
    entries: List<QcPickerEntry>,
    selectedId: String?,
    addLabel: String,
    onDismiss: () -> Unit,
    onPick: (String?) -> Unit,
    onAdd: () -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var query by remember { mutableStateOf("") }
    val showSearch = entries.size > 7
    val filtered = if (!showSearch || query.isBlank()) {
        entries
    } else {
        entries.filter { it.name.contains(query.trim(), ignoreCase = true) }
    }

    QcModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight * 0.86f)
            ) {
                Text(
                    text = title,
                    style = QcType.Headline,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 24.dp, end = 24.dp, top = QcDimens.SheetTitleTop, bottom = 8.dp)
                )
                if (showSearch) {
                    FormTextField(
                        value = query,
                        onValueChange = { query = it },
                        label = searchLabel,
                        helper = "",
                        modifier = Modifier.padding(horizontal = 24.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    if (kind == QcPickerKind.TYPE) {
                        item(key = "__none") {
                            PickerRow(
                                kind = kind,
                                name = "None",
                                selected = selectedId == null,
                                isNone = true,
                                onClick = { onPick(null) }
                            )
                        }
                    }
                    items(filtered, key = { it.id }) { entry ->
                        PickerRow(
                            kind = kind,
                            name = entry.name,
                            selected = entry.id == selectedId,
                            onClick = { onPick(entry.id) }
                        )
                    }
                    item(key = "__add") {
                        SheetRow(
                            title = addLabel,
                            onClick = onAdd,
                            lead = {
                                Icon(
                                    imageVector = QcIcons.add,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(QcDimens.SheetBottom))
            }
        }
    }
}

@Composable
private fun PickerRow(
    kind: QcPickerKind,
    name: String,
    selected: Boolean,
    onClick: () -> Unit,
    isNone: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        PickerLead(kind = kind, name = name, isNone = isNone)
        Spacer(modifier = Modifier.size(width = 16.dp, height = 1.dp))
        Text(
            text = name,
            style = QcType.BodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (selected) {
            Icon(
                imageVector = QcIcons.check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun PickerLead(kind: QcPickerKind, name: String, isNone: Boolean = false) {
    val background = MaterialTheme.colorScheme.surfaceContainerHighest
    Box(
        modifier = Modifier.size(56.dp),
        contentAlignment = Alignment.Center
    ) {
        when (kind) {
            QcPickerKind.HOLDER -> Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(background),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = Monogram.initials(name),
                    fontSize = 13.sp,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight(700),
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1
                )
            }

            QcPickerKind.BANK -> MonogramTile(Monogram.monoFor(name))

            QcPickerKind.TYPE -> Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(background),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isNone) "–" else {
                        name.trim().firstOrNull()?.uppercase(Locale.ROOT) ?: "–"
                    },
                    fontSize = 16.sp,
                    lineHeight = 20.sp,
                    fontWeight = FontWeight(700),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
    }
}
