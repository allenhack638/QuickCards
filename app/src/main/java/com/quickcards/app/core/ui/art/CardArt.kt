package com.quickcards.app.core.ui.art

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.quickcards.app.core.ui.theme.ArtHue
import com.quickcards.app.core.ui.theme.PassColors
import com.quickcards.app.core.ui.theme.QcArtDimens
import com.quickcards.app.core.ui.theme.QcArtShadow
import com.quickcards.app.core.ui.theme.QcSize
import kotlin.math.roundToInt

/** The art surfaces: Home tile, details hero, picker swatch (64x40) and list mini (40x24). */
enum class ArtKind(val corner: Dp) {
    TILE(QcArtDimens.CornerTile),
    HERO(QcArtDimens.CornerTile),
    SWATCH(QcArtDimens.CornerSwatch),
    MINI(QcArtDimens.CornerMini)
}

/**
 * Daylight card art for a stored colour key or custom hex: 135° jewel gradient, top-left sheen,
 * the colour's own pattern, grain, a 1dp lit edge and a soft glow in the hue. Expired cards turn
 * graphite with the stripes fading above the digits. Tile and hero pad their [content] by 16dp.
 */
@Composable
fun CardArt(
    band: String,
    modifier: Modifier = Modifier,
    kind: ArtKind = ArtKind.TILE,
    expired: Boolean = false,
    content: @Composable BoxScope.() -> Unit = {}
) {
    val hue = remember(band, expired) { PassColors.hue(band, expired) }
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val sized = when (kind) {
        ArtKind.TILE -> Modifier
        ArtKind.HERO -> Modifier.defaultMinSize(minHeight = QcArtDimens.HeroMinHeight)
        ArtKind.SWATCH -> Modifier.size(QcArtDimens.SwatchWidth, QcArtDimens.SwatchHeight)
        ArtKind.MINI -> Modifier.size(QcArtDimens.MiniWidth, QcArtDimens.MiniHeight)
    }
    val padding = if (kind == ArtKind.TILE || kind == ArtKind.HERO) QcArtDimens.Padding else 0.dp
    Box(
        modifier = modifier
            .then(sized)
            .cardArt(hue, kind, expired, dark)
            .padding(PaddingValues(padding)),
        content = content
    )
}

/** The art layers on any shape-sized element; [CardArt] is the usual entry point. */
fun Modifier.cardArt(hue: ArtHue, kind: ArtKind, expired: Boolean, dark: Boolean): Modifier {
    val shape = RoundedCornerShape(kind.corner)
    return this
        .drawBehind { drawGlow(hue, kind, dark) }
        .clip(shape)
        .drawWithCache {
            val grain = ArtCache.grain(density)?.let { ShaderBrush(ImageShader(it, TileMode.Repeated, TileMode.Repeated)) }
            val corner = kind.corner.toPx()
            val small = kind == ArtKind.SWATCH || kind == ArtKind.MINI || size.width < QcArtDimens.SmallArt.toPx()
            val fade = when {
                expired -> PatternFade.EXPIRED
                kind == ArtKind.HERO -> PatternFade.HERO
                else -> PatternFade.TILE
            }
            val w = size.width.roundToInt().coerceAtLeast(1)
            val h = size.height.roundToInt().coerceAtLeast(1)
            val pattern = ArtCache.pattern(hue.pattern, fade, small, w, h, density)
            val gradient = linearGradient135(size, hue.c1, hue.c2)
            val edge = QcSize.hairline.toPx()
            val outline = Path().apply { addRoundRect(RoundRect(0f, 0f, size.width, size.height, CornerRadius(corner))) }
            fun shifted(dy: Float) = Path().apply {
                addRoundRect(RoundRect(0f, dy, size.width, size.height + dy, CornerRadius(corner)))
            }
            val topEdge = Path.combine(PathOperation.Difference, outline, shifted(edge))
            val bottomEdge = Path.combine(PathOperation.Difference, outline, shifted(-edge))
            onDrawWithContent {
                drawRect(gradient)
                drawSheen()
                if (expired) {
                    drawRect(Brush.verticalGradient(0f to Color.Transparent, .40f to Color.Transparent, .75f to PassColors.ExpiredShade, 1f to PassColors.ExpiredShade))
                }
                if (pattern != null) drawImage(pattern, dstSize = IntSize(w, h), colorFilter = ColorFilter.tint(PassColors.OnArt))
                if (grain != null) drawRect(grain, alpha = GRAIN_ALPHA, blendMode = BlendMode.Overlay)
                drawPath(topEdge, PassColors.EdgeLight)
                if (kind == ArtKind.TILE || kind == ArtKind.HERO) drawPath(bottomEdge, PassColors.EdgeDark)
                drawContent()
            }
        }
}

/** radial-gradient(120% 90% at 0 0, white 20%, transparent 55%): an ellipse, drawn as a scaled circle. */
private fun DrawScope.drawSheen() {
    val rx = size.width * 1.2f
    val ry = size.height * .9f
    val squash = ry / rx
    scale(1f, squash, pivot = Offset.Zero) {
        drawRect(
            Brush.radialGradient(0f to PassColors.Sheen, .55f to Color.Transparent, center = Offset.Zero, radius = rx),
            size = Size(size.width, size.height / squash)
        )
    }
}

/** CSS linear-gradient(135deg, c1, c2) over the box. */
private fun linearGradient135(size: Size, c1: Color, c2: Color): Brush {
    val half = (size.width + size.height) / 4f
    val center = Offset(size.width / 2f, size.height / 2f)
    return Brush.linearGradient(
        listOf(c1, c2),
        start = Offset(center.x - half, center.y - half),
        end = Offset(center.x + half, center.y + half)
    )
}

private fun DrawScope.drawGlow(hue: ArtHue, kind: ArtKind, dark: Boolean) {
    val glow = when (kind) {
        ArtKind.TILE, ArtKind.HERO -> if (dark) QcArtDimens.GlowTileDark else QcArtDimens.GlowTileLight
        ArtKind.SWATCH -> QcArtDimens.GlowSwatch
        ArtKind.MINI -> QcArtDimens.GlowMini
    }
    drawShadow(glow, hue.glow, kind.corner.toPx(), GLOW_SCALE)
    if (kind == ArtKind.TILE || kind == ArtKind.HERO) {
        drawShadow(QcArtDimens.ContactShadow, if (dark) PassColors.ContactShadowDark else PassColors.ContactShadowLight, kind.corner.toPx(), 1)
    }
}

private fun DrawScope.drawShadow(shadow: QcArtShadow, color: Color, corner: Float, scale: Int) {
    val spread = shadow.spread.toPx()
    val w = (size.width + 2 * spread).roundToInt()
    val h = (size.height + 2 * spread).roundToInt()
    if (w <= 0 || h <= 0) return
    val sigma = shadow.blur.toPx() / 2f
    val mask = ArtCache.glow(w, h, (corner + spread).coerceAtLeast(0f), sigma, scale) ?: return
    val pad = 3f * sigma
    drawImage(
        mask,
        dstOffset = IntOffset((-spread - pad).roundToInt(), (shadow.offsetY.toPx() - spread - pad).roundToInt()),
        dstSize = IntSize((w + 2 * pad).roundToInt(), (h + 2 * pad).roundToInt()),
        colorFilter = ColorFilter.tint(color)
    )
}

private const val GRAIN_ALPHA = .14f
private const val GLOW_SCALE = 4
