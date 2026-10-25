package com.quickcards.app.core.autofill

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.autofill.AutofillManager
import androidx.lifecycle.lifecycleScope
import com.quickcards.app.core.security.UnlockSession
import com.quickcards.app.core.ui.base.BaseActivity
import kotlinx.coroutines.launch

/**
 * Transparent activity the framework opens when a suggestion is tapped. It runs the biometric
 * prompt, decrypts the one card and returns the filled dataset; a cancelled prompt returns
 * nothing, so the form stays empty (fail-closed).
 */
class AutofillAuthActivity : BaseActivity() {

    override fun onSecureCreate(savedInstanceState: Bundle?) {
        val cardId = intent.getStringExtra(EXTRA_CARD_ID)
        val target = FillTarget.from(intent)
        if (cardId == null || target == null) {
            setResult(Activity.RESULT_CANCELED)
            finish()
            return
        }
        lifecycleScope.launch {
            val reason = AutofillFill.unlockReason(this@AutofillAuthActivity, target.requester)
            val granted = UnlockSession.requireUnlock(this@AutofillAuthActivity, reason, fresh = true)
            val dataset = if (granted) AutofillFill.dataset(this@AutofillAuthActivity, cardId, target) else null
            if (dataset == null) {
                setResult(Activity.RESULT_CANCELED)
            } else {
                setResult(Activity.RESULT_OK, Intent().putExtra(AutofillManager.EXTRA_AUTHENTICATION_RESULT, dataset))
            }
            finish()
        }
    }

    companion object {
        private const val EXTRA_CARD_ID = "card_id"

        fun intent(context: Context, cardId: String, target: FillTarget): Intent =
            target.writeTo(Intent(context, AutofillAuthActivity::class.java).putExtra(EXTRA_CARD_ID, cardId))
    }
}
