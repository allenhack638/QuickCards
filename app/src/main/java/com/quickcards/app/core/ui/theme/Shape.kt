package com.quickcards.app.core.ui.theme

import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** DESIGN.md › Shapes: one radius scale. */
object QcShapes {
    val xs = RoundedCornerShape(QcRadius.xs)
    val sm = RoundedCornerShape(QcRadius.sm)
    val md = RoundedCornerShape(QcRadius.md)
    val lg = RoundedCornerShape(QcRadius.lg)
    val xl = RoundedCornerShape(QcRadius.xl)
    val sheet = RoundedCornerShape(topStart = QcRadius.xl, topEnd = QcRadius.xl)
    val full = CircleShape
    val tileDue = nestedCorners(QcArtDimens.CornerTile, QcArtDimens.DueEdgeGap)
    val tileDueAction = nestedCorners(QcArtDimens.CornerTile - QcArtDimens.DueEdgeGap, QcArtDimens.DueActionInset)
}

/** Concentric corners: a shape inset [inset] inside an [outer]-radius shape takes radius outer − inset. */
fun nestedCorners(outer: Dp, inset: Dp) = RoundedCornerShape((outer - inset).coerceAtLeast(0.dp))

val Shapes = Shapes(
    extraSmall = QcShapes.xs,
    small = QcShapes.sm,
    medium = QcShapes.md,
    large = QcShapes.lg,
    extraLarge = QcShapes.xl
)

/** e1: groups and fields. Light lifts with a soft 1dp shadow; both themes keep the 1dp line ring. */
fun Modifier.qcElevation1(colors: QcColors, shape: Shape): Modifier =
    (if (colors.dark) this else shadow(QcElevation.e1, shape, ambientColor = colors.shadow, spotColor = colors.shadow))
        .border(QcSize.hairline, colors.line, shape)

/** e3 ring: dark floating layers lift with tone plus a 6% white ring instead of a shadow. */
fun Modifier.qcRaisedRing(colors: QcColors, shape: Shape): Modifier =
    if (colors.dark) border(QcSize.hairline, Color.White.copy(alpha = 0.06f), shape) else this
