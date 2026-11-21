package com.quickcards.app.core.ui.art

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import com.quickcards.app.core.ui.theme.PassColors
import com.quickcards.app.core.ui.theme.QcRadius
import com.quickcards.app.core.ui.theme.QcShapes
import com.quickcards.app.core.ui.theme.QcSize
import com.quickcards.app.core.ui.theme.QcSpacing

/** The gold contact chip on the details hero (40x32, `r-xs`), drawn from its 40x32 SVG grid. */
@Composable
fun EmvChip(modifier: Modifier = Modifier) {
    Canvas(
        modifier = modifier
            .size(QcSpacing.s10, QcSpacing.s8)
            .shadow(QcSize.hairline, QcShapes.xs, ambientColor = PassColors.EmvShadow, spotColor = PassColors.EmvShadow)
    ) {
        val u = size.width / GRID_W
        val radius = CornerRadius(QcRadius.xs.toPx())
        drawRoundRect(
            Brush.linearGradient(
                0f to PassColors.EmvLight,
                .55f to PassColors.EmvDeep,
                1f to PassColors.EmvEnd,
                start = Offset.Zero,
                end = Offset(size.width, size.height)
            ),
            cornerRadius = radius
        )
        drawLine(PassColors.Sheen, Offset(radius.x, u / 2), Offset(size.width - radius.x, u / 2), u)
        val lines = Path().apply {
            moveTo(0f, 11 * u); lineTo(13 * u, 11 * u); lineTo(13 * u, 21 * u); lineTo(0f, 21 * u)
            moveTo(40 * u, 11 * u); lineTo(27 * u, 11 * u); lineTo(27 * u, 21 * u); lineTo(40 * u, 21 * u)
            moveTo(13 * u, 0f); lineTo(13 * u, 32 * u)
            moveTo(27 * u, 0f); lineTo(27 * u, 32 * u)
            moveTo(13 * u, 16 * u); lineTo(27 * u, 16 * u)
        }
        drawPath(lines, PassColors.EmvLine, style = Stroke(width = u))
    }
}

private const val GRID_W = 40f
