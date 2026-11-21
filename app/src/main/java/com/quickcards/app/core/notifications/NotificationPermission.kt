package com.quickcards.app.core.notifications

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationManagerCompat

enum class PermissionStep { POST, REQUEST, OPEN_SETTINGS, NONE }

/**
 * Android 13+ asks for POST_NOTIFICATIONS once, on the first card number copy; never at install
 * or launch. After that ask, a denied permission is only reachable from system settings.
 */
object NotificationPermission {

    fun canPost(context: Context): Boolean =
        NotificationManagerCompat.from(context).areNotificationsEnabled()

    fun afterNumberCopy(
        canPost: Boolean,
        copyNotificationOn: Boolean,
        remindersOn: Boolean,
        asked: Boolean,
        sdk: Int = Build.VERSION.SDK_INT
    ): PermissionStep = when {
        canPost -> if (copyNotificationOn) PermissionStep.POST else PermissionStep.NONE
        sdk >= Build.VERSION_CODES.TIRAMISU && !asked && (copyNotificationOn || remindersOn) ->
            PermissionStep.REQUEST
        else -> PermissionStep.NONE
    }

    /** A Settings switch that needs notifications was turned on. */
    fun afterSettingOn(
        canPost: Boolean,
        asked: Boolean,
        sdk: Int = Build.VERSION.SDK_INT
    ): PermissionStep = when {
        canPost -> PermissionStep.NONE
        sdk >= Build.VERSION_CODES.TIRAMISU && !asked -> PermissionStep.REQUEST
        else -> PermissionStep.OPEN_SETTINGS
    }

    fun settingsIntent(context: Context): Intent =
        Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
            .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
}
