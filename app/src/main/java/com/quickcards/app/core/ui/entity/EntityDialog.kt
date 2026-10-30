@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.quickcards.app.core.ui.entity

import androidx.compose.foundation.background
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.border
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.error as semanticsError
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quickcards.app.R
import com.quickcards.app.core.data.settings.AppSettingsDefaults
import com.quickcards.app.core.security.AppLockController
import com.quickcards.app.core.ui.components.FormSupportingLine
import com.quickcards.app.core.ui.components.LocalSnackbar
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcType
import com.quickcards.app.core.ui.theme.qcPrimaryButtonColors
import com.quickcards.domain.CardNetwork as RuleNetwork
import com.quickcards.domain.CardNumberRules
import com.quickcards.domain.DefaultNetworks
import com.quickcards.domain.EntityBefore
import com.quickcards.domain.EntityDraft
import com.quickcards.domain.EntityField
import com.quickcards.domain.EntityKind
import com.quickcards.domain.EntityUsage
import com.quickcards.domain.EntityValidationError
import com.quickcards.domain.EntityValidator
import com.quickcards.domain.NetworkLabelMax
import com.quickcards.domain.NetworkRule
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.roundToInt

private val EmphasisLabel = QcType.Body.copy(fontWeight = FontWeight(600))

private data class PendingUpdate(
    val draft: EntityDraft,
    val nextName: String,
    val usedCards: List<ManageCardItem>
)

/** Add or edit one list item. Calls onDismiss before delivering the saved id via onSaved. */
@Composable
fun EntityDialog(
    kind: EntityKind,
    editingId: String?,
    onDismiss: () -> Unit,
    onSaved: (id: String) -> Unit
) {
    val viewModel: EntityEditorViewModel = viewModel()
    val data by viewModel.data.collectAsState()

    if (!data.ready) return

    val before = remember(kind, editingId, data) { data.before(kind, editingId) }
    EntityDialogContent(
        kind = kind,
        editingId = editingId,
        before = before,
        data = data,
        tagMaxLen = AppSettingsDefaults.TAGS_MAX_LEN,
        onDismiss = onDismiss,
        onSaved = onSaved,
        save = { draft -> viewModel.save(kind, editingId, draft) }
    )
}

@Composable
private fun EntityDialogContent(
    kind: EntityKind,
    editingId: String?,
    before: EntityBefore?,
    data: EntityEditorData,
    tagMaxLen: Int,
    onDismiss: () -> Unit,
    onSaved: (id: String) -> Unit,
    save: suspend (EntityDraft) -> Result<String>
) {
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()

    val nameMax = EntityValidator.nameLimit(kind, tagMaxLen)
    val initialName = before?.name.orEmpty()
    val initialCode = before?.code.orEmpty()
    val initialPrefixes = before?.prefixes.orEmpty()
    val initialLengths = before?.lengths ?: "16"

    var name by remember {
        mutableStateOf(TextFieldValue(initialName, TextRange(initialName.length)))
    }
    var code by remember {
        mutableStateOf(TextFieldValue(initialCode, TextRange(initialCode.length)))
    }
    var prefixes by remember {
        mutableStateOf(TextFieldValue(initialPrefixes, TextRange(initialPrefixes.length)))
    }
    var lengths by remember {
        mutableStateOf(TextFieldValue(initialLengths, TextRange(initialLengths.length)))
    }
    var cvvLength by remember { mutableIntStateOf(before?.cvvLength ?: 3) }
    var luhn by remember { mutableStateOf(before?.luhn ?: true) }
    var errors by remember {
        mutableStateOf<Map<EntityField, EntityValidationError>>(emptyMap())
    }
    var pending by remember { mutableStateOf<PendingUpdate?>(null) }
    var rulesWarning by remember { mutableStateOf<EntityDraft?>(null) }
    var saving by remember { mutableStateOf(false) }

    val one = stringResource(kind.oneRes)
    val oneTitle = stringResource(kind.oneTitleRes)
    val addedMessage = stringResource(R.string.manage_entity_added, oneTitle)
    val updatedMessage = stringResource(R.string.manage_entity_updated, oneTitle)
    val saveFailedMessage = stringResource(R.string.manage_save_failed, one)

    val dirty = before == null ||
        name.text != initialName ||
        code.text != initialCode ||
        prefixes.text != initialPrefixes ||
        lengths.text != initialLengths ||
        cvvLength != (before?.cvvLength ?: 3) ||
        luhn != (before?.luhn ?: true)

    // Built-in networks only: compare the form and the saved rules with the standard ones.
    val standard = if (kind == EntityKind.NETWORK) DefaultNetworks.standard(editingId) else null
    fun rulesOf(p: String, l: String, cvv: Int, check: Boolean): Set<NetworkRule> =
        standard?.let {
            DefaultNetworks.changedRules(it.copy(prefixes = p, lengths = l, cvvLength = cvv, luhn = check))
        }.orEmpty()
    val changedNow = rulesOf(prefixes.text, lengths.text, cvvLength, luhn)
    val changedBefore = before?.let {
        rulesOf(it.prefixes, it.lengths, it.cvvLength, it.luhn)
    }.orEmpty()

    val nameFocus = remember { FocusRequester() }
    val codeFocus = remember { FocusRequester() }
    val prefixesFocus = remember { FocusRequester() }
    val lengthsFocus = remember { FocusRequester() }
    val dialogScroll = rememberScrollState()
    val positions = remember { mutableStateMapOf<EntityField, Int>() }
    val errorScrollOffset = with(LocalDensity.current) { QcDimens.Section.roundToPx() }

    LaunchedEffect(Unit) {
        delay(80)
        nameFocus.requestFocus()
    }

    fun normalizedDraft(): EntityDraft = EntityDraft(
        kind = kind,
        name = name.text,
        code = code.text,
        prefixes = prefixes.text,
        lengths = lengths.text,
        cvvLength = cvvLength,
        luhn = luhn
    )

    fun saveNow(draft: EntityDraft) {
        if (saving) return
        saving = true
        scope.launch {
            val result = save(draft)
            saving = false
            result.onSuccess { id ->
                onDismiss()
                onSaved(id)
                snackbar.show(if (before == null) addedMessage else updatedMessage)
            }.onFailure {
                snackbar.show(saveFailedMessage)
            }
        }
    }

    fun confirmUsageThenSave(draft: EntityDraft) {
        val used = data.usedCards(kind, editingId)
        if (EntityUsage.requiresUpdateConfirmation(kind, before, draft, used.size)) {
            pending = PendingUpdate(
                draft = draft,
                nextName = EntityValidator.normalizeName(kind, draft.name),
                usedCards = used.map { it.toManageCardItem() }
            )
            return
        }
        saveNow(draft)
    }

    fun attemptSave() {
        val draft = normalizedDraft()
        val found = EntityValidator.validate(draft, data.refs(kind), editingId)
        errors = found
        if (found.isNotEmpty()) {
            val first = found.keys.first()
            when (first) {
                EntityField.NAME -> {
                    name = name.copy(selection = TextRange(name.text.length))
                    nameFocus.requestFocus()
                }

                EntityField.CODE -> {
                    code = code.copy(selection = TextRange(code.text.length))
                    codeFocus.requestFocus()
                }

                EntityField.PREFIXES -> {
                    prefixes = prefixes.copy(selection = TextRange(prefixes.text.length))
                    prefixesFocus.requestFocus()
                }

                EntityField.LENGTHS -> {
                    lengths = lengths.copy(selection = TextRange(lengths.text.length))
                    lengthsFocus.requestFocus()
                }
            }
            scope.launch {
                delay(60)
                val target = if (first == EntityField.NAME) {
                    0
                } else {
                    ((positions[first] ?: 0) - errorScrollOffset).coerceAtLeast(0)
                }
                dialogScroll.animateScrollTo(target)
            }
            return
        }
        if ((changedNow - changedBefore).isNotEmpty()) {
            rulesWarning = draft
            return
        }
        confirmUsageThenSave(draft)
    }

    val rulesDraft = rulesWarning
    if (rulesDraft != null && standard != null) {
        val networkName = initialName.ifBlank { standard.name }
        ManageDialogFrame(onDismissRequest = onDismiss) {
            Text(
                text = stringResource(R.string.manage_rules_title, networkName),
                style = QcType.DialogTitle,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(QcDimens.Gutter))
            WarningPanel(
                text = AnnotatedString(
                    stringResource(
                        R.string.manage_rules_body,
                        networkName,
                        standardRulesText(standard, changedNow - changedBefore)
                    )
                )
            )
            DialogActions(
                onCancel = { rulesWarning = null },
                confirmLabel = stringResource(R.string.manage_rules_confirm),
                onConfirm = {
                    rulesWarning = null
                    confirmUsageThenSave(rulesDraft)
                },
                confirmEnabled = !saving,
                confirmPrimary = true
            )
        }
        return
    }

    val update = pending
    if (update != null) {
        val usedCount = update.usedCards.size
        val cardsText = pluralStringResource(
            R.plurals.manage_usage_cards,
            usedCount,
            usedCount
        )
        ManageDialogFrame(onDismissRequest = onDismiss) {
            Text(
                text = stringResource(R.string.manage_update_title, cardsText),
                style = QcType.DialogTitle,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(QcDimens.Gutter))
            WarningPanel(
                text = updateWarningText(
                    beforeName = before?.name.orEmpty(),
                    nextName = update.nextName,
                    usedCount = usedCount
                )
            )
            Spacer(modifier = Modifier.height(QcDimens.PassGap))
            ManageCardList(update.usedCards)
            DialogActions(
                onCancel = onDismiss,
                confirmLabel = stringResource(R.string.manage_update_confirm, cardsText),
                onConfirm = {
                    pending = null
                    saveNow(update.draft)
                },
                confirmEnabled = !saving,
                confirmPrimary = true
            )
        }
        return
    }

    ManageDialogFrame(
        onDismissRequest = onDismiss,
        scrollState = dialogScroll,
        actions = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(stringResource(R.string.manage_action_cancel))
            }
            if (before == null) {
                TextButton(
                    onClick = ::attemptSave,
                    enabled = !saving,
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(stringResource(R.string.manage_action_add))
                }
            } else {
                Button(
                    onClick = ::attemptSave,
                    enabled = !saving && dirty,
                    shape = CircleShape,
                    contentPadding = PaddingValues(horizontal = QcDimens.Section),
                    colors = qcPrimaryButtonColors()
                ) {
                    Text(stringResource(R.string.manage_action_save))
                }
            }
        }
    ) {
        Text(
            text = stringResource(
                if (before == null) R.string.manage_dialog_new else R.string.manage_dialog_edit,
                one
            ),
            style = QcType.DialogTitle,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(QcDimens.Gutter))

        EntityTextField(
            value = name,
            onValueChange = { value ->
                val text = value.text.take(nameMax)
                name = TextFieldValue(text, TextRange(text.length))
                if (errors.containsKey(EntityField.NAME)) {
                    errors = errors - EntityField.NAME
                }
            },
            label = stringResource(R.string.manage_field_name),
            helper = if (kind == EntityKind.TAG) {
                stringResource(R.string.manage_helper_tag_no_hash)
            } else {
                ""
            },
            error = errors[EntityField.NAME]?.let { entityErrorText(it, kind) },
            counter = stringResource(R.string.manage_counter, name.text.length, nameMax),
            keyboardOptions = if (kind == EntityKind.TAG) {
                KeyboardOptions(capitalization = KeyboardCapitalization.None)
            } else {
                KeyboardOptions(capitalization = KeyboardCapitalization.Words)
            },
            modifier = Modifier
                .focusRequester(nameFocus)
                .onGloballyPositioned {
                    positions[EntityField.NAME] = it.positionInParent().y.roundToInt()
                }
        )

        if (kind == EntityKind.NETWORK) {
            Spacer(modifier = Modifier.height(10.dp))
            EntityTextField(
                value = code,
                onValueChange = { value ->
                    val text = value.text.uppercase(Locale.ROOT).take(NetworkLabelMax)
                    code = TextFieldValue(text, TextRange(text.length))
                    if (errors.containsKey(EntityField.CODE)) {
                        errors = errors - EntityField.CODE
                    }
                },
                label = stringResource(R.string.manage_field_code),
                helper = stringResource(R.string.manage_helper_code),
                error = errors[EntityField.CODE]?.let { entityErrorText(it, kind) },
                counter = null,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Characters,
                    keyboardType = KeyboardType.Ascii
                ),
                modifier = Modifier
                    .focusRequester(codeFocus)
                    .onGloballyPositioned {
                        positions[EntityField.CODE] = it.positionInParent().y.roundToInt()
                    }
            )
            Spacer(modifier = Modifier.height(10.dp))
            EntityTextField(
                value = prefixes,
                onValueChange = { value ->
                    prefixes = value
                    if (errors.containsKey(EntityField.PREFIXES)) {
                        errors = errors - EntityField.PREFIXES
                    }
                },
                label = stringResource(R.string.manage_field_prefixes),
                helper = stringResource(R.string.manage_helper_prefixes),
                error = errors[EntityField.PREFIXES]?.let { entityErrorText(it, kind) },
                counter = null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .focusRequester(prefixesFocus)
                    .onGloballyPositioned {
                        positions[EntityField.PREFIXES] = it.positionInParent().y.roundToInt()
                    }
            )
            Spacer(modifier = Modifier.height(10.dp))
            EntityTextField(
                value = lengths,
                onValueChange = { value ->
                    lengths = value
                    if (errors.containsKey(EntityField.LENGTHS)) {
                        errors = errors - EntityField.LENGTHS
                    }
                },
                label = stringResource(R.string.manage_field_lengths),
                helper = stringResource(R.string.manage_helper_lengths),
                error = errors[EntityField.LENGTHS]?.let { entityErrorText(it, kind) },
                counter = null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier
                    .focusRequester(lengthsFocus)
                    .onGloballyPositioned {
                        positions[EntityField.LENGTHS] = it.positionInParent().y.roundToInt()
                    }
            )
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = stringResource(R.string.manage_group_cvv),
                style = EmphasisLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(10.dp))
            CvvSegmented(
                selected = cvvLength,
                onSelect = { cvvLength = it }
            )
            Spacer(modifier = Modifier.height(QcDimens.Base))
            LuhnCheckbox(
                checked = luhn,
                onToggle = { luhn = !luhn },
                label = stringResource(R.string.manage_luhn)
            )
            if (standard != null && changedNow.isNotEmpty()) {
                StandardRulesRow(
                    text = stringResource(
                        R.string.manage_rules_changed,
                        initialName.ifBlank { standard.name }
                    ),
                    onReset = {
                        prefixes = TextFieldValue(standard.prefixes, TextRange(standard.prefixes.length))
                        lengths = TextFieldValue(standard.lengths, TextRange(standard.lengths.length))
                        cvvLength = standard.cvvLength
                        luhn = standard.luhn
                        errors = errors - EntityField.PREFIXES - EntityField.LENGTHS
                    }
                )
            }
        }


    }
}

@Composable
private fun entityErrorText(error: EntityValidationError, kind: EntityKind): String =
    when (error) {
        EntityValidationError.NameRequired -> stringResource(R.string.manage_error_name_required)
        EntityValidationError.NameTooShort -> stringResource(R.string.manage_error_name_too_short)
        EntityValidationError.NameTaken -> stringResource(
            R.string.manage_error_name_taken,
            stringResource(kind.oneRes)
        )

        EntityValidationError.CodeFormat -> stringResource(R.string.manage_error_code_format)
        EntityValidationError.CodeTaken -> stringResource(R.string.manage_error_code_taken)
        EntityValidationError.PrefixesFormat -> stringResource(R.string.manage_error_prefixes_format)
        EntityValidationError.LengthsRange -> stringResource(R.string.manage_error_lengths_range)
    }

@Composable
private fun EntityTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    helper: String,
    error: String?,
    counter: String?,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    val errorColor = MaterialTheme.colorScheme.error
    val normalColor = MaterialTheme.colorScheme.onSurfaceVariant
    val scope = rememberCoroutineScope()
    val bringIntoView = remember { BringIntoViewRequester() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoView)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                AppLockController.onUserInteraction()
                onValueChange(it)
            },
            modifier = modifier
                .fillMaxWidth()
                .then(
                    if (error != null) {
                        Modifier.semantics { semanticsError(error) }
                    } else {
                        Modifier
                    }
                )
                .onFocusChanged { state ->
                    if (state.isFocused) {
                        scope.launch {
                            delay(350)
                            runCatching { bringIntoView.bringIntoView() }
                        }
                    }
                },
            singleLine = true,
            isError = error != null,
            textStyle = QcType.BodyLarge,
            label = { Text(label) },
            shape = RoundedCornerShape(QcDimens.CornerXs),
            keyboardOptions = keyboardOptions,
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = MaterialTheme.colorScheme.onSurface,
                unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
                errorTextColor = MaterialTheme.colorScheme.onSurface,
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                errorContainerColor = Color.Transparent,
                cursorColor = MaterialTheme.colorScheme.onSurface,
                errorCursorColor = errorColor,
                focusedBorderColor = MaterialTheme.colorScheme.onSurface,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline,
                errorBorderColor = errorColor,
                focusedLabelColor = MaterialTheme.colorScheme.onSurface,
                unfocusedLabelColor = normalColor,
                errorLabelColor = errorColor
            )
        )
        Box(modifier = Modifier.fillMaxWidth().padding(top = QcDimens.Half)) {
            FormSupportingLine(helper = helper, error = error, counter = counter)
        }
    }
}

/** One quiet line under the rules with the way back to the standard ones. */
@Composable
private fun StandardRulesRow(text: String, onReset: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = QcDimens.Half),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = text,
            style = QcType.Body,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        TextButton(
            onClick = onReset,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Text(stringResource(R.string.manage_rules_reset))
        }
    }
}

@Composable
private fun standardRulesText(standard: RuleNetwork, rules: Set<NetworkRule>): String {
    val parts = NetworkRule.entries.filter { it in rules }.map { rule ->
        when (rule) {
            NetworkRule.CVV -> pluralStringResource(
                R.plurals.manage_rule_cvv,
                standard.cvvLength,
                standard.cvvLength
            )
            NetworkRule.PREFIXES -> stringResource(
                R.string.manage_rule_prefixes,
                standard.prefixes.replace(",", ", ")
            )
            NetworkRule.LENGTHS -> stringResource(
                R.string.manage_rule_lengths,
                CardNumberRules.lengthsText(standard.lengths)
            )
            NetworkRule.LUHN -> stringResource(
                if (standard.luhn) R.string.manage_rule_luhn_on else R.string.manage_rule_luhn_off
            )
        }
    }
    return parts.joinToString(stringResource(R.string.manage_rule_join))
}

@Composable
private fun CvvSegmented(
    selected: Int,
    onSelect: (Int) -> Unit
) {
    val outline = MaterialTheme.colorScheme.outline
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(QcDimens.Touch)
            .clip(CircleShape)
            .border(QcDimens.Divider, outline, CircleShape)
            .selectableGroup()
    ) {
        listOf(3, 4).forEachIndexed { index, value ->
            val isSelected = value == selected
            Row(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(
                        if (isSelected) {
                            MaterialTheme.colorScheme.secondaryContainer
                        } else {
                            Color.Transparent
                        }
                    )
                    .then(
                        if (index > 0) {
                            Modifier.drawBehind {
                                drawLine(
                                    color = outline,
                                    start = Offset(0f, 0f),
                                    end = Offset(0f, size.height),
                                    strokeWidth = QcDimens.Divider.toPx()
                                )
                            }
                        } else {
                            Modifier
                        }
                    )
                    .selectable(
                        selected = isSelected,
                        role = Role.RadioButton,
                        onClick = { onSelect(value) }
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (isSelected) {
                    Icon(
                        imageVector = QcIcons.check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.size(QcDimens.IconSmall)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                }
                Text(
                    text = value.toString(),
                    style = EmphasisLabel,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun LuhnCheckbox(
    checked: Boolean,
    onToggle: () -> Unit,
    label: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(QcDimens.Touch)
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = { onToggle() }
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(20.dp),
            contentAlignment = Alignment.Center
        ) {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                modifier = Modifier.size(QcDimens.IconSmall),
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.secondary,
                    checkmarkColor = MaterialTheme.colorScheme.onSecondary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        Spacer(modifier = Modifier.width(QcDimens.Gutter))
        Text(
            text = label,
            style = QcType.Body,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}
