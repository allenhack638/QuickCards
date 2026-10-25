package com.quickcards.app.core.autofill

import android.content.Context

/**
 * When each card was last copied or autofilled, so autofill can suggest the cards in use.
 * Only card ids and times are stored; no card data.
 */
object CardUsage {

    private const val PREFS = "quickcards_usage"

    fun record(context: Context, cardId: String) {
        prefs(context).edit().putLong(cardId, System.currentTimeMillis()).apply()
    }

    fun lastUsed(context: Context): Map<String, Long> =
        prefs(context).all.mapNotNull { (id, value) -> (value as? Long)?.let { id to it } }.toMap()

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
