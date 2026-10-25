package com.quickcards.app.core.reminders

import android.content.Context
import com.quickcards.domain.ReminderRules
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The due dates the user has marked as paid, shared by the notification action and the
 * card's "Mark paid" button so either one clears both. Keys are [ReminderRules.dueKey].
 */
object PaidStore {

    internal const val PREFS = "quickcards_reminders"
    private const val PAID_PREFIX = "paid:"

    @Volatile
    private var state: MutableStateFlow<Set<String>>? = null

    fun paid(context: Context): StateFlow<Set<String>> = flow(context).asStateFlow()

    fun isPaid(context: Context, dueKey: String): Boolean = dueKey in flow(context).value

    fun markPaid(context: Context, dueKey: String) {
        prefs(context).edit().putLong(ReminderRules.paidKey(dueKey), System.currentTimeMillis()).apply()
        flow(context).value = flow(context).value + dueKey
    }

    fun unmarkPaid(context: Context, dueKey: String) {
        prefs(context).edit().remove(ReminderRules.paidKey(dueKey)).apply()
        flow(context).value = flow(context).value - dueKey
    }

    internal fun reset() {
        state?.value = emptySet()
    }

    private fun flow(context: Context): MutableStateFlow<Set<String>> =
        state ?: synchronized(this) {
            state ?: MutableStateFlow(
                prefs(context).all.keys
                    .filter { it.startsWith(PAID_PREFIX) }
                    .map { it.removePrefix(PAID_PREFIX) }
                    .toSet()
            ).also { state = it }
        }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
