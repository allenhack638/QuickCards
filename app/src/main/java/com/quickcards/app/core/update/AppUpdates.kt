package com.quickcards.app.core.update

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import androidx.annotation.VisibleForTesting
import androidx.core.content.edit
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.quickcards.app.BuildConfig
import com.quickcards.domain.UpdatePolicy
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Google Play in-app updates (flexible flow). The Play Store app checks and downloads;
 * QuickCards makes no network call and adds no permission. On a copy not installed from
 * Play (the GitHub APK, debug builds) the check fails quietly and nothing is shown.
 */
object AppUpdates {

    sealed interface State {
        data object None : State
        data object Available : State
        data object Downloading : State
        data object Downloaded : State
    }

    private const val PREFS = "app_updates"
    private const val KEY_PROMPTED = "prompted_version"

    private val _state = MutableStateFlow<State>(State.None)
    val state: StateFlow<State> = _state.asStateFlow()

    private var manager: AppUpdateManager? = null
    private var latest: AppUpdateInfo? = null
    private var promptedThisProcess = false

    private val listener = InstallStateUpdatedListener { install ->
        _state.value = when (install.installStatus()) {
            InstallStatus.PENDING, InstallStatus.DOWNLOADING -> State.Downloading
            InstallStatus.DOWNLOADED -> State.Downloaded
            InstallStatus.FAILED, InstallStatus.CANCELED -> State.Available
            else -> _state.value
        }
    }

    fun attach(context: Context) {
        if (manager != null) return
        use(AppUpdateManagerFactory.create(context.applicationContext))
    }

    /** Tests swap in Play's `FakeAppUpdateManager`; this also forgets the per-process prompt. */
    @VisibleForTesting
    fun use(updates: AppUpdateManager) {
        manager?.unregisterListener(listener)
        manager = updates.also { it.registerListener(listener) }
        latest = null
        promptedThisProcess = false
        _state.value = State.None
    }

    /**
     * Runs on every resume while unlocked. Offers the Play prompt at most once per new
     * version; a declined update stays reachable from Settings › About.
     */
    fun check(context: Context, launcher: ActivityResultLauncher<IntentSenderRequest>) {
        val updates = manager ?: return
        updates.appUpdateInfo
            .addOnSuccessListener { info ->
                latest = info
                when {
                    info.installStatus() == InstallStatus.DOWNLOADED -> _state.value = State.Downloaded
                    info.installStatus() == InstallStatus.DOWNLOADING -> _state.value = State.Downloading
                    info.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE -> {
                        _state.value = State.Available
                        maybePrompt(context, info, launcher)
                    }
                    else -> _state.value = State.None
                }
            }
            .addOnFailureListener { _state.value = State.None }
    }

    private fun maybePrompt(
        context: Context,
        info: AppUpdateInfo,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        if (promptedThisProcess) return
        promptedThisProcess = true
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val prompt = UpdatePolicy.shouldPrompt(
            installedVersion = BuildConfig.VERSION_CODE,
            availableVersion = info.availableVersionCode(),
            lastPromptedVersion = prefs.getInt(KEY_PROMPTED, 0),
            flexibleAllowed = info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE)
        )
        if (!prompt) return
        prefs.edit { putInt(KEY_PROMPTED, info.availableVersionCode()) }
        start(info, launcher)
    }

    /** Settings › About: start the update the user asked for. */
    fun startFromSettings(launcher: ActivityResultLauncher<IntentSenderRequest>) {
        latest?.takeIf { it.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) }?.let { start(it, launcher) }
    }

    private fun start(info: AppUpdateInfo, launcher: ActivityResultLauncher<IntentSenderRequest>) {
        val updates = manager ?: return
        // No lock grace: if Play's sheet takes the app to the background, it locks as usual.
        runCatching {
            updates.startUpdateFlowForResult(
                info,
                launcher,
                AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
            )
        }
        // An AppUpdateInfo starts one flow only; the next check fetches a fresh one.
        latest = null
    }

    /** Installs the downloaded update; Play restarts the app. */
    fun completeUpdate() {
        manager?.completeUpdate()
    }
}
