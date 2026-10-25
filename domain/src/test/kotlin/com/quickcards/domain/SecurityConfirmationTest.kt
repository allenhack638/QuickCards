package com.quickcards.domain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SecurityConfirmationTest {

    @Test
    fun turningAppLockOffNeedsConfirmation() {
        assertEquals(SecurityConfirmation.APP_LOCK_OFF, SecurityConfirmation.reasonForAppLock(false))
        assertNull(SecurityConfirmation.reasonForAppLock(true))
    }

    @Test
    fun neverInactivityNeedsConfirmationButATimeDoesNot() {
        assertEquals(SecurityConfirmation.INACTIVITY_NEVER, SecurityConfirmation.reasonForInactivity(0))
        assertNull(SecurityConfirmation.reasonForInactivity(10))
        assertNull(SecurityConfirmation.reasonForInactivity(60))
        assertNull(SecurityConfirmation.reasonForInactivity(1800))
    }

    @Test
    fun turningLockOnLeaveOffNeedsConfirmation() {
        assertEquals(
            SecurityConfirmation.LOCK_ON_LEAVE_OFF,
            SecurityConfirmation.reasonForLockOnLeave(false)
        )
        assertNull(SecurityConfirmation.reasonForLockOnLeave(true))
    }

    @Test
    fun showingTheCvvInNotificationsNeedsConfirmation() {
        assertEquals(
            SecurityConfirmation.NOTIF_CVV_ON,
            SecurityConfirmation.reasonForNotificationCvv(true)
        )
        assertNull(SecurityConfirmation.reasonForNotificationCvv(false))
    }
}
