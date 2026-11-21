package com.quickcards.app.core.ui.entity

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.withStyle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.quickcards.app.R
import com.quickcards.app.core.data.settings.AppSettingsDefaults
import com.quickcards.app.core.ui.art.ArtKind
import com.quickcards.app.core.ui.art.CardArt
import com.quickcards.app.core.ui.components.FormTextField
import com.quickcards.app.core.ui.components.LocalSnackbar
import com.quickcards.app.core.ui.components.MarkFit
import com.quickcards.app.core.ui.components.NetworkMark
import com.quickcards.app.core.ui.components.QcDialog
import com.quickcards.app.core.ui.components.dialogEdgeToEdge
import com.quickcards.app.core.ui.components.markTone
import com.quickcards.app.core.ui.theme.QcSize
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTextButton
import com.quickcards.app.core.ui.theme.QcTextTone
import com.quickcards.app.core.ui.theme.QcTheme
import com.quickcards.domain.EntityKind
import com.quickcards.domain.EntityValidator
import kotlinx.coroutines.launch

/**
 * Quick add from a card form picker ("New holder", "New bank", "New type"): one Name field and
 * Add, which stays off until the name is valid. Delivers the new id via [onSaved] after closing.
 */
@Composable
fun EntityDialog(
    kind: EntityKind,
    onDismiss: () -> Unit,
    onSaved: (id: String) -> Unit
) {
    val viewModel: EntityEditorViewModel = viewModel()
    val data by viewModel.data.collectAsState()
    if (!data.ready) return
    val form = rememberEntityForm(kind, null, null, EntityValidator.nameLimit(kind, AppSettingsDefaults.TAGS_MAX_LEN))
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()
    val one = stringResource(kind.oneRes)
    val addedMessage = stringResource(R.string.manage_entity_added, stringResource(kind.oneTitleRes))
    val failedMessage = stringResource(R.string.manage_save_failed, one)
    var saving by remember { mutableStateOf(false) }
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    val errors = EntityValidator.validate(form.draft(), data.refs(kind), null)
    val shown = errors.visible(form)

    QcDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.manage_dialog_new, one),
        actions = {
            QcTextButton(stringResource(R.string.manage_action_cancel), onDismiss, tone = QcTextTone.Neutral)
            QcTextButton(
                text = stringResource(R.string.manage_action_add),
                enabled = errors.isEmpty() && !saving,
                onClick = {
                    saving = true
                    scope.launch {
                        viewModel.save(kind, null, form.draft())
                            .onSuccess { id ->
                                onDismiss()
                                onSaved(id)
                                snackbar.show(addedMessage)
                            }
                            .onFailure { snackbar.show(failedMessage) }
                        saving = false
                    }
                }
            )
        }
    ) {
        FormTextField(
            value = form.name,
            onValueChange = form::updateName,
            label = stringResource(R.string.manage_field_name),
            error = shown.values.firstOrNull()?.let { entityErrorText(it) },
            focusRequester = focus,
            keyboardOptions = KeyboardOptions(
                capitalization = if (kind == EntityKind.TAG) KeyboardCapitalization.None else KeyboardCapitalization.Words
            )
        )
    }
}

/** "Update 3 cards?": what the rename does, then each affected card as a mini row. */
@Composable
internal fun UpdateCardsDialog(
    cards: List<ManageCardItem>,
    beforeName: String,
    nextName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    val count = pluralStringResource(R.plurals.manage_usage_cards, cards.size, cards.size)
    QcDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.manage_update_title, count),
        actions = {
            QcTextButton(stringResource(R.string.manage_action_cancel), onDismiss, tone = QcTextTone.Neutral)
            QcTextButton(stringResource(R.string.manage_update_confirm, count), onConfirm)
        }
    ) {
        if (beforeName != nextName) {
            DialogBody(stringResource(R.string.manage_warning_renaming, beforeName, nextName))
        }
        Column(modifier = Modifier.dialogEdgeToEdge().padding(top = QcSpacing.s2)) {
            cards.forEachIndexed { index, card ->
                if (index > 0) HorizontalDivider(thickness = QcSize.hairline, color = QcTheme.colors.line)
                UsedCardRow(card)
            }
        }
    }
}

@Composable
private fun UsedCardRow(card: ManageCardItem) {
    val colors = QcTheme.colors
    val label = stringResource(R.string.manage_card_list_item, card.bankName, card.lastFour)
    val masked = label.removePrefix(card.bankName)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = QcSize.row2)
            .padding(horizontal = QcSpacing.s6, vertical = QcSpacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(QcSpacing.s4)
    ) {
        CardArt(band = card.bandColor, kind = ArtKind.MINI) {
            val network = card.networkId
            if (network != null) {
                Box(modifier = Modifier.align(Alignment.BottomEnd)) {
                    NetworkMark(network, null, tone = markTone(network, onArt = true, dark = colors.dark), fit = MarkFit.MINI)
                }
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = buildAnnotatedString {
                    append(card.bankName)
                    withStyle(SpanStyle(fontFamily = QcText.MonoF3.fontFamily, fontFeatureSettings = "tnum")) { append(masked) }
                },
                style = QcText.F3,
                color = colors.ink,
                maxLines = 1
            )
            if (card.typeName.isNotBlank()) {
                Text(text = card.typeName, style = QcText.F2, color = colors.ink2, maxLines = 1)
            }
        }
    }
}
