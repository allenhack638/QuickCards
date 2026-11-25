package com.quickcards.app.feature.cardform

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.quickcards.app.R
import com.quickcards.domain.CardError

/** [number] supplies the last four for a duplicate ("Already saved as HDFC Bank ••0366"). */
@Composable
internal fun cardErrorText(error: CardError?, number: String = ""): String? {
    if (error == null) return null
    return when (error) {
        CardError.HolderRequired -> stringResource(R.string.form_error_holder_required)
        CardError.NumberRequired -> stringResource(R.string.form_error_number_required)
        is CardError.NetworkLength -> stringResource(
            R.string.form_error_number_length_network,
            error.networkName,
            error.lengths
        )
        CardError.NumberLength -> stringResource(R.string.form_error_number_length_generic)
        CardError.Luhn -> stringResource(R.string.form_error_number_luhn)
        is CardError.Duplicate -> stringResource(
            R.string.form_error_number_duplicate,
            error.title,
            number.takeLast(4)
        )
        CardError.ExpiryFormat -> stringResource(R.string.form_error_expiry_format)
        CardError.ExpiryMonth -> stringResource(R.string.form_error_expiry_month)
        CardError.Expired -> stringResource(R.string.form_error_expiry_past)
        CardError.ExpiryYear -> stringResource(R.string.form_error_expiry_future)
        is CardError.CvvLength -> pluralStringResource(
            R.plurals.form_error_cvv_length,
            error.digits,
            error.digits
        )
        CardError.CvvUnknown -> stringResource(R.string.form_error_cvv_unknown)
        CardError.BankRequired -> stringResource(R.string.form_error_bank_required)
        CardError.TypeRequired -> stringResource(R.string.form_error_type_required)
    }
}

@Composable
internal fun saveIssueText(issue: SaveIssue): String = stringResource(
    when (issue) {
        SaveIssue.HOLDER -> R.string.form_save_missing_holder
        SaveIssue.NUMBER_MISSING -> R.string.form_save_missing_number
        SaveIssue.NUMBER_BAD -> R.string.form_save_bad_number
        SaveIssue.EXPIRY_MISSING -> R.string.form_save_missing_expiry
        SaveIssue.EXPIRY_BAD -> R.string.form_save_bad_expiry
        SaveIssue.CVV_MISSING -> R.string.form_save_missing_cvv
        SaveIssue.CVV_BAD -> R.string.form_save_bad_cvv
        SaveIssue.BANK -> R.string.form_save_missing_bank
        SaveIssue.TYPE -> R.string.form_save_missing_type
        SaveIssue.STATEMENT_DAY -> R.string.form_save_bad_statement_day
        SaveIssue.DUE_DAY -> R.string.form_save_bad_due_day
    }
)
