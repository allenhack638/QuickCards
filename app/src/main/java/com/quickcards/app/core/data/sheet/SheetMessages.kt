package com.quickcards.app.core.data.sheet

import android.content.res.Resources
import com.quickcards.app.R
import com.quickcards.domain.sheet.RowProblem
import com.quickcards.domain.sheet.SheetFileProblem

/** Plain-language text for every import problem; the only place these sentences are built. */
object SheetMessages {

    fun file(resources: Resources, problem: SheetFileProblem): String = when (problem) {
        is SheetFileProblem.MissingColumns -> resources.getString(R.string.import_file_missing_columns, problem.headers.joinToString(", "))
        is SheetFileProblem.UnknownColumns -> resources.getString(R.string.import_file_unknown_columns, problem.headers.joinToString(", "))
        SheetFileProblem.NoHeaderRow -> resources.getString(R.string.import_file_no_header)
        SheetFileProblem.NoRows -> resources.getString(R.string.import_file_no_rows)
        is SheetFileProblem.TooManyRows -> resources.getString(R.string.import_file_too_many_rows, problem.rows, problem.max)
    }

    fun open(resources: Resources, problem: SheetOpenProblem): String = when (problem) {
        SheetOpenProblem.TooLarge -> resources.getString(R.string.import_file_too_large)
        SheetOpenProblem.NotASheet -> resources.getString(R.string.import_file_not_sheet)
        SheetOpenProblem.Unreadable -> resources.getString(R.string.import_file_unreadable)
    }

    fun row(resources: Resources, problem: RowProblem): String = when (problem) {
        is RowProblem.Missing -> resources.getString(R.string.import_problem_missing, problem.column.header)
        RowProblem.NumberNotDigits -> resources.getString(R.string.import_problem_number_not_digits)
        RowProblem.NumberChangedBySpreadsheet -> resources.getString(R.string.import_problem_number_changed, "4.11111E+15")
        is RowProblem.NumberLength -> resources.getString(R.string.import_problem_number_length, problem.networkName, problem.lengths)
        RowProblem.NumberLuhn -> resources.getString(R.string.import_problem_number_luhn)
        RowProblem.NumberUnknownNetwork -> resources.getString(R.string.import_problem_number_unknown_network)
        is RowProblem.NetworkUnknown -> resources.getString(R.string.import_problem_network_unknown, problem.value)
        is RowProblem.NetworkMismatch -> resources.getString(R.string.import_problem_network_mismatch, problem.looksLike, problem.picked)
        RowProblem.MonthInvalid -> resources.getString(R.string.import_problem_month_invalid)
        RowProblem.YearInvalid -> resources.getString(R.string.import_problem_year_invalid)
        RowProblem.Expired -> resources.getString(R.string.import_problem_expired)
        RowProblem.ExpiryTooFar -> resources.getString(R.string.import_problem_expiry_too_far)
        is RowProblem.CvvLength -> resources.getQuantityString(R.plurals.import_problem_cvv_length, problem.digits, problem.digits)
        is RowProblem.BankUnknown -> resources.getString(R.string.import_problem_bank_unknown, problem.value)
        is RowProblem.TypeUnknown -> resources.getString(R.string.import_problem_type_unknown, problem.value)
        is RowProblem.HolderTooLong -> resources.getString(R.string.import_problem_holder_too_long, problem.max)
        RowProblem.DueDayInvalid -> resources.getString(R.string.import_problem_due_day_invalid)
        is RowProblem.NoteTooLong -> resources.getString(R.string.import_problem_note_too_long, problem.max)
        is RowProblem.RepeatedInFile -> resources.getString(R.string.import_problem_repeated_in_file, problem.firstRow)
        RowProblem.AlreadyInAppWithDifferentDetails -> resources.getString(R.string.import_problem_already_different)
    }
}
