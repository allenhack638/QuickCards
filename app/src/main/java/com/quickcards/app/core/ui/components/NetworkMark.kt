package com.quickcards.app.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import com.quickcards.app.R
import com.quickcards.app.core.ui.art.ArtDimens
import kotlin.math.max
import kotlin.math.min

/**
 * Bundled acceptance marks for the built-in networks. Every drawable is authored on the same
 * 56x28 canvas with the logo fitted into a centred 48x20 box; [bounds] is the logo's own box on
 * that canvas. Custom networks have no mark and keep the text badge.
 */
object NetworkMarks {
    private val marks = mapOf(
        "visa" to R.drawable.net_visa,
        "mc" to R.drawable.net_mc,
        "amex" to R.drawable.net_amex,
        "rupay" to R.drawable.net_rupay,
        "diners" to R.drawable.net_diners,
        "discover" to R.drawable.net_discover,
        "jcb" to R.drawable.net_jcb,
        "maestro" to R.drawable.net_maestro,
        "unionpay" to R.drawable.net_unionpay,
        "mir" to R.drawable.net_mir
    )

    private val artwork = mapOf(
        "visa" to Rect(4f, 6.2f, 52f, 21.8f),
        "mc" to Rect(15.1f, 4f, 40.9f, 20f),
        "rupay" to Rect(4f, 7.7f, 52f, 20.3f),
        "amex" to Rect(18f, 4f, 38f, 24f),
        "diners" to Rect(4f, 7.8f, 52f, 20.3f),
        "discover" to Rect(4f, 9.4f, 54f, 17.7f),
        "jcb" to Rect(15.1f, 4.1f, 40.9f, 23.9f),
        "maestro" to Rect(15.1f, 4f, 40.9f, 19.9f),
        "mir" to Rect(4f, 7.2f, 52f, 20.8f),
        "unionpay" to Rect(12f, 4f, 44f, 24f)
    )

    /** The 56x28 canvas every mark drawable is authored on. */
    val canvas = Rect(0f, 0f, CANVAS_WIDTH, CANVAS_HEIGHT)

    val ids: Set<String> get() = marks.keys

    @DrawableRes
    fun drawable(networkId: String?): Int? = networkId?.let { marks[it] }

    fun bounds(networkId: String): Rect = artwork[networkId] ?: canvas

    /** White fills knock out, every other fill paints white (alpha = 3A - R - G - B, unpremultiplied). */
    val whiteTone: FloatArray get() = floatArrayOf(
        0f, 0f, 0f, 0f, 255f,
        0f, 0f, 0f, 0f, 255f,
        0f, 0f, 0f, 0f, 255f,
        -1f, -1f, -1f, 3f, 0f
    )
}

private const val CANVAS_WIDTH = 56f
private const val CANVAS_HEIGHT = 28f
private const val LOGO_BOX_WIDTH = 48f
private const val LOGO_BOX_HEIGHT = 20f

enum class MarkTone { COLOUR, WHITE }

private val alwaysColour = setOf("mc", "maestro")

// Box and badge marks carry their own ground, so they stay readable on any surface.
private val boxedMarks = setOf("amex", "jcb", "unionpay")

/**
 * Full colour on surfaces in both themes, except wordmarks that vanish on dark (those go white).
 * On card art every mark goes white with its white fills cut out; Mastercard and Maestro never go white.
 */
fun markTone(networkId: String, onArt: Boolean, dark: Boolean): MarkTone = when {
    networkId in alwaysColour -> MarkTone.COLOUR
    onArt -> MarkTone.WHITE
    dark && networkId !in boxedMarks -> MarkTone.WHITE
    else -> MarkTone.COLOUR
}

/**
 * The Manage lead column is the logo width unless a custom text badge needs more, and it never
 * exceeds the row's slot. Every row shares the result so names keep one left edge.
 */
fun networkLeadWidth(badgeWidths: List<Dp>, min: Dp, max: Dp): Dp =
    badgeWidths.maxOrNull()?.coerceIn(min, max) ?: min

/** Where the mark sits, which fixes its box and how the logo is fitted into it. */
enum class MarkFit(val width: Dp, val height: Dp) {
    /**
     * Card header: the 56x28 canvas with the logo grown to fill its centred 48x20 box (never
     * shrunk), shifted so the logo's right edge is the box's right edge.
     */
    HEAD(ArtDimens.MarkWidth, ArtDimens.MarkHeight),
    /** Lists, fields and autofill: the logo's own bounds fitted into 40x24. */
    SLOT(ArtDimens.SlotWidth, ArtDimens.SlotHeight),
    /** On mini card art: the logo fitted into 20x12. */
    MINI(ArtDimens.MiniMarkWidth, ArtDimens.MiniMarkHeight)
}

private val WhiteTone = ColorFilter.colorMatrix(ColorMatrix(NetworkMarks.whiteTone))

@Composable
fun NetworkMark(
    networkId: String,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    tone: MarkTone = MarkTone.COLOUR,
    fit: MarkFit = MarkFit.SLOT
) {
    val resId = NetworkMarks.drawable(networkId) ?: return
    val painter = painterResource(resId)
    val bounds = remember(networkId) { NetworkMarks.bounds(networkId) }
    val filter = if (tone == MarkTone.WHITE) WhiteTone else null
    Box(
        modifier = modifier
            .size(fit.width, fit.height)
            .then(
                if (contentDescription != null) {
                    Modifier.semantics {
                        this.contentDescription = contentDescription
                        role = Role.Image
                    }
                } else {
                    Modifier
                }
            )
            .drawBehind {
                val unit: Float
                val dx: Float
                val dy: Float
                if (fit == MarkFit.HEAD) {
                    val base = size.height / CANVAS_HEIGHT
                    unit = base * max(1f, min(LOGO_BOX_WIDTH / bounds.width, LOGO_BOX_HEIGHT / bounds.height))
                    dx = size.width - bounds.right * unit
                    dy = base * CANVAS_HEIGHT / 2f - (bounds.top + bounds.height / 2f) * unit
                } else {
                    unit = min(size.width / bounds.width, size.height / bounds.height)
                    dx = (size.width - bounds.width * unit) / 2f - bounds.left * unit
                    dy = (size.height - bounds.height * unit) / 2f - bounds.top * unit
                }
                translate(dx, dy) {
                    with(painter) { draw(Size(CANVAS_WIDTH * unit, CANVAS_HEIGHT * unit), colorFilter = filter) }
                }
            }
    )
}
