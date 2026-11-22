package com.quickcards.app.core.ui.entity

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quickcards.app.R
import com.quickcards.app.core.data.settings.AppSettingsDefaults
import com.quickcards.app.core.ui.LocalAppLocked
import com.quickcards.app.core.data.repository.BankColourRules
import com.quickcards.app.core.ui.components.BankColourField
import com.quickcards.app.core.ui.components.CheckRow
import com.quickcards.app.core.ui.components.ColourPreview
import com.quickcards.app.core.ui.components.PreviewCard
import com.quickcards.app.core.ui.components.FormTextField
import com.quickcards.app.core.ui.components.GroupLabel
import com.quickcards.app.core.ui.components.LocalSnackbar
import com.quickcards.app.core.ui.components.NetworkMark
import com.quickcards.app.core.ui.components.NetworkMarks
import com.quickcards.app.core.ui.components.QcBanner
import com.quickcards.app.core.ui.components.QcDialog
import com.quickcards.app.core.ui.components.QcIconButton
import com.quickcards.app.core.ui.components.QcNestedTopBar
import com.quickcards.app.core.ui.components.QcSheetActions
import com.quickcards.app.core.ui.components.markTone
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcShapes
import com.quickcards.app.core.ui.theme.QcSize
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTextButton
import com.quickcards.app.core.ui.theme.QcTextTone
import com.quickcards.app.core.ui.theme.QcTheme
import com.quickcards.domain.EntityDraft
import com.quickcards.domain.EntityField
import com.quickcards.domain.EntityKind
import com.quickcards.domain.EntityUsage
import com.quickcards.domain.EntityValidationError
import com.quickcards.domain.EntityValidator
import com.quickcards.domain.NetworkLabelMax
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Full-screen editor for one list item: back + title (+ Delete when editing), the fields in one
 * 12dp rhythm, and one full-width Save at the bottom. Save stays off until something changes
 * and everything is valid; a disabled New names what is missing above it.
 */
@Composable
fun EntityEditorScreen(
    kind: EntityKind,
    editingId: String?,
    onBack: () -> Unit,
    onSaved: (id: String) -> Unit = {},
    onDelete: (() -> Unit)? = null
) {
    val viewModel: EntityEditorViewModel = viewModel()
    val data by viewModel.data.collectAsState()
    if (!data.ready) return
    val before = remember(kind, editingId) { data.before(kind, editingId) }
    val form = rememberEntityForm(
        kind = kind,
        editingId = editingId,
        before = before,
        nameMax = EntityValidator.nameLimit(kind, AppSettingsDefaults.TAGS_MAX_LEN),
        beforeColour = if (kind == EntityKind.BANK) data.banks.firstOrNull { it.id == editingId }?.let(BankColourRules::colourOf) else null
    )
    EntityEditorContent(
        form = form,
        data = data,
        onBack = onBack,
        onSaved = onSaved,
        onDelete = onDelete,
        save = { draft -> viewModel.save(kind, editingId, draft, form.colour) }
    )
}

@Composable
private fun EntityEditorContent(
    form: EntityForm,
    data: EntityEditorData,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    onDelete: (() -> Unit)?,
    save: suspend (EntityDraft) -> Result<String>
) {
    val kind = form.kind
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val locked = LocalAppLocked.current
    val one = stringResource(kind.oneRes)
    val oneTitle = stringResource(kind.oneTitleRes)
    val addedMessage = stringResource(R.string.manage_entity_added, oneTitle)
    val updatedMessage = stringResource(R.string.manage_entity_updated, oneTitle)
    val failedMessage = stringResource(R.string.manage_save_failed, one)

    var saving by remember { mutableStateOf(false) }
    var rulesWarning by remember { mutableStateOf(false) }
    var usedCards by remember { mutableStateOf<List<ManageCardItem>?>(null) }
    var discardAsk by remember { mutableStateOf(false) }
    val nameFocus = remember { FocusRequester() }

    LaunchedEffect(locked) {
        if (locked) {
            rulesWarning = false
            usedCards = null
            discardAsk = false
        }
    }
    LaunchedEffect(Unit) { if (form.before == null) runCatching { nameFocus.requestFocus() } }

    val errors = EntityValidator.validate(form.draft(), data.refs(kind), form.editingId)
    val shown = errors.visible(form)
    val canSave = form.dirty && errors.isEmpty() && !saving
    val note = if (form.before == null && errors.isNotEmpty()) entityErrorText(errors.values.first()) else null

    fun saveNow() {
        if (saving) return
        saving = true
        scope.launch {
            val result = save(form.draft())
            // A Room transaction can resume on its own executor; navigation must run on main.
            withContext(Dispatchers.Main.immediate) {
                result
                    .onSuccess { id ->
                        snackbar.show(if (form.before == null) addedMessage else updatedMessage)
                        onSaved(id)
                        onBack()
                    }
                    .onFailure { snackbar.show(failedMessage) }
                saving = false
            }
        }
    }

    fun confirmUsageThenSave() {
        val used = data.usedCards(kind, form.editingId)
        if (EntityUsage.requiresUpdateConfirmation(kind, form.before, form.draft(), used.size)) {
            usedCards = used.map { it.toManageCardItem() }
        } else {
            saveNow()
        }
    }

    fun attemptSave() {
        if (!canSave) return
        if (form.newlyCustom) rulesWarning = true else confirmUsageThenSave()
    }

    fun back() {
        if (form.before != null && form.dirty || form.before == null && form.name.text.isNotBlank()) {
            discardAsk = true
        } else {
            onBack()
        }
    }
    BackHandler(onBack = ::back)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(QcTheme.colors.bg)
            .imePadding()
            .navigationBarsPadding()
    ) {
        QcNestedTopBar(
            onBack = ::back,
            backLabel = stringResource(R.string.manage_a11y_back),
            title = stringResource(
                if (form.before == null) R.string.manage_dialog_new else R.string.manage_dialog_edit,
                one
            )
        ) {
            if (onDelete != null && form.before != null) {
                QcIconButton(
                    icon = QcIcons.delete,
                    label = stringResource(R.string.manage_a11y_delete, form.before.name),
                    onClick = onDelete,
                    tint = QcTheme.colors.ink2
                )
            }
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = QcSpacing.s4)
                .padding(top = QcSpacing.s2),
            verticalArrangement = Arrangement.spacedBy(QcSpacing.s3)
        ) {
            val otherBanks = data.banks.filter { it.id != form.editingId }
            val bankColour = if (kind == EntityKind.BANK) {
                BankColourRules.editorColour(form.name.text, form.colour, data.banks.firstOrNull { it.id == form.editingId }, otherBanks)
            } else {
                null
            }
            if (bankColour != null) {
                PreviewCard(bankColour, ColourPreview(form.name.text.ifBlank { stringResource(R.string.manage_dialog_new, one) }))
            }
            val standard = form.standard
            if (standard != null && form.custom) {
                val networkName = form.before?.name ?: standard.name
                val resetLabel = stringResource(R.string.manage_a11y_reset_rules, networkName)
                QcBanner(
                    text = stringResource(R.string.manage_rules_changed),
                    icon = QcIcons.tune,
                    actions = {
                        QcTextButton(
                            text = stringResource(R.string.rules_reset_to_standard),
                            onClick = form::resetToStandard,
                            modifier = Modifier.semantics { contentDescription = resetLabel }
                        )
                    }
                )
            }
            FormTextField(
                value = form.name,
                onValueChange = form::updateName,
                label = stringResource(R.string.manage_field_name),
                error = shown[EntityField.NAME]?.let { entityErrorText(it) },
                focusRequester = nameFocus,
                keyboardOptions = KeyboardOptions(
                    capitalization = if (kind == EntityKind.TAG) KeyboardCapitalization.None else KeyboardCapitalization.Words
                ),
                trailing = form.editingId
                    ?.takeIf { kind == EntityKind.NETWORK && NetworkMarks.drawable(it) != null }
                    ?.let { id ->
                        {
                            NetworkMark(
                                id,
                                null,
                                tone = markTone(id, onArt = false, dark = QcTheme.colors.dark),
                                modifier = Modifier.padding(end = QcSpacing.s2)
                            )
                        }
                    }
            )
            if (kind == EntityKind.NETWORK) NetworkRuleFields(form, shown)
            if (bankColour != null) {
                BankColourField(
                    colour = bankColour,
                    shared = otherBanks.any { BankColourRules.colourOf(it) == bankColour },
                    bankName = form.name.text,
                    onPick = { form.colour = it }
                )
            }
        }
        QcSheetActions(
            primary = stringResource(R.string.manage_action_save),
            onPrimary = ::attemptSave,
            primaryEnabled = canSave,
            filled = true,
            note = note,
            sideInset = QcSpacing.s4
        )
    }

    val standard = form.standard
    if (rulesWarning && standard != null) {
        val networkName = form.before?.name ?: standard.name
        QcDialog(
            onDismissRequest = { rulesWarning = false },
            title = stringResource(R.string.manage_rules_title, networkName),
            actions = {
                QcTextButton(stringResource(R.string.manage_action_cancel), { rulesWarning = false }, tone = QcTextTone.Neutral)
                QcTextButton(stringResource(R.string.manage_rules_confirm), {
                    rulesWarning = false
                    if (form.keepsNameAndCode()) saveNow() else confirmUsageThenSave()
                })
            }
        ) {
            DialogBody(stringResource(R.string.manage_rules_body, standardRulesText(standard)))
        }
    }

    val cards = usedCards
    if (cards != null) {
        UpdateCardsDialog(
            cards = cards,
            beforeName = form.before?.name.orEmpty(),
            nextName = EntityValidator.normalizeName(kind, form.name.text),
            onDismiss = { usedCards = null },
            onConfirm = {
                usedCards = null
                saveNow()
            }
        )
    }

    if (discardAsk) {
        QcDialog(
            onDismissRequest = { discardAsk = false },
            title = stringResource(R.string.form_discard_title),
            actions = {
                QcTextButton(stringResource(R.string.form_discard_keep), { discardAsk = false }, tone = QcTextTone.Neutral)
                QcTextButton(stringResource(R.string.form_discard_confirm), {
                    discardAsk = false
                    onBack()
                }, tone = QcTextTone.Danger)
            }
        )
    }
}

@Composable
private fun NetworkRuleFields(form: EntityForm, shown: Map<EntityField, EntityValidationError>) {
    FormTextField(
        value = form.code,
        onValueChange = form::updateCode,
        label = stringResource(R.string.manage_field_code),
        error = shown[EntityField.CODE]?.let { entityErrorText(it) },
        counterMax = NetworkLabelMax,
        keyboardOptions = KeyboardOptions(
            capitalization = KeyboardCapitalization.Characters,
            keyboardType = KeyboardType.Ascii
        )
    )
    FormTextField(
        value = form.prefixes,
        onValueChange = { form.prefixes = it },
        label = stringResource(R.string.manage_field_prefixes),
        error = shown[EntityField.PREFIXES]?.let { entityErrorText(it) },
        placeholder = stringResource(R.string.manage_helper_prefixes),
        mono = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
    FormTextField(
        value = form.lengths,
        onValueChange = { form.lengths = it },
        label = stringResource(R.string.manage_field_lengths),
        error = shown[EntityField.LENGTHS]?.let { entityErrorText(it) },
        placeholder = stringResource(R.string.manage_helper_lengths),
        mono = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
    )
    Column(verticalArrangement = Arrangement.spacedBy(QcSpacing.s1)) {
        GroupLabel(stringResource(R.string.manage_group_cvv))
        Segmented(options = listOf(3, 4), selected = form.cvvLength, onSelect = { form.cvvLength = it })
    }
    Column {
        CheckRow(
            label = stringResource(R.string.manage_luhn),
            checked = form.luhn,
            onCheckedChange = { form.luhn = it },
            inset = 0.dp,
            spokenLabel = stringResource(R.string.manage_a11y_luhn)
        )
        CheckRow(
            label = stringResource(R.string.manage_cvv_optional),
            checked = form.cvvOptional,
            onCheckedChange = { form.cvvOptional = it },
            inset = 0.dp
        )
    }
}

/**
 * Single choice among a few short values: min 48, `r-full`, 1dp outline drawn above the
 * selected fill so the edge stays visible; the selected segment carries a check.
 */
@Composable
private fun Segmented(options: List<Int>, selected: Int, onSelect: (Int) -> Unit) {
    val colors = QcTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = QcSize.hit)
            .clip(QcShapes.full)
            .drawWithContent {
                drawContent()
                val stroke = QcSize.hairline.toPx()
                val radius = size.height / 2
                drawRoundRect(
                    color = colors.outline,
                    topLeft = Offset(stroke / 2, stroke / 2),
                    size = size.copy(width = size.width - stroke, height = size.height - stroke),
                    cornerRadius = CornerRadius(radius),
                    style = Stroke(stroke)
                )
                val step = size.width / options.size
                for (i in 1 until options.size) {
                    drawLine(colors.outline, Offset(step * i, 0f), Offset(step * i, size.height), stroke)
                }
            }
            .selectableGroup()
    ) {
        options.forEach { value ->
            val on = value == selected
            Box(
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = QcSize.hit)
                    .background(if (on) colors.tonal2 else colors.surface)
                    .selectable(selected = on, role = Role.RadioButton, onClick = { onSelect(value) }),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(QcSpacing.s2)
                ) {
                    if (on) {
                        Icon(QcIcons.check, null, tint = colors.ink, modifier = Modifier.size(QcSize.iconSm))
                    }
                    Text(text = value.toString(), style = QcText.F3Medium, color = colors.ink)
                }
            }
        }
    }
}

@Composable
internal fun DialogBody(text: String) {
    Text(text = text, style = QcText.F3, color = QcTheme.colors.ink2)
}
