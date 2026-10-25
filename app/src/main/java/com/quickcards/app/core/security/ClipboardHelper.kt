package com.quickcards.app.core.security

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

object ClipboardHelper {

    private const val CLEAR_DELAY_MS = 30_000L

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var clearJob: Job? = null
    private var appContext: Context? = null
    private var lastCopiedText: String? = null

    /**
     * The value stays on the clipboard for the chosen time even when the user switches to the
     * app they are pasting into; only the timer, Clear or a new copy removes it.
     *
     * @param clearAfterMillis 0 or null keeps the value until it is replaced or cleared.
     */
    fun copySensitive(
        context: Context,
        label: String,
        text: String,
        clearAfterMillis: Long? = CLEAR_DELAY_MS
    ) {
        appContext = context.applicationContext
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        // Not flagged sensitive on purpose: the keyboard's clipboard preview is how the value
        // is found and pasted, and the timer below removes it.
        val clip = ClipData.newPlainText(label, text)
        clipboard.setPrimaryClip(clip)
        lastCopiedText = text
        clearJob?.cancel()
        if (clearAfterMillis != null && clearAfterMillis > 0) {
            clearJob = scope.launch {
                delay(clearAfterMillis)
                clearIfStillOurs(clipboard)
            }
        } else {
            clearJob = null
        }
    }

    fun clearNow() {
        clearJob?.cancel()
        clearJob = null
        val context = appContext ?: return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clearIfStillOurs(clipboard)
    }

    /** From Android 10 an unfocused app reads `primaryClip` as null; a null is treated as ours. */
    private fun clearIfStillOurs(clipboard: ClipboardManager) {
        if (lastCopiedText == null) return
        val current = runCatching { clipboard.primaryClip?.getItemAt(0)?.text?.toString() }
            .getOrNull()
        if (current == null || current == lastCopiedText) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                clipboard.clearPrimaryClip()
            } else {
                clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
            }
        }
        lastCopiedText = null
    }
}
