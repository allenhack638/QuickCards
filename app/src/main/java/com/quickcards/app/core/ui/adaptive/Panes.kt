package com.quickcards.app.core.ui.adaptive

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcElevation
import com.quickcards.app.core.ui.theme.QcShapes
import com.quickcards.app.core.ui.theme.QcSize
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcTheme
import kotlin.math.roundToInt

/**
 * List pane | detail pane, divided by a hairline. With a [hinge] the list ends at the hinge zone
 * and the detail starts after it, so nothing is drawn across the fold. [detail] gets its width.
 */
@Composable
fun ListDetailPanes(
    listWidth: Dp,
    hinge: Hinge?,
    list: @Composable () -> Unit,
    detail: @Composable (paneWidth: Dp) -> Unit
) {
    val density = LocalDensity.current
    var originX by remember { mutableStateOf<Float?>(null) }
    BoxWithConstraints(modifier = Modifier.fillMaxSize().onGloballyPositioned { originX = it.positionInWindow().x }) {
        val width = maxWidth
        val zone = with(density) {
            val origin = originX
            if (hinge == null || origin == null) return@with null
            val half = maxOf(QcDimens.HingeZone.toPx(), (hinge.right - hinge.left).toFloat()) / 2
            val mid = (hinge.left + hinge.right) / 2f - origin
            ((mid - half).toDp() to (mid + half).toDp()).takeIf { it.first > 0.dp && it.second < width }
        }
        val listEnd = zone?.first ?: (listWidth + QcSize.hairline)
        val detailStart = zone?.second ?: listEnd
        Row(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.width(listEnd - QcSize.hairline).fillMaxHeight()) { list() }
            Box(modifier = Modifier.width(QcSize.hairline).fillMaxHeight().background(QcTheme.colors.line))
            if (zone != null) Spacer(modifier = Modifier.width(detailStart - listEnd))
            Box(modifier = Modifier.weight(1f).fillMaxHeight()) { detail(max(width - detailStart, 0.dp)) }
        }
    }
}

/** Lays the child out at [design] width and scales it down as one object when the pane is narrower. */
fun Modifier.scaledFrom(design: Dp): Modifier = layout { measurable, constraints ->
    val designPx = design.roundToPx()
    if (constraints.maxWidth >= designPx) {
        val placeable = measurable.measure(constraints)
        return@layout layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
    val scale = constraints.maxWidth / designPx.toFloat()
    val placeable = measurable.measure(constraints.copy(minWidth = designPx, maxWidth = designPx, maxHeight = Int.MAX_VALUE))
    layout(constraints.maxWidth, (placeable.height * scale).roundToInt()) {
        placeable.placeWithLayer(0, 0) {
            scaleX = scale
            scaleY = scale
            transformOrigin = TransformOrigin(0f, 0f)
        }
    }
}

/** The selected tile in a list pane: a 2 gap, then a 2 ink ring, outside the tile's own bounds. */
fun Modifier.selectionRing(selected: Boolean, corner: Dp, ink: Color): Modifier =
    if (!selected) this else drawBehind {
        val ring = RING.toPx()
        val inset = GAP.toPx() + ring / 2
        val outline = RoundedCornerShape(corner + GAP + RING / 2)
            .createOutline(Size(size.width + inset * 2, size.height + inset * 2), layoutDirection, this)
        translate(-inset, -inset) { drawOutline(outline, ink, style = Stroke(ring)) }
    }

private val GAP = 2.dp
private val RING = 2.dp

/**
 * Add and Edit on medium and expanded windows: the form in a 560 surface centred over a scrim,
 * drawn in the app window so the lock screen still covers it.
 */
@Composable
fun FormPanel(content: @Composable () -> Unit) {
    val colors = QcTheme.colors
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.scrim)
            .pointerInput(Unit) { detectTapGestures { } }
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .imePadding()
            .padding(vertical = QcSpacing.s6),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = colors.bg,
            contentColor = colors.ink,
            shape = QcShapes.xl,
            shadowElevation = if (colors.dark) 0.dp else QcElevation.e3,
            modifier = Modifier
                .width(min(maxWidth - QcSpacing.s6 * 2, QcSize.dialogMaxWidth))
                .fillMaxHeight()
                .pointerInput(Unit) { detectTapGestures { } }
        ) {
            content()
        }
    }
}
