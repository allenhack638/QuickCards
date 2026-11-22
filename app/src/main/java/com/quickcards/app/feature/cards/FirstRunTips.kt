package com.quickcards.app.feature.cards

import android.content.Context

/** Whether the three first-run tips were shown; they appear once, before the first card. */
object FirstRunTips {

    private const val PREFS = "quickcards_tips"
    private const val KEY_SEEN = "seen"

    fun seen(context: Context): Boolean = prefs(context).getBoolean(KEY_SEEN, false)

    fun markSeen(context: Context) {
        prefs(context).edit().putBoolean(KEY_SEEN, true).apply()
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
