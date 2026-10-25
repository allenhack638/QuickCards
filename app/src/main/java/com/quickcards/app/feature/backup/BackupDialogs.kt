package com.quickcards.app.feature.backup

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.unit.dp
import com.quickcards.app.R
import com.quickcards.app.core.ui.entity.DialogActions
import com.quickcards.app.core.ui.entity.ManageDialogFrame
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcType
import com.quickcards.domain.BackupSection
import com.quickcards.domain.MergeMode
import kotlinx.coroutines.delay

@Composable
internal fun ExportBackupDialog(state: BackupHostState) {
    val focus = remember { FocusRequester() }

    LaunchedEffect(Unit) {
        delay(80)
        state.exportPassphrase = state.exportPassphrase.copy(
            selection = TextRange(state.exportPassphrase.text.length)
        )
        runCatching { focus.requestFocus() }
    }

    ManageDialogFrame(onDismissRequest = state::closeExport) {
        Text(
            text = stringResource(R.string.backup_export_title),
            style = QcType.DialogTitle,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(QcDimens.DialogHeaderGap))
        Text(
            text = pluralStringResource(
                R.plurals.backup_export_body,
                state.exportCardCount,
                state.exportCardCount
            ),
            style = QcType.Body,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(QcDimens.Base))
        BackupCheckRow(
            label = stringResource(R.string.backup_include_settings),
            checked = state.exportWithSettings,
            onCheckedChange = { state.exportWithSettings = it }
        )
        Spacer(modifier = Modifier.height(10.dp))
        BackupPasswordField(
            value = state.exportPassphrase,
            onValueChange = { state.exportPassphrase = it },
            label = stringResource(R.string.backup_passphrase),
            helper = stringResource(R.string.backup_passphrase_helper),
            error = null,
            visible = state.exportShowPassphrase,
            onToggleVisible = { state.exportShowPassphrase = !state.exportShowPassphrase },
            focusRequester = focus,
            enabled = !state.exportBusy
        )
        Spacer(modifier = Modifier.height(10.dp))
        BackupPasswordField(
            value = state.exportConfirm,
            onValueChange = { state.exportConfirm = it },
            label = stringResource(R.string.backup_confirm_passphrase),
            helper = "",
            error = state.exportPasswordError,
            visible = state.exportShowConfirm,
            onToggleVisible = { state.exportShowConfirm = !state.exportShowConfirm },
            enabled = !state.exportBusy
        )
        Spacer(modifier = Modifier.height(QcDimens.Half))
        DialogActions(
            onCancel = state::closeExport,
            cancelLabel = stringResource(R.string.backup_action_cancel),
            confirmLabel = stringResource(R.string.backup_action_export),
            onConfirm = state::confirmExport,
            confirmEnabled = state.exportReady && !state.exportBusy,
            confirmPrimary = true
        )
    }
}

@Composable
internal fun ImportBackupDialog(state: BackupHostState) {
    val review = state.importReview
    if (state.importStep == ImportStep.REVIEW && review != null) {
        ImportReviewDialog(state, review)
        return
    }

    val focus = remember { FocusRequester() }
    val fileName = state.importFile?.name

    LaunchedEffect(fileName) {
        if (fileName != null && state.importFile?.encrypted == true) {
            delay(80)
            state.importPassphrase = state.importPassphrase.copy(
                selection = TextRange(state.importPassphrase.text.length)
            )
            runCatching { focus.requestFocus() }
        }
    }

    ManageDialogFrame(onDismissRequest = state::closeImport) {
        Text(
            text = stringResource(R.string.backup_import_title),
            style = QcType.DialogTitle,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(QcDimens.DialogHeaderGap))
        Text(
            text = stringResource(R.string.backup_import_body),
            style = QcType.Body,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(QcDimens.DialogHeaderGap))

        val file = state.importFile
        if (file == null) {
            BackupOutlinedButton(
                label = stringResource(R.string.backup_choose_file),
                onClick = state::chooseImportFile,
                enabled = !state.importBusy
            )
            Spacer(modifier = Modifier.height(QcDimens.DialogHeaderGap))
        } else {
            val count = file.cardCount
            BackupFileRow(
                fileName = file.name,
                meta = when {
                    file.encrypted -> stringResource(R.string.backup_file_encrypted)
                    count != null -> pluralStringResource(
                        R.plurals.backup_file_card_count,
                        count,
                        count
                    )
                    else -> stringResource(R.string.backup_file_quickcards)
                },
                onChange = state::chooseImportFile
            )
            Spacer(modifier = Modifier.height(QcDimens.DialogHeaderGap))
            // Files from older versions are plain JSON: there is nothing to
            // unlock, so only encrypted backups ask for a passphrase.
            if (file.encrypted) {
                BackupPasswordField(
                    value = state.importPassphrase,
                    onValueChange = {
                        state.importPassphrase = it
                        state.importError = null
                    },
                    label = stringResource(R.string.backup_passphrase),
                    helper = "",
                    error = state.importError,
                    visible = state.importShowPassphrase,
                    onToggleVisible = { state.importShowPassphrase = !state.importShowPassphrase },
                    focusRequester = focus,
                    enabled = !state.importBusy
                )
            } else {
                state.importError?.let { error ->
                    Text(
                        text = error,
                        style = QcType.Body,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(QcDimens.Half))
        DialogActions(
            onCancel = state::closeImport,
            cancelLabel = stringResource(R.string.backup_action_cancel),
            confirmLabel = stringResource(R.string.backup_action_open),
            onConfirm = state::openImport,
            confirmEnabled = state.importFile != null && !state.importBusy
        )
    }
}

@Composable
internal fun ImportReviewDialog(state: BackupHostState, review: ImportReview) {
    val content = review.content
    val plan = review.plan
    val sections = review.sections

    val rows = mutableListOf<Pair<String, String>>()
    rows.add(
        pluralStringResource(
            R.plurals.backup_card_count,
            content.cards.size,
            content.cards.size
        ) to stringResource(
            R.string.backup_review_cards_summary,
            plan.newCards,
            plan.changedCards,
            plan.sameCards
        )
    )
    if (BackupSection.HOLDERS in sections && content.holders.isNotEmpty()) {
        rows.add(
            pluralStringResource(
                R.plurals.backup_holder_count,
                content.holders.size,
                content.holders.size
            ) to listSuffix(plan.newCount(BackupSection.HOLDERS))
        )
    }
    if (BackupSection.BANKS in sections && content.banks.isNotEmpty()) {
        rows.add(
            pluralStringResource(
                R.plurals.backup_bank_count,
                content.banks.size,
                content.banks.size
            ) to listSuffix(plan.newCount(BackupSection.BANKS))
        )
    }
    if (BackupSection.NETWORKS in sections && content.networks.isNotEmpty()) {
        rows.add(
            pluralStringResource(
                R.plurals.backup_network_count,
                content.networks.size,
                content.networks.size
            ) to listSuffix(plan.newCount(BackupSection.NETWORKS))
        )
    }
    if (BackupSection.TYPES in sections && content.types.isNotEmpty()) {
        rows.add(
            pluralStringResource(
                R.plurals.backup_type_count,
                content.types.size,
                content.types.size
            ) to listSuffix(plan.newCount(BackupSection.TYPES))
        )
    }
    if (BackupSection.TAGS in sections && content.tags.isNotEmpty()) {
        rows.add(
            pluralStringResource(
                R.plurals.backup_tag_count,
                content.tags.size,
                content.tags.size
            ) to listSuffix(plan.newCount(BackupSection.TAGS))
        )
    }
    val settingsRowValue = if (content.settings != null) {
        stringResource(R.string.backup_review_settings_included)
    } else {
        stringResource(R.string.backup_review_settings_missing)
    }
    rows.add(stringResource(R.string.backup_review_settings_label) to settingsRowValue)

    val exportedAt = review.exportedAt
    val meta = if (exportedAt.isNullOrBlank()) {
        review.fileName
    } else {
        stringResource(R.string.backup_review_exported, review.fileName, exportedAt)
    }

    ManageDialogFrame(onDismissRequest = state::closeImport) {
        Text(
            text = stringResource(R.string.backup_review_title),
            style = QcType.DialogTitle,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(QcDimens.DialogHeaderGap))
        Text(
            text = meta,
            style = QcType.Body,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(QcDimens.DialogHeaderGap))
        BackupReviewList(rows)
        ImportFormatsSummary(
            formatsFixed = review.formatsFixed,
            needCheck = review.needCheck
        )

        if (plan.changedCards > 0) {
            Spacer(modifier = Modifier.height(QcDimens.PassGap))
            Text(
                text = stringResource(R.string.backup_review_saved_title),
                style = BackupGroupLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(QcDimens.Half))
            BackupRadioRow(
                label = stringResource(R.string.backup_review_update),
                selected = state.importMode == MergeMode.UPDATE,
                onClick = { state.importMode = MergeMode.UPDATE }
            )
            BackupRadioRow(
                label = stringResource(R.string.backup_review_keep),
                selected = state.importMode == MergeMode.KEEP,
                onClick = { state.importMode = MergeMode.KEEP }
            )
        }

        if (content.settings != null) {
            Spacer(modifier = Modifier.height(QcDimens.Half))
            BackupCheckRow(
                label = stringResource(R.string.backup_review_restore),
                checked = state.importRestore,
                onCheckedChange = { state.importRestore = it }
            )
        }

        Spacer(modifier = Modifier.height(QcDimens.Half))
        DialogActions(
            onCancel = state::closeImport,
            cancelLabel = stringResource(R.string.backup_action_cancel),
            confirmLabel = stringResource(R.string.backup_action_import),
            onConfirm = state::confirmImport,
            confirmEnabled = !state.importBusy,
            confirmPrimary = true
        )
    }
}

@Composable
internal fun EraseAllDataDialog(state: BackupHostState) {
    ManageDialogFrame(onDismissRequest = state::closeErase) {
        Text(
            text = stringResource(R.string.backup_erase_title),
            style = QcType.DialogTitle,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(QcDimens.DialogHeaderGap))
        Text(
            text = stringResource(R.string.backup_erase_body),
            style = QcType.Body,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(QcDimens.DialogHeaderGap))
        BackupCheckRow(
            label = stringResource(R.string.backup_erase_ack),
            checked = state.eraseAcknowledged,
            onCheckedChange = { state.eraseAcknowledged = it }
        )
        Spacer(modifier = Modifier.height(QcDimens.Half))
        DialogActions(
            onCancel = state::closeErase,
            cancelLabel = stringResource(R.string.backup_action_cancel),
            confirmLabel = stringResource(R.string.backup_action_erase),
            onConfirm = state::confirmErase,
            confirmDanger = true,
            confirmEnabled = state.eraseAcknowledged && !state.eraseBusy
        )
    }
}

@Composable
private fun listSuffix(newCount: Int): String =
    if (newCount > 0) {
        pluralStringResource(R.plurals.backup_review_new_count, newCount, newCount)
    } else {
        stringResource(R.string.backup_review_all_exist)
    }
