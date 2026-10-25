package com.quickcards.app.core.security

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
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

    init {
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStop(owner: LifecycleOwner) {
                clearNow()
            }
        })
    }

    /** @param clearAfterMillis 0 or null keeps the value until the app is backgrounded. */
    fun copySensitive(
        context: Context,
        label: String,
        text: String,
        clearAfterMillis: Long? = CLEAR_DELAY_MS
    ) {
        appContext = context.applicationContext
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            clip.description.extras = PersistableBundle().apply {
                putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
            }
        }

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

    /**
     * From Android 10 an unfocused app reads `primaryClip` as null, so when backgrounding we
     * clear unconditionally: a card value must not outlive the app.
     */
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
