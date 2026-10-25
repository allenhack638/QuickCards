package com.quickcards.app.core.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.app.core.ui.HomeCard
import com.quickcards.app.core.ui.theme.QuickCardsTheme
import com.quickcards.domain.CardField
import com.quickcards.domain.CardNetwork
import com.quickcards.domain.ExpiryState
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PassCardStatusTest {

    @get:Rule
    val compose = createComposeRule()

    private val network = CardNetwork(
        id = "visa",
        name = "Visa",
        code = "VISA",
        prefixes = "4",
        lengths = "16",
        cvvLength = 3,
        luhn = true
    )

    private fun card(
        expiry: String = "12/29",
        note: String = "",
        archived: Boolean = false,
        needsReview: Boolean = false
    ) = HomeCard(
        card = VaultCard(
            id = "status-card",
            number = "4532778103492211",
            expiry = expiry,
            cvv = "381",
            bankId = "bank-1",
            typeId = "type-1",
            nameOnCard = "AARAV MEHTA",
            note = note,
            archived = archived,
            archiveReason = if (archived) "Card closed" else null,
            needsReview = if (needsReview) setOf(CardField.EXPIRY) else emptySet()
        ),
        bankName = "HDFC Bank",
        holderName = "Aarav Mehta",
        typeName = "Credit",
        network = network,
        networkId = "visa",
        tagNames = emptyList()
    )

    private val longNote =
        "Used for the family account and the joint savings. Renewal is due before " +
            "the annual fee posts in November; ask Priya for the latest statement."

    @After
    fun tearDown() {
        com.quickcards.app.core.security.UnlockSession.lockNow()
    }

    private fun show(
        card: HomeCard,
        expiryState: ExpiryState?,
        needsReview: Boolean = false,
        archivedLabel: String? = null,
        note: String? = null,
        showNote: Boolean = false,
        onCopy: (PassField) -> Unit = {},
        onCopyNote: () -> Unit = {}
    ) {
        compose.setContent {
            QuickCardsTheme(darkTheme = true) {
                Box(modifier = Modifier.width(328.dp)) {
                    PassCard(
                        card = card,
                        maskNumbers = true,
                        tagsShow = true,
                        expiryState = expiryState,
                        needsReview = needsReview,
                        archivedLabel = archivedLabel,
                        note = note,
                        showNote = showNote,
                        onCopy = { field ->
                            onCopy(field)
                            true
                        },
                        onReveal = { true },
                        onCopyNote = {
                            onCopyNote()
                            true
                        }
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    @Test
    fun expiredPassShowsTheChipAndStillCopiesTheExpiry() {
        var copied: PassField? = null
        show(
            card = card(expiry = "01/24"),
            expiryState = ExpiryState.EXPIRED,
            onCopy = { copied = it }
        )

        compose.onNodeWithText("EXPIRED", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("CREDIT", useUnmergedTree = true).assertExists()
        compose.onNodeWithContentDescription("Expiry, 01/24. Expired. Tap to copy.")
            .performClick()
        compose.waitForIdle()

        assertEquals(PassField.EXPIRY, copied)
    }

    @Test
    fun expiresSoonIsAPlainLabel() {
        show(
            card = card(expiry = "11/26"),
            expiryState = ExpiryState.EXPIRES_SOON
        )

        compose.onNodeWithText("EXPIRES SOON", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("EXPIRED", useUnmergedTree = true).assertDoesNotExist()
        compose.onNodeWithContentDescription("Expiry, 11/26. Expires soon. Tap to copy.")
            .assertExists()
    }

    @Test
    fun checkDetailsUsesTheNeutralInfoLabel() {
        show(
            card = card(needsReview = true),
            expiryState = ExpiryState.VALID,
            needsReview = true
        )
        compose.onNodeWithText("CHECK DETAILS", useUnmergedTree = true).assertExists()
    }

    @Test
    fun archivedLabelWinsOverCheckDetails() {
        show(
            card = card(needsReview = true, archived = true),
            expiryState = ExpiryState.VALID,
            needsReview = true,
            archivedLabel = "Archived · Card closed"
        )

        compose.onNodeWithText("ARCHIVED · CARD CLOSED", useUnmergedTree = true).assertExists()
        compose.onNodeWithText("CHECK DETAILS", useUnmergedTree = true).assertDoesNotExist()
    }

    @Test
    fun longNoteShowsMoreAndTogglesToLess() {
        var copied = false
        show(
            card = card(note = longNote),
            expiryState = ExpiryState.VALID,
            note = longNote,
            showNote = true,
            onCopyNote = { copied = true }
        )

        compose.onNodeWithContentDescription("Show full note").assertExists()
        compose.onNodeWithContentDescription("Show full note").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("Less").assertExists()
        compose.onNodeWithContentDescription("Show less").performClick()
        compose.waitForIdle()

        compose.onNodeWithText("More").assertExists()
        assertEquals(false, copied)
    }

    @Test
    fun shortNoteHasNoMoreButton() {
        show(
            card = card(note = "Everyday spending"),
            expiryState = ExpiryState.VALID,
            note = "Everyday spending",
            showNote = true
        )
        compose.onNodeWithText("Everyday spending").assertExists()
        compose.onNodeWithText("More").assertDoesNotExist()
    }

    @Test
    fun notesAreHiddenWhenShowNotesIsOff() {
        show(
            card = card(note = longNote),
            expiryState = ExpiryState.VALID,
            note = longNote,
            showNote = false
        )
        compose.onNodeWithText(longNote).assertDoesNotExist()
        compose.onNodeWithText("More").assertDoesNotExist()
    }

    @Test
    fun longPressingTheNoteCopiesIt() {
        var copied = false
        show(
            card = card(note = longNote),
            expiryState = ExpiryState.VALID,
            note = longNote,
            showNote = true,
            onCopyNote = { copied = true }
        )

        compose.onNodeWithText(longNote, useUnmergedTree = true)
            .performTouchInput { longClick() }
        compose.waitForIdle()

        assertEquals(true, copied)
    }
}
