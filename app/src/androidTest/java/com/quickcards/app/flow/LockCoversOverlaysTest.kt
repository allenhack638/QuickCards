package com.quickcards.app.flow

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quickcards.app.QuickCardsApplication
import com.quickcards.app.core.data.backup.BackupImporter
import com.quickcards.app.core.data.model.SeedData
import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.app.core.ui.LocalAppLocked
import com.quickcards.app.core.ui.components.LocalSnackbar
import com.quickcards.app.core.ui.components.SnackbarController
import com.quickcards.app.core.ui.theme.QuickCardsTheme
import com.quickcards.app.feature.cards.HomeScreen
import com.quickcards.app.feature.cards.HomeViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Locking the app closes an open actions sheet and an open
 * dialog. This test drives [LocalAppLocked] directly so the composition-local
 * guard is covered deterministically; LockFlowTest covers the real
 * AppLockController.lock() trigger and the lock screen on the same device.
 */
@RunWith(AndroidJUnit4::class)
class LockCoversOverlaysTest {

    @get:Rule
    val compose = createComposeRule()

    private lateinit var app: QuickCardsApplication
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var hdfcCard: VaultCard

    @Before
    fun setUp() {
        app = ApplicationProvider.getApplicationContext()
        app.settings.setAppLockEnabled(false)
        app.settings.setPinnedMax(1)
        runBlocking {
            withContext(Dispatchers.IO) {
                BackupImporter.eraseAll(app.database)
                app.database.cardTypeDao().insertCardTypes(SeedData.cardTypes)
            }
            val holder = app.repository.createHolder("Aarav Mehta")
            val hdfc = app.repository.createBank("HDFC Bank")
            val axis = app.repository.createBank("Axis Bank")
            hdfcCard = app.repository.insertCard(
                VaultCard(
                    number = "4532778103492219",
                    expiry = "12/29",
                    cvv = "381",
                    holderId = holder.id,
                    bankId = hdfc.id,
                    networkAuto = true,
                    nameOnCard = "AARAV MEHTA",
                    bandColor = "navy",
                    sortOrder = 0
                )
            ).getOrThrow()
            app.repository.insertCard(
                VaultCard(
                    number = "5555555555554444",
                    expiry = "09/28",
                    cvv = "482",
                    holderId = holder.id,
                    bankId = axis.id,
                    networkAuto = true,
                    nameOnCard = "AARAV MEHTA",
                    bandColor = "plum",
                    sortOrder = 1
                )
            ).getOrThrow()
            app.repository.pin(hdfcCard.id)
        }
        homeViewModel = HomeViewModel(app)
    }

    @Test
    fun lockHidesAnOpenSheet() {
        var locked by mutableStateOf(false)

        compose.setContent {
            val nav = rememberNavController()
            val host = remember { SnackbarHostState() }
            val scope = rememberCoroutineScope()
            val snackbar = remember(host, scope) { SnackbarController(host, scope) }
            CompositionLocalProvider(
                LocalAppLocked provides locked,
                LocalSnackbar provides snackbar
            ) {
                QuickCardsTheme(darkTheme = true) {
                    HomeScreen(
                        navController = nav,
                        viewModel = homeViewModel,
                        bottomInset = 0.dp,
                        onImport = {}
                    )
                }
            }
        }

        compose.waitUntil(10_000) {
            compose.onAllNodesWithContentDescription("More for HDFC Bank")
                .fetchSemanticsNodes().isNotEmpty()
        }

        compose.onNodeWithContentDescription("More for HDFC Bank").performClick()
        waitForText("Unpin")
        waitForText("Delete card")

        compose.runOnUiThread { locked = true }
        compose.waitForIdle()
        compose.onAllNodesWithText("Unpin").assertCountEquals(0)
        compose.onAllNodesWithText("Delete card").assertCountEquals(0)

        // The dialog case needs the real activity (delete asks for authentication);
        // LockFlowTest covers it end to end.
    }

    private fun waitForText(text: String) {
        compose.waitUntil(10_000) {
            compose.onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
        }
    }
}
