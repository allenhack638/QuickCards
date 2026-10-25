package com.quickcards.app.core.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import com.quickcards.app.R
import com.quickcards.app.core.ui.theme.NetworkPlate
import com.quickcards.app.core.ui.theme.NetworkPlateEdge
import com.quickcards.app.core.ui.theme.QcDimens

/**
 * Bundled acceptance marks for the built-in networks. Every drawable is authored on the same
 * 56x28 canvas with the logo fitted into a centred 48x20 box, so marks line up exactly
 * wherever they appear. Custom networks have no mark and keep the text badge.
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

    val ids: Set<String> get() = marks.keys

    @DrawableRes
    fun drawable(networkId: String?): Int? = networkId?.let { marks[it] }
}

/** The mark on its white plate: the brand colours stay legible on any band or surface. */
@Composable
fun NetworkMark(
    @DrawableRes resId: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier
) {
    val shape = RoundedCornerShape(QcDimens.CornerBadge)
    Box(
        modifier = modifier
            .width(QcDimens.NetworkBadgeMinWidth)
            .height(QcDimens.NetworkBadgeHeight)
            .background(NetworkPlate, shape)
            .border(QcDimens.Divider, NetworkPlateEdge, shape)
    ) {
        Image(
            painter = painterResource(resId),
            contentDescription = contentDescription,
            modifier = Modifier.fillMaxSize()
        )
    }
}
