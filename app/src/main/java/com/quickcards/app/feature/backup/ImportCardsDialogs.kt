package com.quickcards.app.feature.backup

import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.quickcards.app.R
import com.quickcards.app.core.data.sheet.SheetMessages
import com.quickcards.app.core.ui.entity.DialogActions
import com.quickcards.app.core.ui.entity.ManageDialogFrame
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcType
import com.quickcards.domain.sheet.SheetReport

/** "Import cards": a backup, or a spreadsheet. */
@Composable
internal fun ImportChoiceDialog(state: BackupHostState) {
    ManageDialogFrame(onDismissRequest = state::closeImportChoice) {
        DialogTitle(stringResource(R.string.import_cards_title))
        Spacer(modifier = Modifier.height(QcDimens.DialogHeaderGap))
        ChoiceRow(
            title = stringResource(R.string.import_choice_backup),
            subtitle = stringResource(R.string.import_choice_backup_sub),
            onClick = state::chooseBackupImport
        )
        Spacer(modifier = Modifier.height(QcDimens.Base))
        ChoiceRow(
            title = stringResource(R.string.import_choice_sheet),
            subtitle = stringResource(R.string.import_choice_sheet_sub),
            onClick = state::chooseSheetImport
        )
        DialogActions(
            onCancel = null,
            confirmLabel = stringResource(R.string.backup_action_cancel),
            onConfirm = state::closeImportChoice
        )
    }
}

/** The spreadsheet flow in one dialog: get the sheet, check the rows, done. */
@Composable
internal fun SheetImportDialog(state: BackupHostState) {
    when (state.sheetStep) {
        SheetStep.GET -> GetSheetStep(state)
        SheetStep.CHECK -> state.sheetReport?.let { CheckSheetStep(state, it) } ?: GetSheetStep(state)
        SheetStep.DONE -> DoneSheetStep(state)
    }
}

@Composable
private fun GetSheetStep(state: BackupHostState) {
    ManageDialogFrame(onDismissRequest = state::closeSheet) {
        DialogTitle(stringResource(R.string.import_sheet_title))
        Spacer(modifier = Modifier.height(QcDimens.DialogHeaderGap))
        BodyText(stringResource(R.string.import_sheet_intro))
        Spacer(modifier = Modifier.height(QcDimens.Base))
        BodyText(stringResource(R.string.import_sheet_rules))
        Spacer(modifier = Modifier.height(QcDimens.DialogHeaderGap))

        ChoiceRow(
            title = stringResource(R.string.import_download_xlsx),
            subtitle = stringResource(R.string.import_download_xlsx_sub),
            onClick = { state.downloadSheet(SheetDownload.XLSX) }
        )
        Spacer(modifier = Modifier.height(QcDimens.Base))
        ChoiceRow(
            title = stringResource(R.string.import_open_google),
            subtitle = stringResource(R.string.import_open_google_sub),
            onClick = state::openGoogleSheet
        )
        Spacer(modifier = Modifier.height(QcDimens.Base))
        ChoiceRow(
            title = stringResource(R.string.import_download_csv),
            subtitle = stringResource(R.string.import_download_csv_sub),
            onClick = { state.downloadSheet(SheetDownload.CSV) }
        )
        Spacer(modifier = Modifier.height(QcDimens.DialogHeaderGap))
        BodyText(stringResource(R.string.import_already_filled))

        val error = state.sheetError
        if (error != null) {
            Spacer(modifier = Modifier.height(QcDimens.Base))
            state.sheetFileName?.let { name ->
                Text(text = name, style = QcType.Body, color = MaterialTheme.colorScheme.onSurface)
            }
            Text(text = error, style = QcType.Body, color = MaterialTheme.colorScheme.error)
        }

        DialogActions(
            onCancel = state::closeSheet,
            cancelLabel = stringResource(R.string.backup_action_cancel),
            confirmLabel = stringResource(R.string.import_action_proceed),
            onConfirm = state::chooseSheetFile,
            confirmEnabled = !state.sheetBusy
        )
    }
}

@Composable
private fun CheckSheetStep(state: BackupHostState, report: SheetReport) {
    val resources = androidx.compose.ui.platform.LocalContext.current.resources
    val ready = report.ready.size
    val fixing = report.needsFixing
    val already = report.alreadyInApp.size
    ManageDialogFrame(onDismissRequest = state::closeSheet) {
        DialogTitle(stringResource(R.string.import_check_title))
        Spacer(modifier = Modifier.height(QcDimens.DialogHeaderGap))
        state.sheetFileName?.let { name ->
            Text(text = name, style = QcType.Body, color = MaterialTheme.colorScheme.onSurface)
            Spacer(modifier = Modifier.height(QcDimens.Base))
        }
        BodyText(SheetMessages.summary(resources, ready, fixing.size, already))
        if (report.newHolders.isNotEmpty()) {
            Spacer(modifier = Modifier.height(QcDimens.Base))
            BodyText(stringResource(R.string.import_new_holders, report.newHolders.joinToString(", ")))
        }
        if (ready == 0) {
            Spacer(modifier = Modifier.height(QcDimens.Base))
            BodyText(stringResource(R.string.import_check_nothing_ready))
        }
        if (fixing.isNotEmpty()) {
            Spacer(modifier = Modifier.height(QcDimens.DialogHeaderGap))
            Text(
                text = stringResource(R.string.import_fixing_heading),
                style = QcType.Body,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(QcDimens.Base))
            fixing.forEach { row ->
                Text(
                    text = stringResource(
                        R.string.import_row_reason,
                        row.row,
                        row.problems.joinToString(" ") { SheetMessages.row(resources, it) }
                    ),
                    style = QcType.Body,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
            }
            Spacer(modifier = Modifier.height(QcDimens.Base))
            BackupOutlinedButton(
                label = stringResource(R.string.import_save_fix),
                onClick = state::saveRowsToFix,
                enabled = !state.sheetBusy
            )
        }
        DialogActions(
            onCancel = state::closeSheet,
            cancelLabel = stringResource(R.string.backup_action_cancel),
            confirmLabel = pluralStringResource(R.plurals.import_action_import, ready, ready),
            onConfirm = state::importSheet,
            confirmEnabled = ready > 0 && !state.sheetBusy,
            confirmPrimary = true
        )
    }
}

@Composable
private fun DoneSheetStep(state: BackupHostState) {
    ManageDialogFrame(onDismissRequest = state::closeSheet) {
        DialogTitle(stringResource(R.string.import_done_title))
        Spacer(modifier = Modifier.height(QcDimens.DialogHeaderGap))
        BodyText(pluralStringResource(R.plurals.import_done_body, state.sheetAdded, state.sheetAdded))
        Spacer(modifier = Modifier.height(QcDimens.Base))
        BodyText(stringResource(R.string.import_done_delete_hint))
        Spacer(modifier = Modifier.height(QcDimens.DialogHeaderGap))
        BackupOutlinedButton(
            label = stringResource(R.string.import_action_delete_file),
            onClick = state::deleteSheetFile
        )
        DialogActions(
            onCancel = null,
            confirmLabel = stringResource(R.string.import_action_done),
            onConfirm = state::closeSheet
        )
    }
}

@Composable
private fun DialogTitle(text: String) {
    Text(text = text, style = QcType.DialogTitle, color = MaterialTheme.colorScheme.onSurface)
}

@Composable
private fun BodyText(text: String) {
    Text(text = text, style = QcType.Body, color = MaterialTheme.colorScheme.onSurfaceVariant)
}

/** A tappable option: title and one line of explanation, in a hairline box like the file row. */
@Composable
private fun ChoiceRow(title: String, subtitle: String, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .border(
                width = QcDimens.Divider,
                color = MaterialTheme.colorScheme.outlineVariant,
                shape = RoundedCornerShape(QcDimens.CornerMd)
            )
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    ) {
        Text(text = title, style = QcType.Body, color = MaterialTheme.colorScheme.onSurface)
        Text(text = subtitle, style = QcType.Body, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
