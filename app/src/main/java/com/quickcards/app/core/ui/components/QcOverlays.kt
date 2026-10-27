package com.quickcards.app.core.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.runtime.getValue
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ModalBottomSheetProperties
import androidx.compose.material3.SheetState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import androidx.compose.ui.window.SecureFlagPolicy
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
    // Tap-outside must reach Material's scrim: keep the sheet shell's inputs stable (its scrim
    // re-keys its tap detector on recomposition) and track lock activity inside the content,
    // not on the sheet modifier, where it swallows scrim taps on tall sheets.
    val latestDismiss by rememberUpdatedState(onDismissRequest)
    val latestContent by rememberUpdatedState(content)
    val dismiss = remember { { latestDismiss() } }
    val stableContent: @Composable ColumnScope.() -> Unit = remember {
        {
            Column(modifier = Modifier.fillMaxWidth().trackAppLockInteraction()) {
                SecureOverlayWindow()
                latestContent()
            }
        }
    }
    ModalBottomSheet(
        onDismissRequest = dismiss,
        modifier = modifier,
        sheetState = sheetState,
        containerColor = containerColor,
        dragHandle = { QcSheetDragHandle() },
        contentWindowInsets = { BottomSheetDefaults.windowInsets.union(WindowInsets.ime) },
        properties = ModalBottomSheetProperties(securePolicy = SecureFlagPolicy.Inherit),
        content = stableContent
    )
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
        content = {
            SecureOverlayWindow()
            content()
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
