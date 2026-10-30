package com.quickcards.app.feature.cardform

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import com.quickcards.app.R
import com.quickcards.domain.CardError
import com.quickcards.domain.CardNetwork
import com.quickcards.domain.DefaultNetworks

@Composable
internal fun cardErrorText(error: CardError?): String? {
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
            error.holderName ?: stringResource(R.string.form_a_holder)
        )
        CardError.ExpiryFormat -> stringResource(R.string.form_error_expiry_format)
        CardError.Expired -> stringResource(R.string.form_error_expiry_past)
        CardError.ExpiryYear -> stringResource(R.string.form_error_expiry_future)
        is CardError.CvvLength -> pluralStringResource(
            R.plurals.form_error_cvv_length,
            error.digits,
            error.digits
        )
        CardError.CvvUnknown -> stringResource(R.string.form_error_cvv_unknown)
        CardError.BankRequired -> stringResource(R.string.form_error_bank_required)
    }
}

@Composable
internal fun cvvHelperText(network: CardNetwork?): String =
    if (network == null) {
        stringResource(R.string.form_cvv_digits_unknown)
    } else if (DefaultNetworks.cvvOptional(network)) {
        stringResource(R.string.form_cvv_digits_optional)
    } else {
        pluralStringResource(R.plurals.form_cvv_digits, network.cvvLength, network.cvvLength)
    }
