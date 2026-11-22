package com.quickcards.app.feature.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quickcards.app.R
import com.quickcards.app.core.data.sheet.SheetMessages
import com.quickcards.app.core.ui.components.QcBanner
import com.quickcards.app.core.ui.components.QcDialog
import com.quickcards.app.core.ui.components.QcGroup
import com.quickcards.app.core.ui.components.QcGroupLabel
import com.quickcards.app.core.ui.components.QcListRow
import com.quickcards.app.core.ui.components.QcSheetActions
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcTextButton
import com.quickcards.app.core.ui.theme.QcTextTone
import com.quickcards.app.core.ui.theme.QcTheme
import com.quickcards.app.core.ui.components.QcHelpSheet
import com.quickcards.app.core.ui.components.HelpTopic
import com.quickcards.domain.sheet.SheetReport

/** Failing rows listed on the check step before "Save fix list" takes over. */
private const val FIX_ROWS_SHOWN = 25

/** The spreadsheet flow: get the sheet, check the rows, done. One title on every step. */
@Composable
internal fun SpreadsheetScreen(state: BackupHostState) {
    val report = state.sheetReport
    when {
        state.sheetStep == SheetStep.DONE -> DoneStep(state)
        state.sheetStep == SheetStep.CHECK && report != null -> CheckStep(state, report)
        else -> GetStep(state)
    }
    state.sheetProblem?.let { problem ->
        QcDialog(
            onDismissRequest = state::closeSheetProblem,
            title = problem.title,
            actions = {
                Column(horizontalAlignment = Alignment.End) {
                    if (problem.offersBlankSheet) {
                        QcTextButton(stringResource(R.string.import_save_blank), state::saveBlankSheet)
                    }
                    QcTextButton(stringResource(R.string.import_choose_another), state::chooseSheetFile)
                    QcTextButton(stringResource(R.string.backup_action_cancel), state::closeSheetProblem, tone = QcTextTone.Neutral)
                }
            },
            content = problem.fix?.let { fix -> { DialogBody(fix) } }
        )
    }
    if (state.sheetHelpOpen) {
        QcHelpSheet(
            onDismiss = { state.sheetHelpOpen = false },
            onOpen = state::openHelpTopic,
            highlight = HelpTopic.SPREADSHEET
        )
    }
}

@Composable
private fun GetStep(state: BackupHostState) {
    val enabled = !state.sheetBusy
    BackupScreen(
        title = stringResource(R.string.import_sheet_title),
        onBack = state::backFromSheet,
        state = state,
        footer = {
            QcSheetActions(
                primary = stringResource(R.string.import_action_choose_file),
                onPrimary = state::chooseSheetFile,
                primaryEnabled = enabled,
                filled = true,
                primaryIcon = QcIcons.folderOpen,
                sideInset = QcSpacing.s4
            )
        }
    ) {
        SheetSteps(step = 1)
        LeadText(stringResource(R.string.import_sheet_intro), top = 0.dp)
        Spacer(modifier = Modifier.height(QcSpacing.s4))
        QcGroup {
            QcListRow(
                title = stringResource(R.string.import_download_xlsx),
                icon = QcIcons.table,
                subtitle = stringResource(R.string.import_download_xlsx_sub),
                enabled = enabled,
                onClick = { state.downloadSheet(SheetDownload.XLSX) },
                trailing = { DownloadGlyph() }
            )
            WrapRow(
                icon = QcIcons.openExternal,
                title = stringResource(R.string.import_open_google),
                subtitle = stringResource(R.string.import_open_google_sub),
                onClick = state::openGoogleSheet.takeIf { enabled }
            )
            QcListRow(
                title = stringResource(R.string.import_download_csv),
                icon = QcIcons.file,
                subtitle = stringResource(R.string.import_download_csv_sub),
                enabled = enabled,
                onClick = { state.downloadSheet(SheetDownload.CSV) },
                trailing = { DownloadGlyph() }
            )
        }
        QcTextButton(
            text = stringResource(R.string.import_help_link),
            onClick = { state.sheetHelpOpen = true },
            icon = QcIcons.infoOutline,
            modifier = Modifier.padding(top = QcSpacing.s2, start = QcSpacing.s1)
        )
    }
}

@Composable
private fun DownloadGlyph() {
    Icon(QcIcons.download, contentDescription = null, tint = QcTheme.colors.ink2, modifier = Modifier.padding(end = QcSpacing.s1))
}

@Composable
private fun CheckStep(state: BackupHostState, report: SheetReport) {
    val resources = LocalContext.current.resources
    val colors = QcTheme.colors
    val ready = report.ready.size
    val fixing = report.needsFixing
    BackupScreen(
        title = stringResource(R.string.import_sheet_title),
        onBack = state::backFromSheet,
        state = state,
        footer = {
            QcSheetActions(
                primary = pluralStringResource(R.plurals.import_action_import, ready, ready),
                onPrimary = state::importSheet,
                primaryEnabled = ready > 0 && !state.sheetBusy,
                filled = true,
                sideInset = QcSpacing.s4
            )
        }
    ) {
        SheetSteps(step = 2)
        LeadText(pluralStringResource(R.plurals.import_check_rows, report.rows.size, report.rows.size), top = 0.dp)
        Spacer(modifier = Modifier.height(QcSpacing.s3))
        QcGroup {
            CountRow(QcIcons.checkCircle, stringResource(R.string.import_check_ready), ready, colors.ok)
            CountRow(QcIcons.error, stringResource(R.string.import_check_fixing), fixing.size, colors.danger)
            CountRow(QcIcons.doneAll, stringResource(R.string.import_check_already), report.alreadyInApp.size)
            if (report.newHolders.isNotEmpty()) {
                CountRow(QcIcons.person, stringResource(R.string.import_check_holders), report.newHolders.size)
            }
        }
        if (ready == 0) {
            LeadText(
                stringResource(if (fixing.isEmpty()) R.string.import_check_all_already else R.string.import_check_nothing_ready),
                top = QcSpacing.s3
            )
        }
        if (fixing.isNotEmpty()) {
            QcGroupLabel(stringResource(R.string.import_fixing_heading))
            QcGroup {
                fixing.take(FIX_ROWS_SHOWN).forEach { row ->
                    WrapRow(
                        icon = QcIcons.error,
                        iconTint = colors.danger,
                        title = stringResource(R.string.import_row_label, row.row),
                        subtitle = row.problems.joinToString("\n") { SheetMessages.row(resources, it) }
                    )
                }
            }
            val more = fixing.size - FIX_ROWS_SHOWN
            if (more > 0) {
                LeadText(pluralStringResource(R.plurals.import_fixing_more, more, more))
            }
            QcTextButton(
                text = stringResource(R.string.import_save_fix),
                onClick = state::saveRowsToFix,
                enabled = !state.sheetBusy,
                icon = QcIcons.download,
                modifier = Modifier.padding(top = QcSpacing.s2, start = QcSpacing.s1)
            )
        }
        Spacer(modifier = Modifier.height(QcSpacing.s4))
    }
}

@Composable
private fun DoneStep(state: BackupHostState) {
    BackupScreen(
        title = stringResource(R.string.import_sheet_title),
        onBack = state::closeSheet,
        state = state,
        scrollable = false,
        footer = {
            if (state.sheetHasFile) {
                QcSheetActions(
                    primary = stringResource(R.string.import_action_delete_file),
                    onPrimary = state::deleteSheetFile,
                    secondary = stringResource(R.string.backup_action_done),
                    onSecondary = state::closeSheet,
                    primaryIcon = QcIcons.delete,
                    stack = true,
                    sideInset = QcSpacing.s4
                )
            } else {
                QcSheetActions(
                    primary = stringResource(R.string.backup_action_done),
                    onPrimary = state::closeSheet,
                    sideInset = QcSpacing.s4
                )
            }
        }
    ) {
        SheetSteps(step = 3)
        Column(verticalArrangement = Arrangement.spacedBy(QcSpacing.s8)) {
            ResultHeading(
                fan = state.sheetFan,
                title = pluralStringResource(R.plurals.import_done_title, state.sheetAdded, state.sheetAdded),
                detail = null,
                top = QcSpacing.s8
            )
            if (state.sheetHasFile) {
                QcBanner(text = stringResource(R.string.import_done_delete_hint), icon = QcIcons.file, quiet = true)
            }
        }
    }
}
