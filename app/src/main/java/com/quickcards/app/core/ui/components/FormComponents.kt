@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.quickcards.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import com.quickcards.app.R
import com.quickcards.app.core.security.AppLockController
import com.quickcards.app.core.ui.theme.LocalQcRaised
import com.quickcards.app.core.ui.theme.QcCheckbox
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcShapes
import com.quickcards.app.core.ui.theme.QcSize
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.semantics.error as semanticsError

val FormHelperText: TextStyle = QcText.F1Regular

val FormInputText: TextStyle = QcText.F3

/** Real bullets for CVV, PIN and password fields, drawn in Geist Mono like masked digits. */
val QcMask = PasswordVisualTransformation('•')

/** Colours for the few screens that still host a Material [androidx.compose.material3.OutlinedTextField]. */
@Composable
fun formFieldColors(): TextFieldColors {
    val colors = QcTheme.colors
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = colors.ink,
        unfocusedTextColor = colors.ink,
        disabledTextColor = colors.ink3,
        errorTextColor = colors.ink,
        focusedContainerColor = colors.surface,
        unfocusedContainerColor = colors.surface,
        disabledContainerColor = colors.tonal,
        errorContainerColor = colors.surface,
        cursorColor = colors.accent,
        errorCursorColor = colors.danger,
        focusedBorderColor = colors.accent,
        unfocusedBorderColor = colors.outline,
        disabledBorderColor = Color.Transparent,
        errorBorderColor = colors.danger,
        focusedLabelColor = colors.accent,
        unfocusedLabelColor = colors.ink2,
        disabledLabelColor = colors.ink3,
        errorLabelColor = colors.danger,
        focusedPlaceholderColor = colors.inkPh,
        unfocusedPlaceholderColor = colors.inkPh,
        focusedSupportingTextColor = colors.ink2,
        unfocusedSupportingTextColor = colors.ink2,
        disabledSupportingTextColor = colors.ink3,
        errorSupportingTextColor = colors.danger,
        focusedTrailingIconColor = colors.ink2,
        unfocusedTrailingIconColor = colors.ink2,
        errorTrailingIconColor = colors.ink2
    )
}

/**
 * The help line under a field (`f1`): [error] in `danger`, [warn] in `warn` (never blocks),
 * otherwise [helper] in `ink-2`. Renders nothing when there is nothing to say.
 */
@Composable
fun FormSupportingLine(
    helper: String,
    error: String?,
    counter: String? = null,
    action: (@Composable () -> Unit)? = null,
    warn: String? = null
) {
    val colors = QcTheme.colors
    val text = error ?: warn ?: helper
    if (text.isEmpty() && counter == null && action == null) return
    val color = when {
        error != null -> colors.danger
        warn != null -> colors.warn
        else -> colors.ink2
    }
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(QcSpacing.s3)
    ) {
        Text(
            text = text,
            style = FormHelperText,
            color = color,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (counter != null) {
            Text(
                text = counter,
                style = FormHelperText,
                color = color,
                maxLines = 1,
                textAlign = TextAlign.End
            )
        }
        action?.invoke()
    }
}

/** Field label above the box: `f2`/500 `ink-2`; accent while focused, `danger` on error. */
@Composable
private fun FieldLabel(label: String, focused: Boolean, error: Boolean) {
    val colors = QcTheme.colors
    Text(
        text = label,
        style = QcText.F2Medium,
        color = when {
            error -> colors.danger
            focused -> colors.accent
            else -> colors.ink2
        },
        modifier = Modifier.clearAndSetSemantics { }
    )
}

/** The outlined box: min 48, `r-md`, 1dp `outline`; 2dp accent focused, 2dp `danger` on error. */
private fun Modifier.fieldBox(
    focused: Boolean,
    error: Boolean,
    surface: Color,
    outline: Color,
    accent: Color,
    danger: Color
): Modifier = this
    .fillMaxWidth()
    .heightIn(min = QcSize.field)
    .background(surface, QcShapes.md)
    .border(
        width = if (focused || error) QcSize.focus else QcSize.hairline,
        color = when {
            error -> danger
            focused -> accent
            else -> outline
        },
        shape = QcShapes.md
    )
    .padding(start = QcSpacing.s3, end = QcSpacing.s1)

@Composable
private fun FieldBox(
    focused: Boolean,
    error: Boolean,
    modifier: Modifier = Modifier,
    content: @Composable RowScope.() -> Unit
) {
    val colors = QcTheme.colors
    Row(
        modifier = modifier.fieldBox(focused, error, colors.surface, colors.outline, colors.accent, colors.danger),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(QcSpacing.s2),
        content = content
    )
}

/** Brings a focused field above the keyboard once the IME has settled. */
@Composable
private fun Modifier.bringIntoViewOnFocus(): Modifier {
    val scope = rememberCoroutineScope()
    val requester = remember { BringIntoViewRequester() }
    return bringIntoViewRequester(requester).onFocusChanged { state ->
        if (state.isFocused) {
            scope.launch {
                delay(350)
                runCatching { requester.bringIntoView() }
            }
        }
    }
}

/**
 * Outlined text field with its label above (DESIGN.md › Inputs). [placeholder] shows in
 * `ink-ph`; [mono] sets number, expiry and CVV in Geist Mono; a [PasswordVisualTransformation]
 * draws real bullets. [trailing] sits inside the box (inline action, eye, network mark).
 */
@Composable
fun FormTextField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    helper: String = "",
    error: String? = null,
    counterMax: Int? = null,
    trailing: (@Composable () -> Unit)? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    fieldTag: String? = null,
    focusRequester: FocusRequester? = null,
    helperAction: (@Composable () -> Unit)? = null,
    placeholder: String? = null,
    warn: String? = null,
    mono: Boolean = false,
    labelAction: (@Composable () -> Unit)? = null,
    keyboardActions: KeyboardActions = KeyboardActions.Default
) {
    val colors = QcTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    val masked = visualTransformation is PasswordVisualTransformation
    val textStyle = when {
        masked && value.text.isNotEmpty() -> QcText.MonoDotsF3
        mono || masked -> QcText.MonoF3
        else -> FormInputText
    }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewOnFocus(),
        verticalArrangement = Arrangement.spacedBy(QcSpacing.s1)
    ) {
        if (labelAction == null) {
            FieldLabel(label, focused, error != null)
        } else {
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.weight(1f)) { FieldLabel(label, focused, error != null) }
                labelAction()
            }
        }
        BasicTextField(
            value = value,
            onValueChange = {
                AppLockController.onUserInteraction()
                onValueChange(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .then(if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier)
                .then(if (fieldTag != null) Modifier.testTag(fieldTag) else Modifier)
                .semantics {
                    contentDescription = label
                    if (error != null) semanticsError(error)
                },
            singleLine = true,
            textStyle = textStyle.copy(color = colors.ink),
            cursorBrush = SolidColor(if (error != null) colors.danger else colors.accent),
            keyboardOptions = keyboardOptions,
            keyboardActions = keyboardActions,
            visualTransformation = visualTransformation,
            interactionSource = interaction,
            decorationBox = { inner ->
                FieldBox(focused = focused, error = error != null) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .padding(vertical = QcSpacing.s3),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (value.text.isEmpty() && placeholder != null) {
                            Text(
                                text = placeholder,
                                style = if (mono) QcText.MonoF3 else FormInputText,
                                color = colors.inkPh,
                                maxLines = 1
                            )
                        }
                        inner()
                    }
                    trailing?.invoke()
                }
            }
        )
        FormSupportingLine(
            helper = helper,
            error = error,
            counter = counterMax?.let { "${value.text.length}/$it" },
            action = helperAction,
            warn = warn
        )
    }
}

@Composable
fun FormTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    helper: String = "",
    error: String? = null,
    counterMax: Int? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    fieldTag: String? = null,
    placeholder: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    var field by remember { mutableStateOf(TextFieldValue(value, TextRange(value.length))) }
    val shown = if (field.text == value) field else TextFieldValue(value, TextRange(value.length))
    FormTextField(
        value = shown,
        onValueChange = {
            field = it
            onValueChange(it.text)
        },
        label = label,
        modifier = modifier,
        helper = helper,
        error = error,
        counterMax = counterMax,
        trailing = trailing,
        keyboardOptions = keyboardOptions,
        fieldTag = fieldTag,
        placeholder = placeholder
    )
}

/** A search field inside sheets and lists: outlined box, leading search icon, `ink-ph` placeholder. */
@Composable
fun QcSearchField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    description: String = placeholder
) {
    val colors = QcTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()
    BasicTextField(
        value = value,
        onValueChange = {
            AppLockController.onUserInteraction()
            onValueChange(it)
        },
        modifier = modifier
            .fillMaxWidth()
            .semantics { contentDescription = description },
        singleLine = true,
        // Searches can hold card digits; keep them out of the keyboard's suggestions.
        keyboardOptions = KeyboardOptions(autoCorrectEnabled = false),
        textStyle = FormInputText.copy(color = colors.ink),
        cursorBrush = SolidColor(colors.accent),
        interactionSource = interaction,
        decorationBox = { inner ->
            FieldBox(focused = focused, error = false) {
                Icon(
                    imageVector = QcIcons.search,
                    contentDescription = null,
                    tint = colors.ink2,
                    modifier = Modifier.size(QcSize.icon)
                )
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .padding(vertical = QcSpacing.s3),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (value.isEmpty()) {
                        Text(text = placeholder, style = FormInputText, color = colors.inkPh, maxLines = 1)
                    }
                    inner()
                }
            }
        }
    )
}

/** Show/hide for masked fields: a 48dp icon button inside the field. */
@Composable
fun QcVisibilityToggle(visible: Boolean, onToggle: () -> Unit, showLabel: String, hideLabel: String) {
    IconButton(onClick = onToggle, modifier = Modifier.size(QcSize.hit)) {
        Icon(
            imageVector = if (visible) QcIcons.visibilityOff else QcIcons.visibility,
            contentDescription = if (visible) hideLabel else showLabel,
            tint = QcTheme.colors.ink2,
            modifier = Modifier.size(QcSize.iconLg)
        )
    }
}

/**
 * A picker field (Holder, Bank, Type, Colour): a button that opens the picker sheet. Shows the
 * value or [placeholder] in `ink-ph`, an optional [lead] (colour swatch) and a caret.
 */
@Composable
fun QcLookupField(
    label: String,
    value: String,
    helper: String,
    error: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "",
    lead: (@Composable () -> Unit)? = null
) {
    val colors = QcTheme.colors
    val state = value.ifBlank { stringResource(R.string.common_not_set) }
    val chooseLabel = stringResource(R.string.common_choose)
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(QcSpacing.s1)
    ) {
        FieldLabel(label, focused = false, error = error != null)
        FieldBox(
            focused = false,
            error = error != null,
            modifier = Modifier
                .clip(QcShapes.md)
                .clickable(role = Role.DropdownList, onClickLabel = chooseLabel, onClick = onClick)
                .semantics {
                    contentDescription = label
                    stateDescription = state
                    role = Role.DropdownList
                    if (error != null) semanticsError(error)
                    this.onClick(label = chooseLabel) {
                        onClick()
                        true
                    }
                }
        ) {
            lead?.invoke()
            Text(
                text = value.ifBlank { placeholder },
                style = FormInputText,
                color = if (value.isBlank()) colors.inkPh else colors.ink,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = QcSpacing.s3)
                    .clearAndSetSemantics { }
            )
            Icon(
                imageVector = QcIcons.dropDown,
                contentDescription = null,
                tint = colors.ink2,
                modifier = Modifier
                    .padding(end = QcSpacing.s2)
                    .size(QcSize.iconLg)
            )
        }
        FormSupportingLine(helper = helper, error = error)
    }
}

@Composable
fun TagChip(name: String, onRemove: () -> Unit) {
    val colors = QcTheme.colors
    Box(
        modifier = Modifier.heightIn(min = QcSize.hit),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = QcSize.ctl)
                .background(colors.surface, QcShapes.full)
                .border(QcSize.hairline, colors.line, QcShapes.full)
                .padding(start = QcSpacing.s3, end = QcSpacing.s10),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = name, style = QcText.F2Medium, color = colors.ink, maxLines = 1)
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(QcSize.hit)
                .clip(QcShapes.full)
                .clickable(role = Role.Button, onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = QcIcons.close,
                contentDescription = stringResource(R.string.common_remove_tag, name),
                tint = colors.ink2,
                modifier = Modifier.size(QcSize.iconSm)
            )
        }
    }
}

@Composable
fun AddTagChip(onClick: () -> Unit) {
    val colors = QcTheme.colors
    val addTag = stringResource(R.string.common_add_tag)
    val outline = colors.outline
    Box(
        modifier = Modifier.heightIn(min = QcSize.hit),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = QcSize.ctl)
                .drawBehind {
                    drawRoundRect(
                        color = outline,
                        cornerRadius = CornerRadius(size.height / 2),
                        style = Stroke(
                            width = QcSize.hairline.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(QcSpacing.s1.toPx(), QcSpacing.s1.toPx())
                            )
                        )
                    )
                }
                .padding(start = QcSpacing.s2, end = QcSpacing.s3),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(QcSpacing.s1)
        ) {
            Icon(
                imageVector = QcIcons.add,
                contentDescription = null,
                tint = colors.ink2,
                modifier = Modifier.size(QcSize.iconSm)
            )
            Text(text = addTag, style = QcText.F2Medium, color = colors.ink2, maxLines = 1)
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = addTag }
        )
    }
}

/** A label above a block of controls inside an editor (`f2`/600 `ink-2`). */
@Composable
fun GroupLabel(label: String, counter: String? = null) {
    val colors = QcTheme.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = QcText.F2Semibold,
            color = colors.ink2,
            modifier = Modifier.weight(1f)
        )
        if (counter != null) {
            Text(text = counter, style = FormHelperText, color = colors.ink2, maxLines = 1)
        }
    }
}

/** Leading slot of a sheet row: as wide as its content, at least one 24dp icon. */
@Composable
fun LeadSlot(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.defaultMinSize(minWidth = QcSize.iconLg),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

/** A row inside a sheet: content on the 24 inset, min 56, lead · 16 · title · trailing. */
@Composable
fun SheetRow(
    title: String,
    onClick: (() -> Unit)? = null,
    lead: (@Composable () -> Unit)?,
    trailing: (@Composable () -> Unit)? = null,
    enabled: Boolean = true
) {
    val colors = QcTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = QcSize.row)
            .then(
                if (onClick != null) {
                    Modifier.clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = QcSpacing.s6, vertical = QcSpacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(QcSpacing.s4)
    ) {
        if (lead != null) LeadSlot(lead)
        Text(
            text = title,
            style = QcText.F3,
            color = if (enabled) colors.ink else colors.ink3,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        trailing?.invoke()
    }
}

/** Bank monogram on a tonal tile (`r-md`, 40). */
@Composable
fun MonogramTile(text: String) {
    val colors = QcTheme.colors
    Box(
        modifier = Modifier
            .size(QcSpacing.s10)
            .background(if (LocalQcRaised.current) colors.tonalHi else colors.tonal, QcShapes.md),
        contentAlignment = Alignment.Center
    ) {
        Text(text = text, style = QcText.F2Semibold, color = colors.ink, maxLines = 1)
    }
}

/** A checkbox line: the whole row toggles; box 20 on the content edge ([inset]), 12 to its label. */
@Composable
fun CheckRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    inset: Dp = QcSpacing.s6,
    spokenLabel: String? = null
) {
    val colors = QcTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = QcSize.hit)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange
            )
            .padding(horizontal = inset),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(QcSpacing.s3)
    ) {
        QcCheckbox(checked = checked, enabled = enabled)
        Text(
            text = label,
            style = FormInputText,
            color = if (enabled) colors.ink else colors.ink3,
            modifier = Modifier
                .weight(1f)
                .then(if (spokenLabel != null) Modifier.clearAndSetSemantics { contentDescription = spokenLabel } else Modifier)
        )
    }
}

fun TextFieldValue.withCaretAtEnd(): TextFieldValue =
    copy(selection = TextRange(text.length))
