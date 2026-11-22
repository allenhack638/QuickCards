package com.quickcards.app.core.security

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.quickcards.app.MainActivity
import com.quickcards.app.QuickCardsApplication
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith

/**
 * A copy whose clear timer died with the process is cleared on the next launch, and the check
 * never crashes it. On Android 8 the first ClipboardManager of a process needs a Looper, so the
 * old IO-thread lookup crashed every launch (APP_AUDIT2 P0-1). Run alone to start from a process
 * whose app context has not created its ClipboardManager yet.
 */
@RunWith(AndroidJUnit4::class)
class ClipboardExpiryRelaunchTest {

    private val app = ApplicationProvider.getApplicationContext<QuickCardsApplication>()
    private val record = app.getSharedPreferences("quickcards_clipboard", Context.MODE_PRIVATE)

    @Test
    fun anOverdueCopyIsClearedOnLaunchWithoutACrash() {
        AppLockController.autoPromptOnLock = false
        app.settings.setAppLockEnabled(false)
        val value = "4532778103492211"
        // A separate context of the app, so the app context's clipboard is first created by the app.
        val testClipboard = InstrumentationRegistry.getInstrumentation().run {
            var manager: ClipboardManager? = null
            runOnMainSync {
                manager = app.createPackageContext(app.packageName, 0)
                    .getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            }
            manager!!
        }
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            testClipboard.setPrimaryClip(ClipData.newPlainText("card", value))
        }
        record.edit()
            .putString("copy_hash", PanHasher.hashText(value))
            .putLong("copy_expires_at", System.currentTimeMillis() - 60_000L)
            .commit()

        ActivityScenario.launch(MainActivity::class.java).use { scenario ->
            val deadline = System.currentTimeMillis() + 10_000
            while (record.getLong("copy_expires_at", 0L) != 0L && System.currentTimeMillis() < deadline) {
                Thread.sleep(100)
            }
            assertEquals("The overdue record is forgotten", 0L, record.getLong("copy_expires_at", 0L))
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
            var current: String? = null
            InstrumentationRegistry.getInstrumentation().runOnMainSync {
                current = testClipboard.primaryClip?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
            }
            assertNotEquals("The overdue value is off the clipboard", value, current)
        }
    }
}
