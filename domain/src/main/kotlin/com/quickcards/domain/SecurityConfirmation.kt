package com.quickcards.domain

/**
 * Turning a protection off (or exposing a secret in notifications) needs the
 * user to prove it is them. A null reason means the change can be applied at
 * once. Cancelling the prompt applies nothing.
 */
object SecurityConfirmation {

    const val APP_LOCK_OFF = "Confirm it’s you to turn off app lock"
    const val INACTIVITY_NEVER = "Confirm it’s you to turn off the inactivity lock"
    const val LOCK_ON_LEAVE_OFF = "Confirm it’s you to stop locking when you leave"
    const val NOTIF_CVV_ON = "Confirm it’s you to show the CVV in notifications"

    fun reasonForAppLock(enabled: Boolean): String? =
        if (enabled) null else APP_LOCK_OFF

    fun reasonForInactivity(seconds: Int): String? =
        if (seconds == 0) INACTIVITY_NEVER else null

    fun reasonForLockOnLeave(enabled: Boolean): String? =
        if (enabled) null else LOCK_ON_LEAVE_OFF

    fun reasonForNotificationCvv(enabled: Boolean): String? =
        if (enabled) NOTIF_CVV_ON else null
}
