package com.quickcards.app.core.ui.components

import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.DisposableEffect
import androidx.activity.findViewTreeOnBackPressedDispatcherOwner
import androidx.activity.compose.LocalOnBackPressedDispatcherOwner
import androidx.activity.compose.BackHandler
import android.window.OnBackInvokedDispatcher
import android.window.OnBackInvokedCallback
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.quickcards.app.core.ui.LocalAppLocked
import com.quickcards.app.core.ui.theme.LocalQcRaised
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcElevation
import com.quickcards.app.core.ui.theme.QcFilledButton
import com.quickcards.app.core.ui.theme.QcOutlinedButton
import com.quickcards.app.core.ui.theme.QcShapes
import com.quickcards.app.core.ui.theme.QcSize
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTheme
import com.quickcards.app.core.ui.theme.QcTonalButton
import com.quickcards.app.core.ui.trackAppLockInteraction

/** The only bottom sheet; its built-in lock guard means a sheet never outlives the lock. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QcModalBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor: Color = QcTheme.colors.surfaceHi,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    if (LocalAppLocked.current) return
    // Tap-outside must reach Material's scrim: keep the sheet shell's inputs stable (its scrim
    // re-keys its tap detector on recomposition) and track lock activity inside the content,
    // not on the sheet modifier, where it swallows scrim taps on tall sheets.
    val latestDismiss by rememberUpdatedState(onDismissRequest)
    val latestContent by rememberUpdatedState(content)
    val dismiss = remember { { latestDismiss() } }
    val stableContent: @Composable ColumnScope.() -> Unit = remember {
        {
            CompositionLocalProvider(LocalQcRaised provides true) {
                Column(modifier = Modifier.fillMaxWidth().trackAppLockInteraction()) {
                    SecureOverlayWindow()
                    latestContent()
                }
            }
        }
    }
    val statusBarTop = WindowInsets.statusBars.getTop(LocalDensity.current)
    ModalBottomSheet(
        onDismissRequest = dismiss,
        modifier = modifier.belowStatusBar(statusBarTop),
        sheetState = sheetState,
        shape = QcShapes.sheet,
        containerColor = containerColor,
        contentColor = QcTheme.colors.ink,
        tonalElevation = 0.dp,
        scrimColor = QcTheme.colors.scrim,
        dragHandle = { QcSheetDragHandle() },
        contentWindowInsets = { BottomSheetDefaults.windowInsets.union(WindowInsets.ime) },
        properties = ModalBottomSheetProperties(),
        content = stableContent
    )
}

/**
 * A tall sheet stops [QcSpacing.s6] below the status bar, so its handle never sits under the clock
 * (Android 8 lets the sheet window run under it) and the scrim shows above it.
 */
private fun Modifier.belowStatusBar(statusBarTop: Int): Modifier = layout { measurable, constraints ->
    if (!constraints.hasBoundedHeight) {
        val placeable = measurable.measure(constraints)
        return@layout layout(placeable.width, placeable.height) { placeable.place(0, 0) }
    }
    // The sheet anchors itself inside the height it is given, so give it less and move it down.
    val reserve = (statusBarTop + QcSpacing.s6.roundToPx()).coerceAtMost(constraints.maxHeight)
    val placeable = measurable.measure(
        constraints.copy(minHeight = 0, maxHeight = constraints.maxHeight - reserve)
    )
    layout(placeable.width, constraints.maxHeight) { placeable.place(0, reserve) }
}

/**
 * Back inside a [QcModalBottomSheet] (a page within the sheet going back to the first): the
 * sheet lives in its own window, so the activity's BackHandler never sees it. Registered on the
 * sheet window after the sheet's own dismiss callback, so it wins while [enabled]; the keyboard,
 * registered later still, keeps closing first.
 */
@Composable
fun QcSheetBackHandler(enabled: Boolean, onBack: () -> Unit) {
    val view = LocalView.current
    val latest by rememberUpdatedState(onBack)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        DisposableEffect(view, enabled) {
            val dispatcher = view.findOnBackInvokedDispatcher()
            if (!enabled || dispatcher == null) return@DisposableEffect onDispose { }
            val callback = OnBackInvokedCallback { latest() }
            dispatcher.registerOnBackInvokedCallback(OnBackInvokedDispatcher.PRIORITY_DEFAULT, callback)
            onDispose { dispatcher.unregisterOnBackInvokedCallback(callback) }
        }
    } else {
        val owner = remember(view) { view.findViewTreeOnBackPressedDispatcherOwner() } ?: return
        CompositionLocalProvider(LocalOnBackPressedDispatcherOwner provides owner) {
            BackHandler(enabled = enabled) { latest() }
        }
    }
}

/** Handle zone: 12 above, 32 × 4 bar, 8 below. */
@Composable
private fun QcSheetDragHandle() {
    Box(
        modifier = Modifier
            .padding(top = QcDimens.SheetHandleTop, bottom = QcDimens.SheetHandleBottom)
            .size(width = QcDimens.SheetHandleWidth, height = QcDimens.SheetHandleHeight)
            .clip(CircleShape)
            .background(QcTheme.colors.outline.copy(alpha = 0.5f))
    )
}

/**
 * Sheet footer, 24 below the content and 16 above the gesture area. A lone [primary] is a
 * full-width tonal button (Done); with a [secondary] (Cancel, outlined) the primary is filled
 * brand. [note] names what a disabled primary is missing, in one line above it. [primaryIcon]
 * leads the primary's label (Choose file, Delete file). [stackWhenTight] stacks the pair, primary on
 * top, only when either label would not fit its half on one line (large text).
 */
@Composable
fun QcSheetActions(
    primary: String?,
    onPrimary: () -> Unit,
    modifier: Modifier = Modifier,
    secondary: String? = null,
    onSecondary: () -> Unit = {},
    primaryEnabled: Boolean = true,
    filled: Boolean = secondary != null,
    note: String? = null,
    stack: Boolean = false,
    sideInset: Dp = QcSpacing.s6,
    primaryIcon: ImageVector? = null,
    stackWhenTight: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = sideInset, end = sideInset, top = QcSpacing.s6, bottom = QcSpacing.s4),
        verticalArrangement = Arrangement.spacedBy(QcSpacing.s2)
    ) {
        if (note != null) {
            Text(
                text = note,
                style = QcText.F2,
                color = QcTheme.colors.ink2,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        val primaryButton: @Composable (Modifier) -> Unit = { buttonModifier ->
            if (primary != null) {
                if (filled) {
                    QcFilledButton(primary, onPrimary, buttonModifier, primaryEnabled, large = true, icon = primaryIcon)
                } else {
                    QcTonalButton(primary, onPrimary, buttonModifier, primaryEnabled, large = true, icon = primaryIcon)
                }
            }
        }
        val secondaryButton: @Composable (Modifier) -> Unit = { buttonModifier ->
            if (secondary != null) QcOutlinedButton(secondary, onSecondary, buttonModifier, large = true)
        }
        if (stack) {
            primaryButton(Modifier.fillMaxWidth())
            secondaryButton(Modifier.fillMaxWidth())
        } else if (stackWhenTight && secondary != null) {
            SideBySideOrStacked(first = { secondaryButton(Modifier) }, second = { primaryButton(Modifier) })
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(QcSpacing.s3)) {
                secondaryButton(Modifier.weight(1f))
                primaryButton(Modifier.weight(1f))
            }
        }
    }
}

/** [first] and [second] in equal halves, or stacked full width with [second] on top when either needs more than its half. */
@Composable
private fun SideBySideOrStacked(first: @Composable () -> Unit, second: @Composable () -> Unit) {
    Layout(contents = listOf(first, second)) { (firstItems, secondItems), constraints ->
        val gap = QcSpacing.s3.roundToPx()
        val stackGap = QcSpacing.s2.roundToPx()
        val half = (constraints.maxWidth - gap) / 2
        val a = firstItems.single()
        val b = secondItems.single()
        val fits = a.maxIntrinsicWidth(Constraints.Infinity) <= half && b.maxIntrinsicWidth(Constraints.Infinity) <= half
        if (fits) {
            val pa = a.measure(Constraints.fixedWidth(half))
            val pb = b.measure(Constraints.fixedWidth(half))
            layout(constraints.maxWidth, maxOf(pa.height, pb.height)) {
                pa.place(0, 0)
                pb.place(half + gap, 0)
            }
        } else {
            val pb = b.measure(Constraints.fixedWidth(constraints.maxWidth))
            val pa = a.measure(Constraints.fixedWidth(constraints.maxWidth))
            layout(constraints.maxWidth, pb.height + stackGap + pa.height) {
                pb.place(0, 0)
                pa.place(0, pb.height + stackGap)
            }
        }
    }
}

/** The only dropdown menu in the app, with the same built-in lock guard. */
@Composable
fun QcDropdownMenu(
    expanded: Boolean,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    offset: DpOffset = DpOffset(0.dp, 0.dp),
    scrollState: ScrollState = rememberScrollState(),
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable ColumnScope.() -> Unit
) {
    if (!expanded || LocalAppLocked.current) return
    val colors = QcTheme.colors
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismissRequest,
        modifier = modifier.trackAppLockInteraction(),
        offset = offset,
        scrollState = scrollState,
        properties = properties,
        shape = QcShapes.md,
        containerColor = colors.surfaceHi,
        tonalElevation = 0.dp,
        shadowElevation = if (colors.dark) 0.dp else QcElevation.e2,
        border = if (colors.dark) BorderStroke(QcSize.hairline, Color.White.copy(alpha = 0.06f)) else null,
        content = {
            SecureOverlayWindow()
            CompositionLocalProvider(LocalQcRaised provides true) { content() }
        }
    )
}

/** The only popup window in the app, with the same built-in lock guard. */
@Composable
fun QcPopup(
    popupPositionProvider: PopupPositionProvider,
    onDismissRequest: () -> Unit,
    properties: PopupProperties = PopupProperties(focusable = true),
    content: @Composable () -> Unit
) {
    if (LocalAppLocked.current) return
    Popup(
        popupPositionProvider = popupPositionProvider,
        onDismissRequest = onDismissRequest,
        properties = properties
    ) {
        SecureOverlayWindow()
        Box(modifier = Modifier.trackAppLockInteraction()) {
            content()
        }
    }
}
