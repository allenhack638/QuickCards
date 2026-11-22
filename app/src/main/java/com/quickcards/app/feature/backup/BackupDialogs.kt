package com.quickcards.app.feature.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import com.quickcards.app.R
import com.quickcards.app.core.ui.components.CheckRow
import com.quickcards.app.core.ui.components.QcDialog
import com.quickcards.app.core.ui.components.QcModalBottomSheet
import com.quickcards.app.core.ui.components.dialogEdgeToEdge
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTextButton
import com.quickcards.app.core.ui.theme.QcTextTone
import com.quickcards.app.core.ui.theme.QcTheme
import kotlinx.coroutines.delay

/** Export: two password rules gate the button live; the backup is encrypted with that password. */
@Composable
internal fun ExportBackupDialog(state: BackupHostState) {
    val password = remember { FocusRequester() }
    val confirm = remember { FocusRequester() }
    LaunchedEffect(Unit) {
        delay(FOCUS_DELAY_MS)
        runCatching { password.requestFocus() }
    }
    QcDialog(
        onDismissRequest = state::closeExport,
        title = stringResource(R.string.backup_export_title),
        actions = {
            QcTextButton(stringResource(R.string.backup_action_cancel), state::closeExport, tone = QcTextTone.Neutral)
            QcTextButton(
                stringResource(R.string.backup_action_export),
                state::confirmExport,
                enabled = state.exportReady && !state.exportBusy
            )
        }
    ) {
        DialogBody(pluralStringResource(R.plurals.backup_export_body, state.exportCardCount, state.exportCardCount))
        Column(
            modifier = Modifier.padding(top = QcSpacing.s4),
            verticalArrangement = Arrangement.spacedBy(QcSpacing.s2)
        ) {
            BackupPasswordField(
                value = state.exportPassphrase,
                onValueChange = { state.exportPassphrase = it },
                label = stringResource(R.string.backup_password),
                visible = state.exportShowPassphrase,
                onToggleVisible = { state.exportShowPassphrase = !state.exportShowPassphrase },
                rule = stringResource(R.string.backup_password_helper),
                ruleMet = state.exportLongEnough,
                focusRequester = password,
                imeAction = ImeAction.Next,
                onImeAction = { runCatching { confirm.requestFocus() } }
            )
            BackupPasswordField(
                value = state.exportConfirm,
                onValueChange = { state.exportConfirm = it },
                label = stringResource(R.string.backup_confirm_password),
                visible = state.exportShowConfirm,
                onToggleVisible = { state.exportShowConfirm = !state.exportShowConfirm },
                rule = stringResource(if (state.exportReady) R.string.backup_password_match else R.string.backup_password_must_match),
                ruleMet = state.exportReady,
                focusRequester = confirm,
                onImeAction = state::confirmExport
            )
        }
        CheckRow(
            label = stringResource(R.string.backup_include_settings),
            checked = state.exportWithSettings,
            onCheckedChange = { state.exportWithSettings = it },
            modifier = Modifier
                .padding(top = QcSpacing.s3)
                .dialogEdgeToEdge()
        )
    }
}

/** The picked backup, and its password when it is encrypted. */
@Composable
internal fun ImportBackupDialog(state: BackupHostState) {
    val file = state.importFile ?: return
    val focus = remember { FocusRequester() }
    LaunchedEffect(file.name) {
        if (file.encrypted) {
            delay(FOCUS_DELAY_MS)
            runCatching { focus.requestFocus() }
        }
    }
    QcDialog(
        onDismissRequest = state::closeImport,
        title = stringResource(R.string.backup_import_title),
        actions = {
            QcTextButton(stringResource(R.string.backup_action_cancel), state::closeImport, tone = QcTextTone.Neutral)
            QcTextButton(stringResource(R.string.backup_action_open), state::openImport, enabled = !state.importBusy)
        }
    ) {
        val count = file.cardCount
        BackupFileRow(
            title = stringResource(if (file.encrypted) R.string.backup_file_encrypted else R.string.backup_file_quickcards),
            icon = if (file.encrypted) QcIcons.encrypted else QcIcons.file,
            subtitle = if (count != null) pluralStringResource(R.plurals.backup_file_card_count, count, count) else file.name
        ) {
            QcTextButton(stringResource(R.string.backup_action_change), state::chooseImportFile, enabled = !state.importBusy)
        }
        // Older files are plain JSON; only encrypted backups ask for a password.
        if (file.encrypted) {
            BackupPasswordField(
                value = state.importPassphrase,
                onValueChange = {
                    state.importPassphrase = it
                    state.importError = null
                },
                label = stringResource(R.string.backup_password),
                visible = state.importShowPassphrase,
                onToggleVisible = { state.importShowPassphrase = !state.importShowPassphrase },
                error = state.importError,
                focusRequester = focus,
                onImeAction = state::openImport,
                modifier = Modifier.padding(top = QcSpacing.s4)
            )
        }
    }
}

/** A picked file that isn't a QuickCards backup: the fix is one tap. */
@Composable
internal fun NotABackupDialog(state: BackupHostState) {
    QcDialog(
        onDismissRequest = state::closeBadFile,
        title = stringResource(R.string.backup_error_format),
        actions = {
            QcTextButton(stringResource(R.string.backup_action_cancel), state::closeBadFile, tone = QcTextTone.Neutral)
            QcTextButton(stringResource(R.string.import_choose_another), state::chooseImportFile)
        }
    )
}

/** Erase starts unticked; Erase stays off until "This can't be undone" is ticked. */
@Composable
internal fun EraseAllDataDialog(state: BackupHostState) {
    QcDialog(
        onDismissRequest = state::closeErase,
        title = stringResource(R.string.backup_erase_title),
        actions = {
            QcTextButton(stringResource(R.string.backup_action_cancel), state::closeErase, tone = QcTextTone.Neutral)
            QcTextButton(
                stringResource(R.string.backup_action_erase),
                state::confirmErase,
                enabled = state.eraseAcknowledged && !state.eraseBusy,
                tone = QcTextTone.Danger
            )
        }
    ) {
        DialogBody(stringResource(R.string.backup_erase_body))
        CheckRow(
            label = stringResource(R.string.backup_erase_ack),
            checked = state.eraseAcknowledged,
            onCheckedChange = { state.eraseAcknowledged = it },
            modifier = Modifier
                .padding(top = QcSpacing.s3)
                .dialogEdgeToEdge()
        )
    }
}

/** "Import": a QuickCards backup or a spreadsheet, two visually distinct sources. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ImportChoiceSheet(state: BackupHostState) {
    QcModalBottomSheet(onDismissRequest = state::closeImportChoice) {
        Text(
            text = stringResource(R.string.import_cards_title),
            style = QcText.F4,
            color = QcTheme.colors.ink,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = QcSpacing.s6, end = QcSpacing.s6, top = QcSpacing.s2, bottom = QcSpacing.s3)
                .semantics { heading() }
        )
        SourceRow(
            title = stringResource(R.string.import_choice_backup),
            subtitle = null,
            onClick = state::chooseBackupImport
        ) { BackupSourceTile() }
        SourceRow(
            title = stringResource(R.string.import_choice_sheet),
            subtitle = stringResource(R.string.import_choice_sheet_sub),
            onClick = state::chooseSheetImport
        ) { SheetSourceTile() }
        Spacer(modifier = Modifier.height(QcSpacing.s4))
    }
}

@Composable
internal fun DialogBody(text: String) {
    Text(text = text, style = QcText.F2, color = QcTheme.colors.ink2)
}

// The dialog window must be attached before a field can take focus and raise the keyboard.
private const val FOCUS_DELAY_MS = 60L
