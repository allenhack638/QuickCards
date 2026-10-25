package com.quickcards.app.core.notifications

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.drawable.Icon
import com.quickcards.app.MainActivity
import com.quickcards.app.R

/**
 * Heads-up notification raised after a card number is copied. Card data never
 * travels in an Intent: the actions carry only the card id and the field.
 */
object CopyCompanionNotifier {

    const val CHANNEL_ID = "card_details"
    const val NOTIFICATION_ID = 1001

    const val EXTRA_CARD_ID = "card_id"
    const val EXTRA_FIELD = "field"
    const val ACTION_COPY_EXPIRY = "com.quickcards.app.action.COPY_EXPIRY"
    const val ACTION_COPY_CVV = "com.quickcards.app.action.COPY_CVV"
    const val FIELD_EXPIRY = "exp"
    const val FIELD_CVV = "cvv"

    fun notifyCardCopied(
        context: Context,
        cardId: String,
        title: String,
        expiry: String,
        cvv: String,
        showExpiry: Boolean,
        showCvv: Boolean,
        seconds: Int
    ) {
        if (!showExpiry && !showCvv) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        ensureChannel(context, manager)

        val values = buildList {
            if (showExpiry && expiry.isNotBlank()) {
                add(context.getString(R.string.notif_expiry_value, expiry))
            }
            if (showCvv && cvv.isNotBlank()) {
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

        if (showExpiry && expiry.isNotBlank()) {
            builder.addAction(
                action(
                    context,
                    context.getString(R.string.notif_action_copy_expiry),
                    ACTION_COPY_EXPIRY,
                    cardId,
                    FIELD_EXPIRY
                )
            )
        }
        if (showCvv && cvv.isNotBlank()) {
            builder.addAction(
                action(
                    context,
                    context.getString(R.string.notif_action_copy_cvv),
                    ACTION_COPY_CVV,
                    cardId,
                    FIELD_CVV
                )
            )
        }

        manager.notify(NOTIFICATION_ID, builder.build())
    }

    fun cancel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.cancel(NOTIFICATION_ID)
    }

    private fun ensureChannel(context: Context, manager: NotificationManager) {
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notif_channel_name),
            NotificationManager.IMPORTANCE_HIGH
        )
        manager.createNotificationChannel(channel)
    }

    private fun action(
        context: Context,
        label: String,
        action: String,
        cardId: String,
        field: String
    ): Notification.Action {
        val intent = Intent(context, CopyActionReceiver::class.java).apply {
            this.action = action
            putExtra(EXTRA_CARD_ID, cardId)
            putExtra(EXTRA_FIELD, field)
        }
        val pending = PendingIntent.getBroadcast(
            context,
            cardId.hashCode() * 31 + field.hashCode(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return Notification.Action.Builder(
            Icon.createWithResource(context, R.drawable.ic_stat_quickcards),
            label,
            pending
        ).build()
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
