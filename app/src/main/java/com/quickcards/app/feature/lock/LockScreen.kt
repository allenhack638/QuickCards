package com.quickcards.app.feature.lock

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.quickcards.app.R
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcType

/**
 * Full-window app lock from prototype/v2.html .lockscr: app mark, title,
 * fingerprint disc, Use PIN, shield footer. No reason line. Rendered as an
 * in-window overlay so it covers the app in every orientation and system
 * configuration.
 */
@SuppressLint("ReturnFromAwaitPointerEventScope")
@Composable
fun LockScreen(onUnlock: () -> Unit) {
    BackHandler(enabled = true) {}
    val background = MaterialTheme.colorScheme.background
    val primary = MaterialTheme.colorScheme.primary
    val unlockLabel = stringResource(R.string.lock_unlock_description)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(background)
    ) {
        val compact = maxHeight < 600.dp
        Box(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(Unit) {
                    awaitPointerEventScope {
                        while (true) {
                            val event = awaitPointerEvent(PointerEventPass.Initial)
                            event.changes.forEach { it.consume() }
                        }
                    }
                }
        )
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(
                    start = QcDimens.Xl,
                    end = QcDimens.Xl,
                    top = if (compact) 12.dp else QcDimens.DialogCorner,
                    bottom = if (compact) 12.dp else 40.dp
                ),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .verticalScroll(rememberScrollState())
                        .heightIn(min = maxHeight),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Box(
                            modifier = Modifier
                                .size(
                                    if (compact) {
                                        QcDimens.LockGlowCompact
                                    } else {
                                        QcDimens.LockGlow
                                    }
                                )
                                .background(
                                    Brush.radialGradient(
                                        listOf(
                                            primary.copy(alpha = 0.28f),
                                            Color.Transparent
                                        )
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
                    Spacer(modifier = Modifier.height(if (compact) 12.dp else QcDimens.DialogCorner))
                    Text(
                        text = stringResource(R.string.lock_title),
                        style = if (compact) QcType.Headline else QcType.ScreenTitle,
                        color = MaterialTheme.colorScheme.onSurface,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(if (compact) QcDimens.Gutter else 64.dp))
                    Box(
                        modifier = Modifier
                            .size(
                                if (compact) QcDimens.LockDiscCompact else QcDimens.LockDisc
                            )
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .border(
                                width = QcDimens.Divider,
                                color = MaterialTheme.colorScheme.outlineVariant,
                                shape = CircleShape
                            )
                            .clickable(
                                role = Role.Button,
                                onClickLabel = unlockLabel,
                                onClick = onUnlock
                            )
                            .semantics {
                                contentDescription = unlockLabel
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = QcIcons.fingerprint,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.size(if (compact) 36.dp else 44.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(if (compact) QcDimens.Base else 14.dp))
                    Text(
                        text = stringResource(R.string.lock_touch_to_unlock),
                        style = QcType.Body.copy(fontWeight = FontWeight(600)),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(if (compact) 0.dp else QcDimens.Base))
                    TextButton(onClick = onUnlock) {
                        Text(
                            text = stringResource(R.string.lock_use_pin),
                            style = QcType.Body.copy(fontWeight = FontWeight(600)),
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = QcIcons.shield,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(QcDimens.IconTiny)
                )
                Text(
                    text = stringResource(R.string.lock_footer),
                    style = QcType.Label.copy(fontWeight = FontWeight(500)),
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
