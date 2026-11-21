package com.quickcards.app.feature.settings

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.fragment.app.FragmentActivity
import com.quickcards.app.R
import com.quickcards.app.core.data.settings.AppSettings
import com.quickcards.app.core.data.settings.AppSettingsDefaults
import com.quickcards.app.core.data.settings.AppTheme
import com.quickcards.app.core.security.UnlockSession
import com.quickcards.app.core.ui.components.QcChoice
import com.quickcards.app.core.ui.components.QcChoiceSheet
import com.quickcards.domain.ExpiredPlacement
import com.quickcards.domain.SecurityConfirmation
import com.quickcards.domain.TimeText
import kotlinx.coroutines.launch

/** One open single-choice sheet. */
internal class SettingsChoice(
    val title: String,
    val options: List<QcChoice<Int>>,
    val selected: Int,
    val onPick: (Int) -> Unit,
    val subtitle: String? = null
)

/** Holds the one open choice sheet of a Settings page. */
internal class SettingsChoiceHost {
    var open by mutableStateOf<SettingsChoice?>(null)

    @Composable
    fun Sheet() {
        val choice = open ?: return
        QcChoiceSheet(
            title = choice.title,
            subtitle = choice.subtitle,
            options = choice.options,
            selected = choice.selected,
            onPick = choice.onPick,
            onDismiss = { open = null }
        )
    }
}

@Composable
internal fun rememberSettingsChoiceHost(): SettingsChoiceHost = remember { SettingsChoiceHost() }

/**
 * Runs [block] at once, or after a fresh unlock when [reason] names a change that lowers
 * protection or exposes a secret. A cancelled prompt changes nothing.
 */
@Composable
internal fun rememberSettingsGuard(): (reason: String?, block: () -> Unit) -> Unit {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    return remember(context) {
        { reason, block ->
            if (reason == null) {
                block()
            } else {
                scope.launch {
                    val host = context as? FragmentActivity ?: return@launch
                    if (UnlockSession.requireUnlock(host, reason, fresh = true)) block()
                }
            }
        }
    }
}

internal fun securityReason(context: Context, reason: String?): String? = when (reason) {
    null -> null
    SecurityConfirmation.APP_LOCK_OFF -> context.getString(R.string.settings_reason_app_lock_off)
    SecurityConfirmation.INACTIVITY_NEVER -> context.getString(R.string.settings_reason_inactivity_never)
    SecurityConfirmation.LOCK_ON_LEAVE_OFF -> context.getString(R.string.settings_reason_lock_on_leave_off)
    SecurityConfirmation.NOTIF_CVV_ON -> context.getString(R.string.settings_reason_notif_cvv_on)
    else -> reason
}

internal fun themeLabel(context: Context, theme: AppTheme): String = context.getString(
    when (theme) {
        AppTheme.SYSTEM -> R.string.settings_theme_system
        AppTheme.LIGHT -> R.string.settings_theme_light
        AppTheme.DARK -> R.string.settings_theme_dark
    }
)

internal fun expiredLabel(context: Context, placement: ExpiredPlacement): String = context.getString(
    when (placement) {
        ExpiredPlacement.KEEP -> R.string.settings_expired_keep
        ExpiredPlacement.END -> R.string.settings_expired_end
        ExpiredPlacement.HIDE -> R.string.settings_expired_hide
    }
)

internal fun soonLabel(context: Context, months: Int): String =
    context.resources.getQuantityString(R.plurals.settings_expires_soon_value, months, months)

internal fun clipboardLabel(context: Context, seconds: Int): String =
    if (seconds == 0) {
        context.getString(R.string.settings_option_never)
    } else {
        context.getString(R.string.settings_clipboard_after, TimeText.seconds(seconds))
    }

internal fun autoLockLabel(context: Context, seconds: Int): String =
    if (seconds == 0) {
        context.getString(R.string.settings_option_never)
    } else {
        context.getString(R.string.settings_inactivity_subtitle, TimeText.seconds(seconds))
    }

internal fun themeChoice(context: Context, settings: AppSettings, theme: AppTheme) = SettingsChoice(
    title = context.getString(R.string.settings_theme_title),
    options = AppTheme.entries.map { QcChoice(it.ordinal, themeLabel(context, it)) },
    selected = theme.ordinal,
    onPick = { settings.setTheme(AppTheme.entries[it]) }
)

internal fun expiredChoice(context: Context, settings: AppSettings, current: ExpiredPlacement) = SettingsChoice(
    title = context.getString(R.string.settings_expired_title),
    options = AppSettingsDefaults.EXPIRED_CARDS_OPTIONS.map { QcChoice(it.ordinal, expiredLabel(context, it)) },
    selected = current.ordinal,
    onPick = { settings.setExpiredCards(ExpiredPlacement.entries[it]) }
)

internal fun soonChoice(context: Context, settings: AppSettings, months: Int) = SettingsChoice(
    title = context.getString(R.string.settings_expires_soon_title),
    options = AppSettingsDefaults.EXPIRES_SOON_MONTHS_OPTIONS.map { QcChoice(it, soonLabel(context, it)) },
    selected = months,
    onPick = settings::setExpiresSoonMonths
)

internal fun clipboardChoice(context: Context, settings: AppSettings, seconds: Int) = SettingsChoice(
    title = context.getString(R.string.settings_clipboard_title),
    options = AppSettingsDefaults.CLIP_CLEAR_OPTIONS.map {
        QcChoice(it, if (it == 0) context.getString(R.string.settings_option_never) else TimeText.seconds(it))
    },
    selected = seconds,
    onPick = settings::setClipClearSec
)

internal fun notificationTimeChoice(context: Context, settings: AppSettings, seconds: Int) = SettingsChoice(
    title = context.getString(R.string.settings_notification_time_title),
    options = AppSettingsDefaults.NOTIF_SEC_OPTIONS.map { QcChoice(it, TimeText.seconds(it)) },
    selected = seconds,
    onPick = settings::setNotifSec
)

internal fun autoLockChoice(
    context: Context,
    settings: AppSettings,
    seconds: Int,
    guard: (String?, () -> Unit) -> Unit
) = SettingsChoice(
    title = context.getString(R.string.settings_inactivity_title),
    options = AppSettingsDefaults.APP_LOCK_AFTER_OPTIONS.map { QcChoice(it, autoLockLabel(context, it)) },
    selected = seconds,
    onPick = { value ->
        guard(securityReason(context, SecurityConfirmation.reasonForInactivity(value))) {
            settings.setAppLockAfterSec(value)
        }
    }
)

internal fun instantCopyChoice(context: Context, settings: AppSettings, seconds: Int) = SettingsChoice(
    title = context.getString(R.string.settings_copy_window_title),
    subtitle = context.getString(R.string.settings_copy_window_body),
    options = AppSettingsDefaults.COPY_WINDOW_OPTIONS.map { QcChoice(it, TimeText.seconds(it)) },
    selected = seconds,
    onPick = settings::setCopyWindowSec
)
