package com.quickcards.app.core.reminders

import android.app.AlarmManager
import com.quickcards.app.core.security.PanHasher
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.drawable.Icon
import android.os.Build
import androidx.core.content.ContextCompat
import com.quickcards.app.MainActivity
import com.quickcards.app.QuickCardsApplication
import com.quickcards.app.R
import com.quickcards.domain.ReminderRules
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Calendar
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * Local reminders: a card expiring within 30 days, and a payment due day 3 days ahead.
 * A daily inexact alarm and every app start run the same check. An expiry reminder is posted
 * once; a payment reminder repeats daily inside its window until Paid. Notifications carry only
 * the bank and last four digits and are private on the lock screen.
 */
object Reminders {

    const val CHANNEL_ID = "reminders"
    private const val PREFS = PaidStore.PREFS
    private const val ACTION_CHECK = "com.quickcards.app.action.REMINDER_CHECK"
    const val ACTION_PAID = "com.quickcards.app.action.REMINDER_PAID"
    const val EXTRA_KEY = "key"
    private const val REQUEST_DAILY = 2001

    fun ensureScheduled(context: Context) {
        val alarms = context.getSystemService(AlarmManager::class.java) ?: return
        val at = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 9)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            if (timeInMillis <= System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, 1)
        }
        val intent = Intent(context, ReminderReceiver::class.java).setAction(ACTION_CHECK)
        val pending = PendingIntent.getBroadcast(
            context, REQUEST_DAILY, intent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        alarms.setInexactRepeating(AlarmManager.RTC, at.timeInMillis, AlarmManager.INTERVAL_DAY, pending)
    }

    suspend fun check(context: Context, today: LocalDate = LocalDate.now()) {
        val app = context.applicationContext as? QuickCardsApplication ?: return
        val settings = app.settings
        val expiryOn = settings.expiryReminders.value
        val dueOn = settings.paymentReminders.value
        if (!expiryOn && !dueOn) return
        if (!notificationsAllowed(context)) return
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        // 2.2 builds keyed expiry reminders on the plaintext expiry; drop those entries.
        prefs.all.keys.filter { it.startsWith("exp:") }.takeIf { it.isNotEmpty() }?.let { stale ->
            prefs.edit().apply { stale.forEach(::remove) }.apply()
        }
        val cards = app.repository.cards.first().filterNot { it.archived }
        val banks = app.repository.banks.first().associateBy { it.id }
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        ensureChannel(context, manager)
        for (card in cards) {
            val bank = card.bankId?.let { banks[it]?.name }.orEmpty().ifBlank { context.getString(R.string.app_name) }
            val title = context.getString(R.string.reminder_card_title, bank, card.number.takeLast(4))
            if (expiryOn) {
                ReminderRules.expiryReminderDays(card.expiry, today)?.let { days ->
                    val key = ReminderRules.expiryKey(card.id, PanHasher.hash("exp:" + card.expiry).take(16))
                    if (!prefs.contains(key)) {
                        val text = context.resources.getQuantityString(R.plurals.reminder_expiry_text, days.toInt(), days.toInt())
                        post(context, manager, key, title, text)
                        prefs.edit().putLong(key, System.currentTimeMillis()).apply()
                    }
                }
            }
            if (dueOn) {
                ReminderRules.dueReminderDate(card.dueDay, today)?.let { due ->
                    val period = ReminderRules.dueKey(card.id, due)
                    val todayKey = ReminderRules.dueDayKey(period, today)
                    if (!PaidStore.isPaid(context, period) && !prefs.contains(todayKey)) {
                        val text = context.getString(
                            R.string.reminder_due_text,
                            due.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM))
                        )
                        post(context, manager, period, title, text, paidAction = true)
                        prefs.edit().putLong(todayKey, System.currentTimeMillis()).apply()
                    }
                }
            }
        }
    }

    private fun post(
        context: Context,
        manager: NotificationManager,
        key: String,
        title: String,
        text: String,
        paidAction: Boolean = false
    ) {
        val open = PendingIntent.getActivity(
            context, key.hashCode(), Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val publicVersion = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_quickcards)
            .setContentTitle(context.getString(R.string.app_name))
            .setContentText(context.getString(R.string.reminder_public_text))
            .setVisibility(Notification.VISIBILITY_PUBLIC)
            .build()
        val builder = Notification.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_stat_quickcards)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(Notification.BigTextStyle().bigText(text))
            .setVisibility(Notification.VISIBILITY_PRIVATE)
            .setPublicVersion(publicVersion)
            .setLocalOnly(true)
            .setAutoCancel(true)
            .setContentIntent(open)
            .setCategory(Notification.CATEGORY_REMINDER)
        if (paidAction) {
            val paid = PendingIntent.getBroadcast(
                context,
                key.hashCode(),
                Intent(context, ReminderReceiver::class.java).setAction(ACTION_PAID).putExtra(EXTRA_KEY, key),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.addAction(
                Notification.Action.Builder(
                    Icon.createWithResource(context, R.drawable.ic_stat_quickcards),
                    context.getString(R.string.reminder_paid),
                    paid
                ).build()
            )
        }
        manager.notify(key.hashCode(), builder.build())
    }

    /** Undoes [markPaid]: the reminder comes back today rather than waiting for tomorrow. */
    suspend fun undoPaid(context: Context, key: String, today: LocalDate = LocalDate.now()) {
        PaidStore.unmarkPaid(context, key)
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .remove(ReminderRules.dueDayKey(key, today)).apply()
        check(context, today)
    }

    /** Erase all: forget every reminder sent and every payment marked. */
    fun clearAll(context: Context) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().clear().apply()
        PaidStore.reset()
        context.getSystemService(NotificationManager::class.java)?.cancelAll()
    }

    /** Records the payment for this due date and clears the reminder; no further nudges this month. */
    fun markPaid(context: Context, key: String) {
        PaidStore.markPaid(context, key)
        context.getSystemService(NotificationManager::class.java)?.cancel(key.hashCode())
    }

    private fun notificationsAllowed(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, android.Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED

    private fun ensureChannel(context: Context, manager: NotificationManager) {
        if (manager.getNotificationChannel(CHANNEL_ID) != null) return
        manager.createNotificationChannel(
            NotificationChannel(CHANNEL_ID, context.getString(R.string.reminder_channel_name), NotificationManager.IMPORTANCE_DEFAULT)
        )
    }
}

/** Runs the daily check off the main thread and finishes the broadcast when done. */
class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Reminders.ACTION_PAID) {
            intent.getStringExtra(Reminders.EXTRA_KEY)?.let { Reminders.markPaid(context, it) }
            return
        }
        val result = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                runCatching { Reminders.check(context) }
            } finally {
                result.finish()
            }
        }
    }
}

/** Alarms do not survive a reboot; re-arm the daily check. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) Reminders.ensureScheduled(context)
    }
}
