package com.quickcards.app.core.ui.components

import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.domain.CardNetwork
import com.quickcards.app.core.security.UnlockSession
import com.quickcards.app.core.ui.HomeCard
import com.quickcards.app.core.ui.theme.QuickCardsTheme
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PassCardTest {

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

    private val card = HomeCard(
        card = VaultCard(
            id = "test-card",
            number = "4532778103492211",
            expiry = "12/29",
            cvv = "381",
            holderId = "holder-1",
            bankId = "bank-1",
            networkAuto = true,
            typeId = "type-1",
            nameOnCard = "AARAV MEHTA",
            cardName = "Regalia",
            bandColor = "navy",
            sortOrder = 0
        ),
        bankName = "HDFC Bank",
        holderName = "Aarav Mehta",
        typeName = "Credit",
        network = network,
        networkId = "visa",
        tagNames = listOf("Travel")
    )

    private val maskedCvv = "CVV, hidden. Tap to copy, hold to show."
    private val revealedCvv = "CVV, 381. Tap to copy, hold to show."

    @After
    fun tearDown() {
        UnlockSession.lockNow()
    }

    @Test
    fun longPressRevealsCvvThenMasksAfterFiveSeconds() {
        UnlockSession.unlockFor(60)
        compose.mainClock.autoAdvance = false
        compose.setContent {
            QuickCardsTheme(darkTheme = true) {
                PassCard(
                    card = card,
                    maskNumbers = true,
                    tagsShow = true,
                    onCopy = { false },
                    onReveal = { true },
                    onActions = {}
                )
            }
        }
        compose.mainClock.advanceTimeBy(100)

        compose.onNodeWithContentDescription(maskedCvv).assertExists()

        compose.onNodeWithContentDescription(maskedCvv).performTouchInput {
            longClick(durationMillis = 600)
        }
        compose.mainClock.advanceTimeBy(700)

        compose.onNodeWithContentDescription(revealedCvv).assertExists()
        compose.onNodeWithText("381", useUnmergedTree = true).assertExists()

        compose.mainClock.advanceTimeBy(5_000)

        compose.onNodeWithContentDescription(maskedCvv).assertExists()
    }

    @Test
    fun networkMarkSitsLevelWithTheBankName() {
        compose.setContent {
            QuickCardsTheme(darkTheme = true) {
                PassCard(
                    card = card,
                    maskNumbers = true,
                    tagsShow = true,
                    onCopy = { false },
                    onReveal = { true },
                    onActions = {}
                )
            }
        }

        val mark = compose.onNodeWithContentDescription("Visa").getUnclippedBoundsInRoot()
        val bank = compose.onNodeWithText("HDFC Bank").getUnclippedBoundsInRoot()

        assertEquals(22.dp.value, mark.height.value, 1f)
        val markCentre = (mark.top.value + mark.bottom.value) / 2f
        val bankCentre = (bank.top.value + bank.bottom.value) / 2f
        assertEquals(bankCentre, markCentre, 1f)
    }
}
