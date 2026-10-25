package com.quickcards.app.feature.lock

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quickcards.app.BuildConfig
import com.quickcards.app.R
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcType
import com.quickcards.app.core.ui.theme.qcPrimaryButtonColors
import kotlinx.coroutines.launch

private val EmphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)

/** In-window overlay so the lock covers the app in every orientation; swallows all input. */
@SuppressLint("ReturnFromAwaitPointerEventScope")
@Composable
fun LockScreen(onUnlock: () -> Unit) {
    BackHandler(enabled = true) {}
    val primary = MaterialTheme.colorScheme.primary
    val unlockLabel = stringResource(R.string.lock_unlock_description)

    // One authored moment: the mark settles in, then the copy and the action follow.
    val mark = remember { Animatable(0f) }
    val copy = remember { Animatable(0f) }
    val action = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        launch { mark.animateTo(1f, tween(420, easing = EmphasizedDecelerate)) }
        launch { copy.animateTo(1f, tween(360, delayMillis = 90, easing = EmphasizedDecelerate)) }
        launch { action.animateTo(1f, tween(360, delayMillis = 160, easing = EmphasizedDecelerate)) }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        val compact = maxHeight < 600.dp
        // A sibling layer, not a parent modifier: it blocks touches to the app underneath
        // without consuming the Unlock button's own taps.
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            awaitPointerEvent(PointerEventPass.Initial).changes.forEach { it.consume() }
                        }
                    }
                }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = QcDimens.Section, vertical = QcDimens.Section),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().weight(1f)) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier.graphicsLayer {
                            alpha = mark.value
                            val scale = 0.92f + 0.08f * mark.value
                            scaleX = scale
                            scaleY = scale
                        }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(if (compact) QcDimens.LockGlowCompact else QcDimens.LockGlow)
                                .background(
                                    Brush.radialGradient(
                                        listOf(primary.copy(alpha = 0.30f), Color.Transparent)
                                    ),
                                    CircleShape
                                )
                        )
                        Image(
                            painter = painterResource(R.drawable.qc_app_mark),
                            contentDescription = null,
                            modifier = Modifier.size(
                                if (compact) QcDimens.LockMarkCompact else QcDimens.LockMark
                            )
                        )
                    }
                    Spacer(modifier = Modifier.height(if (compact) QcDimens.Gutter else QcDimens.Section))
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.graphicsLayer {
                            alpha = copy.value
                            translationY = (1f - copy.value) * 12.dp.toPx()
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.lock_title),
                            style = if (compact) QcType.Headline else QcType.ScreenTitle,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(QcDimens.Base))
                        Text(
                            text = stringResource(R.string.lock_subtitle),
                            style = QcType.Body,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            Button(
                onClick = onUnlock,
                colors = qcPrimaryButtonColors(),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = QcDimens.LockButtonMaxWidth)
                    .height(QcDimens.LockButtonHeight)
                    .graphicsLayer {
                        alpha = action.value
                        translationY = (1f - action.value) * 12.dp.toPx()
                    }
                    .semantics { contentDescription = unlockLabel }
            ) {
                Icon(
                    imageVector = QcIcons.fingerprint,
                    contentDescription = null,
                    modifier = Modifier.size(QcDimens.Icon)
                )
                Spacer(modifier = Modifier.size(QcDimens.Base))
                Text(
                    text = stringResource(R.string.lock_unlock),
                    style = QcType.BodyLarge.copy(fontWeight = FontWeight(600))
                )
            }
            Spacer(modifier = Modifier.height(QcDimens.Section))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.graphicsLayer { alpha = action.value }
            ) {
                Icon(
                    imageVector = QcIcons.shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(QcDimens.IconTiny)
                )
                Text(
                    text = stringResource(R.string.lock_footer, BuildConfig.VERSION_NAME),
                    style = QcType.Label.copy(fontWeight = FontWeight(500)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
