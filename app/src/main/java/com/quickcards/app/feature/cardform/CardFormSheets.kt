package com.quickcards.app.feature.cardform

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.TextFieldValue
import com.quickcards.app.R
import com.quickcards.app.core.ui.LockGate
import com.quickcards.app.core.ui.components.CheckRow
import com.quickcards.app.core.ui.components.FormTextField
import com.quickcards.app.core.ui.components.QcModalBottomSheet
import com.quickcards.app.core.ui.components.QcPickerEntry
import com.quickcards.app.core.ui.components.QcPickerKind
import com.quickcards.app.core.ui.components.QcPickerSheet
import com.quickcards.app.core.ui.components.QcSheetActions
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTextButton
import com.quickcards.app.core.ui.theme.QcTheme
import com.quickcards.domain.NamedRef
import kotlinx.coroutines.launch

/** Which picker sheet is open over the form. */
internal enum class FormPicker { HOLDER, BANK, TYPE }

@Composable
internal fun FormPickerSheet(
    picker: FormPicker,
    session: CardFormSession,
    locked: Boolean,
    onDismiss: () -> Unit,
    onPick: (String) -> Unit,
    onAdd: () -> Unit
) {
    if (!LockGate.visible(true, locked)) return
    val lists = session.lists
    val search = stringResource(R.string.form_picker_search)
    when (picker) {
        FormPicker.HOLDER -> QcPickerSheet(
            kind = QcPickerKind.HOLDER,
            title = stringResource(R.string.form_picker_holder_title),
            searchLabel = search,
            searchDescription = stringResource(R.string.form_a11y_search_holders),
            entries = lists.holders.map { QcPickerEntry(it.id, it.name) },
            selectedId = session.form.holderId,
            addLabel = stringResource(R.string.form_picker_holder_add),
            onDismiss = onDismiss,
            onPick = { id -> id?.let(onPick) },
            onAdd = onAdd
        )

        FormPicker.BANK -> QcPickerSheet(
            kind = QcPickerKind.BANK,
            title = stringResource(R.string.form_picker_bank_title),
            searchLabel = search,
            searchDescription = stringResource(R.string.form_a11y_search_banks),
            entries = lists.banks.map { QcPickerEntry(it.id, it.name, lists.bankColours[it.id]) },
            selectedId = session.form.bankId,
            addLabel = stringResource(R.string.form_picker_bank_add),
            onDismiss = onDismiss,
            onPick = { id -> id?.let(onPick) },
            onAdd = onAdd
        )

        FormPicker.TYPE -> QcPickerSheet(
            kind = QcPickerKind.TYPE,
            title = stringResource(R.string.form_picker_type_title),
            searchLabel = search,
            searchDescription = stringResource(R.string.form_a11y_search_types),
            entries = lists.types.map { QcPickerEntry(it.id, it.name) },
            selectedId = session.form.typeId,
            addLabel = stringResource(R.string.form_picker_type_add),
            onDismiss = onDismiss,
            onPick = { id -> id?.let(onPick) },
            onAdd = onAdd
        )
    }
}

/** What the New tag field allows: Add is off while empty or already on the card. */
internal enum class TagInput { EMPTY, SHORT, DUPLICATE, LIMIT, OK }

internal fun tagInput(raw: String, session: CardFormSession): TagInput {
    val name = normalizeTag(raw)
    val lists = session.lists
    val existing = lists.tags.firstOrNull { it.name.equals(name, ignoreCase = true) }
    return when {
        name.isEmpty() -> TagInput.EMPTY
        existing != null && existing.id in session.form.tagIds -> TagInput.DUPLICATE
        name.length < 2 -> TagInput.SHORT
        !session.canAddTag() -> TagInput.LIMIT
        else -> TagInput.OK
    }
}

internal fun normalizeTag(raw: String): String = raw.trim().replace(Regex("\\s+"), " ").replace(Regex("^#+"), "")

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
internal fun TagSheet(
    session: CardFormSession,
    locked: Boolean,
    onDismiss: () -> Unit,
    onToggle: (String) -> Unit,
    onCreate: suspend (String) -> NamedRef?,
    onTagCreated: (NamedRef) -> Unit
) {
    if (!LockGate.visible(true, locked)) return
    val colors = QcTheme.colors
    val lists = session.lists
    val scope = rememberCoroutineScope()
    var newTag by remember { mutableStateOf(TextFieldValue("")) }
    var tried by remember { mutableStateOf(false) }
    val input = tagInput(newTag.text, session)
    val error = when (input) {
        TagInput.DUPLICATE -> stringResource(R.string.form_tag_error_duplicate)
        TagInput.SHORT -> if (tried) stringResource(R.string.form_tag_error_short) else null
        TagInput.LIMIT -> pluralStringResource(R.plurals.form_tag_error_limit, lists.tagLimit, lists.tagLimit)
        TagInput.EMPTY -> if (tried) stringResource(R.string.form_tag_error_empty) else null
        TagInput.OK -> null
    }

    fun submit() {
        tried = true
        if (input != TagInput.OK) return
        val name = normalizeTag(newTag.text)
        val existing = lists.tags.firstOrNull { it.name.equals(name, ignoreCase = true) }
        if (existing != null) {
            onToggle(existing.id)
            newTag = TextFieldValue("")
            tried = false
            return
        }
        scope.launch {
            onCreate(name)?.let { tag ->
                onTagCreated(tag)
                newTag = TextFieldValue("")
                tried = false
            }
        }
    }

    QcModalBottomSheet(onDismissRequest = onDismiss) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = maxHeight)) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = QcSpacing.s6, end = QcSpacing.s6, top = QcSpacing.s2, bottom = QcSpacing.s3),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.form_group_tags),
                        style = QcText.F4,
                        color = colors.ink,
                        modifier = Modifier
                            .weight(1f)
                            .semantics { heading() }
                    )
                    Text(
                        text = stringResource(R.string.form_tags_count, session.form.tagIds.size, lists.tagLimit),
                        style = QcText.F1Regular,
                        color = colors.ink2
                    )
                }
                FormTextField(
                    value = newTag,
                    onValueChange = { value ->
                        val next = value.text.take(lists.tagMaxLen)
                        newTag = TextFieldValue(next, TextRange(next.length))
                        tried = false
                    },
                    label = stringResource(R.string.form_field_new_tag),
                    error = error,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(onDone = { submit() }),
                    trailing = {
                        QcTextButton(
                            text = stringResource(R.string.form_action_add),
                            onClick = ::submit,
                            enabled = input == TagInput.OK
                        )
                    },
                    modifier = Modifier.padding(start = QcSpacing.s6, end = QcSpacing.s6, bottom = QcSpacing.s2)
                )
                LazyColumn(modifier = Modifier.weight(1f, fill = false)) {
                    items(lists.tags, key = { it.id }) { tag ->
                        val checked = tag.id in session.form.tagIds
                        CheckRow(
                            label = tag.name,
                            checked = checked,
                            enabled = checked || session.canAddTag(),
                            onCheckedChange = { onToggle(tag.id) }
                        )
                    }
                }
                QcSheetActions(primary = stringResource(R.string.form_action_done), onPrimary = onDismiss)
            }
        }
    }
}
