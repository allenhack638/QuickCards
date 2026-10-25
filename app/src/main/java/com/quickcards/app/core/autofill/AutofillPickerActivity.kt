package com.quickcards.app.core.autofill

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.autofill.AutofillManager
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.lifecycleScope
import com.quickcards.app.QuickCardsApplication
import com.quickcards.app.core.data.model.toDomain
import com.quickcards.app.core.security.UnlockSession
import com.quickcards.app.core.ui.base.BaseActivity
import com.quickcards.app.core.ui.theme.QuickCardsTheme
import com.quickcards.app.core.ui.theme.ThemeMode
import com.quickcards.app.core.ui.theme.ThemeSettings
import com.quickcards.domain.CardNumberRules
import com.quickcards.domain.ExpiryState
import com.quickcards.domain.ExpiryStatus
import java.time.LocalDate
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * "Search all cards…": unlock first, then list every non-archived card with search. Picking
 * one returns the filled dataset; Back, Cancel or leaving the screen fills nothing.
 */
class AutofillPickerActivity : BaseActivity() {

    private var unlocked = false

    override fun onSecureCreate(savedInstanceState: Bundle?) {
        val target = FillTarget.from(intent)
        if (target == null) return cancel()
        lifecycleScope.launch {
            val reason = AutofillFill.unlockReason(this@AutofillPickerActivity, target.requester)
            if (!UnlockSession.requireUnlock(this@AutofillPickerActivity, reason, fresh = true)) return@launch cancel()
            unlocked = true
            val cards = loadCards()
            val themeSettings = ThemeSettings(getSharedPreferences("quickcards_settings", MODE_PRIVATE))
            setContent {
                val themeMode by themeSettings.themeMode.collectAsState()
                val dynamicColor by themeSettings.dynamicColor.collectAsState()
                val dark = when (themeMode) {
                    ThemeMode.SYSTEM -> isSystemInDarkTheme()
                    ThemeMode.LIGHT -> false
                    ThemeMode.DARK -> true
                }
                QuickCardsTheme(darkTheme = dark, dynamicColor = dynamicColor) {
                    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                        AutofillPickerContent(
                            cards = cards,
                            requester = target.requester,
                            onPick = { id -> fill(id, target) },
                            onCancel = ::cancel
                        )
                    }
                }
            }
        }
    }

    private suspend fun loadCards(): List<PickerCard> {
        val app = application as QuickCardsApplication
        val repo = app.repository
        val banks = repo.banks.first().associateBy { it.id }
        val holders = repo.holders.first().associateBy { it.id }
        val types = repo.cardTypes.first().associateBy { it.id }
        val tags = repo.tags.first().associateBy { it.id }
        val networks = repo.networks.first().map { it.toDomain() }
        val today = LocalDate.now()
        return repo.cards.first().filterNot { it.archived }.map { card ->
            val network = if (card.networkAuto) {
                CardNumberRules.detect(card.number, networks)
            } else {
                networks.firstOrNull { it.id == card.networkId }
            }
            PickerCard(
                id = card.id,
                bankName = card.bankId?.let { banks[it]?.name }.orEmpty(),
                typeName = card.typeId?.let { types[it]?.name }.orEmpty(),
                holderName = card.holderId?.let { holders[it]?.name }.orEmpty(),
                number = card.number,
                expiry = card.expiry,
                tagNames = card.tagIds.mapNotNull { tags[it]?.name },
                networkId = network?.id,
                expired = ExpiryStatus.expiryState(card.expiry, today, 1) == ExpiryState.EXPIRED,
                networkName = network?.name.orEmpty(),
                networkCode = network?.code.orEmpty(),
                note = card.note
            )
        }
    }

    private fun fill(cardId: String, target: FillTarget) {
        lifecycleScope.launch {
            val dataset = AutofillFill.dataset(this@AutofillPickerActivity, cardId, target)
            if (dataset == null) return@launch cancel()
            setResult(Activity.RESULT_OK, Intent().putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, dataset))
            finish()
        }
    }

    private fun cancel() {
        setResult(Activity.RESULT_CANCELED)
        finish()
    }

    /** Decrypted cards never stay on screen behind another app: leaving the list cancels it. */
    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (unlocked && !isFinishing) cancel()
    }

    companion object {
        fun intent(context: Context, target: FillTarget): Intent =
            target.writeTo(Intent(context, AutofillPickerActivity::class.java))
    }
}
