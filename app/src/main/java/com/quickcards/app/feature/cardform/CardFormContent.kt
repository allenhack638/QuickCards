package com.quickcards.app.feature.cardform

import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.navigation.NavController
import com.quickcards.app.R
import com.quickcards.app.core.data.settings.AppSettings
import com.quickcards.app.core.ui.LocalAppLocked
import com.quickcards.app.core.ui.LockGate
import com.quickcards.app.core.ui.components.AddTagChip
import com.quickcards.app.core.ui.components.CardColourSheet
import com.quickcards.app.core.ui.components.ColourField
import com.quickcards.app.core.ui.components.ColourPreview
import com.quickcards.app.core.ui.components.FormTextField
import com.quickcards.app.core.ui.components.GroupLabel
import com.quickcards.app.core.ui.components.LocalSnackbar
import com.quickcards.app.core.ui.components.NetworkMark
import com.quickcards.app.core.ui.components.NetworkMarks
import com.quickcards.app.core.ui.components.QcLookupField
import com.quickcards.app.core.ui.components.QcMask
import com.quickcards.app.core.ui.components.QcVisibilityToggle
import com.quickcards.app.core.ui.components.RulesResetRow
import com.quickcards.app.core.ui.components.TagChip
import com.quickcards.app.core.ui.components.markTone
import com.quickcards.app.core.ui.components.withCaretAtEnd
import com.quickcards.app.core.ui.entity.EntityDialog
import com.quickcards.app.core.ui.theme.QcFilledButton
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcSize
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTextButton
import com.quickcards.app.core.ui.theme.QcTheme
import com.quickcards.domain.CardField
import com.quickcards.domain.CardNumberRules
import com.quickcards.domain.EntityKind
import com.quickcards.domain.Field
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

/**
 * Number pad for the number and expiry. A text keyboard drops back to letters after each
 * group space, forcing a layout switch every four digits. Paste and Tap card still fill.
 */
private val DigitsKeyboard = KeyboardOptions(
    capitalization = KeyboardCapitalization.None,
    autoCorrectEnabled = false,
    keyboardType = KeyboardType.Number,
    imeAction = ImeAction.Next
)

private const val CVV_REVEAL_MS = 5_000L
private const val AUTOFOCUS_DELAY_MS = 300L

private sealed interface FormSheet {
    data class Picker(val picker: FormPicker) : FormSheet
    data object Tags : FormSheet
    data object Colour : FormSheet
    data object Tap : FormSheet
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CardFormContent(
    viewModel: CardFormViewModel,
    session: CardFormSession,
    saving: Boolean,
    navController: NavController,
    scrollState: androidx.compose.foundation.ScrollState,
    onOpenCard: (cardId: String) -> Unit
) {
    val form = session.form
    val lists = session.lists
    val network = session.network(lists.networks)
    val repairs by viewModel.repairs.collectAsState()
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val locked = LocalAppLocked.current
    val colors = QcTheme.colors
    val topOffsetPx = with(LocalDensity.current) { QcSize.field.roundToPx() }
    val positions = remember { mutableStateMapOf<Field, Int>() }

    var sheet by remember { mutableStateOf<FormSheet?>(null) }
    var pendingEntity by remember { mutableStateOf<EntityKind?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var numberFocused by remember { mutableStateOf(false) }
    var expiryFocused by remember { mutableStateOf(false) }
    var cvvFocused by remember { mutableStateOf(false) }
    var cvvShown by remember { mutableStateOf(false) }
    var autoFocused by rememberSaveable { mutableStateOf(false) }
    val numberFocus = remember { FocusRequester() }
    val expiryFocus = remember { FocusRequester() }
    val cvvFocus = remember { FocusRequester() }
    val dueDayFocus = remember { FocusRequester() }
    val noteFocus = remember { FocusRequester() }
    fun nextTo(target: FocusRequester) = KeyboardActions(onNext = { runCatching { target.requestFocus() } })

    var numberValue by remember { mutableStateOf(TextFieldValue("")) }
    var expiryValue by remember { mutableStateOf(TextFieldValue("")) }
    var cvvValue by remember { mutableStateOf(TextFieldValue("")) }
    var noteValue by remember { mutableStateOf(TextFieldValue("")) }
    var dueDayValue by remember { mutableStateOf(TextFieldValue(form.dueDay)) }

    val numberDisplay = form.formattedNumber(lists.networks, typing = numberFocused)
    LaunchedEffect(numberDisplay) {
        if (numberValue.text != numberDisplay) numberValue = TextFieldValue(numberDisplay, TextRange(numberDisplay.length))
    }
    LaunchedEffect(form.expiry) {
        if (expiryValue.text != form.expiry) expiryValue = TextFieldValue(form.expiry, TextRange(form.expiry.length))
    }
    LaunchedEffect(form.cvv) {
        if (cvvValue.text != form.cvv) cvvValue = TextFieldValue(form.cvv, TextRange(form.cvv.length))
    }
    LaunchedEffect(form.note) {
        if (noteValue.text != form.note) {
            noteValue = TextFieldValue(form.note, TextRange(form.note.length))
            dueDayValue = TextFieldValue(form.dueDay, TextRange(form.dueDay.length))
        }
    }
    LaunchedEffect(cvvShown) {
        if (cvvShown) {
            delay(CVV_REVEAL_MS)
            cvvShown = false
        }
    }
    LaunchedEffect(locked) {
        if (locked) {
            sheet = null
            pendingEntity = null
            cvvShown = false
            focusManager.clearFocus(force = true)
        }
    }

    fun focus(field: Field) {
        positions[field]?.let { y -> scope.launch { scrollState.animateScrollTo((y - topOffsetPx).coerceAtLeast(0)) } }
        when (field) {
            Field.NUMBER -> {
                numberValue = numberValue.withCaretAtEnd()
                runCatching { numberFocus.requestFocus() }
            }
            Field.EXPIRY -> {
                expiryValue = expiryValue.withCaretAtEnd()
                runCatching { expiryFocus.requestFocus() }
            }
            Field.CVV -> {
                cvvValue = cvvValue.withCaretAtEnd()
                runCatching { cvvFocus.requestFocus() }
            }
            Field.HOLDER, Field.BANK, Field.TYPE -> Unit
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CardFormEvent.FocusField -> {
                    saveError = null
                    focus(event.field)
                }
                is CardFormEvent.Saved -> {
                    focusManager.clearFocus(force = true)
                    saveError = null
                    navController.previousBackStackEntry?.savedStateHandle?.set("savedCardId", event.cardId)
                    snackbar.show(
                        if (event.isEdit) {
                            context.getString(R.string.form_saved_changes)
                        } else {
                            context.getString(R.string.form_saved_card, event.networkName ?: context.getString(R.string.form_card))
                        }
                    )
                    if (!navController.popBackStack("cards", inclusive = false)) navController.popBackStack()
                }
                CardFormEvent.SaveFailed -> saveError = context.getString(R.string.form_save_failed)
            }
        }
    }

    LaunchedEffect(form.mode, form.editingId) {
        if (form.mode == CardFormMode.ADD && !autoFocused) {
            delay(AUTOFOCUS_DELAY_MS)
            if (scrollState.value > 0 || sheet != null) return@LaunchedEffect
            numberValue = numberValue.withCaretAtEnd()
            runCatching { numberFocus.requestFocus() }
            autoFocused = true
        }
    }

    fun fillNumber(number: String, expiry: String?) {
        viewModel.updateNumber(number)
        val display = CardFieldInput.number(number, lists.networks, typing = numberFocused)
        numberValue = TextFieldValue(display, TextRange(display.length))
        expiry?.let {
            viewModel.updateExpiry(it)
            expiryValue = TextFieldValue(it, TextRange(it.length))
        }
    }

    fun openDuplicate(cardId: String) {
        focusManager.clearFocus(force = true)
        onOpenCard(cardId)
    }

    fun changedFrom(field: CardField, current: String): String? =
        repairs[field]?.before?.takeIf { before -> before.filter(Char::isDigit) != current.filter(Char::isDigit) }

    val nfcAvailable = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC) }
    val nfcAddEnabled by remember { AppSettings.get(context) }.nfcAddEnabled.collectAsState()
    val bankName = lists.bankName(form.bankId)
    val holderName = lists.holderName(form.holderId)
    val typeName = lists.typeName(form.typeId)
    val saveIssue = session.saveIssue()

    Column(modifier = Modifier.fillMaxSize().background(colors.bg), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(
            modifier = Modifier
                .widthIn(max = QcDimens.PageMax)
                .fillMaxWidth()
                .weight(1f)
                .pointerInput(Unit) { detectTapGestures { focusManager.clearFocus() } }
                .verticalScroll(scrollState)
                .padding(start = QcSpacing.s4, end = QcSpacing.s4, top = QcSpacing.s2, bottom = QcSpacing.s4),
            verticalArrangement = Arrangement.spacedBy(QcSpacing.s3)
        ) {
            FormPreview(session = session, bankName = bankName, holderName = holderName, typeName = typeName)

            QcLookupField(
                label = stringResource(R.string.form_field_holder),
                value = holderName.orEmpty(),
                placeholder = stringResource(R.string.form_placeholder_holder),
                helper = "",
                error = cardErrorText(form.errors[Field.HOLDER]),
                onClick = { sheet = FormSheet.Picker(FormPicker.HOLDER) },
                modifier = Modifier.recordY(positions, Field.HOLDER)
            )

            val numberError = cardErrorText(form.errors[Field.NUMBER], form.number)
            val duplicate = session.duplicateOf()
            val numberRepair = changedFrom(CardField.NUMBER, form.number)
            val tapLabel = stringResource(R.string.form_a11y_tap_card)
            FormTextField(
                value = numberValue,
                onValueChange = { input ->
                    val paste = SmartPaste.apply(
                        previous = numberValue.text,
                        incoming = input.text,
                        field = SmartPasteField.NUMBER,
                        networks = lists.networks,
                        today = session.today,
                        expiryEmpty = form.expiry.isBlank()
                    )
                    if (paste.number != null) {
                        fillNumber(paste.number, paste.expiry)
                        snackbar.show(
                            context.getString(
                                if (paste.expiry != null) R.string.form_paste_number_and_expiry else R.string.form_paste_number
                            )
                        )
                    } else {
                        viewModel.updateNumber(input.text)
                        val display = CardFieldInput.number(input.text, lists.networks, typing = numberFocused)
                        numberValue = TextFieldValue(display, TextRange(display.length))
                    }
                },
                label = stringResource(R.string.form_field_number),
                placeholder = stringResource(R.string.form_placeholder_number),
                mono = true,
                helper = numberRepair?.let { stringResource(R.string.form_repair_changed_from, it) }.orEmpty(),
                error = numberError,
                keyboardOptions = DigitsKeyboard,
                keyboardActions = nextTo(expiryFocus),
                maxLines = 2,
                fieldTag = "form_number",
                focusRequester = numberFocus,
                labelAction = if (form.mode == CardFormMode.ADD && nfcAvailable && nfcAddEnabled) {
                    {
                        QcTextButton(
                            text = stringResource(R.string.form_nfc_button),
                            onClick = { sheet = FormSheet.Tap },
                            icon = QcIcons.nfc,
                            modifier = Modifier.semantics { contentDescription = tapLabel }
                        )
                    }
                } else {
                    null
                },
                trailing = network?.let { net -> { NumberMark(net.id, net.name, net.code) } },
                helperAction = when {
                    duplicate != null -> {
                        {
                            val openLabel = stringResource(R.string.form_duplicate_open_a11y, duplicate.bankName, form.number.takeLast(4))
                            HelpAction(
                                text = stringResource(R.string.form_duplicate_open),
                                onClick = { openDuplicate(duplicate.cardId) },
                                description = openLabel
                            )
                        }
                    }
                    numberRepair != null -> { { RepairUndo { viewModel.undoRepair(CardField.NUMBER) } } }
                    else -> null
                },
                modifier = Modifier
                    .recordY(positions, Field.NUMBER)
                    .onFocusChanged { state ->
                        if (state.isFocused && !numberFocused) {
                            numberFocused = true
                        } else if (!state.isFocused && numberFocused) {
                            numberFocused = false
                            viewModel.numberBlur()
                        }
                    }
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .onGloballyPositioned {
                        val y = it.positionInParent().y.roundToInt()
                        positions[Field.EXPIRY] = y
                        positions[Field.CVV] = y
                    },
                horizontalArrangement = Arrangement.spacedBy(QcSpacing.s3)
            ) {
                val expiryRepair = changedFrom(CardField.EXPIRY, form.expiry)
                FormTextField(
                    value = expiryValue,
                    onValueChange = { input ->
                        val paste = SmartPaste.apply(
                            previous = expiryValue.text,
                            incoming = input.text,
                            field = SmartPasteField.EXPIRY,
                            networks = lists.networks,
                            today = session.today
                        )
                        val next = paste.expiry ?: CardFieldInput.expiry(input.text)
                        viewModel.updateExpiry(paste.expiry ?: input.text)
                        expiryValue = TextFieldValue(next, TextRange(next.length))
                    },
                    label = stringResource(R.string.form_field_expiry),
                    placeholder = stringResource(R.string.form_helper_expiry),
                    mono = true,
                    helper = expiryRepair?.let { stringResource(R.string.form_repair_changed_from, it) }.orEmpty(),
                    error = if (session.expiryWarning) null else cardErrorText(form.errors[Field.EXPIRY]),
                    warn = if (session.expiryWarning) stringResource(R.string.form_error_expiry_past) else null,
                    keyboardOptions = DigitsKeyboard,
                    keyboardActions = nextTo(cvvFocus),
                    fieldTag = "form_expiry",
                    focusRequester = expiryFocus,
                    helperAction = expiryRepair?.let { { RepairUndo { viewModel.undoRepair(CardField.EXPIRY) } } },
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { state ->
                            if (state.isFocused) {
                                expiryFocused = true
                            } else if (expiryFocused) {
                                expiryFocused = false
                                viewModel.expiryBlur()
                            }
                        }
                )
                val cvvRepair = changedFrom(CardField.CVV, form.cvv)
                FormTextField(
                    value = cvvValue,
                    onValueChange = { input ->
                        val next = CardFieldInput.cvv(input.text, network)
                        viewModel.updateCvv(input.text)
                        cvvValue = TextFieldValue(next, TextRange(next.length))
                    },
                    label = stringResource(R.string.form_field_cvv),
                    placeholder = stringResource(R.string.form_placeholder_cvv),
                    mono = true,
                    helper = cvvRepair?.let { stringResource(R.string.form_repair_changed_from, it) }.orEmpty(),
                    error = cardErrorText(form.errors[Field.CVV]),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
                    keyboardActions = nextTo(dueDayFocus),
                    visualTransformation = if (cvvShown) VisualTransformation.None else QcMask,
                    fieldTag = "form_cvv",
                    focusRequester = cvvFocus,
                    trailing = {
                        QcVisibilityToggle(
                            visible = cvvShown,
                            onToggle = { cvvShown = !cvvShown },
                            showLabel = stringResource(R.string.form_cvv_show),
                            hideLabel = stringResource(R.string.form_cvv_hide)
                        )
                    },
                    helperAction = cvvRepair?.let { { RepairUndo { viewModel.undoRepair(CardField.CVV) } } },
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { state ->
                            if (state.isFocused) {
                                cvvFocused = true
                            } else if (cvvFocused) {
                                cvvFocused = false
                                cvvShown = false
                                viewModel.cvvBlur()
                            }
                        }
                )
            }

            session.changedRulesBlocking()?.let { blocking ->
                val blocked = blocking.network
                RulesResetRow(
                    text = when (blocking.kind) {
                        BlockingRule.Kind.CVV -> pluralStringResource(
                            R.plurals.form_rules_cvv,
                            blocked.cvvLength,
                            blocked.name,
                            blocked.cvvLength
                        )
                        BlockingRule.Kind.NUMBER -> stringResource(R.string.form_rules_number, blocked.name)
                    },
                    networkName = blocked.name,
                    onReset = { viewModel.resetNetworkRules(blocked.id) }
                )
            }

            QcLookupField(
                label = stringResource(R.string.form_field_bank),
                value = bankName.orEmpty(),
                placeholder = stringResource(R.string.form_placeholder_bank),
                helper = "",
                error = cardErrorText(form.errors[Field.BANK]),
                onClick = { sheet = FormSheet.Picker(FormPicker.BANK) },
                modifier = Modifier.recordY(positions, Field.BANK)
            )

            Row(
                modifier = Modifier.fillMaxWidth().recordY(positions, Field.TYPE),
                horizontalArrangement = Arrangement.spacedBy(QcSpacing.s3)
            ) {
                QcLookupField(
                    label = stringResource(R.string.form_field_type),
                    value = typeName.orEmpty(),
                    placeholder = stringResource(R.string.form_placeholder_type),
                    helper = "",
                    error = cardErrorText(form.errors[Field.TYPE]),
                    onClick = { sheet = FormSheet.Picker(FormPicker.TYPE) },
                    modifier = Modifier.weight(1f)
                )
                ColourField(
                    key = if (!form.bandColorManual && form.bankId == null) null else form.bandColor,
                    onClick = { sheet = FormSheet.Colour },
                    modifier = Modifier.weight(1f)
                )
            }

            Column(verticalArrangement = Arrangement.spacedBy(QcSpacing.s2)) {
                GroupLabel(
                    stringResource(R.string.form_group_tags),
                    stringResource(R.string.form_tags_count, form.tagIds.size, lists.tagLimit)
                )
                FlowRow(horizontalArrangement = Arrangement.spacedBy(QcSpacing.s2)) {
                    form.tagIds.forEach { id ->
                        val name = lists.tagName(id) ?: return@forEach
                        TagChip(name = name, onRemove = { viewModel.removeTag(id) })
                    }
                    if (session.canAddTag()) AddTagChip(onClick = { sheet = FormSheet.Tags })
                }
            }

            FormTextField(
                value = dueDayValue,
                onValueChange = { input ->
                    val next = input.text.filter(Char::isDigit).take(2)
                    viewModel.updateDueDay(next)
                    dueDayValue = TextFieldValue(next, TextRange(next.length))
                },
                label = stringResource(R.string.form_due_day_label),
                placeholder = stringResource(R.string.form_due_day_helper),
                error = if (form.dueDayValid) null else stringResource(R.string.form_due_day_error),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                keyboardActions = nextTo(noteFocus),
                focusRequester = dueDayFocus
            )

            FormTextField(
                value = noteValue,
                onValueChange = { input ->
                    val next = CardFieldInput.note(input.text)
                    viewModel.updateNote(next)
                    noteValue = TextFieldValue(next, TextRange(next.length))
                },
                label = stringResource(R.string.form_field_note),
                placeholder = stringResource(R.string.form_helper_note),
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { focusManager.clearFocus() }),
                focusRequester = noteFocus
            )
        }

        FormFooter(
            label = stringResource(if (form.isEdit) R.string.form_save_changes else R.string.form_save_card),
            enabled = !saving && session.canSave(),
            note = saveError ?: saveIssue?.takeIf { !form.isEdit }?.let { saveIssueText(it) },
            noteIsError = saveError != null,
            onClick = {
                saveError = null
                viewModel.save()
            }
        )
    }

    when (val open = sheet) {
        is FormSheet.Picker -> FormPickerSheet(
            picker = open.picker,
            session = session,
            locked = locked,
            onDismiss = { sheet = null },
            onPick = { id ->
                sheet = null
                when (open.picker) {
                    FormPicker.HOLDER -> viewModel.selectHolder(id)
                    FormPicker.BANK -> viewModel.selectBank(id)
                    FormPicker.TYPE -> viewModel.selectType(id)
                }
            },
            onAdd = {
                sheet = null
                pendingEntity = when (open.picker) {
                    FormPicker.HOLDER -> EntityKind.HOLDER
                    FormPicker.BANK -> EntityKind.BANK
                    FormPicker.TYPE -> EntityKind.TYPE
                }
            }
        )
        FormSheet.Tags -> TagSheet(
            session = session,
            locked = locked,
            onDismiss = { sheet = null },
            onToggle = viewModel::toggleTag,
            onCreate = viewModel::createTag,
            onTagCreated = viewModel::onTagCreated
        )
        FormSheet.Colour -> if (LockGate.visible(true, locked)) {
            CardColourSheet(
                selected = form.bandColor,
                bankColour = session.bankColour(),
                preview = ColourPreview(
                    title = bankName ?: stringResource(R.string.form_preview_new),
                    networkId = network?.id?.takeIf { NetworkMarks.drawable(it) != null },
                    last4 = form.number.takeLast(4).takeIf { form.number.isNotEmpty() }
                ),
                recent = lists.recentColours,
                onPick = viewModel::selectBand,
                onBankColour = viewModel::followBankColour,
                onDismiss = { sheet = null }
            )
        }
        FormSheet.Tap -> if (LockGate.visible(true, locked)) {
            NfcTapSheet(
                onDismiss = { sheet = null },
                onRead = { data -> fillNumber(data.number, data.expiry?.takeIf { form.expiry.isBlank() }) },
                networkIdFor = { number -> CardNumberRules.detect(number, lists.networks)?.id }
            )
        }
        null -> Unit
    }

    val entityKind = pendingEntity
    if (entityKind != null && LockGate.visible(true, locked)) {
        EntityDialog(
            kind = entityKind,
            onDismiss = { pendingEntity = null },
            onSaved = { id -> viewModel.onEntitySaved(entityKind, id) }
        )
    }
}

private fun Modifier.recordY(positions: MutableMap<Field, Int>, field: Field): Modifier =
    onGloballyPositioned { positions[field] = it.positionInParent().y.roundToInt() }

/** The detected network inside the number field: its mark, or a custom network's code. */
@Composable
private fun NumberMark(id: String, name: String, code: String) {
    val colors = QcTheme.colors
    Box(modifier = Modifier.padding(end = QcSpacing.s2)) {
        if (NetworkMarks.drawable(id) != null) {
            NetworkMark(networkId = id, contentDescription = name, tone = markTone(id, onArt = false, dark = colors.dark))
        } else {
            Text(
                text = code,
                style = QcText.F1Semibold,
                color = colors.ink2,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.End,
                modifier = Modifier.widthIn(max = QcSpacing.s16 * 2)
            )
        }
    }
}

/**
 * The one Save: a hairline above, the line naming what is missing, then the full-width filled
 * button. It rides on the keyboard (imePadding).
 */
@Composable
internal fun FormFooter(label: String, enabled: Boolean, note: String?, noteIsError: Boolean, onClick: () -> Unit) {
    val colors = QcTheme.colors
    Column(
        modifier = Modifier
            .widthIn(max = QcDimens.PageMax)
            .fillMaxWidth()
            .background(colors.bg)
            .imePadding()
            .navigationBarsPadding()
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(QcSize.hairline).background(colors.line))
        Column(
            modifier = Modifier.padding(start = QcSpacing.s4, end = QcSpacing.s4, top = QcSpacing.s3, bottom = QcSpacing.s4),
            verticalArrangement = Arrangement.spacedBy(QcSpacing.s2)
        ) {
            if (!note.isNullOrEmpty()) {
                Text(
                    text = note,
                    style = QcText.F2,
                    color = if (noteIsError) colors.danger else colors.ink2,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
            QcFilledButton(text = label, onClick = onClick, enabled = enabled, large = true, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
private fun RepairUndo(onUndo: () -> Unit) {
    HelpAction(text = stringResource(R.string.form_repair_undo), onClick = onUndo)
}

/** A text action on a help line: keeps the line's own height; its 48 target overflows it. */
@Composable
private fun HelpAction(text: String, onClick: () -> Unit, description: String? = null) {
    Box(modifier = Modifier.height(QcSpacing.s4).wrapContentHeight(unbounded = true)) {
        QcTextButton(
            text = text,
            onClick = onClick,
            modifier = if (description != null) Modifier.semantics { contentDescription = description } else Modifier
        )
    }
}
