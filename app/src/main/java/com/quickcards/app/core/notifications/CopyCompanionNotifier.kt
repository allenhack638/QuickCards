package com.quickcards.app.core.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import android.os.Build
import android.os.Handler
import android.os.Looper
import com.quickcards.app.MainActivity
import com.quickcards.app.R
import com.quickcards.app.core.data.settings.AppSettings
import com.quickcards.app.core.security.ClipboardHelper

/**
 * The Expiry and CVV notification posted after an unlocked card number copy. It shows both
 * masked; its "Copy expiry" and "Copy CVV" actions copy without a second unlock because the
 * notification only exists after one.
 *
 * Silent and never heads-up (low-importance channel). VISIBILITY_SECRET rather than PRIVATE:
 * PRIVATE still shows the content on a lock screen set to "show all notification content".
 * The channel id changed with 2.4.0 because channel importance is immutable once created.
 */
object CopyCompanionNotifier {

    const val CHANNEL_ID = "card_details_v2"
    private const val LEGACY_CHANNEL_ID = "card_details"
    const val NOTIFICATION_ID = 1001

    internal const val ACTION_COPY = "com.quickcards.app.action.COPY_FROM_NOTIFICATION"
    internal const val EXTRA_TOKEN = "token"
    internal const val EXTRA_FIELD = "field"

    internal val tokens = CopyTokenStore()
    private val main = Handler(Looper.getMainLooper())
    private val expire = Runnable { tokens.clear() }

    fun notifyCardCopied(
        context: Context,
        bank: String,
        last4: String,
        expiry: String,
        cvv: String,
        seconds: Int
    ) {
        val fields = CopyNotificationText.fields(expiry, cvv)
        if (fields.isEmpty()) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        ensureChannel(context, manager)

        val title = context.getString(R.string.notif_card_title, bank, last4)
        val ttl = seconds * 1000L
        val token = tokens.issue(title, expiry, cvv, ttl)
        main.removeCallbacks(expire)
        main.postDelayed(expire, ttl)

        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_quickcards)
            .setColor(context.getColor(R.color.primary_brand))
            .setContentTitle(title)
            .setContentText(
                CopyNotificationText.text(
                    fields,
                    { context.getString(if (it == CopyField.EXPIRY) R.string.notif_expiry_masked else R.string.notif_cvv_masked) },
                    context.getString(R.string.notif_value_separator)
                )
            )
            .setVisibility(Notification.VISIBILITY_SECRET)
            .setOnlyAlertOnce(true)
            // Card values stay on this phone: no bridging to watches.
            .setLocalOnly(true)
            .setAutoCancel(false)
            .setCategory(Notification.CATEGORY_STATUS)
            .setContentIntent(openAppIntent(context))
            .setTimeoutAfter(ttl)
        fields.forEach { builder.addAction(copyAction(context, token, it)) }
        // No system smart replies or suggested actions built from the card values.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) builder.setAllowSystemGeneratedContextualActions(false)
        manager.notify(NOTIFICATION_ID, builder.build())
    }

    fun cancel(context: Context) {
        tokens.clear()
        context.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
    }

    private fun copyAction(context: Context, token: String, field: CopyField): Notification.Action {
        val label = if (field == CopyField.EXPIRY) R.string.notif_action_copy_expiry else R.string.notif_action_copy_cvv
        val intent = Intent(context, CopyActionReceiver::class.java)
            .setAction(ACTION_COPY)
            .putExtra(EXTRA_TOKEN, token)
            .putExtra(EXTRA_FIELD, field.name)
        val pending = PendingIntent.getBroadcast(
            context,
            NOTIFICATION_ID + field.ordinal + 1,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Action.Builder(
            Icon.createWithResource(context, R.drawable.ic_stat_quickcards),
            context.getString(label),
            pending
        ).build()
    }

    private fun ensureChannel(context: Context, manager: NotificationManager) {
        // Users upgrading from 2.3 keep the old channel; its importance cannot be lowered, so
        // delete it and post future copies through the silent channel.
        if (manager.getNotificationChannel(LEGACY_CHANNEL_ID) != null) {
            manager.deleteNotificationChannel(LEGACY_CHANNEL_ID)
        }
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_SECRET
        }
        manager.createNotificationChannel(channel)
    }

    private fun openAppIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        return PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

/** Copies the expiry or CVV for a live notification token; a stale token copies nothing. */
class CopyActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != CopyCompanionNotifier.ACTION_COPY) return
        if (!AppSettings.get(context).notifEnabled.value) {
            CopyCompanionNotifier.cancel(context)
            return
        }
        val field = intent.getStringExtra(CopyCompanionNotifier.EXTRA_FIELD)
            ?.let { name -> CopyField.entries.firstOrNull { it.name == name } } ?: return
        val value = CopyCompanionNotifier.tokens.value(
            intent.getStringExtra(CopyCompanionNotifier.EXTRA_TOKEN),
            field
        )
        if (value == null) {
            CopyCompanionNotifier.cancel(context)
            return
        }
        val label = context.getString(
            if (field == CopyField.EXPIRY) R.string.cards_field_expiry else R.string.cards_field_cvv
        )
        ClipboardHelper.copyTimed(context, context.getString(R.string.cards_clipboard_field, value.title, label), value.text)
    }
}
