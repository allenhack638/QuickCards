package com.quickcards.app.core.ui.art

import android.graphics.BlurMaskFilter
import android.graphics.Bitmap
import android.graphics.Paint
import android.util.LruCache
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageBitmapConfig
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.CanvasDrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import com.quickcards.domain.ArtPattern
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.roundToInt

/** How the pattern fades down the card (the prototype's .art-pat masks). */
internal enum class PatternFade(val stops: Array<Pair<Float, Float>>) {
    TILE(arrayOf(0f to 1f, .40f to 1f, .55f to .5f, 1f to .5f)),
    HERO(arrayOf(0f to 1f, .35f to 1f, .50f to .5f, .60f to .25f, 1f to .25f)),
    EXPIRED(arrayOf(0f to 1f, .25f to 1f, .55f to 0f, 1f to 0f))
}

/**
 * Every art layer that costs more than a gradient is rendered once into a bitmap and reused:
 * patterns per (pattern, fade, size), the grain tile per density, and blurred glow masks per size.
 * A cached bitmap is returned at once; a missing one returns null and is built on
 * [Dispatchers.Default], so the main thread never renders art. Callers read [landed] through a
 * miss, so a draw that skipped a layer is invalidated when that layer lands.
 */
internal object ArtCache {

    private val cache = object : LruCache<Any, ImageBitmap>(MAX_BYTES) {
        override fun sizeOf(key: Any, value: ImageBitmap): Int =
            value.width * value.height * if (value.config == ImageBitmapConfig.Alpha8) 1 else 4
    }

    private data class PatternKey(val pattern: ArtPattern, val fade: PatternFade, val small: Boolean, val w: Int, val h: Int)
    private data class GrainKey(val density: Float)
    private data class GlowKey(val w: Int, val h: Int, val radius: Int, val sigma: Int, val scale: Int)

    private val pending = HashSet<Any>()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var landed by mutableIntStateOf(0)

    private fun get(key: Any, build: () -> ImageBitmap): ImageBitmap? {
        synchronized(cache) { cache.get(key) }?.let { return it }
        landed
        val first = synchronized(cache) { pending.add(key) }
        if (first) {
            scope.launch {
                val image = build()
                synchronized(cache) {
                    cache.put(key, image)
                    pending.remove(key)
                }
                landed++
            }
        }
        return null
    }

    /** White pattern alpha, already faded; small art draws the 328x207 frame scaled to cover. */
    fun pattern(pattern: ArtPattern, fade: PatternFade, small: Boolean, w: Int, h: Int, density: Float): ImageBitmap? =
        get(PatternKey(pattern, fade, small, w, h)) {
            val image = ImageBitmap(w, h, ImageBitmapConfig.Alpha8)
            CanvasDrawScope().draw(Density(density), LayoutDirection.Ltr, Canvas(image), Size(w.toFloat(), h.toFloat())) {
                if (small) {
                    val unit = max(w / BASIS_W, h / BASIS_H)
                    translate((w - BASIS_W * unit) / 2f, (h - BASIS_H * unit) / 2f) {
                        drawArtPattern(pattern, BASIS_W, BASIS_H, unit)
                    }
                } else {
                    drawArtPattern(pattern, w / density, h / density, density)
                }
                drawRect(
                    Brush.verticalGradient(*fade.stops.map { (at, a) -> at to Color.Black.copy(alpha = a) }.toTypedArray()),
                    blendMode = BlendMode.DstIn
                )
            }
            image
        }

    /** A tileable, deterministic grey value-noise tile (feTurbulence-like, 0.9 per dp, 2 octaves). */
    fun grain(density: Float): ImageBitmap? = get(GrainKey(density)) {
        val tileDp = ArtDimens.GrainTile.value
        val target = (tileDp * density).roundToInt()
        val size = minOf(target, GRAIN_SOURCE_PX)
        val cells = (tileDp * GRAIN_FREQUENCY).roundToInt()
        val pixels = IntArray(size * size)
        for (y in 0 until size) {
            for (x in 0 until size) {
                val u = x.toFloat() / size
                val v = y.toFloat() / size
                val n = (valueNoise(u * cells, v * cells, cells) + .5f * valueNoise(u * cells * 2, v * cells * 2, cells * 2)) / 1.5f
                val g = ((.5f + (n - .5f) * GRAIN_CONTRAST).coerceIn(0f, 1f) * 255).roundToInt()
                pixels[y * size + x] = (0xFF shl 24) or (g shl 16) or (g shl 8) or g
            }
        }
        val source = Bitmap.createBitmap(pixels, size, size, Bitmap.Config.ARGB_8888)
        val tile = if (size == target) source else Bitmap.createScaledBitmap(source, target, target, true)
        tile.asImageBitmap()
    }

    /**
     * A blurred rounded-rect alpha mask of [w] x [h] px with [sigma] px of blur, padded by 3 sigma on
     * each side and rendered at 1/[scale] resolution (the blur hides it).
     */
    fun glow(w: Int, h: Int, radius: Float, sigma: Float, scale: Int): ImageBitmap? =
        get(GlowKey(w, h, radius.roundToInt(), sigma.roundToInt(), scale)) {
            val pad = 3f * sigma
            val bw = ceil((w + 2 * pad) / scale).toInt().coerceAtLeast(1)
            val bh = ceil((h + 2 * pad) / scale).toInt().coerceAtLeast(1)
            val bitmap = Bitmap.createBitmap(bw, bh, Bitmap.Config.ALPHA_8)
            val s = sigma / scale
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                // Skia turns a blur radius into sigma = r * 0.57735 + 0.5.
                if (s > .5f) maskFilter = BlurMaskFilter((s - .5f) / .57735f, BlurMaskFilter.Blur.NORMAL)
            }
            val p = pad / scale
            android.graphics.Canvas(bitmap).drawRoundRect(
                p, p, p + w.toFloat() / scale, p + h.toFloat() / scale, radius / scale, radius / scale, paint
            )
            bitmap.asImageBitmap()
        }

    private fun valueNoise(x: Float, y: Float, period: Int): Float {
        val x0 = floor(x).toInt()
        val y0 = floor(y).toInt()
        val fx = smooth(x - x0)
        val fy = smooth(y - y0)
        fun at(i: Int, j: Int) = lattice(Math.floorMod(i, period), Math.floorMod(j, period), period)
        val top = at(x0, y0) + (at(x0 + 1, y0) - at(x0, y0)) * fx
        val bottom = at(x0, y0 + 1) + (at(x0 + 1, y0 + 1) - at(x0, y0 + 1)) * fx
        return top + (bottom - top) * fy
    }

    private fun smooth(t: Float) = t * t * (3 - 2 * t)

    private fun lattice(i: Int, j: Int, period: Int): Float {
        var h = (i * 374761393 + j * 668265263 + period * 1442695041 + GRAIN_SEED)
        h = (h xor (h ushr 13)) * 1274126177
        h = h xor (h ushr 16)
        return (h and 0xFFFF) / 65535f
    }

    private const val BASIS_W = 328f
    private const val BASIS_H = 207f
    private const val GRAIN_FREQUENCY = .9f
    private const val GRAIN_CONTRAST = 1f
    private const val GRAIN_SEED = 0x51C0DE
    private const val GRAIN_SOURCE_PX = 128
    private const val MAX_BYTES = 16 * 1024 * 1024
}
