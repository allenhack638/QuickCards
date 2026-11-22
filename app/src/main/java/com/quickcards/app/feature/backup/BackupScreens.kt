package com.quickcards.app.feature.backup

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.intl.Locale
import com.quickcards.app.R
import com.quickcards.app.core.ui.components.CheckRow
import com.quickcards.app.core.ui.components.QcGroup
import com.quickcards.app.core.ui.components.QcGroupLabel
import com.quickcards.app.core.ui.components.QcListRow
import com.quickcards.app.core.ui.components.QcSheetActions
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcRadio
import com.quickcards.app.core.ui.theme.QcSize
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTheme
import com.quickcards.domain.BackupSection
import com.quickcards.domain.MergeMode
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/** What the backup holds against this phone, then how to merge; nothing changes before Import. */
@Composable
internal fun ImportReviewScreen(state: BackupHostState, review: ImportReview) {
    val content = review.content
    val plan = review.plan
    BackupScreen(
        title = stringResource(R.string.backup_review_title),
        onBack = state::closeImport,
        state = state,
        footer = {
            QcSheetActions(
                primary = stringResource(R.string.backup_action_import),
                onPrimary = state::confirmImport,
                primaryEnabled = !state.importBusy,
                filled = true,
                sideInset = QcSpacing.s4
            )
        }
    ) {
        val exported = rememberExportDate(review.exportedAt)
        LeadText(
            if (exported == null) {
                stringResource(R.string.backup_review_lead)
            } else {
                stringResource(R.string.backup_review_lead_exported, exported)
            }
        )
        Spacer(modifier = Modifier.height(QcSpacing.s3))
        QcGroup {
            QcListRow(
                title = stringResource(R.string.backup_review_cards),
                icon = QcIcons.network,
                subtitle = stringResource(R.string.backup_review_cards_summary, plan.newCards, plan.changedCards, plan.sameCards)
            )
            ListRow(review, BackupSection.HOLDERS, content.holders.isNotEmpty(), R.string.backup_review_holders, QcIcons.person)
            ListRow(review, BackupSection.BANKS, content.banks.isNotEmpty(), R.string.backup_review_banks, QcIcons.bank)
            ListRow(review, BackupSection.NETWORKS, content.networks.isNotEmpty(), R.string.backup_review_networks, QcIcons.payments)
            ListRow(review, BackupSection.TYPES, content.types.isNotEmpty(), R.string.backup_review_types, QcIcons.type)
            ListRow(review, BackupSection.TAGS, content.tags.isNotEmpty(), R.string.backup_review_tags, QcIcons.tag)
            if (plan.duplicateCards > 0) {
                QcListRow(
                    title = stringResource(R.string.backup_review_duplicates_label),
                    icon = QcIcons.copy,
                    subtitle = pluralStringResource(R.plurals.backup_review_duplicates, plan.duplicateCards, plan.duplicateCards)
                )
            }
            ImportFormatsRow(review.formatsFixed, review.needCheck)
        }
        if (plan.changedCards > 0) {
            QcGroupLabel(stringResource(R.string.backup_review_saved_title))
            QcGroup(modifier = Modifier.selectableGroup()) {
                MergeRow(R.string.backup_review_update, state.importMode == MergeMode.UPDATE) { state.importMode = MergeMode.UPDATE }
                MergeRow(R.string.backup_review_keep, state.importMode == MergeMode.KEEP) { state.importMode = MergeMode.KEEP }
            }
        }
        if (content.settings != null) {
            Spacer(modifier = Modifier.height(QcSpacing.s3))
            QcGroup {
                CheckRow(
                    label = stringResource(R.string.backup_review_restore),
                    checked = state.importRestore,
                    onCheckedChange = { state.importRestore = it },
                    inset = QcSpacing.s4,
                    modifier = Modifier.heightIn(min = QcSize.row)
                )
            }
        }
    }
}

/** "Formats · n fixed · m to check", only when the file changed anything. */
@Composable
internal fun ImportFormatsRow(formatsFixed: Int, needCheck: Int) {
    if (!showsFormatSummary(formatsFixed, needCheck)) return
    QcListRow(
        title = stringResource(R.string.backup_review_formats_label),
        icon = QcIcons.autoFixHigh,
        subtitle = stringResource(R.string.backup_review_formats, formatsFixed, needCheck)
    )
}

@Composable
private fun ListRow(
    review: ImportReview,
    section: BackupSection,
    present: Boolean,
    title: Int,
    icon: ImageVector
) {
    if (section !in review.sections || !present) return
    val fresh = review.plan.newCount(section)
    QcListRow(
        title = stringResource(title),
        icon = icon,
        subtitle = pluralStringResource(R.plurals.backup_review_new_count, fresh, fresh)
    )
}

@Composable
private fun MergeRow(label: Int, selected: Boolean, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = QcSize.row)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = QcSpacing.s4),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(QcSpacing.s3)
    ) {
        QcRadio(selected = selected)
        Text(text = stringResource(label), style = QcText.F3, color = QcTheme.colors.ink)
    }
}

/** Done after a backup import: the imported cards and what happened to each. */
@Composable
internal fun ImportResultScreen(state: BackupHostState, outcome: ImportOutcome) {
    val result = outcome.result
    val parts = buildList {
        if (result.added > 0) add(pluralStringResource(R.plurals.backup_result_added, result.added, result.added))
        if (result.updated > 0) add(pluralStringResource(R.plurals.backup_result_updated, result.updated, result.updated))
        if (result.kept > 0) add(pluralStringResource(R.plurals.backup_result_kept, result.kept, result.kept))
        if (result.same > 0) add(pluralStringResource(R.plurals.backup_result_unchanged, result.same, result.same))
    }
    val details = parts.drop(1) + listOfNotNull(
        stringResource(R.string.backup_result_settings).takeIf { result.settingsRestored }
    )
    BackupScreen(
        title = stringResource(R.string.backup_import_title),
        onBack = state::closeImport,
        state = state,
        scrollable = false,
        footer = {
            QcSheetActions(
                primary = stringResource(R.string.backup_action_done),
                onPrimary = state::closeImport,
                sideInset = QcSpacing.s4
            )
        }
    ) {
        ResultHeading(
            fan = outcome.fan,
            title = parts.firstOrNull() ?: stringResource(R.string.backup_result_nothing),
            detail = details.takeIf { it.isNotEmpty() }?.joinToString(stringResource(R.string.backup_value_separator))
        )
    }
}

@Composable
private fun rememberExportDate(iso: String?): String? {
    val locale = Locale.current.platformLocale
    return remember(iso, locale) {
        if (iso.isNullOrBlank()) return@remember null
        runCatching { LocalDate.parse(iso).format(DateTimeFormatter.ofPattern("d MMM yyyy", locale)) }.getOrDefault(iso)
    }
}
