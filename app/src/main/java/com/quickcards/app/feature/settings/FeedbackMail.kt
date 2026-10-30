package com.quickcards.app.feature.settings

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.net.toUri
import com.quickcards.app.BuildConfig
import com.quickcards.app.R
import com.quickcards.app.core.security.AppLockController

/**
 * Opens the user's own email app, addressed to support, with the app and device versions
 * filled in. QuickCards sends nothing itself, needs no permission and adds no card data.
 */
object FeedbackMail {

    const val ADDRESS = "support@surety.money"

    fun body(context: Context): String = context.getString(
        R.string.settings_feedback_body,
        BuildConfig.VERSION_NAME,
        BuildConfig.VERSION_CODE,
        Build.VERSION.RELEASE,
        Build.VERSION.SDK_INT,
        Build.MANUFACTURER,
        Build.MODEL
    )

    fun intent(context: Context): Intent =
        Intent(Intent.ACTION_SENDTO, "mailto:$ADDRESS".toUri())
            .putExtra(Intent.EXTRA_EMAIL, arrayOf(ADDRESS))
            .putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.settings_feedback_subject))
            .putExtra(Intent.EXTRA_TEXT, body(context))

    /** False when no app can send email. */
    fun open(context: Context): Boolean = try {
        // The email app is a deliberate trip out: keep "Lock when you leave" from firing.
        AppLockController.beginSystemPicker()
        context.startActivity(intent(context))
        true
    } catch (_: ActivityNotFoundException) {
        false
    }
}
