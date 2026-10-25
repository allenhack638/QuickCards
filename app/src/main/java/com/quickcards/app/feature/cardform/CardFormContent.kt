package com.quickcards.app.feature.cardform

import androidx.compose.foundation.background
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import com.quickcards.app.core.ui.theme.QcIcons
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.TextButton
import com.quickcards.app.core.data.settings.AppSettings
import android.content.pm.PackageManager
import com.quickcards.app.core.ui.components.MarkSurface
import com.quickcards.app.core.ui.components.NetworkMarks
import com.quickcards.app.core.ui.components.NetworkMark
import com.quickcards.app.core.ui.components.networkMarkPlate
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.quickcards.app.R
import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.app.core.ui.sharedPass
import com.quickcards.app.core.ui.HomeCard
import com.quickcards.app.core.ui.LocalAppLocked
import com.quickcards.app.core.ui.LockGate
import com.quickcards.app.core.ui.components.AddTagChip
import com.quickcards.app.core.ui.components.BandColorGroup
import com.quickcards.app.core.ui.components.FormHelperText
import com.quickcards.app.core.ui.components.FormTextField
import com.quickcards.app.core.ui.components.GroupLabel
import com.quickcards.app.core.ui.components.LocalSnackbar
import com.quickcards.app.core.ui.components.PassCard
import com.quickcards.app.core.ui.components.QcLookupField
import com.quickcards.app.core.ui.components.TagChip
import com.quickcards.app.core.ui.components.withCaretAtEnd
import com.quickcards.app.core.ui.entity.EntityDialog
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcType
import com.quickcards.app.core.ui.theme.qcPrimaryButtonColors
import com.quickcards.domain.CardField
import com.quickcards.domain.CardValidator
import com.quickcards.domain.EntityKind
import com.quickcards.domain.ExpiryStatus
import com.quickcards.domain.Field
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.time.LocalDate
import kotlin.math.roundToInt

/**
 * Text keyboard (not the number pad) so the keyboard's own scan-text tool is offered;
 * the field filters to digits. Suggestions off so the keyboard does not learn card numbers.
 */
private val ScannableKeyboard = KeyboardOptions(
    capitalization = KeyboardCapitalization.None,
    autoCorrectEnabled = false,
    keyboardType = KeyboardType.Text
)

private val SaveButtonText = QcType.MenuItem.copy(fontWeight = FontWeight(600))

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun CardFormContent(
    viewModel: CardFormViewModel,
    session: CardFormSession,
    saving: Boolean,
    navController: NavController
) {
    val form = session.form
    val lists = session.lists
    val network = form.network(lists.networks)
    val repairs by viewModel.repairs.collectAsState()
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val context = LocalContext.current
    val locked = LocalAppLocked.current
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val configuration = LocalConfiguration.current
    val topOffsetPx = with(LocalDensity.current) { QcDimens.FieldHeight.roundToPx() }

    val scrollState = rememberScrollState()
    val positions = remember { mutableStateMapOf<Field, Int>() }

    var holderSheetOpen by remember { mutableStateOf(false) }
    var typeSheetOpen by remember { mutableStateOf(false) }
    var bankSheetOpen by remember { mutableStateOf(false) }
    var tagSheetOpen by remember { mutableStateOf(false) }
    var pendingEntity by remember { mutableStateOf<EntityKind?>(null) }
    var saveError by remember { mutableStateOf<String?>(null) }
    var previewExpanded by remember { mutableStateOf(false) }

    var numberFocused by remember { mutableStateOf(false) }
    var expiryFocused by remember { mutableStateOf(false) }
    var cvvFocused by remember { mutableStateOf(false) }
    var autoFocused by rememberSaveable { mutableStateOf(false) }
    val numberFocus = remember { FocusRequester() }
    val expiryFocus = remember { FocusRequester() }
    val cvvFocus = remember { FocusRequester() }

    var numberValue by remember { mutableStateOf(TextFieldValue("")) }
    var expiryValue by remember { mutableStateOf(TextFieldValue("")) }
    var cvvValue by remember { mutableStateOf(TextFieldValue("")) }
    var noteValue by remember { mutableStateOf(TextFieldValue("")) }
    var dueDayValue by remember { mutableStateOf(TextFieldValue(form.dueDay)) }

    val numberDisplay = form.formattedNumber(lists.networks, typing = numberFocused)
    LaunchedEffect(numberDisplay) {
        if (numberValue.text != numberDisplay) {
            numberValue = TextFieldValue(numberDisplay, TextRange(numberDisplay.length))
        }
    }
    LaunchedEffect(form.expiry) {
        if (expiryValue.text != form.expiry) {
            expiryValue = TextFieldValue(form.expiry, TextRange(form.expiry.length))
        }
    }
    LaunchedEffect(form.cvv) {
        if (cvvValue.text != form.cvv) {
            cvvValue = TextFieldValue(form.cvv, TextRange(form.cvv.length))
        }
    }
    LaunchedEffect(form.note) {
        if (noteValue.text != form.note) {
            noteValue = TextFieldValue(form.note, TextRange(form.note.length))
            dueDayValue = TextFieldValue(form.dueDay, TextRange(form.dueDay.length))
        }
    }

    LaunchedEffect(locked) {
        if (locked) {
            holderSheetOpen = false
            typeSheetOpen = false
            bankSheetOpen = false
            tagSheetOpen = false
            pendingEntity = null
            focusManager.clearFocus(force = true)
        }
    }

    fun scrollTo(field: Field) {
        val y = positions[field] ?: return
        scope.launch {
            scrollState.animateScrollTo((y - topOffsetPx).coerceAtLeast(0))
        }
    }

    fun focus(field: Field) {
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
            Field.HOLDER, Field.BANK -> Unit
        }
    }

    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is CardFormEvent.FocusField -> {
                    saveError = null
                    scrollTo(event.field)
                    focus(event.field)
                }
                is CardFormEvent.Saved -> {
                    focusManager.clearFocus(force = true)
                    saveError = null
                    navController.previousBackStackEntry
                        ?.savedStateHandle
                        ?.set("savedCardId", event.cardId)
                    val message = if (event.isEdit) {
                        context.getString(R.string.form_saved_changes)
                    } else {
                        context.getString(
                            R.string.form_saved_card,
                            event.networkName ?: context.getString(R.string.form_card)
                        )
                    }
                    snackbar.show(message)
                    if (!navController.popBackStack("cards", inclusive = false)) {
                        navController.popBackStack()
                    }
                }
                CardFormEvent.SaveFailed -> {
                    saveError = context.getString(R.string.form_save_failed)
                }
            }
        }
    }

    LaunchedEffect(form.mode, form.editingId) {
        if (form.mode == CardFormMode.ADD && !autoFocused) {
            delay(300)
            val interrupted = scrollState.value > 0 ||
                holderSheetOpen || typeSheetOpen || bankSheetOpen || tagSheetOpen
            if (interrupted) return@LaunchedEffect
            numberValue = numberValue.withCaretAtEnd()
            runCatching { numberFocus.requestFocus() }
            autoFocused = true
        }
    }

    val bankLabel = stringResource(R.string.form_field_bank)
    val previewCard = remember(session, bankLabel) { session.previewCard(bankLabel) }
    val previewExpiryState = remember(session.form.expiry, session.options.expiresSoonMonths) {
        ExpiryStatus.expiryState(
            expiry = session.form.expiry,
            today = LocalDate.now(),
            soonMonths = session.options.expiresSoonMonths
        )
    }
    val previewArchiveReason = session.archiveReason
    val previewArchivedLabel = if (session.archived) {
        if (previewArchiveReason.isNullOrBlank()) {
            stringResource(R.string.cards_status_archived)
        } else {
            stringResource(R.string.cards_status_archived_reason, previewArchiveReason)
        }
    } else {
        null
    }
    val tagsShow = session.options.tagsShow
    val compactPreview = configuration.screenHeightDp < 700 && !previewExpanded
    val expandPreviewLabel = stringResource(R.string.form_expand_preview)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .weight(1f)
                .pointerInput(Unit) {
                    detectTapGestures { focusManager.clearFocus() }
                }
                .verticalScroll(scrollState)
        ) {
            Column(modifier = Modifier.padding(horizontal = QcDimens.Gutter)) {
                Spacer(modifier = Modifier.height(QcDimens.Base))
                if (compactPreview) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .sharedPass(form.editingId)
                            .clickable(onClickLabel = expandPreviewLabel) { previewExpanded = true }
                    ) {
                        PassCard(
                            card = previewCard,
                            maskNumbers = false,
                            tagsShow = tagsShow,
                            preview = true,
                            compact = true,
                            expiryState = previewExpiryState,
                            needsReview = session.reviewFields.isNotEmpty(),
                            archivedLabel = previewArchivedLabel,
                            note = form.note,
                            showNote = session.options.showNotes
                        )
                    }
                } else {
                    PassCard(
                        card = previewCard,
                        maskNumbers = false,
                        tagsShow = tagsShow,
                        modifier = Modifier.sharedPass(form.editingId),
                        preview = true,
                        expiryState = previewExpiryState,
                        needsReview = session.reviewFields.isNotEmpty(),
                        archivedLabel = previewArchivedLabel,
                        note = form.note,
                        showNote = session.options.showNotes
                    )
                }
                Spacer(modifier = Modifier.height(if (compactPreview) QcDimens.Gutter else 30.dp))

                QcLookupField(
                    label = stringResource(R.string.form_field_holder),
                    value = lists.holderName(form.holderId).orEmpty(),
                    helper = "",
                    error = cardErrorText(form.errors[Field.HOLDER]),
                    onClick = { holderSheetOpen = true },
                    modifier = Modifier.onGloballyPositioned {
                        positions[Field.HOLDER] = it.positionInParent().y.roundToInt()
                    }
                )

                Spacer(modifier = Modifier.height(QcDimens.FormFieldGap))

                val nfcAvailable = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC) }
                val nfcAddEnabled by remember { AppSettings.get(context) }.nfcAddEnabled.collectAsState()
                var nfcSheetOpen by remember { mutableStateOf(false) }
                if (form.mode == CardFormMode.ADD && nfcAvailable && nfcAddEnabled) {
                    TextButton(
                        onClick = { nfcSheetOpen = true },
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                    ) {
                        Icon(
                            imageVector = QcIcons.nfc,
                            contentDescription = null,
                            modifier = Modifier.size(QcDimens.IconSmall)
                        )
                        Spacer(modifier = Modifier.width(QcDimens.Base))
                        Text(stringResource(R.string.form_nfc_button))
                    }
                    Spacer(modifier = Modifier.height(QcDimens.Base))
                }
                if (nfcSheetOpen) {
                    NfcTapSheet(
                        onDismiss = { nfcSheetOpen = false },
                        onRead = { data ->
                            nfcSheetOpen = false
                            viewModel.updateNumber(data.number)
                            val display = CardFieldInput.number(data.number, lists.networks, typing = false)
                            numberValue = TextFieldValue(display, TextRange(display.length))
                            data.expiry?.takeIf { form.expiry.isBlank() }?.let { expiry ->
                                viewModel.updateExpiry(expiry)
                                expiryValue = TextFieldValue(expiry, TextRange(expiry.length))
                            }
                            snackbar.show(context.getString(R.string.form_nfc_read))
                        }
                    )
                }
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
                            viewModel.updateNumber(paste.number)
                            val display = CardFieldInput.number(
                                paste.number,
                                lists.networks,
                                typing = numberFocused
                            )
                            numberValue = TextFieldValue(display, TextRange(display.length))
                            paste.expiry?.let { expiry ->
                                viewModel.updateExpiry(expiry)
                                expiryValue = TextFieldValue(expiry, TextRange(expiry.length))
                            }
                            snackbar.show(
                                context.getString(
                                    if (paste.expiry != null) {
                                        R.string.form_paste_number_and_expiry
                                    } else {
                                        R.string.form_paste_number
                                    }
                                )
                            )
                        } else {
                            viewModel.updateNumber(input.text)
                            val display = CardFieldInput.number(
                                input.text,
                                lists.networks,
                                typing = numberFocused
                            )
                            numberValue = TextFieldValue(display, TextRange(display.length))
                        }
                    },
                    label = stringResource(R.string.form_field_number),
                    helper = repairs[CardField.NUMBER]?.let {
                        context.getString(R.string.form_repair_changed_from, it.before)
                    }.orEmpty(),
                    error = cardErrorText(form.errors[Field.NUMBER]),
                    keyboardOptions = ScannableKeyboard,
                    trailing = if (network != null) {
                        {
                            // The trailing slot does not pad arbitrary content,
                            // so keep the label 16dp from the outline's end edge.
                            val mark = NetworkMarks.drawable(network.id)
                            if (mark != null) {
                                NetworkMark(
                                    resId = mark,
                                    contentDescription = network.name,
                                    modifier = Modifier.padding(end = QcDimens.Gutter),
                                    plate = networkMarkPlate(MarkSurface.FIELD, dark),
                                    width = QcDimens.NetworkMarkBandWidth,
                                    height = QcDimens.NetworkMarkBandHeight,
                                    corner = QcDimens.CornerBandFrame
                                )
                            } else Text(
                                text = network.code,
                                style = QcType.FieldLabel.copy(
                                    fontWeight = FontWeight(800),
                                    letterSpacing = QcType.BandNetwork.letterSpacing
                                ),
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.End,
                                modifier = Modifier
                                    .widthIn(
                                        min = QcDimens.NetworkBadgeMinWidth,
                                        max = QcDimens.NetworkBadgeMaxWidth
                                    )
                                    .padding(end = QcDimens.Gutter)
                            )
                        }
                    } else {
                        null
                    },
                    modifier = Modifier
                        .onGloballyPositioned {
                            positions[Field.NUMBER] = it.positionInParent().y.roundToInt()
                        }
                        .focusRequester(numberFocus)
                        .onFocusChanged { state ->
                            if (state.isFocused && !numberFocused) {
                                numberFocused = true
                            } else if (!state.isFocused && numberFocused) {
                                numberFocused = false
                                viewModel.numberBlur()
                            }
                        },
                    fieldTag = "form_number",
                    helperAction = if (repairs.containsKey(CardField.NUMBER)) {
                        { RepairUndo(onUndo = { viewModel.undoRepair(CardField.NUMBER) }) }
                    } else {
                        null
                    }
                )

                Spacer(modifier = Modifier.height(QcDimens.FormFieldGap))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .onGloballyPositioned {
                            val y = it.positionInParent().y.roundToInt()
                            positions[Field.EXPIRY] = y
                            positions[Field.CVV] = y
                        },
                    horizontalArrangement = Arrangement.spacedBy(QcDimens.Gutter)
                ) {
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
                            if (paste.expiry != null) {
                                viewModel.updateExpiry(paste.expiry)
                                expiryValue = TextFieldValue(
                                    paste.expiry,
                                    TextRange(paste.expiry.length)
                                )
                            } else {
                                val next = CardFieldInput.expiry(input.text)
                                viewModel.updateExpiry(input.text)
                                expiryValue = TextFieldValue(next, TextRange(next.length))
                            }
                        },
                        label = stringResource(R.string.form_field_expiry),
                        helper = repairs[CardField.EXPIRY]?.let {
                            context.getString(R.string.form_repair_changed_from, it.before)
                        } ?: stringResource(R.string.form_helper_expiry),
                        error = cardErrorText(form.errors[Field.EXPIRY]),
                        keyboardOptions = ScannableKeyboard,
                        fieldTag = "form_expiry",
                        helperAction = if (repairs.containsKey(CardField.EXPIRY)) {
                            { RepairUndo(onUndo = { viewModel.undoRepair(CardField.EXPIRY) }) }
                        } else {
                            null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(expiryFocus)
                            .onFocusChanged { state ->
                                if (state.isFocused) {
                                    expiryFocused = true
                                } else if (expiryFocused) {
                                    expiryFocused = false
                                    viewModel.expiryBlur()
                                }
                            }
                    )
                    FormTextField(
                        value = cvvValue,
                        onValueChange = { input ->
                            val next = CardFieldInput.cvv(input.text, form.network(lists.networks))
                            viewModel.updateCvv(input.text)
                            cvvValue = TextFieldValue(next, TextRange(next.length))
                        },
                        label = stringResource(R.string.form_field_cvv),
                        helper = repairs[CardField.CVV]?.let {
                            context.getString(R.string.form_repair_changed_from, it.before)
                        } ?: cvvHelperText(network),
                        error = cardErrorText(form.errors[Field.CVV]),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                        visualTransformation = PasswordVisualTransformation(),
                        fieldTag = "form_cvv",
                        helperAction = if (repairs.containsKey(CardField.CVV)) {
                            { RepairUndo(onUndo = { viewModel.undoRepair(CardField.CVV) }) }
                        } else {
                            null
                        },
                        modifier = Modifier
                            .weight(1f)
                            .focusRequester(cvvFocus)
                            .onFocusChanged { state ->
                                if (state.isFocused) {
                                    cvvFocused = true
                                } else if (cvvFocused) {
                                    cvvFocused = false
                                    viewModel.cvvBlur()
                                }
                            }
                    )
                }

                Spacer(modifier = Modifier.height(QcDimens.FormFieldGap))

                QcLookupField(
                    label = stringResource(R.string.form_field_bank),
                    value = lists.bankName(form.bankId).orEmpty(),
                    helper = "",
                    error = cardErrorText(form.errors[Field.BANK]),
                    onClick = { bankSheetOpen = true },
                    modifier = Modifier.onGloballyPositioned {
                        positions[Field.BANK] = it.positionInParent().y.roundToInt()
                    }
                )

                Spacer(modifier = Modifier.height(QcDimens.FormFieldGap))

                QcLookupField(
                    label = stringResource(R.string.form_field_type),
                    value = lists.typeName(form.typeId).orEmpty(),
                    helper = stringResource(R.string.form_helper_type),
                    error = null,
                    onClick = { typeSheetOpen = true }
                )

                Spacer(modifier = Modifier.height(QcDimens.FormFieldGap))

                Column {
                    GroupLabel(
                        stringResource(R.string.form_group_tags),
                        stringResource(R.string.form_tags_count, form.tagIds.size, lists.tagLimit)
                    )
                    Spacer(modifier = Modifier.height(10.dp))
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(QcDimens.Base),
                        verticalArrangement = Arrangement.spacedBy(QcDimens.Base)
                    ) {
                        form.tagIds.forEach { id ->
                            val name = lists.tagName(id) ?: return@forEach
                            TagChip(name = name, onRemove = { viewModel.removeTag(id) })
                        }
                        if (form.tagIds.size < lists.tagLimit) {
                            AddTagChip(onClick = { tagSheetOpen = true })
                        }
                    }
                }

                Spacer(modifier = Modifier.height(QcDimens.FormGroupGap))

                Column {
                    GroupLabel(stringResource(R.string.form_group_band))
                    Spacer(modifier = Modifier.height(10.dp))
                    BandColorGroup(
                        selected = form.bandColor,
                        onSelect = viewModel::selectBand
                    )
                }

                Spacer(modifier = Modifier.height(QcDimens.FormGroupGap))

                FormTextField(
                    value = dueDayValue,
                    onValueChange = { input ->
                        val next = input.text.filter(Char::isDigit).take(2)
                        viewModel.updateDueDay(next)
                        dueDayValue = TextFieldValue(next, TextRange(next.length))
                    },
                    label = stringResource(R.string.form_due_day_label),
                    helper = stringResource(R.string.form_due_day_helper),
                    error = if (form.dueDayValid) null else stringResource(R.string.form_due_day_error),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                Spacer(modifier = Modifier.height(QcDimens.FormFieldGap))

                FormTextField(
                    value = noteValue,
                    onValueChange = { input ->
                        val next = CardFieldInput.note(input.text)
                        viewModel.updateNote(next)
                        noteValue = TextFieldValue(next, TextRange(next.length))
                    },
                    label = stringResource(R.string.form_field_note),
                    helper = stringResource(R.string.form_helper_note),
                    counterMax = CardValidator.NOTE_MAX_LENGTH
                )

                Spacer(modifier = Modifier.height(QcDimens.Section))
            }
        }

        FormFooter(
            label = stringResource(
                if (form.mode == CardFormMode.EDIT) {
                    R.string.form_save_changes
                } else {
                    R.string.form_save_card
                }
            ),
            enabled = !saving && form.dueDayValid && (form.mode == CardFormMode.ADD || session.isDirty()),
            error = saveError,
            onClick = {
                saveError = null
                viewModel.save()
            }
        )
    }

    HolderPickerSheet(
        session = session,
        visible = holderSheetOpen,
        locked = locked,
        onDismiss = { holderSheetOpen = false },
        onPick = { id ->
            holderSheetOpen = false
            if (id != null) viewModel.selectHolder(id)
        },
        onAdd = {
            holderSheetOpen = false
            pendingEntity = EntityKind.HOLDER
        }
    )

    BankPickerSheet(
        session = session,
        visible = bankSheetOpen,
        locked = locked,
        onDismiss = { bankSheetOpen = false },
        onPick = { id ->
            bankSheetOpen = false
            if (id != null) viewModel.selectBank(id)
        },
        onAdd = {
            bankSheetOpen = false
            pendingEntity = EntityKind.BANK
        }
    )

    TypePickerSheet(
        session = session,
        visible = typeSheetOpen,
        locked = locked,
        onDismiss = { typeSheetOpen = false },
        onPick = { id ->
            typeSheetOpen = false
            viewModel.selectType(id)
        },
        onAdd = {
            typeSheetOpen = false
            pendingEntity = EntityKind.TYPE
        }
    )

    TagSheet(
        session = session,
        visible = tagSheetOpen,
        locked = locked,
        onDismiss = { tagSheetOpen = false },
        onToggle = { id -> viewModel.toggleTag(id) },
        onAdd = { name -> viewModel.createTag(name) },
        onTagCreated = { tag -> viewModel.onTagCreated(tag) }
    )

    val entityKind = pendingEntity
    if (entityKind != null && LockGate.visible(true, locked)) {
        EntityDialog(
            kind = entityKind,
            editingId = null,
            onDismiss = { pendingEntity = null },
            onSaved = { id -> viewModel.onEntitySaved(entityKind, id) }
        )
    }
}

@Composable
private fun FormFooter(label: String, enabled: Boolean, error: String?, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.background)
            .imePadding()
            .navigationBarsPadding()
    ) {
        HorizontalDivider(
            thickness = QcDimens.Divider,
            color = MaterialTheme.colorScheme.outlineVariant
        )
        if (error != null) {
            Text(
                text = error,
                style = QcType.Body,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(
                    start = QcDimens.Gutter,
                    end = QcDimens.Gutter,
                    top = QcDimens.PassGap
                )
            )
        }
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = QcDimens.Gutter,
                    end = QcDimens.Gutter,
                    top = QcDimens.PassGap,
                    bottom = 20.dp
                )
                .height(QcDimens.FooterAction),
            shape = CircleShape,
            contentPadding = PaddingValues(),
            colors = qcPrimaryButtonColors()
        ) {
            Text(text = label, style = SaveButtonText)
        }
    }
}

/** Compose extends this small target's touch bounds to 48dp without changing the line height. */
@Composable
private fun RepairUndo(onUndo: () -> Unit) {
    Text(
        text = stringResource(R.string.form_repair_undo),
        style = FormHelperText.copy(fontWeight = FontWeight(600)),
        color = MaterialTheme.colorScheme.onSurface,
        maxLines = 1,
        modifier = Modifier.clickable(role = Role.Button, onClick = onUndo)
    )
}

private fun CardFormSession.previewCard(bankFallback: String): HomeCard {
    val form = this.form
    val network = form.network(lists.networks)
    val holderName = lists.holderName(form.holderId).orEmpty()
    return HomeCard(
        card = VaultCard(
            id = form.editingId.orEmpty(),
            number = form.number,
            expiry = form.expiry,
            cvv = "",
            holderId = form.holderId,
            bankId = form.bankId,
            networkId = null,
            networkAuto = true,
            typeId = form.typeId,
            nameOnCard = holderName.uppercase(java.util.Locale.ROOT),
            cardName = "",
            note = form.note,
            bandColor = form.bandColor,
            tagIds = form.tagIds
        ),
        bankName = lists.bankName(form.bankId) ?: bankFallback,
        holderName = holderName,
        typeName = lists.typeName(form.typeId),
        network = network,
        networkId = network?.id,
        tagNames = form.tagIds.mapNotNull { lists.tagName(it) }
    )
}
