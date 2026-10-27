@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.quickcards.app.feature.backup

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.error as semanticsError
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quickcards.app.R
import com.quickcards.app.core.security.AppLockController
import com.quickcards.app.core.ui.components.FormInputText
import com.quickcards.app.core.ui.components.FormSupportingLine
import com.quickcards.app.core.ui.components.formFieldColors
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal val BackupCheckboxLabel: TextStyle = QcType.Body
internal val BackupRadioLabel: TextStyle = QcType.BodyLarge.copy(fontWeight = FontWeight(400))
internal val BackupFileName: TextStyle = QcType.Body.copy(fontWeight = FontWeight(600))
internal val BackupFileMeta: TextStyle = QcType.Label.copy(fontWeight = FontWeight(400))
internal val BackupGroupLabel: TextStyle = QcType.Body.copy(fontWeight = FontWeight(600))

@Composable
internal fun BackupPasswordField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    helper: String,
    error: String?,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    modifier: Modifier = Modifier,
    focusRequester: FocusRequester? = null,
    enabled: Boolean = true
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
                    if (focusRequester != null) Modifier.focusRequester(focusRequester) else Modifier
                )
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
            enabled = enabled,
            isError = error != null,
            textStyle = FormInputText,
            label = { Text(label) },
            trailingIcon = {
                IconButton(
                    onClick = onToggleVisible,
                    modifier = Modifier.size(QcDimens.Touch)
                ) {
                    Icon(
                        imageVector = if (visible) QcIcons.visibilityOff else QcIcons.visibility,
                        contentDescription = stringResource(
                            if (visible) {
                                R.string.backup_a11y_hide_password
                            } else {
                                R.string.backup_a11y_show_password
                            }
                        ),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(QcDimens.Icon)
                    )
                }
            },
            visualTransformation = if (visible) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Done
            ),
            shape = RoundedCornerShape(QcDimens.CornerXs),
            colors = formFieldColors()
        )
        Box(modifier = Modifier.fillMaxWidth().padding(top = QcDimens.Half)) {
            FormSupportingLine(helper = helper, error = error)
        }
    }
}

@Composable
internal fun BackupCheckRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    labelStyle: TextStyle = BackupCheckboxLabel
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = QcDimens.Touch)
            .toggleable(
                value = checked,
                role = Role.Checkbox,
                onValueChange = onCheckedChange
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(QcDimens.HelperLine), contentAlignment = Alignment.Center) {
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
            style = labelStyle,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
internal fun BackupRadioRow(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = QcDimens.Touch)
            .selectable(
                selected = selected,
                role = Role.RadioButton,
                onClick = onClick
            ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(QcDimens.HelperLine), contentAlignment = Alignment.Center) {
            val selectedColor = MaterialTheme.colorScheme.secondary
            val unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
            Canvas(modifier = Modifier.size(QcDimens.HelperLine)) {
                val stroke = 2.dp.toPx()
                val radius = (size.minDimension - stroke) / 2f
                drawCircle(
                    color = if (selected) selectedColor else unselectedColor,
                    radius = radius,
                    style = Stroke(width = stroke)
                )
                if (selected) {
                    drawCircle(color = selectedColor, radius = 5.dp.toPx())
                }
            }
        }
        Spacer(modifier = Modifier.width(QcDimens.Gutter))
        Text(
            text = label,
            style = BackupRadioLabel,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

/** The framed count list used by the import review. */
@Composable
internal fun BackupReviewList(rows: List<Pair<String, String>>) {
    if (rows.isEmpty()) return
    val stacked = LocalDensity.current.fontScale > 1.15f
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = QcDimens.Divider,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(QcDimens.CornerMd)
            )
    ) {
        rows.forEachIndexed { index, row ->
            if (index > 0) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            }
            if (stacked) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = row.first,
                        style = QcType.Body,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = row.second,
                        style = QcType.Body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = row.first,
                        style = QcType.Body,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = row.second,
                        style = QcType.Body,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.End
                    )
                }
            }
        }
    }
}

/** One line under the review list: "Formats fixed: n · Need a check: m". */
@Composable
internal fun ImportFormatsSummary(formatsFixed: Int, needCheck: Int) {
    if (!showsFormatSummary(formatsFixed, needCheck)) return
    Text(
        text = stringResource(R.string.backup_review_formats, formatsFixed, needCheck),
        style = QcType.Body,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = QcDimens.PassGap)
    )
}

@Composable
internal fun BackupFileRow(
    fileName: String,
    meta: String,
    onChange: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = QcDimens.Divider,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(QcDimens.CornerMd)
            )
            .padding(start = 12.dp, top = 10.dp, bottom = 10.dp, end = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = QcIcons.file,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(QcDimens.Icon)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = fileName,
                style = BackupFileName,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = meta,
                style = BackupFileMeta,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        TextButton(
            onClick = onChange,
            colors = ButtonDefaults.textButtonColors(
                contentColor = MaterialTheme.colorScheme.onSurface
            )
        ) {
            Text(stringResource(R.string.backup_action_change))
        }
    }
}

@Composable
internal fun BackupOutlinedButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        enabled = enabled,
        shape = RoundedCornerShape(20.dp),
        border = BorderStroke(QcDimens.Divider, MaterialTheme.colorScheme.outline),
        contentPadding = PaddingValues(horizontal = 24.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface,
            disabledContentColor = MaterialTheme.colorScheme.onSurface.copy(
                alpha = QcDimens.DisabledAlpha
            )
        )
    ) {
        Icon(
            imageVector = QcIcons.file,
            contentDescription = null,
            modifier = Modifier.size(QcDimens.IconSmall)
        )
        Spacer(modifier = Modifier.width(QcDimens.Base))
        Text(label, style = QcType.Body.copy(fontWeight = FontWeight(600)))
    }
}
