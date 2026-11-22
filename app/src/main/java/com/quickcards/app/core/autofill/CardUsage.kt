package com.quickcards.app.core.autofill

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.update

/**
 * When each card was last copied or autofilled, so autofill can suggest the cards in use and
 * Home and Card details agree on "Last used". Only card ids and times are stored; no card data.
 */
object CardUsage {

    private const val PREFS = "quickcards_usage"

    /** Null until the first [observe] reads the file off the main thread. */
    private val copiedAt = MutableStateFlow<Map<String, Long>?>(null)

    fun record(context: Context, cardId: String) {
        val now = System.currentTimeMillis()
        prefs(context).edit().putLong(cardId, now).apply()
        copiedAt.update { it?.plus(cardId to now) }
    }

    /** Card id to last use time; the first collector loads it on the IO dispatcher. */
    fun observe(context: Context): Flow<Map<String, Long>> = flow {
        if (copiedAt.value == null) copiedAt.compareAndSet(null, lastUsed(context))
        emitAll(copiedAt.filterNotNull())
    }.flowOn(Dispatchers.IO)

    fun lastUsed(context: Context): Map<String, Long> =
        prefs(context).all.mapNotNull { (id, value) -> (value as? Long)?.let { id to it } }.toMap()

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
        copiedAt.value = emptyMap()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
