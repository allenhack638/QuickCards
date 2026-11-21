package com.quickcards.app.feature.cardform

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.IntentFilter
import android.nfc.NfcAdapter
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.quickcards.app.R
import com.quickcards.app.core.nfc.NfcCardReader
import com.quickcards.app.core.ui.components.NetworkMark
import com.quickcards.app.core.ui.components.QcDialog
import com.quickcards.app.core.ui.components.QcModalBottomSheet
import com.quickcards.app.core.ui.components.QcSheetActions
import com.quickcards.app.core.ui.components.markTone
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcSize
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTextButton
import com.quickcards.app.core.ui.theme.QcTextTone
import com.quickcards.app.core.ui.theme.QcTheme
import com.quickcards.domain.EmvCardData

private enum class TapState { WAIT, READ, FAIL }

/**
 * Tap to add: reader mode is on only while this sheet is open. A read fills number and expiry
 * at once and the sheet confirms it; a failed read offers Try again. NFC off is a dialog with
 * Turn on, and the sheet takes over as soon as NFC comes on.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun NfcTapSheet(
    onDismiss: () -> Unit,
    onRead: (EmvCardData) -> Unit,
    networkIdFor: (String) -> String?
) {
    val context = LocalContext.current
    val adapter = remember { NfcAdapter.getDefaultAdapter(context) }
    val activity = remember { context.findActivity() }
    val latestRead by rememberUpdatedState(onRead)
    var state by remember { mutableStateOf(TapState.WAIT) }
    var read by remember { mutableStateOf<EmvCardData?>(null) }
    var enabled by remember { mutableStateOf(adapter?.isEnabled == true) }

    DisposableEffect(adapter) {
        if (adapter == null) return@DisposableEffect onDispose {}
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(c: Context, intent: Intent) {
                enabled = adapter.isEnabled
            }
        }
        context.registerReceiver(receiver, IntentFilter(NfcAdapter.ACTION_ADAPTER_STATE_CHANGED))
        onDispose { context.unregisterReceiver(receiver) }
    }

    DisposableEffect(adapter, activity, enabled) {
        if (adapter == null || activity == null || !enabled) return@DisposableEffect onDispose {}
        val main = Handler(Looper.getMainLooper())
        // A slow read can finish after Cancel; a closed sheet must not fill the form.
        var open = true
        adapter.enableReaderMode(
            activity,
            { tag ->
                val data = NfcCardReader.read(tag)
                main.post {
                    if (!open) return@post
                    if (data != null) {
                        read = data
                        state = TapState.READ
                        latestRead(data)
                    } else {
                        state = TapState.FAIL
                    }
                }
            },
            NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK or NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
            null
        )
        onDispose {
            open = false
            adapter.disableReaderMode(activity)
        }
    }

    if (!enabled) {
        NfcOffDialog(onDismiss = onDismiss)
        return
    }

    val colors = QcTheme.colors
    QcModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = QcSpacing.s6, end = QcSpacing.s6, top = QcSpacing.s2),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.form_nfc_title),
                style = QcText.F4,
                color = colors.ink,
                textAlign = TextAlign.Center,
                modifier = Modifier.semantics { heading() }
            )
            TapRing(state)
            when (state) {
                TapState.WAIT -> Text(
                    text = stringResource(R.string.form_nfc_body),
                    style = QcText.F2,
                    color = colors.ink2,
                    textAlign = TextAlign.Center
                )

                TapState.FAIL -> Text(
                    text = stringResource(R.string.form_nfc_failed),
                    style = QcText.F2,
                    color = colors.danger,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Assertive }
                )

                TapState.READ -> Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = stringResource(R.string.form_nfc_read),
                        style = QcText.F2,
                        color = colors.ink2,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                    )
                    read?.let { data -> ReadLine(data, networkIdFor(data.number)) }
                }
            }
        }
        when (state) {
            TapState.WAIT -> QcSheetActions(
                primary = null,
                onPrimary = {},
                secondary = stringResource(R.string.form_nfc_cancel),
                onSecondary = onDismiss
            )

            TapState.READ -> QcSheetActions(primary = stringResource(R.string.form_action_done), onPrimary = onDismiss)

            TapState.FAIL -> QcSheetActions(
                primary = stringResource(R.string.form_nfc_try_again),
                onPrimary = { state = TapState.WAIT },
                secondary = stringResource(R.string.form_nfc_cancel),
                onSecondary = onDismiss
            )
        }
    }
}

/** 64dp disc with two soft halos (20 and 40 out) in the state's tint; 48 above and below. */
@Composable
private fun TapRing(state: TapState) {
    val colors = QcTheme.colors
    val tint = when (state) {
        TapState.WAIT -> colors.accent
        TapState.READ -> colors.ok
        TapState.FAIL -> colors.danger
    }
    val waiting = stringResource(R.string.form_nfc_waiting_a11y)
    Box(
        modifier = Modifier
            .padding(vertical = QcSpacing.s12)
            .size(QcSpacing.s16)
            .then(
                if (state == TapState.WAIT) {
                    Modifier.semantics {
                        contentDescription = waiting
                        liveRegion = LiveRegionMode.Polite
                    }
                } else {
                    Modifier
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(QcSpacing.s16)) {
            val r = size.minDimension / 2
            drawCircle(tint.copy(alpha = HALO_FAR), radius = r + QcSpacing.s10.toPx())
            drawCircle(tint.copy(alpha = HALO_NEAR), radius = r + QcSpacing.s5.toPx())
            drawCircle(colors.surfaceHi, radius = r)
            drawCircle(tint.copy(alpha = HALO_NEAR), radius = r)
        }
        Icon(
            imageVector = when (state) {
                TapState.WAIT -> QcIcons.contactless
                TapState.READ -> QcIcons.checkCircle
                TapState.FAIL -> QcIcons.error
            },
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(QcSize.iconLg)
        )
    }
}

private const val HALO_NEAR = 0.14f
private const val HALO_FAR = 0.07f

@Composable
private fun ReadLine(data: EmvCardData, networkId: String?) {
    val colors = QcTheme.colors
    Row(
        modifier = Modifier.padding(top = QcSpacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(QcSpacing.s3)
    ) {
        networkId?.let { id ->
            NetworkMark(networkId = id, contentDescription = null, tone = markTone(id, onArt = false, dark = colors.dark))
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(text = MASK, style = QcText.MonoDotsF3, color = colors.ink, modifier = Modifier.padding(end = QcSpacing.s1))
            Text(text = data.number.takeLast(4), style = QcText.MonoF3, color = colors.ink)
        }
        data.expiry?.let { Text(text = it, style = QcText.MonoF3, color = colors.ink) }
    }
}

private const val MASK = "••"

@Composable
private fun NfcOffDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val turnOnA11y = stringResource(R.string.form_nfc_turn_on_a11y)
    QcDialog(
        onDismissRequest = onDismiss,
        title = stringResource(R.string.form_nfc_off),
        actions = {
            QcTextButton(
                text = stringResource(R.string.form_nfc_cancel),
                onClick = onDismiss,
                tone = QcTextTone.Neutral
            )
            QcTextButton(
                text = stringResource(R.string.form_nfc_turn_on),
                onClick = { context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS)) },
                modifier = Modifier.semantics { contentDescription = turnOnA11y }
            )
        }
    )
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
