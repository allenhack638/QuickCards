package com.quickcards.app.core.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.quickcards.app.MainActivity
import com.quickcards.app.R

/**
 * The Expiry and CVV notification posted after an unlocked card number copy. It shows the
 * card's expiry and CVV for the configured time; the values come from the in-memory card and
 * are never put in an intent.
 *
 * Silent and never heads-up (low-importance channel). VISIBILITY_SECRET rather than PRIVATE:
 * PRIVATE still shows the content on a lock screen set to "show all notification content".
 * The channel id changed with 2.4.0 because channel importance is immutable once created.
 */
object CopyCompanionNotifier {

    const val CHANNEL_ID = "card_details_v2"
    private const val LEGACY_CHANNEL_ID = "card_details"
    const val NOTIFICATION_ID = 1001

    fun notifyCardCopied(
        context: Context,
        bank: String,
        last4: String,
        expiry: String,
        cvv: String,
        seconds: Int
    ) {
        val text = CopyNotificationText.text(
            expiry,
            cvv,
            { context.getString(R.string.notif_expiry_value, it) },
            { context.getString(R.string.notif_cvv_value, it) },
            context.getString(R.string.notif_value_separator)
        )
        if (text.isEmpty()) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        ensureChannel(context, manager)

        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_quickcards)
            .setColor(context.getColor(R.color.primary_brand))
            .setContentTitle(context.getString(R.string.notif_card_title, bank, last4))
            .setContentText(text)
            .setVisibility(Notification.VISIBILITY_SECRET)
            .setOnlyAlertOnce(true)
            // Card values stay on this phone: no bridging to watches.
            .setLocalOnly(true)
            .setAutoCancel(false)
            .setCategory(Notification.CATEGORY_STATUS)
            .setContentIntent(openAppIntent(context))
            .setTimeoutAfter(seconds * 1000L)
        // No system smart replies or suggested actions built from the card values.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) builder.setAllowSystemGeneratedContextualActions(false)
        manager.notify(NOTIFICATION_ID, builder.build())
    }

    fun cancel(context: Context) {
        context.getSystemService(NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
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
