package com.quickcards.app.feature.cardform

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import com.quickcards.app.R
import com.quickcards.app.core.ui.LockGate
import com.quickcards.app.core.ui.components.CheckRow
import com.quickcards.app.core.ui.components.FormHelperText
import com.quickcards.app.core.ui.components.FormTextField
import com.quickcards.app.core.ui.components.QcModalBottomSheet
import com.quickcards.app.core.ui.components.QcPickerEntry
import com.quickcards.app.core.ui.components.QcPickerKind
import com.quickcards.app.core.ui.components.QcPickerSheet
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcType
import kotlinx.coroutines.launch

@Composable
internal fun HolderPickerSheet(
    session: CardFormSession,
    visible: Boolean,
    locked: Boolean,
    onDismiss: () -> Unit,
    onPick: (String?) -> Unit,
    onAdd: () -> Unit
) {
    if (!LockGate.visible(visible, locked)) return
    QcPickerSheet(
        kind = QcPickerKind.HOLDER,
        title = stringResource(R.string.form_picker_holder_title),
        searchLabel = stringResource(R.string.form_picker_holder_search),
        entries = session.lists.holders.map { QcPickerEntry(it.id, it.name) },
        selectedId = session.form.holderId,
        addLabel = stringResource(R.string.form_picker_holder_add),
        onDismiss = onDismiss,
        onPick = onPick,
        onAdd = onAdd
    )
}

@Composable
internal fun BankPickerSheet(
    session: CardFormSession,
    visible: Boolean,
    locked: Boolean,
    onDismiss: () -> Unit,
    onPick: (String?) -> Unit,
    onAdd: () -> Unit
) {
    if (!LockGate.visible(visible, locked)) return
    QcPickerSheet(
        kind = QcPickerKind.BANK,
        title = stringResource(R.string.form_picker_bank_title),
        searchLabel = stringResource(R.string.form_picker_bank_search),
        entries = session.lists.banks.map { QcPickerEntry(it.id, it.name) },
        selectedId = session.form.bankId,
        addLabel = stringResource(R.string.form_picker_bank_add),
        onDismiss = onDismiss,
        onPick = onPick,
        onAdd = onAdd
    )
}

@Composable
internal fun TypePickerSheet(
    session: CardFormSession,
    visible: Boolean,
    locked: Boolean,
    onDismiss: () -> Unit,
    onPick: (String?) -> Unit,
    onAdd: () -> Unit
) {
    if (!LockGate.visible(visible, locked)) return
    QcPickerSheet(
        kind = QcPickerKind.TYPE,
        title = stringResource(R.string.form_picker_type_title),
        searchLabel = stringResource(R.string.form_picker_type_search),
        entries = session.lists.types.map { QcPickerEntry(it.id, it.name) },
        selectedId = session.form.typeId,
        addLabel = stringResource(R.string.form_picker_type_add),
        onDismiss = onDismiss,
        onPick = onPick,
        onAdd = onAdd
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TagSheet(
    session: CardFormSession,
    visible: Boolean,
    locked: Boolean,
    onDismiss: () -> Unit,
    onToggle: (String) -> Unit,
    onAdd: suspend (String) -> com.quickcards.app.core.data.model.Tag?,
    onTagCreated: (com.quickcards.app.core.data.model.Tag) -> Unit
) {
    if (!LockGate.visible(visible, locked)) return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val lists = session.lists
    val max = lists.tagLimit
    val maxLen = lists.tagMaxLen
    val scope = rememberCoroutineScope()
    var newTag by remember { mutableStateOf(TextFieldValue("")) }
    var error by remember { mutableStateOf<String?>(null) }
    val newTagFocus = remember { FocusRequester() }

    val emptyError = stringResource(R.string.form_tag_error_empty)
    val shortError = stringResource(R.string.form_tag_error_short)
    val limitError = pluralStringResource(R.plurals.form_tag_error_limit, max, max)
    val duplicateError = stringResource(R.string.form_tag_error_duplicate)

    fun submit() {
        val name = newTag.text.trim().replace(Regex("\\s+"), " ").replace(Regex("^#+"), "")
        error = when {
            name.isEmpty() -> emptyError
            name.length < 2 -> shortError
            session.form.tagIds.size >= max -> limitError
            else -> {
                val existing = lists.tags.firstOrNull { it.name.equals(name, ignoreCase = true) }
                if (existing != null && existing.id in session.form.tagIds) {
                    duplicateError
                } else {
                    null
                }
            }
        }
        if (error != null) return
        val existing = lists.tags.firstOrNull { it.name.equals(name, ignoreCase = true) }
        if (existing != null) {
            onToggle(existing.id)
            newTag = TextFieldValue("")
            error = null
            return
        }
        scope.launch {
            val tag = onAdd(name)
            if (tag != null) {
                onTagCreated(tag)
                newTag = TextFieldValue("")
                error = null
                runCatching { newTagFocus.requestFocus() }
            }
        }
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
                    .heightIn(max = maxHeight * QcDimens.SheetMaxHeightFraction)
            ) {
                Text(
                    text = stringResource(R.string.form_group_tags),
                    style = QcType.Headline,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(
                        start = QcDimens.Section,
                        end = QcDimens.Section,
                        top = QcDimens.SheetTitleTop,
                        bottom = QcDimens.Base
                    )
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = QcDimens.Section),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(modifier = Modifier.weight(1f)) {
                        FormTextField(
                            value = newTag,
                            onValueChange = { input ->
                                val next = input.text.take(maxLen)
                                newTag = TextFieldValue(next, TextRange(next.length))
                                if (error != null) error = null
                            },
                            label = stringResource(R.string.form_field_new_tag),
                            helper = "",
                            error = error,
                            counterMax = maxLen,
                            keyboardOptions = KeyboardOptions(
                                capitalization = KeyboardCapitalization.None
                            ),
                            modifier = Modifier.focusRequester(newTagFocus)
                        )
                    }
                    Spacer(modifier = Modifier.width(QcDimens.Base))
                    Button(
                        onClick = ::submit,
                        // OutlinedTextField reserves 8dp above its box for the floating
                        // label; offset the button so it centres on the box itself.
                        modifier = Modifier
                            .padding(top = 8.dp)
                            .height(QcDimens.FieldHeight),
                        shape = CircleShape,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = QcDimens.Section
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.form_action_add),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                Text(
                    text = pluralStringResource(
                        R.plurals.form_tags_selected,
                        session.form.tagIds.size,
                        session.form.tagIds.size,
                        max
                    ),
                    style = FormHelperText,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(
                        start = QcDimens.Section,
                        end = QcDimens.Section,
                        top = QcDimens.Half,
                        bottom = QcDimens.Base
                    )
                )
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(lists.tags, key = { it.id }) { tag ->
                        val checked = tag.id in session.form.tagIds
                        val enabled = checked || session.form.tagIds.size < max
                        CheckRow(
                            label = tag.name,
                            checked = checked,
                            enabled = enabled,
                            onCheckedChange = { onToggle(tag.id) }
                        )
                    }
                }
                HorizontalDivider(
                    thickness = QcDimens.Divider,
                    color = MaterialTheme.colorScheme.outlineVariant
                )
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = QcDimens.Section,
                            end = QcDimens.Section,
                            top = QcDimens.PassGap,
                            bottom = QcDimens.Half
                        ),
                    horizontalArrangement = Arrangement.End
                ) {
                    Button(
                        onClick = onDismiss,
                        shape = CircleShape,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(
                            horizontal = QcDimens.Section
                        ),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(
                            text = stringResource(R.string.form_action_done),
                            style = MaterialTheme.typography.labelLarge
                        )
                    }
                }
                Spacer(modifier = Modifier.height(QcDimens.SheetBottom))
            }
        }
    }
}
