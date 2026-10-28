package com.quickcards.app.core.security

internal enum class ClipboardExpiryAction { KEEP_RECORD, CLEAR_AND_FORGET, FORGET_RECORD }

/** Pure decision for the clipboard deadline; [ClipboardHelper] owns the Android side effects. */
internal object ClipboardExpiry {

    /**
     * A null read means the app is not focused yet (Android 10+), so the record must survive for
     * the next focus. A readable value is compared through the keyed token and then either
     * cleared (ours) or forgotten (someone else's).
     */
    fun decide(
        current: String?,
        expectedToken: String?,
        tokenFor: (String) -> String?
    ): ClipboardExpiryAction {
        if (current == null) return ClipboardExpiryAction.KEEP_RECORD
        if (expectedToken == null) return ClipboardExpiryAction.FORGET_RECORD
        return if (tokenFor(current) == expectedToken) {
            ClipboardExpiryAction.CLEAR_AND_FORGET
        } else {
            ClipboardExpiryAction.FORGET_RECORD
        }
    }
}
