package com.quickcards.app.feature.details

import android.text.format.DateFormat
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import com.quickcards.app.R
import com.quickcards.app.core.ui.HomeCard
import com.quickcards.app.core.ui.components.MASK_DOTS
import com.quickcards.app.core.ui.components.QcModalBottomSheet
import com.quickcards.app.core.ui.components.QcSheetActions
import com.quickcards.app.core.ui.components.maskedNumberText
import com.quickcards.app.core.ui.dayMonth
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTheme
import com.quickcards.domain.BestCardRules
import com.quickcards.domain.CardField
import com.quickcards.domain.CardStatus
import com.quickcards.domain.ExpiryStatus
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.temporal.ChronoUnit
import java.util.Date
import java.util.Locale

/** What the sheet says for one status chip: its values come from the card and today. */
private class StatusWhy(val title: String, val body: AnnotatedString, val basis: String? = null, val action: String? = null)

/**
 * Explains a status chip: title, the card, one or two lines with the real dates, and Got it. Edit card
 * (Change days for Best today) runs [onEdit], which unlocks first.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CardStatusSheet(
    card: HomeCard,
    status: CardStatus,
    lastUsedAt: Long?,
    today: LocalDate,
    onEdit: () -> Unit,
    onDismiss: () -> Unit
) {
    val why = statusWhy(card, status, lastUsedAt, today)
    val colors = QcTheme.colors
    QcModalBottomSheet(onDismissRequest = onDismiss) {
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = QcSpacing.s6)) {
            Text(
                text = why.title,
                style = QcText.F4,
                color = colors.ink,
                modifier = Modifier.padding(top = QcSpacing.s2).semantics { heading() }
            )
            Text(
                text = maskedNumberText("${card.bankName} $MASK_DOTS ${card.card.number.takeLast(4)}"),
                style = QcText.F2,
                color = colors.ink2
            )
            Text(
                text = why.body,
                style = QcText.F3,
                color = colors.ink,
                modifier = Modifier.padding(top = QcSpacing.s3)
            )
            why.basis?.let {
                Text(text = it, style = QcText.F2, color = colors.ink2, modifier = Modifier.padding(top = QcSpacing.s2))
            }
        }
        QcSheetActions(
            primary = stringResource(R.string.cards_why_got_it),
            onPrimary = onDismiss,
            secondary = why.action,
            onSecondary = onEdit,
            filled = true,
            stackWhenTight = true
        )
    }
}

@Composable
private fun statusWhy(card: HomeCard, status: CardStatus, lastUsedAt: Long?, today: LocalDate): StatusWhy {
    val bank = card.bankName
    val expiry = card.card.expiry
    val editCard = stringResource(R.string.cards_why_edit)
    val dayMonth = dayMonth()
    return when (status) {
        CardStatus.BEST_TODAY -> {
            val bill = BestCardRules.bill(card.card.statementDay ?: 0, card.card.dueDay ?: 0, today)
            val days = bill?.let { ChronoUnit.DAYS.between(today, it.due).toInt() } ?: 0
            StatusWhy(
                title = stringResource(R.string.cards_why_best_title),
                body = bolded(
                    stringResource(R.string.cards_why_best_body),
                    bill?.statement?.format(dayMonth).orEmpty(),
                    bill?.due?.format(dayMonth).orEmpty(),
                    pluralStringResource(R.plurals.cards_why_days, days, days)
                ),
                basis = stringResource(R.string.cards_why_best_basis),
                action = stringResource(R.string.cards_why_change_days)
            )
        }
        CardStatus.EXPIRES_SOON -> {
            val lastDay = ExpiryStatus.lastDay(expiry) ?: today
            val days = ChronoUnit.DAYS.between(today, lastDay).toInt()
            val body = if (days <= 0) {
                bolded(stringResource(R.string.cards_why_soon_today_body), stringResource(R.string.cards_why_today), plain(bank))
            } else {
                val whenEnds = if (days == 1) {
                    stringResource(R.string.cards_why_tomorrow)
                } else {
                    pluralStringResource(R.plurals.cards_why_in_days, days, days)
                }
                bolded(stringResource(R.string.cards_why_soon_body), lastDay.format(dayMonth), plain(whenEnds), plain(bank))
            }
            StatusWhy(stringResource(R.string.cards_why_soon_title, expiry), body, action = editCard)
        }
        CardStatus.EXPIRED -> StatusWhy(
            title = stringResource(R.string.cards_why_expired_title, expiry),
            body = bolded(
                stringResource(R.string.cards_why_expired_body),
                ExpiryStatus.lastDay(expiry)?.format(dayMonth).orEmpty(),
                plain(bank)
            ),
            action = editCard
        )
        CardStatus.CHECK -> {
            val fields = flaggedFields(card.card.needsReview).map { stringResource(fieldName(it)) }
            val listed = when (fields.size) {
                1 -> fields[0]
                2 -> stringResource(R.string.cards_why_check_two, fields[0], fields[1])
                else -> stringResource(R.string.cards_why_check_three, fields[0], fields[1], fields[2])
            }.replaceFirstChar { it.titlecase(Locale.getDefault()) }
            StatusWhy(
                title = stringResource(R.string.cards_why_check_title),
                body = AnnotatedString(
                    if (fields.size > 1) {
                        stringResource(R.string.cards_why_check_body_many, listed)
                    } else {
                        stringResource(R.string.cards_why_check_body, listed)
                    }
                ),
                action = editCard
            )
        }
        CardStatus.LAST_USED -> StatusWhy(
            title = stringResource(R.string.cards_last_used),
            body = lastUsedAt?.let { bolded(stringResource(R.string.cards_why_last_body), copiedWhen(it, today)) }
                ?: AnnotatedString(stringResource(R.string.cards_why_last_body_plain))
        )
        CardStatus.ARCHIVED -> StatusWhy(
            title = card.card.archiveReason?.takeIf { it.isNotBlank() }
                ?.let { stringResource(R.string.cards_status_archived_reason, it) }
                ?: stringResource(R.string.cards_status_archived),
            body = AnnotatedString(stringResource(R.string.cards_why_archived_body))
        )
    }
}

/** The fields a legacy repair left to check, in form order. */
internal fun flaggedFields(needsReview: Set<CardField>): List<CardField> =
    CardField.entries.filter { it in needsReview }

private fun fieldName(field: CardField): Int = when (field) {
    CardField.NUMBER -> R.string.cards_why_check_number
    CardField.EXPIRY -> R.string.cards_why_check_expiry
    CardField.CVV -> R.string.cards_why_check_cvv
}

/** "today at 9:38" or "22 Nov". */
@Composable
private fun copiedWhen(at: Long, today: LocalDate): String {
    val day = Instant.ofEpochMilli(at).atZone(ZoneId.systemDefault()).toLocalDate()
    if (day != today) return day.format(dayMonth())
    val time = DateFormat.getTimeFormat(LocalContext.current).format(Date(at))
    return stringResource(R.string.cards_why_today_at, time)
}

/** An argument wrapped this way stays regular weight; every other argument is bold. */
private class Plain(val text: String)

private fun plain(text: String) = Plain(text)

/** Formats [template] (`%1$s` …) with [args], setting the values in semibold. */
private fun bolded(template: String, vararg args: Any): AnnotatedString = buildAnnotatedString {
    var index = 0
    PLACEHOLDER.findAll(template).forEach { match ->
        append(template.substring(index, match.range.first))
        when (val arg = args[match.groupValues[1].toInt() - 1]) {
            is Plain -> append(arg.text)
            else -> withStyle(QcText.Emphasis) { append(arg.toString()) }
        }
        index = match.range.last + 1
    }
    append(template.substring(index))
}

private val PLACEHOLDER = Regex("%(\\d+)\\\$s")
