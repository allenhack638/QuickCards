package com.quickcards.app.core.reminders

import android.app.NotificationManager
import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quickcards.app.QuickCardsApplication
import com.quickcards.app.core.data.backup.BackupImporter
import com.quickcards.app.core.data.model.SeedData
import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.domain.ReminderRules
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** A payment reminder repeats daily inside its window until Paid, then stays quiet. */
@RunWith(AndroidJUnit4::class)
class PaymentReminderTest {

    private lateinit var app: QuickCardsApplication
    private lateinit var manager: NotificationManager
    private val today = LocalDate.of(2026, 3, 3)
    private val due = LocalDate.of(2026, 3, 5)

    @Before
    fun setUp() = runBlocking {
        app = ApplicationProvider.getApplicationContext()
        manager = app.getSystemService(NotificationManager::class.java)
        manager.cancelAll()
        app.getSharedPreferences("quickcards_reminders", 0).edit().clear().commit()
        BackupImporter.eraseAll(app.database)
        app.database.cardTypeDao().insertCardTypes(SeedData.cardTypes)
        app.settings.setPaymentReminders(true)
        app.settings.setExpiryReminders(false)
    }

    /** The notification service applies posts and cancels asynchronously; wait for it to settle. */
    private fun reminderCount(key: String, expected: Int? = null): Int {
        repeat(20) {
            val count = manager.activeNotifications.count { it.id == key.hashCode() }
            if (expected == null || count == expected) return count
            Thread.sleep(100)
        }
        return manager.activeNotifications.count { it.id == key.hashCode() }
    }

    @Test
    fun paidStopsTheReminderForThatMonth() = runBlocking<Unit> {
        val card = app.repository.insertCard(
            VaultCard(number = "4532778103492211", expiry = "12/29", cvv = "381", dueDay = 5)
        ).getOrThrow()
        val key = ReminderRules.dueKey(card.id, due)

        Reminders.check(app, today)
        assertEquals(1, reminderCount(key, 1))
        val action = manager.activeNotifications.single { it.id == key.hashCode() }.notification.actions.single()
        assertEquals("Paid", action.title.toString())

        ReminderReceiver().onReceive(app, Intent(Reminders.ACTION_PAID).putExtra(Reminders.EXTRA_KEY, key))
        assertEquals(0, reminderCount(key, 0))

        Thread.sleep(1_000)
        Reminders.check(app, today.plusDays(1))
        Thread.sleep(1_000)
        assertEquals(0, reminderCount(key))
    }

    @Test
    fun unpaidReminderComesBackTheNextDay() = runBlocking<Unit> {
        val card = app.repository.insertCard(
            VaultCard(number = "5555555555554444", expiry = "12/29", cvv = "502", dueDay = 5)
        ).getOrThrow()
        val key = ReminderRules.dueKey(card.id, due)

        Reminders.check(app, today)
        assertEquals(1, reminderCount(key, 1))
        manager.cancel(key.hashCode())
        assertEquals(0, reminderCount(key, 0))
        Reminders.check(app, today)
        Thread.sleep(1_000)
        assertEquals("no second post on the same day", 0, reminderCount(key))

        Reminders.check(app, today.plusDays(1))
        assertEquals("comes back the next day", 1, reminderCount(key, 1))
    }
}
