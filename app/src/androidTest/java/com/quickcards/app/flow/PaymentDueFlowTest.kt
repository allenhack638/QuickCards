package com.quickcards.app.flow

import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.app.core.reminders.PaidStore
import com.quickcards.domain.ReminderRules
import java.time.LocalDate
import kotlinx.coroutines.runBlocking
import org.junit.Test
import org.junit.runner.RunWith

/** A card whose bill is due shows the strip; Mark paid hides it and Undo brings it back. */
@RunWith(AndroidJUnit4::class)
class PaymentDueFlowTest : FlowTestBase() {

    @Test
    fun markPaidHidesTheStripAndUndoRestoresIt() {
        val today = LocalDate.now()
        app.settings.setPaymentReminders(true)
        val card = runBlocking {
            val holder = app.repository.createHolder("Aarav Mehta")
            val bank = app.repository.createBank("HDFC Bank")
            app.repository.insertCard(
                VaultCard(
                    number = "4532778103492211",
                    expiry = "12/29",
                    cvv = "381",
                    holderId = holder.id,
                    bankId = bank.id,
                    dueDay = today.dayOfMonth
                )
            ).getOrThrow()
        }
        PaidStore.unmarkPaid(app, ReminderRules.dueKey(card.id, today))

        waitForText("Bill due today")
        compose.onNodeWithContentDescription("Mark this payment as paid").performClick()
        waitForTextGone("Bill due today")

        compose.onNodeWithText("Undo").performClick()
        waitForText("Bill due today")
    }

    @Test
    fun noStripWhenPaymentRemindersAreOff() {
        app.settings.setPaymentReminders(false)
        runBlocking {
            val holder = app.repository.createHolder("Aarav Mehta")
            val bank = app.repository.createBank("HDFC Bank")
            app.repository.insertCard(
                VaultCard(
                    number = "4532778103492211",
                    expiry = "12/29",
                    cvv = "381",
                    holderId = holder.id,
                    bankId = bank.id,
                    dueDay = LocalDate.now().dayOfMonth
                )
            ).getOrThrow()
        }
        waitForContentDescription("More for HDFC Bank")
        waitForTextGone("Bill due today")
        app.settings.setPaymentReminders(true)
    }
}
