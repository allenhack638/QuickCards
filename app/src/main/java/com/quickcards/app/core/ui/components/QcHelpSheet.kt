package com.quickcards.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import com.quickcards.app.R
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcSize
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTheme

enum class HelpTopic { INSTANT_COPY, AUTOFILL, SPREADSHEET, CHECK }

/**
 * The Help sheet (Settings ⓘ, Spreadsheet step 1): the four hardest concepts, each opening its setting or screen. [highlight] marks
 * the row a deep link landed on (Spreadsheet step 1's "Spreadsheet columns").
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QcHelpSheet(
    onDismiss: () -> Unit,
    onOpen: (HelpTopic) -> Unit,
    highlight: HelpTopic? = null
) {
    QcModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
            Text(
                text = stringResource(R.string.settings_help),
                style = QcText.F4,
                color = QcTheme.colors.ink,
                modifier = Modifier
                    .padding(start = QcSpacing.s6, end = QcSpacing.s6, top = QcSpacing.s2, bottom = QcSpacing.s3)
                    .semantics { heading() }
            )
            fun open(topic: HelpTopic) {
                onDismiss()
                onOpen(topic)
            }
            HelpRow(
                icon = QcIcons.touchApp,
                title = stringResource(R.string.settings_copy_window_title),
                body = stringResource(R.string.settings_help_instant_body),
                target = stringResource(R.string.settings_app_lock_title),
                onClick = { open(HelpTopic.INSTANT_COPY) }
            )
            HelpRow(
                icon = QcIcons.password,
                title = stringResource(R.string.settings_help_autofill_title),
                body = stringResource(R.string.settings_help_autofill_body),
                target = stringResource(R.string.settings_autofill_title),
                onClick = { open(HelpTopic.AUTOFILL) }
            )
            HelpRow(
                icon = QcIcons.table,
                title = stringResource(R.string.settings_help_sheet_title),
                body = stringResource(R.string.settings_help_sheet_body),
                target = stringResource(R.string.settings_help_sheet_target),
                highlighted = highlight == HelpTopic.SPREADSHEET,
                onClick = { open(HelpTopic.SPREADSHEET) }
            )
            HelpRow(
                icon = QcIcons.info,
                title = stringResource(R.string.settings_help_check_title),
                body = stringResource(R.string.settings_help_check_body),
                target = stringResource(R.string.settings_help_check_target),
                onClick = { open(HelpTopic.CHECK) }
            )
            QcSheetActions(primary = stringResource(R.string.settings_help_done), onPrimary = onDismiss)
        }
    }
}

@Composable
private fun HelpRow(
    icon: ImageVector,
    title: String,
    body: String,
    target: String,
    onClick: () -> Unit,
    highlighted: Boolean = false
) {
    val colors = QcTheme.colors
    val spoken = "$title. $body ${stringResource(R.string.settings_help_opens, target)}"
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = QcSize.row2)
            .then(if (highlighted) Modifier.background(colors.tonal) else Modifier)
            .clickable(onClick = onClick)
            .clearAndSetSemantics {
                contentDescription = spoken
                role = Role.Button
            }
            .padding(horizontal = QcSpacing.s6, vertical = QcSpacing.s3),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(QcSpacing.s4)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colors.ink2,
            modifier = Modifier.size(QcSize.iconLg)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = QcText.F3, color = colors.ink)
            Text(text = body, style = QcText.F2, color = colors.ink2)
        }
        QcChevron()
    }
}
