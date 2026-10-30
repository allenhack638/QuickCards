package com.quickcards.app.core.update

import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContract
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityOptionsCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.play.core.appupdate.testing.FakeAppUpdateManager
import com.quickcards.app.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/** The whole Play update flow against Play's own fake, as a Play install would see it. */
@RunWith(AndroidJUnit4::class)
class AppUpdatesTest {

    private val context = ApplicationProvider.getApplicationContext<Context>()
    private val instrumentation = InstrumentationRegistry.getInstrumentation()
    private lateinit var launcher: ActivityResultLauncher<IntentSenderRequest>

    @Before
    fun setUp() {
        context.getSharedPreferences("app_updates", Context.MODE_PRIVATE).edit().clear().commit()
        val registry = object : ActivityResultRegistry() {
            override fun <I, O> onLaunch(
                requestCode: Int,
                contract: ActivityResultContract<I, O>,
                input: I,
                options: ActivityOptionsCompat?
            ) = Unit
        }
        launcher = registry.register("update", ActivityResultContracts.StartIntentSenderForResult()) { }
    }

    private fun fake(): FakeAppUpdateManager = FakeAppUpdateManager(context).also { AppUpdates.use(it) }

    private fun onMain(block: () -> Unit) {
        instrumentation.runOnMainSync(block)
        instrumentation.waitForIdleSync()
    }

    @Test
    fun aNewVersionShowsPlaysSheetThenOffersRestartOnceDownloaded() {
        val play = fake()
        onMain {
            play.setUpdateAvailable(BuildConfig.VERSION_CODE + 1)
            AppUpdates.check(context, launcher)
        }
        assertEquals(AppUpdates.State.Available, AppUpdates.state.value)
        assertTrue(play.isConfirmationDialogVisible)

        onMain {
            play.userAcceptsUpdate()
            play.downloadStarts()
        }
        assertEquals(AppUpdates.State.Downloading, AppUpdates.state.value)

        onMain { play.downloadCompletes() }
        assertEquals(AppUpdates.State.Downloaded, AppUpdates.state.value)

        onMain { AppUpdates.completeUpdate() }
        assertTrue(play.isInstallSplashScreenVisible)
    }

    @Test
    fun aDeclinedVersionIsNotOfferedAgainButStaysInSettings() {
        val first = fake()
        onMain {
            first.setUpdateAvailable(BuildConfig.VERSION_CODE + 1)
            AppUpdates.check(context, launcher)
        }
        assertTrue(first.isConfirmationDialogVisible)
        onMain { first.userRejectsUpdate() }

        // A later launch of the app with the same version waiting on Play.
        val second = fake()
        onMain {
            second.setUpdateAvailable(BuildConfig.VERSION_CODE + 1)
            AppUpdates.check(context, launcher)
        }
        assertFalse(second.isConfirmationDialogVisible)
        assertEquals(AppUpdates.State.Available, AppUpdates.state.value)

        onMain { AppUpdates.startFromSettings(launcher) }
        assertTrue(second.isConfirmationDialogVisible)
    }

    @Test
    fun noUpdateShowsNothing() {
        val play = fake()
        onMain {
            play.setUpdateNotAvailable()
            AppUpdates.check(context, launcher)
        }
        assertEquals(AppUpdates.State.None, AppUpdates.state.value)
        assertFalse(play.isConfirmationDialogVisible)
    }
}
