package com.quickcards.app.feature.settings

import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import android.view.autofill.AutofillManager
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.quickcards.app.BuildConfig
import com.quickcards.app.R
import com.quickcards.app.core.data.settings.AppSettings
import com.quickcards.app.core.notifications.CopyCompanionNotifier
import com.quickcards.app.core.notifications.rememberCardNotifications
import com.quickcards.app.core.security.AppLockController
import com.quickcards.app.core.ui.LocalAppLocked
import com.quickcards.app.core.ui.components.QcIconButton
import com.quickcards.app.core.ui.components.QcChevron
import com.quickcards.app.core.ui.components.QcGroup
import com.quickcards.app.core.ui.components.QcGroupLabel
import com.quickcards.app.core.ui.components.QcListRow
import com.quickcards.app.core.ui.components.QcScreenBar
import com.quickcards.app.core.ui.components.QcSwitchRow
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcTextButton
import com.quickcards.app.core.ui.theme.QcTheme
import com.quickcards.app.core.update.AppUpdates
import com.quickcards.domain.SecurityConfirmation
import com.quickcards.domain.TimeText
import kotlinx.coroutines.launch

/**
 * Settings: Theme alone at the top, then Cards, Copying, Security, Reminders, Backup and About,
 * and Erase all data on its own. Every single choice is a sheet of radio rows.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SettingsScreen(
    bottomInset: Dp = 0.dp,
    onExport: () -> Unit = {},
    onImport: () -> Unit = {},
    onErase: () -> Unit = {},
    onPrivacy: () -> Unit = {},
    onAppLock: () -> Unit = {},
    onHelpSpreadsheet: () -> Unit = {},
    onHelpCheck: () -> Unit = {},
    updateState: AppUpdates.State = AppUpdates.State.None,
    onUpdate: () -> Unit = {},
    onCompleteUpdate: () -> Unit = {}
) {
    val context = LocalContext.current
    val settings = remember(context) { AppSettings.get(context) }
    val notifications = rememberCardNotifications()
    val guard = rememberSettingsGuard()
    val choices = rememberSettingsChoiceHost()
    val scope = rememberCoroutineScope()
    val autofillRow = remember { BringIntoViewRequester() }
    var helpOpen by remember { mutableStateOf(false) }

    val theme by settings.theme.collectAsState()
    val maskNumbers by settings.maskNumbers.collectAsState()
    val showNotes by settings.showNotes.collectAsState()
    val tagsShow by settings.tagsShow.collectAsState()
    val nfcAddEnabled by settings.nfcAddEnabled.collectAsState()
    val expiredCards by settings.expiredCards.collectAsState()
    val expiresSoonMonths by settings.expiresSoonMonths.collectAsState()
    val clipClearSec by settings.clipClearSec.collectAsState()
    val notifEnabled by settings.notifEnabled.collectAsState()
    val notifSec by settings.notifSec.collectAsState()
    val appLockEnabled by settings.appLockEnabled.collectAsState()
    val appLockAfterSec by settings.appLockAfterSec.collectAsState()
    val expiryReminders by settings.expiryReminders.collectAsState()
    val paymentReminders by settings.paymentReminders.collectAsState()
    val hasNfc = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_NFC) }

    val locked = LocalAppLocked.current
    LaunchedEffect(locked) {
        if (locked) {
            choices.open = null
            helpOpen = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        QcScreenBar(title = stringResource(R.string.settings_title)) {
            QcIconButton(QcIcons.infoOutline, stringResource(R.string.settings_help), { helpOpen = true })
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = QcSpacing.s4)
                .padding(top = QcSpacing.s2)
        ) {
            QcGroup {
                QcListRow(
                    title = stringResource(R.string.settings_theme_title),
                    icon = QcIcons.contrast,
                    subtitle = themeLabel(context, theme),
                    onClick = { choices.open = themeChoice(context, settings, theme) },
                    trailing = { QcChevron() }
                )
            }

            QcGroupLabel(stringResource(R.string.settings_section_cards))
            QcGroup {
                QcSwitchRow(
                    title = stringResource(R.string.settings_mask_title),
                    icon = QcIcons.hideNumbers,
                    checked = maskNumbers,
                    onCheckedChange = settings::setMaskNumbers
                )
                QcSwitchRow(
                    title = stringResource(R.string.settings_notes_title),
                    icon = QcIcons.notes,
                    checked = showNotes,
                    onCheckedChange = settings::setShowNotes
                )
                QcSwitchRow(
                    title = stringResource(R.string.settings_tags_show_title),
                    icon = QcIcons.tag,
                    checked = tagsShow,
                    onCheckedChange = settings::setTagsShow
                )
                if (hasNfc) {
                    QcSwitchRow(
                        title = stringResource(R.string.settings_nfc_title),
                        icon = QcIcons.contactless,
                        checked = nfcAddEnabled,
                        onCheckedChange = settings::setNfcAddEnabled
                    )
                }
                QcListRow(
                    title = stringResource(R.string.settings_expired_title),
                    icon = QcIcons.eventBusy,
                    subtitle = expiredLabel(context, expiredCards),
                    onClick = { choices.open = expiredChoice(context, settings, expiredCards) },
                    trailing = { QcChevron() }
                )
                QcListRow(
                    title = stringResource(R.string.settings_expires_soon_title),
                    icon = QcIcons.hourglassBottom,
                    subtitle = soonLabel(context, expiresSoonMonths),
                    onClick = { choices.open = soonChoice(context, settings, expiresSoonMonths) },
                    trailing = { QcChevron() }
                )
            }

            QcGroupLabel(stringResource(R.string.settings_section_copying))
            QcGroup {
                QcListRow(
                    title = stringResource(R.string.settings_clipboard_title),
                    icon = QcIcons.clipboard,
                    subtitle = clipboardLabel(context, clipClearSec),
                    onClick = { choices.open = clipboardChoice(context, settings, clipClearSec) },
                    trailing = { QcChevron() }
                )
                QcSwitchRow(
                    title = stringResource(R.string.settings_notification_title),
                    icon = QcIcons.bell,
                    subtitle = stringResource(R.string.settings_notification_subtitle),
                    checked = notifEnabled,
                    onCheckedChange = { on ->
                        guard(securityReason(context, SecurityConfirmation.reasonForNotificationCvv(on))) {
                            settings.setNotifEnabled(on)
                            if (on) notifications.settingTurnedOn() else CopyCompanionNotifier.cancel(context)
                        }
                    }
                )
                if (notifEnabled) {
                    QcListRow(
                        title = stringResource(R.string.settings_notification_time_title),
                        icon = QcIcons.timer,
                        subtitle = TimeText.seconds(notifSec),
                        onClick = { choices.open = notificationTimeChoice(context, settings, notifSec) },
                        trailing = { QcChevron() }
                    )
                }
                AutofillRow(settings, Modifier.bringIntoViewRequester(autofillRow))
            }

            QcGroupLabel(stringResource(R.string.settings_section_security))
            QcGroup {
                QcListRow(
                    title = stringResource(R.string.settings_app_lock_title),
                    icon = QcIcons.lock,
                    subtitle = if (appLockEnabled) {
                        autoLockLabel(context, appLockAfterSec)
                    } else {
                        stringResource(R.string.settings_app_lock_off)
                    },
                    onClick = onAppLock,
                    trailing = { QcChevron() }
                )
                LockNowRow(appLockEnabled = appLockEnabled)
            }

            QcGroupLabel(stringResource(R.string.settings_section_reminders))
            QcGroup {
                QcSwitchRow(
                    title = stringResource(R.string.settings_expiry_reminders_title),
                    icon = QcIcons.eventUpcoming,
                    subtitle = stringResource(R.string.settings_expiry_reminders_subtitle),
                    checked = expiryReminders,
                    onCheckedChange = { on ->
                        settings.setExpiryReminders(on)
                        if (on) notifications.settingTurnedOn()
                    }
                )
                QcSwitchRow(
                    title = stringResource(R.string.settings_payment_reminders_title),
                    icon = QcIcons.payments,
                    subtitle = stringResource(R.string.settings_payment_reminders_subtitle),
                    checked = paymentReminders,
                    onCheckedChange = { on ->
                        settings.setPaymentReminders(on)
                        if (on) notifications.settingTurnedOn()
                    }
                )
            }

            QcGroupLabel(stringResource(R.string.settings_section_backup))
            QcGroup {
                QcListRow(
                    title = stringResource(R.string.settings_backup_export_title),
                    icon = QcIcons.upload,
                    onClick = onExport
                )
                QcListRow(
                    title = stringResource(R.string.settings_backup_import_title),
                    icon = QcIcons.download,
                    subtitle = stringResource(R.string.settings_backup_import_subtitle),
                    onClick = onImport
                )
            }

            QcGroupLabel(stringResource(R.string.settings_section_about))
            QcGroup {
                VersionRow(updateState, onUpdate, onCompleteUpdate)
                QcListRow(
                    title = stringResource(R.string.settings_privacy_title),
                    icon = QcIcons.shield,
                    onClick = onPrivacy,
                    trailing = { QcChevron() }
                )
            }

            Spacer(Modifier.height(QcSpacing.s6))
            QcGroup {
                QcListRow(
                    title = stringResource(R.string.settings_backup_erase_title),
                    icon = QcIcons.delete,
                    danger = true,
                    onClick = onErase
                )
            }
            Spacer(Modifier.height(QcSpacing.s6 + bottomInset))
        }
    }

    choices.Sheet()
    if (helpOpen) {
        SettingsHelpSheet(
            onDismiss = { helpOpen = false },
            onOpen = { topic ->
                when (topic) {
                    SettingsHelpTopic.INSTANT_COPY -> onAppLock()
                    SettingsHelpTopic.AUTOFILL -> scope.launch { autofillRow.bringIntoView() }
                    SettingsHelpTopic.SPREADSHEET -> onHelpSpreadsheet()
                    SettingsHelpTopic.CHECK -> onHelpCheck()
                }
            }
        )
    }
}

/**
 * Until Android has chosen QuickCards, "Not set up" and one action that opens Android's
 * autofill settings; afterwards a switch that turns the feature off entirely.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AutofillRow(settings: AppSettings, modifier: Modifier) {
    val context = LocalContext.current
    val enabled by settings.autofillEnabled.collectAsState()
    val manager = remember { context.getSystemService(AutofillManager::class.java) }
    var chosen by remember { mutableStateOf(manager?.hasEnabledAutofillServices() == true) }
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) chosen = manager?.hasEnabledAutofillServices() == true
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    val title = stringResource(R.string.settings_autofill_title)
    if (chosen) {
        QcSwitchRow(
            title = title,
            icon = QcIcons.password,
            checked = enabled,
            onCheckedChange = settings::setAutofillEnabled,
            modifier = modifier
        )
        return
    }
    val finishLabel = stringResource(R.string.settings_a11y_autofill_finish)
    QcListRow(
        title = title,
        icon = QcIcons.password,
        subtitle = stringResource(R.string.settings_autofill_subtitle_choose),
        subtitleColor = QcTheme.colors.warn,
        modifier = modifier,
        trailing = {
            QcTextButton(
                text = stringResource(R.string.settings_autofill_finish),
                onClick = {
                    settings.setAutofillEnabled(true)
                    AppLockController.beginSystemPicker()
                    runCatching {
                        context.startActivity(
                            Intent(
                                Settings.ACTION_REQUEST_SET_AUTOFILL_SERVICE,
                                Uri.parse("package:" + context.packageName)
                            )
                        )
                    }
                },
                modifier = Modifier.semantics { contentDescription = finishLabel }
            )
        }
    )
}

/** The version, plus the Google Play update while one is waiting. */
@Composable
private fun VersionRow(state: AppUpdates.State, onUpdate: () -> Unit, onCompleteUpdate: () -> Unit) {
    val version = BuildConfig.VERSION_NAME
    val accent = QcTheme.colors.accent
    val (subtitle, action) = when (state) {
        AppUpdates.State.Available -> stringResource(R.string.settings_update_available, version) to onUpdate
        AppUpdates.State.Downloading -> stringResource(R.string.settings_update_downloading, version) to null
        AppUpdates.State.Downloaded -> stringResource(R.string.settings_update_ready, version) to onCompleteUpdate
        AppUpdates.State.None -> stringResource(R.string.settings_version_value, version) to null
    }
    QcListRow(
        title = stringResource(R.string.settings_version_title),
        icon = if (state == AppUpdates.State.None) QcIcons.info else QcIcons.update,
        subtitle = subtitle,
        subtitleColor = if (action != null) accent else null,
        onClick = action
    )
}
