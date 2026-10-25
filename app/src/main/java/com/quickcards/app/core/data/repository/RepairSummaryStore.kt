package com.quickcards.app.core.data.repository

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

/** One-time repair summary. [isCompleted] keeps the upgrade job one-shot, independent of [dismiss]. */
data class RepairSummary(
    val fixedCards: Int,
    val needsCheck: Int
)

interface RepairSummaryStore {

    val summary: Flow<RepairSummary?>

    fun isCompleted(): Boolean

    fun markCompleted()

    fun record(summary: RepairSummary)

    fun dismiss()
}

/** Recording merges into the current banner so upgrade and import share one message. */
class RepairSummaryPrefs(context: Context) : RepairSummaryStore {

    private val preferences = context.applicationContext
        .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _summary = MutableStateFlow(readSummary())

    override val summary: Flow<RepairSummary?> = _summary.asStateFlow()

    override fun isCompleted(): Boolean = preferences.getBoolean(KEY_COMPLETED, false)

    override fun markCompleted() {
        preferences.edit().putBoolean(KEY_COMPLETED, true).apply()
    }

    override fun record(summary: RepairSummary) {
        val current = _summary.value
        val merged = if (current == null) {
            summary
        } else {
            RepairSummary(
                fixedCards = current.fixedCards + summary.fixedCards,
                needsCheck = current.needsCheck + summary.needsCheck
            )
        }
        preferences.edit()
            .putInt(KEY_FIXED_CARDS, merged.fixedCards)
            .putInt(KEY_NEEDS_CHECK, merged.needsCheck)
            .apply()
        _summary.value = merged
    }

    override fun dismiss() {
        preferences.edit()
            .remove(KEY_FIXED_CARDS)
            .remove(KEY_NEEDS_CHECK)
            .apply()
        _summary.value = null
    }

    private fun readSummary(): RepairSummary? {
        val fixed = preferences.getInt(KEY_FIXED_CARDS, 0)
        val needs = preferences.getInt(KEY_NEEDS_CHECK, 0)
        return if (fixed == 0 && needs == 0) null else RepairSummary(fixed, needs)
    }

    private companion object {
        const val PREFS_NAME = "quickcards_repairs"
        const val KEY_COMPLETED = "repair_completed"
        const val KEY_FIXED_CARDS = "repair_summary_fixed"
        const val KEY_NEEDS_CHECK = "repair_summary_needs_check"
    }
}

/** JVM-friendly store for repository defaults and tests. */
class InMemoryRepairSummaryStore : RepairSummaryStore {

    private val _summary = MutableStateFlow<RepairSummary?>(null)
    private var completed = false

    override val summary: Flow<RepairSummary?> = _summary.asStateFlow()

    override fun isCompleted(): Boolean = completed

    override fun markCompleted() {
        completed = true
    }

    override fun record(summary: RepairSummary) {
        val current = _summary.value
        _summary.value = if (current == null) {
            summary
        } else {
            RepairSummary(
                fixedCards = current.fixedCards + summary.fixedCards,
                needsCheck = current.needsCheck + summary.needsCheck
            )
        }
    }

    override fun dismiss() {
        _summary.value = null
    }
}
