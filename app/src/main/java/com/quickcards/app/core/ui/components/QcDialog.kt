package com.quickcards.app.core.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.quickcards.app.core.ui.theme.QcDimens
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import com.quickcards.app.core.ui.LocalAppLocked
import com.quickcards.app.core.ui.trackAppLockInteraction

/**
 * The only dialog surface. The lock guard lives here: a dialog is a separate window and must
 * never be created while the lock screen covers the app.
 */
@Composable
fun QcDialog(
    onDismissRequest: () -> Unit,
    scrollState: ScrollState = rememberScrollState(),
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    if (LocalAppLocked.current) return
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val avoidAppBar = density.fontScale > 1.15f || configuration.screenHeightDp < 500
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        SecureOverlayWindow()
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .trackAppLockInteraction()
                .imePadding()
                .navigationBarsPadding()
                .then(
                    if (avoidAppBar) {
                        Modifier
                            .windowInsetsPadding(WindowInsets.statusBars)
                            .padding(top = 64.dp)
                    } else {
                        Modifier
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                shape = RoundedCornerShape(QcDimens.DialogCorner),
                tonalElevation = 0.dp,
                modifier = Modifier
                    .width(minOf(maxWidth - 48.dp, 360.dp))
                    .heightIn(max = maxHeight - 48.dp)
            ) {
                if (actions == null) {
                    Column(
                        modifier = Modifier
                            .verticalScroll(scrollState)
                            .padding(24.dp),
                        content = content
                    )
                } else {
                    // Actions stay pinned so Save is reachable with the keyboard open.
                    Column {
                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .verticalScroll(scrollState)
                                .padding(start = 24.dp, end = 24.dp, top = 24.dp),
                            content = content
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(start = 24.dp, end = 24.dp, top = QcDimens.DialogActionGap, bottom = 24.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.End),
                            verticalAlignment = Alignment.CenterVertically,
                            content = actions
                        )
                    }
                }
            }
        }
    }
}
