package com.quickcards.app.feature.settings

import android.content.Context
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.quickcards.app.BuildConfig
import com.quickcards.app.QuickCardsApplication
import com.quickcards.app.R
import com.quickcards.app.core.data.settings.AppSettings
import com.quickcards.app.core.data.settings.AppSettingsDefaults
import com.quickcards.app.core.data.settings.AppTheme
import com.quickcards.app.core.security.AppLockController
import com.quickcards.app.core.security.UnlockSession
import com.quickcards.app.core.ui.LocalAppLocked
import com.quickcards.app.core.ui.components.LocalSnackbar
import com.quickcards.app.core.ui.components.QcDialog
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcType
import com.quickcards.domain.ArchivedVisibility
import com.quickcards.domain.ExpiredPlacement
import com.quickcards.domain.SecurityConfirmation
import com.quickcards.domain.TimeText
import kotlinx.coroutines.launch

private data class RadioDialogConfig(
    val title: String,
    val body: String,
    val options: List<Pair<Int, String>>,
    val current: Int,
    val onConfirm: (Int) -> Unit
)

private fun localizedReason(context: Context, reason: String?): String? = when (reason) {
    null -> null
    SecurityConfirmation.APP_LOCK_OFF -> context.getString(R.string.settings_reason_app_lock_off)
    SecurityConfirmation.INACTIVITY_NEVER ->
        context.getString(R.string.settings_reason_inactivity_never)
    SecurityConfirmation.LOCK_ON_LEAVE_OFF ->
        context.getString(R.string.settings_reason_lock_on_leave_off)
    SecurityConfirmation.NOTIF_CVV_ON ->
        context.getString(R.string.settings_reason_notification_cvv)
    else -> reason
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    bottomInset: Dp = 0.dp,
    onExport: () -> Unit = {},
    onImport: () -> Unit = {},
    onErase: () -> Unit = {}
) {
    val context = LocalContext.current
    val settings = remember(context) { AppSettings.get(context) }
    val repository = remember(context) {
        (context.applicationContext as QuickCardsApplication).repository
    }
    val snackbar = LocalSnackbar.current
    val scope = rememberCoroutineScope()

    val theme by settings.theme.collectAsState()
    val dynamicColor by settings.dynamicColor.collectAsState()
    val clipClearSec by settings.clipClearSec.collectAsState()
    val maskNumbers by settings.maskNumbers.collectAsState()
    val showCardType by settings.showCardType.collectAsState()
    val pinnedMax by settings.pinnedMax.collectAsState()
    val expiredCards by settings.expiredCards.collectAsState()
    val expiresSoonMonths by settings.expiresSoonMonths.collectAsState()
    val archivedCards by settings.archivedCards.collectAsState()
    val showNotes by settings.showNotes.collectAsState()
    val notifEnabled by settings.notifEnabled.collectAsState()
    val notifSec by settings.notifSec.collectAsState()
    val notifShowExpiry by settings.notifShowExpiry.collectAsState()
    val notifShowCvv by settings.notifShowCvv.collectAsState()
    val tagsShow by settings.tagsShow.collectAsState()
    val tagsSearch by settings.tagsSearch.collectAsState()
    val tagsMax by settings.tagsMax.collectAsState()
    val tagsMaxLen by settings.tagsMaxLen.collectAsState()
    val appLockEnabled by settings.appLockEnabled.collectAsState()
    val appLockAfterSec by settings.appLockAfterSec.collectAsState()
    val lockOnLeave by settings.lockOnLeave.collectAsState()
    val copyWindowSec by settings.copyWindowSec.collectAsState()

    val locked by AppLockController.isLocked.collectAsState()

    var dialog by remember { mutableStateOf<RadioDialogConfig?>(null) }
    val appLocked = LocalAppLocked.current

    LaunchedEffect(locked) {
        if (locked) dialog = null
    }

    fun guarded(reason: String?, block: () -> Unit) {
        if (reason == null) {
            block()
            return
        }
        scope.launch {
            val host = context as? androidx.fragment.app.FragmentActivity ?: return@launch
            // Only protection-lowering changes are guarded; they always re-ask.
            if (UnlockSession.requireUnlock(host, reason, fresh = true)) block()
        }
    }

    fun openRadio(
        title: String,
        body: String,
        options: List<Pair<Int, String>>,
        current: Int,
        onConfirm: (Int) -> Unit
    ) {
        dialog = RadioDialogConfig(title, body, options, current, onConfirm)
    }

    val themeLabel = when (theme) {
        AppTheme.SYSTEM -> stringResource(R.string.settings_theme_system)
        AppTheme.LIGHT -> stringResource(R.string.settings_theme_light)
        AppTheme.DARK -> stringResource(R.string.settings_theme_dark)
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(QcDimens.AppBar)
                .padding(start = QcDimens.Gutter, end = QcDimens.Half),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.settings_title),
                style = QcType.ScreenTitle,
                color = MaterialTheme.colorScheme.onSurface
            )
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
        ) {
            SettingsSubhead(stringResource(R.string.settings_section_appearance))
            ValueRow(
                icon = QcIcons.contrast,
                title = stringResource(R.string.settings_theme_title),
                subtitle = themeLabel,
                onClick = {
                    openRadio(
                        title = context.getString(R.string.settings_theme_title),
                        body = context.getString(R.string.settings_theme_body),
                        options = listOf(
                            0 to context.getString(R.string.settings_theme_system),
                            1 to context.getString(R.string.settings_theme_light),
                            2 to context.getString(R.string.settings_theme_dark)
                        ),
                        current = theme.ordinal,
                        onConfirm = { value ->
                            settings.setTheme(AppTheme.entries[value])
                        }
                    )
                }
            )
            SwitchRow(
                icon = QcIcons.palette,
                title = stringResource(R.string.settings_dynamic_title),
                subtitle = stringResource(R.string.settings_dynamic_subtitle),
                checked = dynamicColor,
                onCheckedChange = settings::setDynamicColor
            )

            SettingsSubhead(stringResource(R.string.settings_section_cards))
            SwitchRow(
                icon = QcIcons.password,
                title = stringResource(R.string.settings_mask_title),
                subtitle = stringResource(R.string.settings_mask_subtitle),
                checked = maskNumbers,
                onCheckedChange = settings::setMaskNumbers
            )
            SwitchRow(
                icon = QcIcons.type,
                title = stringResource(R.string.settings_show_type_title),
                subtitle = stringResource(R.string.settings_show_type_subtitle),
                checked = showCardType,
                onCheckedChange = settings::setShowCardType
            )
            ValueRow(
                icon = QcIcons.pin,
                title = stringResource(R.string.settings_pinned_title),
                subtitle = pluralStringResource(
                    R.plurals.settings_pinned_subtitle,
                    pinnedMax,
                    pinnedMax
                ),
                onClick = {
                    openRadio(
                        title = context.getString(R.string.settings_pinned_title),
                        body = context.getString(R.string.settings_pinned_body),
                        options = AppSettingsDefaults.PINNED_MAX_OPTIONS.map { it to it.toString() },
                        current = pinnedMax,
                        onConfirm = { value ->
                            settings.setPinnedMax(value)
                            scope.launch {
                                val removed = repository.trimPinned(value)
                                if (removed > 0) {
                                    snackbar.show(
                                        context.resources.getQuantityString(
                                            R.plurals.settings_unpinned_cards,
                                            removed,
                                            removed
                                        )
                                    )
                                }
                            }
                        }
                    )
                }
            )

            SettingsSubhead(stringResource(R.string.settings_section_card_list))
            ValueRow(
                icon = QcIcons.eventBusy,
                title = stringResource(R.string.settings_expired_title),
                subtitle = when (expiredCards) {
                    ExpiredPlacement.KEEP -> stringResource(R.string.settings_expired_keep)
                    ExpiredPlacement.END -> stringResource(R.string.settings_expired_end)
                    ExpiredPlacement.HIDE -> stringResource(R.string.settings_expired_hide)
                },
                onClick = {
                    openRadio(
                        title = context.getString(R.string.settings_expired_title),
                        body = context.getString(R.string.settings_expired_body),
                        options = AppSettingsDefaults.EXPIRED_CARDS_OPTIONS.map { placement ->
                            placement.ordinal to context.getString(
                                when (placement) {
                                    ExpiredPlacement.KEEP -> R.string.settings_expired_keep
                                    ExpiredPlacement.END -> R.string.settings_expired_end
                                    ExpiredPlacement.HIDE -> R.string.settings_expired_hide
                                }
                            )
                        },
                        current = expiredCards.ordinal,
                        onConfirm = { value ->
                            settings.setExpiredCards(ExpiredPlacement.entries[value])
                        }
                    )
                }
            )
            ValueRow(
                icon = QcIcons.hourglassBottom,
                title = stringResource(R.string.settings_expires_soon_title),
                subtitle = pluralStringResource(
                    R.plurals.settings_expires_soon_value,
                    expiresSoonMonths,
                    expiresSoonMonths
                ),
                onClick = {
                    openRadio(
                        title = context.getString(R.string.settings_expires_soon_title),
                        body = context.getString(R.string.settings_expires_soon_body),
                        options = AppSettingsDefaults.EXPIRES_SOON_MONTHS_OPTIONS.map { months ->
                            months to context.resources.getQuantityString(
                                R.plurals.settings_expires_soon_value,
                                months,
                                months
                            )
                        },
                        current = expiresSoonMonths,
                        onConfirm = settings::setExpiresSoonMonths
                    )
                }
            )
            ValueRow(
                icon = QcIcons.inventory2,
                title = stringResource(R.string.settings_archived_title),
                subtitle = when (archivedCards) {
                    ArchivedVisibility.ONLY_ARCHIVE ->
                        stringResource(R.string.settings_archived_only)
                    ArchivedVisibility.IN_SEARCH ->
                        stringResource(R.string.settings_archived_search)
                },
                onClick = {
                    openRadio(
                        title = context.getString(R.string.settings_archived_title),
                        body = context.getString(R.string.settings_archived_body),
                        options = AppSettingsDefaults.ARCHIVED_CARDS_OPTIONS.map { visibility ->
                            visibility.ordinal to context.getString(
                                when (visibility) {
                                    ArchivedVisibility.ONLY_ARCHIVE ->
                                        R.string.settings_archived_only
                                    ArchivedVisibility.IN_SEARCH ->
                                        R.string.settings_archived_search
                                }
                            )
                        },
                        current = archivedCards.ordinal,
                        onConfirm = { value ->
                            settings.setArchivedCards(ArchivedVisibility.entries[value])
                        }
                    )
                }
            )
            SwitchRow(
                icon = QcIcons.notes,
                title = stringResource(R.string.settings_notes_title),
                subtitle = stringResource(R.string.settings_notes_subtitle),
                checked = showNotes,
                onCheckedChange = settings::setShowNotes
            )

            SettingsSubhead(stringResource(R.string.settings_section_copying))
            ValueRow(
                icon = QcIcons.clipboard,
                title = stringResource(R.string.settings_clipboard_title),
                subtitle = if (clipClearSec == 0) {
                    stringResource(R.string.settings_option_never)
                } else {
                    stringResource(
                        R.string.settings_clipboard_after,
                        TimeText.seconds(clipClearSec)
                    )
                },
                onClick = {
                    openRadio(
                        title = context.getString(R.string.settings_clipboard_title),
                        body = context.getString(R.string.settings_clipboard_body),
                        options = AppSettingsDefaults.CLIP_CLEAR_OPTIONS.map { value ->
                            value to if (value == 0) {
                                context.getString(R.string.settings_option_never)
                            } else {
                                TimeText.seconds(value)
                            }
                        },
                        current = clipClearSec,
                        onConfirm = settings::setClipClearSec
                    )
                }
            )
            SwitchRow(
                icon = QcIcons.bell,
                title = stringResource(R.string.settings_notification_title),
                subtitle = stringResource(R.string.settings_notification_subtitle),
                checked = notifEnabled,
                onCheckedChange = settings::setNotifEnabled
            )
            ValueRow(
                icon = QcIcons.timer,
                title = stringResource(R.string.settings_notification_time_title),
                subtitle = stringResource(
                    R.string.settings_notification_time_subtitle,
                    TimeText.seconds(notifSec)
                ),
                enabled = notifEnabled,
                onClick = {
                    openRadio(
                        title = context.getString(R.string.settings_notification_time_title),
                        body = context.getString(R.string.settings_notification_time_body),
                        options = AppSettingsDefaults.NOTIF_SEC_OPTIONS.map {
                            it to TimeText.seconds(it)
                        },
                        current = notifSec,
                        onConfirm = settings::setNotifSec
                    )
                }
            )
            SwitchRow(
                icon = QcIcons.visibility,
                title = stringResource(R.string.settings_notification_expiry_title),
                subtitle = stringResource(R.string.settings_notification_expiry_subtitle),
                checked = notifShowExpiry,
                enabled = notifEnabled,
                onCheckedChange = settings::setNotifShowExpiry
            )
            SwitchRow(
                icon = QcIcons.visibility,
                title = stringResource(R.string.settings_notification_cvv_title),
                subtitle = stringResource(R.string.settings_notification_cvv_subtitle),
                checked = notifShowCvv,
                enabled = notifEnabled,
                onCheckedChange = { enabled ->
                    guarded(
                        localizedReason(
                            context,
                            SecurityConfirmation.reasonForNotificationCvv(enabled)
                        )
                    ) {
                        settings.setNotifShowCvv(enabled)
                    }
                }
            )

            SettingsSubhead(stringResource(R.string.settings_section_tags))
            SwitchRow(
                icon = QcIcons.tag,
                title = stringResource(R.string.settings_tags_show_title),
                subtitle = stringResource(R.string.settings_tags_show_subtitle),
                checked = tagsShow,
                onCheckedChange = settings::setTagsShow
            )
            SwitchRow(
                icon = QcIcons.search,
                title = stringResource(R.string.settings_tags_search_title),
                subtitle = stringResource(R.string.settings_tags_search_subtitle),
                checked = tagsSearch,
                onCheckedChange = settings::setTagsSearch
            )
            ValueRow(
                icon = QcIcons.filter3,
                title = stringResource(R.string.settings_tags_max_title),
                subtitle = stringResource(R.string.settings_tags_max_subtitle, tagsMax),
                onClick = {
                    openRadio(
                        title = context.getString(R.string.settings_tags_max_title),
                        body = context.getString(R.string.settings_tags_max_body),
                        options = AppSettingsDefaults.TAGS_MAX_OPTIONS.map { it to it.toString() },
                        current = tagsMax,
                        onConfirm = settings::setTagsMax
                    )
                }
            )
            ValueRow(
                icon = QcIcons.ruler,
                title = stringResource(R.string.settings_tag_length_title),
                subtitle = pluralStringResource(
                    R.plurals.settings_tag_length_subtitle,
                    tagsMaxLen,
                    tagsMaxLen
                ),
                onClick = {
                    openRadio(
                        title = context.getString(R.string.settings_tag_length_title),
                        body = context.getString(R.string.settings_tag_length_body),
                        options = AppSettingsDefaults.TAGS_MAX_LEN_OPTIONS.map {
                            it to context.resources.getQuantityString(
                                R.plurals.settings_tag_length_option,
                                it,
                                it
                            )
                        },
                        current = tagsMaxLen,
                        onConfirm = settings::setTagsMaxLen
                    )
                }
            )

            SettingsSubhead(stringResource(R.string.settings_section_security))
            SwitchRow(
                icon = QcIcons.fingerprint,
                title = stringResource(R.string.settings_app_lock_title),
                subtitle = stringResource(R.string.settings_app_lock_subtitle),
                checked = appLockEnabled,
                onCheckedChange = { enabled ->
                    guarded(
                        localizedReason(
                            context,
                            SecurityConfirmation.reasonForAppLock(enabled)
                        )
                    ) {
                        settings.setAppLockEnabled(enabled)
                    }
                }
            )
            ValueRow(
                icon = QcIcons.timer,
                title = stringResource(R.string.settings_inactivity_title),
                subtitle = if (appLockAfterSec == 0) {
                    stringResource(R.string.settings_inactivity_never)
                } else {
                    stringResource(
                        R.string.settings_inactivity_subtitle,
                        TimeText.seconds(appLockAfterSec)
                    )
                },
                enabled = appLockEnabled,
                onClick = {
                    openRadio(
                        title = context.getString(R.string.settings_inactivity_title),
                        body = context.getString(R.string.settings_inactivity_body),
                        options = AppSettingsDefaults.APP_LOCK_AFTER_OPTIONS.map { value ->
                            value to if (value == 0) {
                                context.getString(R.string.settings_option_never)
                            } else {
                                TimeText.seconds(value)
                            }
                        },
                        current = appLockAfterSec,
                        onConfirm = { value ->
                            guarded(
                                localizedReason(
                                    context,
                                    SecurityConfirmation.reasonForInactivity(value)
                                )
                            ) {
                                settings.setAppLockAfterSec(value)
                            }
                        }
                    )
                }
            )
            SwitchRow(
                icon = QcIcons.lock,
                title = stringResource(R.string.settings_lock_on_leave_title),
                subtitle = stringResource(R.string.settings_lock_on_leave_subtitle),
                checked = lockOnLeave,
                enabled = appLockEnabled,
                onCheckedChange = { enabled ->
                    guarded(
                        localizedReason(
                            context,
                            SecurityConfirmation.reasonForLockOnLeave(enabled)
                        )
                    ) {
                        settings.setLockOnLeave(enabled)
                    }
                }
            )
            ValueRow(
                icon = QcIcons.copy,
                title = stringResource(R.string.settings_copy_window_title),
                subtitle = stringResource(
                    R.string.settings_copy_window_subtitle,
                    TimeText.seconds(copyWindowSec)
                ),
                onClick = {
                    openRadio(
                        title = context.getString(R.string.settings_copy_window_title),
                        body = context.getString(R.string.settings_copy_window_body),
                        options = AppSettingsDefaults.COPY_WINDOW_OPTIONS.map {
                            it to TimeText.seconds(it)
                        },
                        current = copyWindowSec,
                        onConfirm = settings::setCopyWindowSec
                    )
                }
            )
            ValueRow(
                icon = QcIcons.lock,
                title = stringResource(R.string.settings_lock_now_title),
                subtitle = stringResource(R.string.settings_lock_now_subtitle),
                onClick = {
                    if (appLockEnabled) {
                        AppLockController.lock()
                    } else {
                        UnlockSession.lockNow()
                        snackbar.show(context.getString(R.string.settings_snackbar_copying_locked))
                    }
                }
            )
            StaticRow(
                icon = QcIcons.shield,
                title = stringResource(R.string.settings_screenshots_title),
                subtitle = stringResource(R.string.settings_screenshots_subtitle)
            )

            SettingsSubhead(stringResource(R.string.settings_section_backup))
            ValueRow(
                icon = QcIcons.upload,
                title = stringResource(R.string.settings_backup_export_title),
                subtitle = stringResource(R.string.settings_backup_export_subtitle),
                onClick = onExport
            )
            ValueRow(
                icon = QcIcons.download,
                title = stringResource(R.string.settings_backup_import_title),
                subtitle = stringResource(R.string.settings_backup_import_subtitle),
                onClick = onImport
            )
            ValueRow(
                icon = QcIcons.delete,
                title = stringResource(R.string.settings_backup_erase_title),
                subtitle = stringResource(R.string.settings_backup_erase_subtitle),
                danger = true,
                onClick = onErase
            )

            SettingsSubhead(stringResource(R.string.settings_section_about))
            StaticRow(
                icon = QcIcons.info,
                title = stringResource(R.string.settings_version_title),
                subtitle = stringResource(R.string.settings_version_value, BuildConfig.VERSION_NAME)
            )
            StaticRow(
                icon = QcIcons.offline,
                title = stringResource(R.string.settings_offline_title),
                subtitle = stringResource(R.string.settings_offline_subtitle),
                modifier = Modifier.padding(bottom = QcDimens.Section + bottomInset)
            )
        }
    }

    val activeDialog = dialog
    if (!appLocked && activeDialog != null) {
        RadioDialog(
            config = activeDialog,
            onDismiss = { dialog = null }
        )
    }
}

@Composable
private fun SettingsSubhead(text: String) {
    Text(
        text = text,
        style = QcType.Body.copy(fontWeight = FontWeight(600)),
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(
            start = QcDimens.Gutter,
            end = QcDimens.Gutter,
            top = QcDimens.SubheadTop,
            bottom = QcDimens.SubheadBottom
        )
    )
}

@Composable
private fun SettingsRow(
    icon: ImageVector,
    title: String,
    subtitle: String?,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    danger: Boolean = false,
    trailing: (@Composable () -> Unit)? = null,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(
                min = if (subtitle == null) QcDimens.ListRowOneLine else QcDimens.ListRowTwoLine
            )
            .alpha(if (enabled) 1f else QcDimens.DisabledAlpha)
            .then(
                if (onClick != null) {
                    Modifier.clickable(enabled = enabled, onClick = onClick)
                } else {
                    Modifier
                }
            )
            .padding(horizontal = QcDimens.Gutter, vertical = QcDimens.SettingsRowVertical),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(QcDimens.LeadSlot),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (danger) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.size(QcDimens.Icon)
            )
        }
        Spacer(modifier = Modifier.width(QcDimens.Gutter))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = QcType.BodyLarge,
                color = if (danger) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = QcType.Body.copy(lineBreak = LineBreak.Paragraph),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        if (trailing != null) {
            Spacer(modifier = Modifier.width(QcDimens.Gutter))
            trailing()
        }
    }
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    SettingsRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        modifier = Modifier.toggleable(
            value = checked,
            enabled = enabled,
            role = Role.Switch,
            onValueChange = onCheckedChange
        ),
        enabled = enabled,
        trailing = {
            Switch(
                checked = checked,
                onCheckedChange = null,
                colors = SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onSecondary,
                    checkedTrackColor = MaterialTheme.colorScheme.secondary,
                    checkedBorderColor = MaterialTheme.colorScheme.secondary,
                    checkedIconColor = MaterialTheme.colorScheme.onSecondary,
                    uncheckedThumbColor = MaterialTheme.colorScheme.outline,
                    uncheckedTrackColor = MaterialTheme.colorScheme.surfaceContainerHighest,
                    uncheckedBorderColor = MaterialTheme.colorScheme.outline,
                    uncheckedIconColor = Color.Transparent
                )
            )
        }
    )
}

@Composable
private fun ValueRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean = true,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    SettingsRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        enabled = enabled,
        danger = danger,
        onClick = onClick
    )
}

@Composable
private fun StaticRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier
) {
    SettingsRow(
        icon = icon,
        title = title,
        subtitle = subtitle,
        modifier = modifier
    )
}

@Composable
private fun RadioDialog(
    config: RadioDialogConfig,
    onDismiss: () -> Unit
) {
    var selected by remember(config) { mutableIntStateOf(config.current) }

    QcDialog(onDismissRequest = onDismiss) {
        Text(
            text = config.title,
            style = QcType.DialogTitle,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(modifier = Modifier.height(QcDimens.DialogHeaderGap))
        if (config.body.isNotBlank()) {
            Text(
                text = config.body,
                style = QcType.Body,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(QcDimens.DialogHeaderGap))
        }
        config.options.forEach { (value, label) ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = QcDimens.Touch)
                    .clickable { selected = value },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(QcDimens.HelperLine),
                    contentAlignment = Alignment.Center
                ) {
                    RadioButton(
                        selected = selected == value,
                        onClick = null,
                        modifier = Modifier.size(QcDimens.HelperLine),
                        colors = RadioButtonDefaults.colors(
                            selectedColor = MaterialTheme.colorScheme.secondary,
                            unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
                Spacer(modifier = Modifier.width(QcDimens.Gutter))
                Text(
                    text = label,
                    style = QcType.BodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
        Spacer(modifier = Modifier.height(QcDimens.DialogActionGap))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(QcDimens.Base, Alignment.End),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(stringResource(R.string.settings_action_cancel))
            }
            TextButton(
                onClick = {
                    config.onConfirm(selected)
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(
                    contentColor = MaterialTheme.colorScheme.onSurface
                )
            ) {
                Text(stringResource(R.string.settings_action_ok))
            }
        }
    }
}
