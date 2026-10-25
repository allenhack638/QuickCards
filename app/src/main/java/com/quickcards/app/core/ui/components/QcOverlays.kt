package com.quickcards.app.core.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.quickcards.app.core.ui.LocalAppLocked
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.trackAppLockInteraction

/** The only bottom sheet; its built-in lock guard means a sheet never outlives the lock. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QcModalBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    if (LocalAppLocked.current) return
    // Read in the activity window: the sheet's own popup window does not receive
    // navigation-bar insets on older Android, so reading them inside gives 0.
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val imeBottom = WindowInsets.ime.asPaddingValues().calculateBottomPadding()
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier.trackAppLockInteraction(),
        sheetState = sheetState,
        containerColor = containerColor,
        windowInsets = WindowInsets.ime,
        dragHandle = { QcSheetDragHandle() }
    ) {
        content()
        // Last row clears the navigation bar; with the keyboard up, the sheet already sits above it.
        Spacer(modifier = Modifier.height((navBottom - imeBottom).coerceAtLeast(0.dp)))
    }
}

/** Compact handle (16 + 4 + 8dp) so a sheet's top spacing matches its bottom. */
@Composable
private fun QcSheetDragHandle() {
    Box(
        modifier = Modifier
            .padding(top = QcDimens.SheetHandleTop, bottom = QcDimens.SheetHandleBottom)
            .size(width = QcDimens.SheetHandleWidth, height = QcDimens.SheetHandleHeight)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
    )
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
    DropdownMenu(
        expanded = true,
        onDismissRequest = onDismissRequest,
        modifier = modifier.trackAppLockInteraction(),
        offset = offset,
        scrollState = scrollState,
        properties = properties,
        content = content
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
        Box(modifier = Modifier.trackAppLockInteraction()) {
            content()
        }
    }
}
