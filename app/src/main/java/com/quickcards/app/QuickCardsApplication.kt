package com.quickcards.app

import android.app.Application
import android.os.StrictMode
import com.quickcards.app.core.data.database.QuickCardsDatabase
import com.quickcards.app.core.data.repository.DataMaintenance
import com.quickcards.app.core.data.repository.RepairSummaryPrefs
import com.quickcards.app.core.data.repository.RepairSummaryStore
import com.quickcards.app.core.data.repository.VaultRepository
import com.quickcards.app.core.data.settings.AppSettings
import com.quickcards.app.core.security.EncryptionHelper
import com.quickcards.app.core.security.KeySentinel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class QuickCardsApplication : Application() {

    val applicationScope: CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val database: QuickCardsDatabase by lazy {
        QuickCardsDatabase.getDatabase(this, applicationScope)
    }

    val repairSummaryStore: RepairSummaryStore by lazy { RepairSummaryPrefs(this) }

    val repository: VaultRepository by lazy {
        VaultRepository(database, repairSummaries = repairSummaryStore)
    }

    val settings: AppSettings by lazy { AppSettings.get(this) }

    val keySentinel: KeySentinel by lazy {
        KeySentinel(
            getSharedPreferences(KeySentinel.PREFS_NAME, MODE_PRIVATE),
            database.cardDao()
        )
    }

    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            StrictMode.setThreadPolicy(
                StrictMode.ThreadPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectAll()
                    .penaltyLog()
                    .build()
            )
        }
        applicationScope.launch {
            // Keep Keystore key generation, Room open/migration, maintenance
            // and the settings singleton off the main thread so the lock
            // screen can draw on the first frame.
            runCatching { EncryptionHelper.getInstance() }
            runCatching { settings }
            runCatching { DataMaintenance.run(database, repairSummaryStore) }
        }
    }
}
