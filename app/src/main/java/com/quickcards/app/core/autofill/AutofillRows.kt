package com.quickcards.app.core.autofill

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrixColorFilter
import android.text.SpannableString
import android.text.Spanned
import android.text.style.TypefaceSpan
import android.widget.RemoteViews
import androidx.core.content.ContextCompat
import androidx.core.graphics.createBitmap
import com.quickcards.app.R
import com.quickcards.app.core.ui.components.MASK_DOTS
import com.quickcards.app.core.ui.components.NetworkMarks
import com.quickcards.app.core.ui.components.markTone
import com.quickcards.app.core.ui.components.MarkTone
import kotlin.math.min
import kotlin.math.roundToInt

/** The dropdown rows the framework draws in the other app: plain RemoteViews. */
internal object AutofillRows {

    fun search(context: Context, count: Int) = RemoteViews(context.packageName, R.layout.autofill_search).apply {
        setTextViewText(R.id.autofill_title, context.resources.getQuantityString(R.plurals.autofill_search_all, count, count))
    }

    /**
     * "HDFC Bank •• 0366" with the network mark in the 40x24 slot, or the app mark. The row shows in
     * another app before any unlock, so it carries no holder name.
     */
    fun card(context: Context, bank: String, lastFour: String, networkId: String?) =
        RemoteViews(context.packageName, R.layout.autofill_suggestion).apply {
            setTextViewText(R.id.autofill_title, title(context, bank, lastFour))
            markBitmap(context, networkId)?.let { setImageViewBitmap(R.id.autofill_icon, it) }
        }

    private fun title(context: Context, bank: String, lastFour: String): CharSequence {
        val text = context.getString(R.string.autofill_suggestion, bank, lastFour)
        val start = text.lastIndexOf(MASK_DOTS)
        if (start < 0) return text
        return SpannableString(text).apply {
            setSpan(TypefaceSpan("monospace"), start, text.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        }
    }

    /** The logo's own bounds fitted into 40x24, as in the app; wordmarks go white on a dark dropdown. */
    private fun markBitmap(context: Context, networkId: String?): Bitmap? {
        val id = networkId ?: return null
        val res = NetworkMarks.drawable(id) ?: return null
        val drawable = ContextCompat.getDrawable(context, res)?.mutate() ?: return null
        val density = context.resources.displayMetrics.density
        val width = (SLOT_WIDTH_DP * density).roundToInt()
        val height = (SLOT_HEIGHT_DP * density).roundToInt()
        val logo = NetworkMarks.bounds(id)
        val canvasRect = NetworkMarks.canvas
        val unit = min(width / logo.width, height / logo.height)
        val dark = (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        if (markTone(id, onArt = false, dark = dark) == MarkTone.WHITE) {
            drawable.colorFilter = ColorMatrixColorFilter(NetworkMarks.whiteTone)
        }
        drawable.setBounds(0, 0, (canvasRect.width * unit).roundToInt(), (canvasRect.height * unit).roundToInt())
        return createBitmap(width, height).also { bitmap ->
            val canvas = Canvas(bitmap)
            canvas.translate(
                (width - logo.width * unit) / 2f - logo.left * unit,
                (height - logo.height * unit) / 2f - logo.top * unit
            )
            drawable.draw(canvas)
        }
    }

    private const val SLOT_WIDTH_DP = 40
    private const val SLOT_HEIGHT_DP = 24
}
