package com.quickcards.app.flow

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import com.quickcards.app.MainActivity
import com.quickcards.app.QuickCardsApplication
import com.quickcards.app.core.data.backup.BackupImporter
import com.quickcards.app.core.data.model.SeedData
import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.app.core.data.settings.AppSettingsDefaults
import com.quickcards.app.core.data.settings.AppTheme
import com.quickcards.app.core.security.AppLockController
import com.quickcards.app.core.security.ClipboardHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.junit.Before
import org.junit.Rule

/**
 * Each flow starts from an empty vault with default types restored (Erase removes them),
 * app lock off so no system prompt blocks it, and default card options.
 */
abstract class FlowTestBase {

    init {
        AppLockController.autoPromptOnLock = false
    }

    @get:Rule
    val compose = createAndroidComposeRule<MainActivity>()

    protected lateinit var app: QuickCardsApplication

    @Before
    fun setUpFlow() {
        app = ApplicationProvider.getApplicationContext()
        runBlocking {
            withContext(Dispatchers.IO) {
                BackupImporter.eraseAll(app.database)
                app.database.cardTypeDao().insertCardTypes(SeedData.cardTypes)
            }
        }
        app.settings.apply {
            setAppLockEnabled(false)
            setAppLockAfterSec(AppSettingsDefaults.APP_LOCK_AFTER_SEC)
            setLockOnLeave(AppSettingsDefaults.LOCK_ON_LEAVE)
            setCopyWindowSec(AppSettingsDefaults.COPY_WINDOW_SEC)
            setPaymentReminders(AppSettingsDefaults.PAYMENT_REMINDERS)
            setExpiryReminders(AppSettingsDefaults.EXPIRY_REMINDERS)
            setMaskNumbers(AppSettingsDefaults.MASK_NUMBERS)
            setTagsShow(AppSettingsDefaults.TAGS_SHOW)
            setExpiredCards(AppSettingsDefaults.EXPIRED_CARDS)
            setExpiresSoonMonths(AppSettingsDefaults.EXPIRES_SOON_MONTHS)
            setShowNotes(AppSettingsDefaults.SHOW_NOTES)
            setTheme(AppTheme.DARK)
        }
        app.repairSummaryStore.dismiss()
        AppLockController.unlock()
        // ClipboardHelper registers a ProcessLifecycleOwner observer in its
        // static initializer; the app touches it on the main thread first.
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            ClipboardHelper.clearNow()
        }
        awaitUnlocked()
    }

    protected fun awaitUnlocked(timeout: Long = 10_000) {
        compose.waitUntil(timeout) { !AppLockController.isLocked.value }
        compose.waitForIdle()
    }

    protected fun waitForText(
        text: String,
        substring: Boolean = false,
        timeout: Long = 10_000,
        unmerged: Boolean = false
    ) {
        compose.waitUntil(timeout) {
            compose.onAllNodes(
                if (substring) hasText(text, substring = true) else hasText(text),
                useUnmergedTree = unmerged
            ).fetchSemanticsNodes().isNotEmpty()
        }
    }

    protected fun waitForTextGone(
        text: String,
        timeout: Long = 10_000,
        unmerged: Boolean = false
    ) {
        compose.waitUntil(timeout) {
            compose.onAllNodes(
                hasText(text),
                useUnmergedTree = unmerged
            ).fetchSemanticsNodes().isEmpty()
        }
    }

    protected fun waitForContentDescription(
        description: String,
        substring: Boolean = false,
        timeout: Long = 10_000
    ) {
        compose.waitUntil(timeout) {
            compose.onAllNodes(
                androidx.compose.ui.test.hasContentDescription(
                    description,
                    substring = substring
                )
            ).fetchSemanticsNodes().isNotEmpty()
        }
    }

    /** A Home tile: its spoken name is "<bank>, ending <last4>…"; tap opens, hold opens actions. */
    protected fun tile(bank: String): SemanticsNodeInteraction =
        compose.onNode(hasContentDescription("$bank, ending", substring = true) and hasClickAction())

    protected fun awaitTile(bank: String, timeout: Long = 10_000) {
        compose.waitUntil(timeout) {
            compose.onAllNodes(hasContentDescription("$bank, ending", substring = true) and hasClickAction())
                .fetchSemanticsNodes().isNotEmpty()
        }
    }

    protected fun awaitTileGone(bank: String, timeout: Long = 10_000) {
        compose.waitUntil(timeout) {
            compose.onAllNodes(hasContentDescription("$bank, ending", substring = true) and hasClickAction())
                .fetchSemanticsNodes().isEmpty()
        }
    }

    /** Hold a tile: the card actions sheet. */
    protected fun openCardActions(bank: String) {
        awaitTile(bank)
        tile(bank).performScrollTo().performSemanticsAction(SemanticsActions.OnLongClick)
        waitForText("Delete")
    }

    /**
     * A text field by its accessible name: the Qc form field's label (its content description,
     * "<label>, edit box" in TalkBack) or a Material label slot (merged text).
     */
    protected fun field(label: String): SemanticsNodeInteraction = compose.onNode(fieldNamed(label))

    protected fun awaitField(label: String, timeout: Long = 10_000) {
        compose.waitUntil(timeout) {
            compose.onAllNodes(fieldNamed(label)).fetchSemanticsNodes().isNotEmpty()
        }
    }

    private fun fieldNamed(label: String): SemanticsMatcher =
        hasSetTextAction() and (hasContentDescription(label) or hasText(label))

    /** A picker field (Holder, Bank, Type): spoken as its label, the chosen value as its state. */
    protected fun awaitLookup(label: String, value: String, timeout: Long = 10_000) {
        compose.waitUntil(timeout) {
            compose.onAllNodes(
                hasContentDescription(label) and
                    SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, value)
            ).fetchSemanticsNodes().isNotEmpty()
        }
    }

    protected fun cardsNow(): List<VaultCard> = runBlocking { app.repository.cards.first() }

    protected fun awaitCards(
        expected: Int,
        timeout: Long = 10_000
    ): List<VaultCard> {
        compose.waitUntil(timeout) { cardsNow().size == expected }
        return cardsNow()
    }

    protected fun awaitCardsWhere(
        description: String,
        timeout: Long = 10_000,
        predicate: (List<VaultCard>) -> Boolean
    ): List<VaultCard> {
        compose.waitUntil(timeout) { predicate(cardsNow()) }
        val cards = cardsNow()
        check(predicate(cards)) { "$description: $cards" }
        return cards
    }

    protected fun seedCard(
        number: String,
        holderId: String? = null,
        bankId: String? = null,
        cardName: String = "",
        expiry: String = "12/29",
        cvv: String = "381",
        note: String = "",
        pinned: Boolean = false,
        pinnedAt: Long? = null,
        sortOrder: Int = 0
    ): VaultCard = runBlocking {
        app.repository.insertCard(
            VaultCard(
                number = number,
                expiry = expiry,
                cvv = cvv,
                holderId = holderId,
                bankId = bankId,
                networkAuto = true,
                nameOnCard = "AARAV MEHTA",
                cardName = cardName,
                note = note,
                bandColor = "navy",
                pinned = pinned,
                pinnedAt = pinnedAt,
                sortOrder = sortOrder
            )
        ).getOrThrow()
    }
}
