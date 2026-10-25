package com.quickcards.app.core.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import com.quickcards.app.core.ui.LocalAppLocked
import com.quickcards.app.core.ui.trackAppLockInteraction

/**
 * The only bottom sheet in the app. The app-lock guard is built in so a sheet
 * window can never outlive the lock screen; call sites only decide when to open.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QcModalBottomSheet(
    onDismissRequest: () -> Unit,
    sheetState: SheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainer,
    modifier: Modifier = Modifier,
    windowInsets: WindowInsets = BottomSheetDefaults.windowInsets,
    content: @Composable ColumnScope.() -> Unit
) {
    if (LocalAppLocked.current) return
    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        modifier = modifier.trackAppLockInteraction(),
        sheetState = sheetState,
        containerColor = containerColor,
        windowInsets = windowInsets,
        content = content
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
