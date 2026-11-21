package com.quickcards.app.core.ui.art

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import com.quickcards.app.core.ui.theme.PassColors
import com.quickcards.domain.ArtPattern
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * The prototype's stroke patterns (QC.pattern in 00-head.html), in its own units: [w] x [h] is the
 * art box in dp and one unit is [unit] px, so geometry matches the CSS frame exactly.
 */
internal fun DrawScope.drawArtPattern(pattern: ArtPattern, w: Float, h: Float, unit: Float) {
    val white = PassColors.OnArt
    val stroke = Stroke(width = unit)
    fun p(x: Float, y: Float) = Offset(x * unit, y * unit)

    when (pattern) {
        ArtPattern.RINGS -> for (i in 1..22) {
            drawCircle(white, radius = i * 12f * unit, center = p(w * .86f, h * .1f), alpha = .16f - i * .005f, style = stroke)
        }
        ArtPattern.LINES -> {
            var i = -h
            while (i < w) {
                drawLine(white, p(i, h), p(i + h, 0f), strokeWidth = unit, alpha = .07f)
                i += 8f
            }
            drawCircle(white, radius = h * .9f * unit, center = p(w * .92f, h * 1.05f), alpha = .05f)
        }
        ArtPattern.CONTOURS -> for (k in 0 until 16) {
            val base = h * .2f + k * 10f
            val path = Path().apply {
                moveTo(0f, base * unit)
                var x = 0f
                while (x <= w) {
                    val y = base + sin(x / 46f + k * .35f) * 14f + cos(x / 90f) * 10f
                    lineTo(x * unit, y * unit)
                    x += 16f
                }
            }
            drawPath(path, white, alpha = .04f + k * .006f, style = stroke)
        }
        ArtPattern.ROSETTE -> {
            val cx = w * .86f
            val cy = h * .62f
            for (k in 0 until 40) {
                val a = k * PI.toFloat() / 20f
                drawCircle(
                    white,
                    radius = h * .62f * unit,
                    center = p(cx + cos(a) * h * .34f, cy + sin(a) * h * .34f),
                    alpha = .07f,
                    style = stroke
                )
            }
        }
        ArtPattern.WEAVE -> {
            var i = -h
            while (i < w + h) {
                drawPath(
                    Path().apply {
                        moveTo(i * unit, 0f)
                        quadraticTo((i + h * .5f) * unit, h * .5f * unit, i * unit, h * unit)
                    },
                    white, alpha = .08f, style = stroke
                )
                drawPath(
                    Path().apply {
                        moveTo((i + 7f) * unit, 0f)
                        quadraticTo((i + 7f - h * .5f) * unit, h * .5f * unit, (i + 7f) * unit, h * unit)
                    },
                    white, alpha = .06f, style = stroke
                )
                i += 14f
            }
        }
    }
}
