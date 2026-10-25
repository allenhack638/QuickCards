package com.quickcards.app.feature.cardform

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.nfc.NfcAdapter
import android.os.Handler
import android.os.Looper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.quickcards.app.R
import com.quickcards.app.core.nfc.NfcCardReader
import com.quickcards.app.core.ui.components.QcModalBottomSheet
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcType
import com.quickcards.domain.EmvCardData

/**
 * "Hold your card to the phone": reader mode is on only while this sheet is open, and the
 * chip's number and expiry go straight into the form. A failed read keeps the sheet open
 * with a retry hint; closing the sheet ends the reader.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NfcTapSheet(onDismiss: () -> Unit, onRead: (EmvCardData) -> Unit) {
    val context = LocalContext.current
    val adapter = remember { NfcAdapter.getDefaultAdapter(context) }
    val activity = remember { context.findActivity() }
    val latestRead by rememberUpdatedState(onRead)
    var failed by remember { mutableStateOf(false) }
    val enabled = adapter?.isEnabled == true

    DisposableEffect(adapter, activity, enabled) {
        if (adapter == null || activity == null || !enabled) return@DisposableEffect onDispose {}
        val main = Handler(Looper.getMainLooper())
        adapter.enableReaderMode(
            activity,
            { tag ->
                val data = NfcCardReader.read(tag)
                main.post { if (data != null) latestRead(data) else failed = true }
            },
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
            null
        )
        onDispose { adapter.disableReaderMode(activity) }
    }

    QcModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = QcDimens.Gutter)
                .padding(top = QcDimens.SheetTitleTop, bottom = QcDimens.SheetBottom),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.form_nfc_title),
                style = QcType.Headline,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(QcDimens.Gutter))
            Icon(
                imageVector = QcIcons.nfc,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(QcDimens.LockMarkCompact)
            )
            Spacer(modifier = Modifier.height(QcDimens.Gutter))
            Text(
                text = stringResource(
                    when {
                        !enabled -> R.string.form_nfc_off
                        failed -> R.string.form_nfc_failed
                        else -> R.string.form_nfc_body
                    }
                ),
                style = QcType.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (enabled) {
                Spacer(modifier = Modifier.height(QcDimens.Base))
                Text(
                    text = stringResource(R.string.form_nfc_waiting),
                    style = QcType.Label,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Spacer(modifier = Modifier.height(QcDimens.Gutter))
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(
                    onClick = onDismiss,
                    colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.onSurface)
                ) {
                    Text(stringResource(R.string.form_nfc_cancel))
                }
            }
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
