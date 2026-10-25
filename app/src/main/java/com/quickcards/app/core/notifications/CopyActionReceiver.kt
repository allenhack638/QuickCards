package com.quickcards.app.core.notifications

import android.app.NotificationManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.quickcards.app.QuickCardsApplication
import com.quickcards.app.R
import com.quickcards.app.core.security.ClipboardHelper
import com.quickcards.app.core.security.UnlockSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class CopyActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as? QuickCardsApplication ?: return
        val cardId = intent.getStringExtra(CopyCompanionNotifier.EXTRA_CARD_ID) ?: return
        val field = intent.getStringExtra(CopyCompanionNotifier.EXTRA_FIELD) ?: return
        val isCvv = field == CopyCompanionNotifier.FIELD_CVV
        val fieldLabel = context.getString(
            if (isCvv) R.string.notif_field_cvv else R.string.notif_field_expiry
        )
        val copiedText = context.getString(
            if (isCvv) R.string.notif_cvv_copied else R.string.notif_expiry_copied
        )
        // The copy window is the only thing that authorises a copy. After a
        // crash or force-stop it is closed, so a notification that outlived the
        // process must not hand out the CVV.
        if (!UnlockSession.isUnlocked.value) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.cancel(CopyCompanionNotifier.NOTIFICATION_ID)
            Toast.makeText(context, R.string.notif_open_to_copy, Toast.LENGTH_SHORT).show()
            return
        }
        val pending = goAsync()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
        scope.launch {
            try {
                val card = app.repository.getCard(cardId) ?: return@launch
                val value = if (isCvv) card.cvv else card.expiry
                val bank = card.bankId?.let { app.repository.getBank(it) }
                val title = bank?.name ?: context.getString(R.string.notif_fallback_title)
                val label = context.getString(
                    R.string.notif_clipboard_label,
                    title,
                    fieldLabel
                )
                val clearSec = app.settings.clipClearSec.value
                ClipboardHelper.copySensitive(
                    context,
                    label,
                    value,
                    if (clearSec > 0) clearSec * 1000L else null
                )
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                manager.cancel(CopyCompanionNotifier.NOTIFICATION_ID)
                Toast.makeText(context, copiedText, Toast.LENGTH_SHORT).show()
            } catch (_: Exception) {
                Toast.makeText(context, copiedText, Toast.LENGTH_SHORT).show()
            } finally {
                pending.finish()
            }
        }
    }
}
