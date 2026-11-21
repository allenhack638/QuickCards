package com.quickcards.app.core.notifications

import org.junit.Assert.assertEquals
import org.junit.Test

class NotificationPermissionTest {

    private val android13 = 33
    private val android12 = 32

    private fun afterCopy(
        canPost: Boolean = false,
        copyOn: Boolean = true,
        remindersOn: Boolean = true,
        asked: Boolean = false,
        sdk: Int = android13
    ) = NotificationPermission.afterNumberCopy(canPost, copyOn, remindersOn, asked, sdk)

    @Test
    fun theFirstNumberCopyOnAndroid13AsksOnce() {
        assertEquals(PermissionStep.REQUEST, afterCopy())
        assertEquals(PermissionStep.NONE, afterCopy(asked = true))
    }

    @Test
    fun aGrantedPermissionPostsOnlyWhileTheNotificationIsOn() {
        assertEquals(PermissionStep.POST, afterCopy(canPost = true))
        assertEquals(PermissionStep.NONE, afterCopy(canPost = true, copyOn = false))
    }

    @Test
    fun remindersAloneStillAskOnTheFirstCopy() {
        assertEquals(PermissionStep.REQUEST, afterCopy(copyOn = false, remindersOn = true))
        assertEquals(PermissionStep.NONE, afterCopy(copyOn = false, remindersOn = false))
    }

    @Test
    fun beforeAndroid13ThereIsNoRuntimePrompt() {
        assertEquals(PermissionStep.POST, afterCopy(canPost = true, sdk = android12))
        assertEquals(PermissionStep.NONE, afterCopy(canPost = false, sdk = android12))
    }

    @Test
    fun aSettingTurnedOnAsksOnceThenSendsToSystemSettings() {
        assertEquals(PermissionStep.NONE, NotificationPermission.afterSettingOn(true, false, android13))
        assertEquals(PermissionStep.REQUEST, NotificationPermission.afterSettingOn(false, false, android13))
        assertEquals(PermissionStep.OPEN_SETTINGS, NotificationPermission.afterSettingOn(false, true, android13))
        assertEquals(PermissionStep.OPEN_SETTINGS, NotificationPermission.afterSettingOn(false, false, android12))
    }
}
