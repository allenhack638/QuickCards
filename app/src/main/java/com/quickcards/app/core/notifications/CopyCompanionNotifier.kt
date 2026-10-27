package com.quickcards.app.core.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.quickcards.app.MainActivity
import com.quickcards.app.R

/**
 * Post-copy notification: shows the expiry and CVV when enabled in Settings; no actions.
 *
 * The channel is low-importance and silent on purpose: this is a reference card the user can
 * pull down while pasting, not a heads-up that renders the CVV over whatever app is in front.
 * The channel id changed with 2.4.0 because channel importance is immutable once created.
 */
object CopyCompanionNotifier {

    const val CHANNEL_ID = "card_details_v2"
    private const val LEGACY_CHANNEL_ID = "card_details"
    const val NOTIFICATION_ID = 1001


    fun notifyCardCopied(
        context: Context,
        title: String,
        expiry: String,
        cvv: String,
        seconds: Int
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(context, manager)

        val values = buildList {
            if (expiry.isNotBlank()) {
                add(context.getString(R.string.notif_expiry_value, expiry))
            }
            if (cvv.isNotBlank()) {
                add(context.getString(R.string.notif_cvv_value, cvv))
            }
        }
        if (values.isEmpty()) return

        val separator = context.getString(R.string.notif_value_separator)
        val publicVersion = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_quickcards)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.notif_hidden))
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .build()

        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_quickcards)
            .setContentTitle(title)
            .setContentText(values.joinToString(separator))
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setOnlyAlertOnce(true)
            // Card values stay on this phone: no bridging to watches.
            .setLocalOnly(true)
            .setAutoCancel(false)
            .setCategory(Notification.CATEGORY_STATUS)
            .setContentIntent(openAppIntent(context))
            .setTimeoutAfter(seconds * 1000L)

        manager.notify(NOTIFICATION_ID, builder.build())
    }

    fun cancel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
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
            lockscreenVisibility = Notification.VISIBILITY_PRIVATE
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
