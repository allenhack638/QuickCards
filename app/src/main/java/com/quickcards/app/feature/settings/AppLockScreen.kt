package com.quickcards.app.feature.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import com.quickcards.app.R
import com.quickcards.app.core.data.settings.AppSettings
import com.quickcards.app.core.security.AppLockController
import com.quickcards.app.core.security.UnlockSession
import com.quickcards.app.core.ui.LocalAppLocked
import com.quickcards.app.core.ui.components.LocalSnackbar
import com.quickcards.app.core.ui.components.QcChevron
import com.quickcards.app.core.ui.components.QcGroup
import com.quickcards.app.core.ui.components.QcListRow
import com.quickcards.app.core.ui.components.QcNestedTopBar
import com.quickcards.app.core.ui.components.QcSwitchRow
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.domain.SecurityConfirmation
import com.quickcards.domain.TimeText

/** Settings › App lock: the master switch, then Auto-lock, Lock when you leave, Instant copy; Lock now. */
@Composable
fun AppLockScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val settings = remember(context) { AppSettings.get(context) }
    val enabled by settings.appLockEnabled.collectAsState()
    val afterSec by settings.appLockAfterSec.collectAsState()
    val lockOnLeave by settings.lockOnLeave.collectAsState()
    val copyWindowSec by settings.copyWindowSec.collectAsState()
    val guard = rememberSettingsGuard()
    val choices = rememberSettingsChoiceHost()
    val locked = LocalAppLocked.current
    LaunchedEffect(locked) { if (locked) choices.open = null }

    Column(modifier = Modifier.fillMaxSize()) {
        QcNestedTopBar(
            onBack = onBack,
            backLabel = stringResource(R.string.settings_privacy_back),
            title = stringResource(R.string.settings_app_lock_title)
        )
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = QcSpacing.s4)
                .padding(top = QcSpacing.s2, bottom = QcSpacing.s6)
                .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(QcSpacing.s6)
        ) {
            QcGroup {
                QcSwitchRow(
                    title = stringResource(R.string.settings_app_lock_title),
                    icon = QcIcons.lock,
                    checked = enabled,
                    onCheckedChange = { on ->
                        guard(securityReason(context, SecurityConfirmation.reasonForAppLock(on))) {
                            settings.setAppLockEnabled(on)
                        }
                    }
                )
            }
            QcGroup {
                QcListRow(
                    title = stringResource(R.string.settings_inactivity_title),
                    icon = QcIcons.timer,
                    subtitle = autoLockLabel(context, afterSec),
                    enabled = enabled,
                    onClick = { choices.open = autoLockChoice(context, settings, afterSec, guard) },
                    trailing = { QcChevron() }
                )
                QcSwitchRow(
                    title = stringResource(R.string.settings_lock_on_leave_title),
                    icon = QcIcons.phonelinkLock,
                    checked = lockOnLeave,
                    enabled = enabled,
                    onCheckedChange = { on ->
                        guard(securityReason(context, SecurityConfirmation.reasonForLockOnLeave(on))) {
                            settings.setLockOnLeave(on)
                        }
                    }
                )
                QcListRow(
                    title = stringResource(R.string.settings_copy_window_title),
                    icon = QcIcons.touchApp,
                    subtitle = stringResource(R.string.settings_copy_window_subtitle, TimeText.seconds(copyWindowSec)),
                    onClick = { choices.open = instantCopyChoice(context, settings, copyWindowSec) },
                    trailing = { QcChevron() }
                )
            }
            QcGroup { LockNowRow(appLockEnabled = enabled) }
        }
    }
    choices.Sheet()
}

/** Locks the app, or with app lock off ends the instant-copy window. */
@Composable
internal fun LockNowRow(appLockEnabled: Boolean) {
    val context = LocalContext.current
    val snackbar = LocalSnackbar.current
    QcListRow(
        title = stringResource(R.string.settings_lock_now_title),
        icon = QcIcons.lockClock,
        onClick = {
            if (appLockEnabled) {
                AppLockController.lock()
            } else {
                UnlockSession.lockNow()
                snackbar.show(context.getString(R.string.settings_snackbar_copying_locked))
            }
        }
    )
}
