package com.quickcards.app.core.notifications

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import com.quickcards.app.R
import com.quickcards.app.core.data.settings.AppSettings
import com.quickcards.app.core.reminders.Reminders
import com.quickcards.app.core.ui.HomeCard
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** What the copy notification can copy for one card; held in memory only, never saved state. */
internal class CopiedCard(val bank: String, val last4: String, val expiry: String, val cvv: String)

/**
 * Screen-side entry point: call [numberCopied] after an unlocked card number copy and
 * [settingTurnedOn] when a switch that needs notifications is turned on. It owns the one-time
 * Android 13+ permission request.
 */
class CardNotifications internal constructor(
    private val context: Context,
    private val scope: CoroutineScope
) {
    private val settings get() = AppSettings.get(context)
    internal var launcher: ActivityResultLauncher<String>? = null
    private var pending: CopiedCard? = null

    fun numberCopied(card: HomeCard) {
        val copied = CopiedCard(
            bank = card.bankName.ifBlank { context.getString(R.string.app_name) },
            last4 = card.card.number.takeLast(4),
            expiry = card.card.expiry,
            cvv = card.card.cvv
        )
        val step = NotificationPermission.afterNumberCopy(
            canPost = NotificationPermission.canPost(context),
            copyNotificationOn = settings.notifEnabled.value,
            remindersOn = settings.expiryReminders.value || settings.paymentReminders.value,
            asked = settings.notifPermissionAsked
        )
        when (step) {
            PermissionStep.POST -> post(copied)
            PermissionStep.REQUEST -> request(copied)
            PermissionStep.OPEN_SETTINGS, PermissionStep.NONE -> Unit
        }
    }

    fun settingTurnedOn() {
        val step = NotificationPermission.afterSettingOn(
            canPost = NotificationPermission.canPost(context),
            asked = settings.notifPermissionAsked
        )
        when (step) {
            PermissionStep.REQUEST -> request(null)
            PermissionStep.OPEN_SETTINGS -> context.startActivity(NotificationPermission.settingsIntent(context))
            PermissionStep.POST, PermissionStep.NONE -> Unit
        }
    }

    private fun request(card: CopiedCard?) {
        val permission = launcher ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        settings.markNotifPermissionAsked()
        pending = card
        permission.launch(Manifest.permission.POST_NOTIFICATIONS)
    }

    internal fun onPermissionResult(granted: Boolean) {
        val card = pending
        pending = null
        if (!granted) return
        if (card != null && settings.notifEnabled.value) post(card)
        scope.launch(Dispatchers.Default) { runCatching { Reminders.check(context) } }
    }

    private fun post(card: CopiedCard) {
        CopyCompanionNotifier.notifyCardCopied(
            context = context,
            bank = card.bank,
            last4 = card.last4,
            expiry = card.expiry,
            cvv = card.cvv,
            seconds = settings.notifSec.value
        )
    }
}

@Composable
fun rememberCardNotifications(): CardNotifications {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val notification = remember(context, scope) { CardNotifications(context, scope) }
    notification.launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
        notification::onPermissionResult
    )
    return notification
}
