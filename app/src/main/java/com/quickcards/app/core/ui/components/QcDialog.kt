package com.quickcards.app.core.ui.components

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import com.quickcards.app.core.ui.LocalAppLocked
import com.quickcards.app.core.ui.theme.LocalQcRaised
import com.quickcards.app.core.ui.theme.QcElevation
import com.quickcards.app.core.ui.theme.QcShapes
import com.quickcards.app.core.ui.theme.QcSize
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTheme
import com.quickcards.app.core.ui.theme.qcRaisedRing
import com.quickcards.app.core.ui.trackAppLockInteraction

/** The dialog's own content inset (title, body and actions all sit inside it). */
val QcDialogPadding = QcSpacing.s6

/**
 * The only dialog surface. The lock guard lives here: a dialog is a separate window and must
 * never be created while the lock screen covers the app.
 *
 * Layout: 24 padding · [title] · 16 · body · 24 · text actions on the right; no icon. With a
 * [title] and [actions], both stay fixed and only the body scrolls (hairlines show while it
 * can). A destructive confirm uses `QcTextButton(tone = QcTextTone.Danger)`.
 */
@Composable
fun QcDialog(
    onDismissRequest: () -> Unit,
    scrollState: ScrollState = rememberScrollState(),
    title: String? = null,
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null
) = DialogWindow(onDismissRequest, scrollState, title?.let(::AnnotatedString), actions, content)

/** A [QcDialog] whose title carries spans (the masked card number). */
@Composable
fun QcDialog(
    onDismissRequest: () -> Unit,
    title: AnnotatedString,
    scrollState: ScrollState = rememberScrollState(),
    actions: (@Composable RowScope.() -> Unit)? = null,
    content: (@Composable ColumnScope.() -> Unit)? = null
) = DialogWindow(onDismissRequest, scrollState, title, actions, content)

@Composable
private fun DialogWindow(
    onDismissRequest: () -> Unit,
    scrollState: ScrollState,
    title: AnnotatedString?,
    actions: (@Composable RowScope.() -> Unit)?,
    content: (@Composable ColumnScope.() -> Unit)?
) {
    if (LocalAppLocked.current) return
    val colors = QcTheme.colors
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false
        )
    ) {
        SecureOverlayWindow()
        val view = LocalView.current
        SideEffect {
            (view.parent as? DialogWindowProvider)?.window?.setDimAmount(colors.scrim.alpha)
        }
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .trackAppLockInteraction()
                .imePadding()
                .windowInsetsPadding(WindowInsets.safeDrawing)
                .padding(vertical = QcSpacing.s6),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                color = colors.surfaceHi,
                contentColor = colors.ink,
                shape = QcShapes.xl,
                tonalElevation = 0.dp,
                shadowElevation = if (colors.dark) 0.dp else QcElevation.e3,
                modifier = Modifier
                    .width(minOf(maxWidth - QcSpacing.s6 * 2, QcSize.dialogMaxWidth))
                    .heightIn(max = maxHeight)
                    .qcRaisedRing(colors, QcShapes.xl)
            ) {
                CompositionLocalProvider(LocalQcRaised provides true) {
                    DialogLayout(scrollState, title, actions, content)
                }
            }
        }
    }
}

@Composable
private fun DialogLayout(
    scrollState: ScrollState,
    title: AnnotatedString?,
    actions: (@Composable RowScope.() -> Unit)?,
    content: (@Composable ColumnScope.() -> Unit)?
) {
    if (actions == null && title == null) {
        Column(
            modifier = Modifier
                .verticalScroll(scrollState)
                .padding(QcDialogPadding),
            content = content ?: {}
        )
        return
    }
    val line = QcTheme.colors.line
    Column(modifier = Modifier.padding(top = QcDialogPadding)) {
        if (title != null) {
            Text(
                text = title,
                style = QcText.F4.copy(lineBreak = LineBreak.Heading),
                color = QcTheme.colors.ink,
                modifier = Modifier
                    .padding(horizontal = QcDialogPadding)
                    .padding(bottom = if (content != null) QcSpacing.s4 else 0.dp)
                    .semantics { heading() }
            )
        }
        // The body scrolls between a fixed title and fixed actions so Save stays reachable.
        if (content != null) Column(
            modifier = Modifier
                .weight(1f, fill = false)
                .drawWithContent {
                    drawContent()
                    val stroke = QcSize.hairline.toPx()
                    if (scrollState.value > 0) {
                        drawLine(line, Offset.Zero, Offset(size.width, 0f), stroke)
                    }
                    if (scrollState.value < scrollState.maxValue) {
                        drawLine(line, Offset(0f, size.height), Offset(size.width, size.height), stroke)
                    }
                }
                .verticalScroll(scrollState)
                .padding(horizontal = QcDialogPadding),
            content = content
        )
        if (actions != null) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = QcDialogPadding,
                        end = QcDialogPadding - QcSpacing.s3,
                        top = QcSpacing.s6 - TextButtonInset,
                        bottom = QcDialogPadding - TextButtonInset
                    ),
                horizontalArrangement = Arrangement.spacedBy(QcSpacing.s2, Alignment.End),
                verticalAlignment = Alignment.CenterVertically,
                content = actions
            )
        } else {
            Spacer(Modifier.height(QcDialogPadding))
        }
    }
}

/** A text button's 36dp face sits centred in its 48dp touch target. */
private val TextButtonInset = (QcSize.hit - QcSize.ctlMd) / 2

/**
 * Lets a row inside a dialog run from edge to edge of the dialog surface, so a tappable
 * list row and its ripple span the full width while its content keeps the 24dp inset.
 */
fun Modifier.dialogEdgeToEdge(): Modifier = layout { measurable, constraints ->
    val bleed = (QcDialogPadding * 2).roundToPx()
    val placeable = measurable.measure(
        constraints.copy(
            minWidth = (constraints.minWidth + bleed).coerceAtMost(constraints.maxWidth + bleed),
            maxWidth = constraints.maxWidth + bleed
        )
    )
    layout(placeable.width - bleed, placeable.height) {
        placeable.placeRelative(-bleed / 2, 0)
    }
}
