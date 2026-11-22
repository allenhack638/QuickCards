package com.quickcards.app.feature.lock

import android.annotation.SuppressLint
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.quickcards.app.BuildConfig
import com.quickcards.app.R
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcElevation
import com.quickcards.app.core.ui.theme.QcFilledButton
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcShapes
import com.quickcards.app.core.ui.theme.QcSize
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTheme

/** In-window overlay so the lock covers the app in every orientation; swallows all input. */
@SuppressLint("ReturnFromAwaitPointerEventScope")
@Composable
fun LockScreen(onUnlock: () -> Unit) {
    BackHandler(enabled = true) {}
    val colors = QcTheme.colors
    val unlockLabel = stringResource(R.string.lock_unlock_description)
    val lockedLabel = stringResource(R.string.lock_a11y_title)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.bg)
    ) {
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
        val compact = maxHeight < QcDimens.CompactHeight
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .heightIn(min = maxHeight)
                    .padding(start = QcSpacing.s6, end = QcSpacing.s6, bottom = QcSpacing.s10),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    LockMark(
                        modifier = Modifier.padding(
                            top = if (compact) QcDimens.LockMarkTopCompact else QcDimens.LockMarkTop
                        )
                    )
                    Text(
                        text = stringResource(R.string.lock_title),
                        style = QcText.F4,
                        color = colors.ink,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .padding(top = QcSpacing.s6)
                            .semantics {
                                heading()
                                contentDescription = lockedLabel
                            }
                    )
                }
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(top = QcSpacing.s8)
                ) {
                    QcFilledButton(
                        text = stringResource(R.string.lock_unlock),
                        onClick = onUnlock,
                        large = true,
                        icon = QcIcons.fingerprint,
                        modifier = Modifier.semantics { contentDescription = unlockLabel }
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(QcSpacing.s1),
                        modifier = Modifier.padding(top = QcSpacing.s6)
                    ) {
                        Icon(
                            imageVector = QcIcons.shield,
                            contentDescription = null,
                            tint = colors.ink2,
                            modifier = Modifier.size(QcSize.iconXs)
                        )
                        Text(
                            text = stringResource(R.string.lock_footer),
                            style = QcText.F1Regular,
                            color = colors.ink2
                        )
                    }
                    Text(
                        text = stringResource(R.string.settings_version_value, BuildConfig.VERSION_NAME),
                        style = QcText.MonoF1,
                        color = colors.ink2,
                        modifier = Modifier.padding(top = QcSpacing.s1)
                    )
                }
            }
        }
    }
}

@Composable
private fun LockMark(modifier: Modifier = Modifier) {
    val colors = QcTheme.colors
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(QcDimens.LockMark)
            .shadow(
                elevation = QcElevation.e3,
                shape = QcShapes.lg,
                ambientColor = colors.brand,
                spotColor = colors.brand
            )
            .background(colors.brand, QcShapes.lg)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_stat_quickcards),
            contentDescription = null,
            tint = colors.onBrand,
            modifier = Modifier.size(QcDimens.LockGlyph)
        )
    }
}
