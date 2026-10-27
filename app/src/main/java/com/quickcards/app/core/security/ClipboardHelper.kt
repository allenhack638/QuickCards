package com.quickcards.app.core.security

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Copies one card value with a clear timer.
 *
 * The clip is flagged sensitive on Android 13+ so the system's clipboard preview does not
 * render the value. The flag is deliberately not treated as a reason to skip the timer: the
 * value stays on the clipboard while the user switches to the app they are pasting into.
 *
 * A timer cannot outlive the process, so the deadline is also persisted (as a keyed
 * HMAC-SHA256 token, never the value). [expireIfNeeded] runs when the app is resumed and
 * clears a copy whose deadline passed while the process was dead.
 */
object ClipboardHelper {

    private const val CLEAR_DELAY_MS = 30_000L
    private const val PREFS_NAME = "quickcards_clipboard"
    private const val KEY_HASH = "copy_hash"
    private const val KEY_EXPIRES_AT = "copy_expires_at"

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var clearJob: Job? = null
    private var appContext: Context? = null
    private var lastCopiedText: String? = null

    /** Called once from the Application; only stores the application context. */
    fun init(context: Context) {
        appContext = context.applicationContext
    }

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
        val app = context.applicationContext
        appContext = app
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = ClipData.newPlainText(label, text)
        markSensitive(clip)
        clipboard.setPrimaryClip(clip)
        lastCopiedText = text
        clearJob?.cancel()
        if (clearAfterMillis != null && clearAfterMillis > 0) {
            rememberCopy(app, text, System.currentTimeMillis() + clearAfterMillis)
            clearJob = scope.launch {
                delay(clearAfterMillis)
                clearIfStillOurs(clipboard)
                forgetCopy(app)
            }
        } else {
            rememberCopy(app, text, 0L)
            clearJob = null
        }
    }

    fun clearNow() {
        clearJob?.cancel()
        clearJob = null
        val context = appContext ?: return
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        clearIfStillOurs(clipboard)
        forgetCopy(context)
    }

    /**
     * Called from the foreground: a copy whose timer was lost to process death is cleared if it
     * is still the clipboard value. A null read (unfocused) leaves it for the next resume.
     */
    fun expireIfNeeded(context: Context) {
        val app = context.applicationContext
        appContext = app
        val prefs = prefs(app)
        val expiresAt = prefs.getLong(KEY_EXPIRES_AT, 0L)
        if (expiresAt <= 0L) return
        if (System.currentTimeMillis() < expiresAt) return

        val expected = prefs.getString(KEY_HASH, null)
        if (expected == null) {
            forgetCopy(app)
            return
        }
        val clipboard = app.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val current = runCatching { clipboard.primaryClip?.getItemAt(0)?.text?.toString() }
            .getOrNull()
        // An unfocused app reads null on Android 10+; keep the record and retry next resume.
        if (current == null) return
        val currentHash = runCatching { PanHasher.hashText(current) }.getOrNull()
        if (currentHash == null) {
            forgetCopy(app)
            return
        }
        if (currentHash == expected) {
            clearClipboard(clipboard)
            lastCopiedText = null
            clearJob?.cancel()
            clearJob = null
        }
        forgetCopy(app)
    }

    private fun markSensitive(clip: ClipData) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        clip.description.extras = PersistableBundle().apply {
            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
        }
    }

    /** From Android 10 an unfocused app reads `primaryClip` as null; a null is treated as ours. */
    private fun clearIfStillOurs(clipboard: ClipboardManager) {
        if (lastCopiedText == null) return
        val current = runCatching { clipboard.primaryClip?.getItemAt(0)?.text?.toString() }
            .getOrNull()
        if (current == null || current == lastCopiedText) {
            clearClipboard(clipboard)
        }
        lastCopiedText = null
    }

    private fun clearClipboard(clipboard: ClipboardManager) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            clipboard.clearPrimaryClip()
        } else {
            clipboard.setPrimaryClip(ClipData.newPlainText("", ""))
        }
    }

    private fun rememberCopy(context: Context, text: String, expiresAt: Long) {
        val token = runCatching { PanHasher.hashText(text) }.getOrNull() ?: return
        prefs(context).edit()
            .putString(KEY_HASH, token)
            .putLong(KEY_EXPIRES_AT, expiresAt)
            .apply()
    }

    private fun forgetCopy(context: Context) {
        prefs(context).edit()
            .remove(KEY_HASH)
            .remove(KEY_EXPIRES_AT)
            .apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
