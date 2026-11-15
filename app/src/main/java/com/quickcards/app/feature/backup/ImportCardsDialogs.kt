package com.quickcards.app.feature.backup

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.foundation.rememberScrollState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quickcards.app.R
import com.quickcards.app.core.data.sheet.SheetMessages
import com.quickcards.app.core.ui.components.QcDialogPadding
import com.quickcards.app.core.ui.components.dialogEdgeToEdge
import com.quickcards.app.core.ui.entity.DialogActionButtons
import com.quickcards.app.core.ui.entity.DialogActions
import com.quickcards.app.core.ui.entity.ManageDialogFrame
import com.quickcards.app.core.ui.entity.WarningPanel
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcType
import com.quickcards.domain.sheet.SheetReport

/** Failing rows shown in the dialog before "Save rows to fix" takes over. */
private const val FIX_ROWS_SHOWN = 25

/** "Import cards": a backup, or a spreadsheet. */
@Composable
internal fun ImportChoiceDialog(state: BackupHostState) {
    ManageDialogFrame(onDismissRequest = state::closeImportChoice) {
        DialogTitle(stringResource(R.string.import_cards_title))
        // The rows carry 12dp of their own padding, so this makes the 16dp title gap.
        Spacer(modifier = Modifier.height(QcDimens.Half))
        OptionRow(
            icon = QcIcons.file,
            title = stringResource(R.string.import_choice_backup),
            subtitle = stringResource(R.string.import_choice_backup_sub),
            onClick = state::chooseBackupImport
        )
        OptionRow(
            icon = QcIcons.table,
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
    val scrollState = rememberScrollState()
    val error = state.sheetError
    // A refused file is reported under the options; bring it into view on large fonts.
    LaunchedEffect(error) {
        if (error != null) scrollState.animateScrollTo(scrollState.maxValue)
    }
    ManageDialogFrame(
        onDismissRequest = state::closeSheet,
        scrollState = scrollState,
        actions = {
            DialogActionButtons(
                onCancel = state::closeSheet,
                cancelLabel = stringResource(R.string.backup_action_cancel),
                confirmLabel = stringResource(R.string.import_action_proceed),
                onConfirm = state::chooseSheetFile,
                confirmEnabled = !state.sheetBusy
            )
        }
    ) {
        DialogTitle(stringResource(R.string.import_sheet_title))
        Spacer(modifier = Modifier.height(QcDimens.DialogHeaderGap))
        BodyText(stringResource(R.string.import_sheet_intro))
        Spacer(modifier = Modifier.height(QcDimens.Base))
        BodyText(stringResource(R.string.import_sheet_rules))
        Spacer(modifier = Modifier.height(QcDimens.Half))

        OptionRow(
            icon = QcIcons.download,
            title = stringResource(R.string.import_download_xlsx),
            subtitle = stringResource(R.string.import_download_xlsx_sub),
            onClick = { state.downloadSheet(SheetDownload.XLSX) },
            enabled = !state.sheetBusy
        )
        OptionRow(
            icon = QcIcons.openExternal,
            title = stringResource(R.string.import_open_google),
            subtitle = stringResource(R.string.import_open_google_sub),
            onClick = state::openGoogleSheet,
            enabled = !state.sheetBusy
        )
        OptionRow(
            icon = QcIcons.download,
            title = stringResource(R.string.import_download_csv),
            subtitle = stringResource(R.string.import_download_csv_sub),
            onClick = { state.downloadSheet(SheetDownload.CSV) },
            enabled = !state.sheetBusy
        )

        Spacer(modifier = Modifier.height(QcDimens.Half))
        BodyText(stringResource(R.string.import_already_filled))

        if (error != null) {
            Spacer(modifier = Modifier.height(QcDimens.DialogHeaderGap))
            FilePanel(fileName = state.sheetFileName, message = error)
        }
    }
}

@Composable
private fun CheckSheetStep(state: BackupHostState, report: SheetReport) {
    val resources = LocalContext.current.resources
    val ready = report.ready.size
    val fixing = report.needsFixing
    val already = report.alreadyInApp.size
    val checked = report.rows.size
    ManageDialogFrame(
        onDismissRequest = state::closeSheet,
        actions = {
            DialogActionButtons(
                onCancel = state::closeSheet,
                cancelLabel = stringResource(R.string.backup_action_cancel),
                confirmLabel = pluralStringResource(R.plurals.import_action_import, ready, ready),
                onConfirm = state::importSheet,
                confirmEnabled = ready > 0 && !state.sheetBusy,
                confirmPrimary = true
            )
        }
    ) {
        DialogTitle(stringResource(R.string.import_check_title))
        Spacer(modifier = Modifier.height(QcDimens.DialogHeaderGap))
        BackupFileRow(
            fileName = state.sheetFileName ?: "",
            meta = pluralStringResource(R.plurals.import_check_rows, checked, checked),
            onChange = state::chooseSheetFile
        )
        Spacer(modifier = Modifier.height(QcDimens.DialogHeaderGap))
        BackupReviewList(
            buildList {
                add(stringResource(R.string.import_check_ready) to ready.toString())
                add(stringResource(R.string.import_check_fixing) to fixing.size.toString())
                add(stringResource(R.string.import_check_already) to already.toString())
                if (report.newHolders.isNotEmpty()) {
                    add(stringResource(R.string.import_check_holders) to report.newHolders.joinToString(", "))
                }
            }
        )
        if (ready == 0) {
            Spacer(modifier = Modifier.height(QcDimens.PassGap))
            BodyText(
                stringResource(
                    if (fixing.isEmpty()) R.string.import_check_all_already else R.string.import_check_nothing_ready
                )
            )
        }
        if (fixing.isNotEmpty()) {
            Spacer(modifier = Modifier.height(QcDimens.PassGap))
            Text(
                text = stringResource(R.string.import_fixing_heading),
                style = BackupGroupLabel,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(QcDimens.Base))
            Column(verticalArrangement = Arrangement.spacedBy(QcDimens.Base)) {
                fixing.take(FIX_ROWS_SHOWN).forEach { row ->
                    FixRow(
                        label = stringResource(R.string.import_row_label, row.row),
                        problems = row.problems.map { SheetMessages.row(resources, it) }
                    )
                }
                val more = fixing.size - FIX_ROWS_SHOWN
                if (more > 0) {
                    BodyText(pluralStringResource(R.plurals.import_fixing_more, more, more))
                }
            }
            Spacer(modifier = Modifier.height(QcDimens.DialogHeaderGap))
            BackupOutlinedButton(
                label = stringResource(R.string.import_save_fix),
                onClick = state::saveRowsToFix,
                enabled = !state.sheetBusy,
                icon = QcIcons.download
            )
        }
    }
}

@Composable
private fun DoneSheetStep(state: BackupHostState) {
    ManageDialogFrame(onDismissRequest = state::closeSheet) {
        DialogTitle(stringResource(R.string.import_done_title))
        Spacer(modifier = Modifier.height(QcDimens.DialogHeaderGap))
        Text(
            text = pluralStringResource(R.plurals.import_done_body, state.sheetAdded, state.sheetAdded),
            style = QcType.Body,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(QcDimens.DialogHeaderGap))
        WarningPanel(AnnotatedString(stringResource(R.string.import_done_delete_hint)))
        if (state.sheetHasFile) {
            Spacer(modifier = Modifier.height(QcDimens.DialogHeaderGap))
            BackupOutlinedButton(
                label = stringResource(R.string.import_action_delete_file),
                onClick = state::deleteSheetFile,
                icon = QcIcons.delete
            )
        }
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

/**
 * A choice in a dialog: the standard two-line list row (icon, Body Large title, Body
 * supporting text, chevron), running edge to edge of the dialog so the ripple spans it.
 */
@Composable
private fun OptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Row(
        modifier = Modifier
            .dialogEdgeToEdge()
            .fillMaxWidth()
            .heightIn(min = QcDimens.ListRowTwoLine)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = QcDialogPadding, vertical = QcDimens.PassGap),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(QcDimens.Icon)
        )
        Spacer(modifier = Modifier.width(QcDimens.Gutter))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = QcType.BodyLarge, color = MaterialTheme.colorScheme.onSurface)
            Text(text = subtitle, style = QcType.Body, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Spacer(modifier = Modifier.width(QcDimens.Base))
        Icon(
            imageVector = QcIcons.chevronRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(QcDimens.Icon)
        )
    }
}

/** The file that could not be opened and why, in the dialog warning panel. */
@Composable
private fun FilePanel(fileName: String?, message: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                MaterialTheme.colorScheme.surfaceContainerHighest,
                RoundedCornerShape(QcDimens.CornerMd)
            )
            .padding(QcDimens.CornerMd),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Icon(
            imageVector = QcIcons.warning,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.error,
            modifier = Modifier.size(QcDimens.Icon)
        )
        Column {
            if (!fileName.isNullOrBlank()) {
                Text(
                    text = fileName,
                    style = BackupFileName,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(QcDimens.Half))
            }
            Text(text = message, style = QcType.Body, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

/** One failing row: its number in a fixed column, then one line per problem. */
@Composable
private fun FixRow(label: String, problems: List<String>) {
    Row(verticalAlignment = Alignment.Top) {
        Text(
            text = label,
            style = BackupFileName,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.widthIn(min = QcDimens.LeadSlot)
        )
        Spacer(modifier = Modifier.width(QcDimens.Base))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(QcDimens.Half)) {
            problems.forEach { problem ->
                Text(text = problem, style = QcType.Body, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}
