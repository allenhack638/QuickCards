package com.quickcards.app.core.reminders

import android.content.Intent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * A reminder tap asks to open one card's details. The intent carries only the card id; QcApp
 * opens it once the app is unlocked, so the lock screen always comes first.
 */
object OpenCardRequest {

    const val EXTRA_CARD_ID = "open_card_id"

    private val _cardId = MutableStateFlow<String?>(null)
    val cardId: StateFlow<String?> = _cardId.asStateFlow()

    /** Takes the request off [intent] so a recreated activity does not open the card again. */
    fun offer(intent: Intent?) {
        val id = intent?.getStringExtra(EXTRA_CARD_ID) ?: return
        intent.removeExtra(EXTRA_CARD_ID)
        if (intent.flags and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0) return
        _cardId.value = id
    }

    fun consume(id: String) {
        _cardId.compareAndSet(id, null)
    }
}
