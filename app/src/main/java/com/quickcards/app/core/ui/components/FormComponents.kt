@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.quickcards.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.error as semanticsError
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.quickcards.app.R
import com.quickcards.app.core.security.AppLockController
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

val FormHelperText = TextStyle(
    fontSize = 12.sp,
    lineHeight = 16.sp,
    fontWeight = FontWeight(400),
    fontFeatureSettings = "tnum",
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)

val FormInputText = TextStyle(
    fontSize = 16.sp,
    lineHeight = 24.sp,
    fontWeight = FontWeight(400),
    fontFeatureSettings = "tnum",
    platformStyle = PlatformTextStyle(includeFontPadding = false)
)

@Composable
fun formFieldColors() = OutlinedTextFieldDefaults.colors(
    focusedTextColor = MaterialTheme.colorScheme.onSurface,
    unfocusedTextColor = MaterialTheme.colorScheme.onSurface,
    disabledTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    errorTextColor = MaterialTheme.colorScheme.onSurface,
    focusedContainerColor = Color.Transparent,
    unfocusedContainerColor = Color.Transparent,
    disabledContainerColor = Color.Transparent,
    errorContainerColor = Color.Transparent,
    cursorColor = MaterialTheme.colorScheme.onSurface,
    errorCursorColor = MaterialTheme.colorScheme.onSurface,
    focusedBorderColor = MaterialTheme.colorScheme.onSurface,
    unfocusedBorderColor = MaterialTheme.colorScheme.outline,
    disabledBorderColor = MaterialTheme.colorScheme.outlineVariant,
    errorBorderColor = MaterialTheme.colorScheme.error,
    focusedLabelColor = MaterialTheme.colorScheme.onSurface,
    unfocusedLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
    errorLabelColor = MaterialTheme.colorScheme.error,
    focusedSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    disabledSupportingTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
    errorSupportingTextColor = MaterialTheme.colorScheme.error,
    focusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    unfocusedTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
    errorTrailingIconColor = MaterialTheme.colorScheme.onSurfaceVariant
)

@Composable
fun FormSupportingLine(
    helper: String,
    error: String?,
    counter: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    val color = if (error != null) {
        MaterialTheme.colorScheme.error
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 16.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = error ?: helper,
            style = FormHelperText,
            color = color,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (counter != null) {
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = counter,
                style = FormHelperText,
                color = color,
                maxLines = 1,
                textAlign = TextAlign.End
            )
        }
        if (action != null) {
            Spacer(modifier = Modifier.width(12.dp))
            action()
        }
    }
}

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
    helperAction: (@Composable () -> Unit)? = null
) {
    val scope = rememberCoroutineScope()
    val bringIntoView = remember { BringIntoViewRequester() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoView)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                AppLockController.onUserInteraction()
                onValueChange(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (focusRequester != null) {
                        Modifier.focusRequester(focusRequester)
                    } else {
                        Modifier
                    }
                )
                .then(if (fieldTag != null) Modifier.testTag(fieldTag) else Modifier)
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
            textStyle = FormInputText,
            label = { Text(label) },
            trailingIcon = trailing,
            isError = error != null,
            visualTransformation = visualTransformation,
            keyboardOptions = keyboardOptions,
            shape = RoundedCornerShape(4.dp),
            colors = formFieldColors()
        )
        Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            FormSupportingLine(
                helper = helper,
                error = error,
                counter = counterMax?.let { "${value.text.length}/$it" },
                action = helperAction
            )
        }
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
    fieldTag: String? = null
) {
    val scope = rememberCoroutineScope()
    val bringIntoView = remember { BringIntoViewRequester() }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .bringIntoViewRequester(bringIntoView)
    ) {
        OutlinedTextField(
            value = value,
            onValueChange = {
                AppLockController.onUserInteraction()
                onValueChange(it)
            },
            modifier = Modifier
                .fillMaxWidth()
                .then(if (fieldTag != null) Modifier.testTag(fieldTag) else Modifier)
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
            textStyle = FormInputText,
            label = { Text(label) },
            isError = error != null,
            keyboardOptions = keyboardOptions,
            shape = RoundedCornerShape(4.dp),
            colors = formFieldColors()
        )
        Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            FormSupportingLine(
                helper = helper,
                error = error,
                counter = counterMax?.let { "${value.length}/$it" }
            )
        }
    }
}

@Composable
fun QcLookupField(
    label: String,
    value: String,
    helper: String,
    error: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val description = if (value.isBlank()) label else "$label, $value"
    Column(modifier = modifier.fillMaxWidth()) {
        Box {
            OutlinedTextField(
                value = value,
                onValueChange = {},
                readOnly = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .clearAndSetSemantics { },
                singleLine = true,
                textStyle = FormInputText,
                label = { Text(label) },
                trailingIcon = {
                    Icon(
                        imageVector = QcIcons.dropDown,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp)
                    )
                },
                isError = error != null,
                shape = RoundedCornerShape(4.dp),
                colors = formFieldColors()
            )
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .clickable(role = Role.Button, onClick = onClick)
                    .semantics {
                        contentDescription = description
                        role = Role.Button
                        if (error != null) semanticsError(error)
                        this.onClick(label = description) {
                            onClick()
                            true
                        }
                    }
            )
        }
        Box(modifier = Modifier.fillMaxWidth().padding(top = 4.dp)) {
            FormSupportingLine(helper = helper, error = error)
        }
    }
}

@Composable
fun TagChip(name: String, onRemove: () -> Unit) {
    Box(
        modifier = Modifier.height(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .height(32.dp)
                .background(
                    MaterialTheme.colorScheme.surfaceContainerHigh,
                    RoundedCornerShape(8.dp)
                )
                .padding(start = 12.dp, end = 40.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = name,
                style = QcType.Chip,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .size(48.dp)
                .clickable(role = Role.Button, onClick = onRemove),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = QcIcons.close,
                contentDescription = stringResource(R.string.common_remove_tag, name),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

@Composable
fun AddTagChip(onClick: () -> Unit) {
    val addTag = stringResource(R.string.common_add_tag)
    val outline = MaterialTheme.colorScheme.outlineVariant
    Box(
        modifier = Modifier.height(48.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .height(32.dp)
                .drawBehind {
                    drawRoundRect(
                        color = outline,
                        cornerRadius = androidx.compose.ui.geometry.CornerRadius(8.dp.toPx()),
                        style = Stroke(
                            width = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(
                                floatArrayOf(4.dp.toPx(), 4.dp.toPx())
                            )
                        )
                    )
                }
                .padding(start = 8.dp, end = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                imageVector = QcIcons.add,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(18.dp)
            )
            Text(
                text = addTag,
                style = QcType.Chip,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
        Box(
            modifier = Modifier
                .matchParentSize()
                .clickable(role = Role.Button, onClick = onClick)
                .semantics { contentDescription = addTag }
        )
    }
}

@Composable
fun GroupLabel(label: String, counter: String? = null) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = QcType.Body.copy(fontWeight = FontWeight(600)),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.weight(1f)
        )
        if (counter != null) {
            Text(
                text = counter,
                style = FormHelperText,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
fun LeadSlot(content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.size(width = 56.dp, height = 56.dp),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Composable
fun SheetRow(
    title: String,
    onClick: (() -> Unit)? = null,
    lead: @Composable () -> Unit,
    trailing: (@Composable () -> Unit)? = null,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .alpha(if (enabled) 1f else 0.38f)
            .then(
                if (onClick != null && enabled) {
                    Modifier.clickable(role = Role.Button, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        LeadSlot(lead)
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = title,
            style = QcType.BodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
        if (trailing != null) {
            Spacer(modifier = Modifier.width(8.dp))
            trailing()
        }
    }
}

@Composable
fun MonogramTile(text: String) {
    Box(
        modifier = Modifier
            .size(40.dp)
            .background(
                MaterialTheme.colorScheme.surfaceContainerHighest,
                RoundedCornerShape(12.dp)
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = QcType.Label.copy(fontSize = 13.sp, fontWeight = FontWeight(700)),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Composable
fun CheckRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .alpha(if (enabled) 1f else 0.38f)
            .toggleable(
                value = checked,
                enabled = enabled,
                role = Role.Checkbox,
                onValueChange = onCheckedChange
            )
            .padding(horizontal = 24.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
            Checkbox(
                checked = checked,
                onCheckedChange = null,
                enabled = enabled,
                modifier = Modifier.size(18.dp),
                colors = CheckboxDefaults.colors(
                    checkedColor = MaterialTheme.colorScheme.secondary,
                    checkmarkColor = MaterialTheme.colorScheme.onSecondary,
                    uncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    disabledCheckedColor = MaterialTheme.colorScheme.secondary,
                    disabledUncheckedColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = label,
            style = FormInputText,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f)
        )
    }
}

fun TextFieldValue.withCaretAtEnd(): TextFieldValue =
    copy(selection = TextRange(text.length))
