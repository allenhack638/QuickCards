package com.quickcards.app.feature.backup

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.quickcards.app.R
import com.quickcards.app.core.ui.art.ArtKind
import com.quickcards.app.core.ui.art.CardArt
import com.quickcards.app.core.ui.components.FormTextField
import com.quickcards.app.core.ui.components.MarkFit
import com.quickcards.app.core.ui.components.NetworkMark
import com.quickcards.app.core.ui.components.QcChevron
import com.quickcards.app.core.ui.components.QcMask
import com.quickcards.app.core.ui.components.QcNestedTopBar
import com.quickcards.app.core.ui.components.QcSnackbarHost
import com.quickcards.app.core.ui.components.QcVisibilityToggle
import com.quickcards.app.core.ui.components.markTone
import com.quickcards.app.core.ui.theme.QcElevation
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcShapes
import com.quickcards.app.core.ui.theme.QcSize
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTheme

/**
 * A backup or spreadsheet step drawn over the app as a nested screen: bar, scrolling body,
 * pinned footer. System Back runs [onBack]. Its own snackbar host keeps file feedback visible.
 */
@Composable
internal fun BackupScreen(
    title: String,
    onBack: () -> Unit,
    state: BackupHostState,
    footer: @Composable () -> Unit,
    scrollable: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    BackHandler(onBack = onBack)
    var footerHeight by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(QcTheme.colors.bg)
            // Opaque to touch: the screen below must not react through this one.
            .pointerInput(Unit) { awaitPointerEventScope { while (true) awaitPointerEvent() } }
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            QcNestedTopBar(onBack = onBack, backLabel = stringResource(R.string.backup_back), title = title)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .then(if (scrollable) Modifier.verticalScroll(rememberScrollState()) else Modifier)
                    .padding(horizontal = QcSpacing.s4),
                content = content
            )
            Box(modifier = Modifier.onSizeChanged { footerHeight = it.height }) { footer() }
        }
        QcSnackbarHost(
            hostState = state.snackbar.host,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = with(density) { footerHeight.toDp() })
        )
    }
}

/** The muted line under a screen bar ("12 rows checked"), on the group-label edge. */
@Composable
internal fun LeadText(text: String, top: Dp = QcSpacing.s2) {
    Text(
        text = text,
        style = QcText.F2,
        color = QcTheme.colors.ink2,
        modifier = Modifier.padding(top = top)
    )
}

/** The three-step bar of the spreadsheet flow. */
@Composable
internal fun SheetSteps(step: Int, of: Int = SHEET_STEPS) {
    val colors = QcTheme.colors
    val label = stringResource(R.string.import_a11y_steps, step, of)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = QcSpacing.s2, bottom = QcSpacing.s4)
            .semantics {
                contentDescription = label
                role = Role.Image
            },
        horizontalArrangement = Arrangement.spacedBy(QcSpacing.s1)
    ) {
        repeat(of) { index ->
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(QcSpacing.s1)
                    .background(if (index < step) colors.ink else colors.tonal2, QcShapes.full)
            )
        }
    }
}

private const val SHEET_STEPS = 3

/**
 * Password field with show/hide and a live rule line under it: a neutral dot in `ink-2` until
 * the rule is met, then a check and the met text in `ok`.
 */
@Composable
internal fun BackupPasswordField(
    value: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit,
    label: String,
    visible: Boolean,
    onToggleVisible: () -> Unit,
    modifier: Modifier = Modifier,
    error: String? = null,
    rule: String? = null,
    ruleMet: Boolean = false,
    focusRequester: FocusRequester? = null,
    imeAction: ImeAction = ImeAction.Done,
    onImeAction: () -> Unit = {}
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(QcSpacing.s1)) {
        FormTextField(
            value = value,
            onValueChange = onValueChange,
            label = label,
            error = error,
            focusRequester = focusRequester,
            visualTransformation = if (visible) VisualTransformation.None else QcMask,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                autoCorrectEnabled = false,
                imeAction = imeAction
            ),
            keyboardActions = KeyboardActions(onAny = { onImeAction() }),
            trailing = {
                QcVisibilityToggle(
                    visible = visible,
                    onToggle = onToggleVisible,
                    showLabel = stringResource(R.string.backup_a11y_show_password),
                    hideLabel = stringResource(R.string.backup_a11y_hide_password)
                )
            }
        )
        if (rule != null) PasswordRule(rule, ruleMet)
    }
}

@Composable
private fun PasswordRule(text: String, met: Boolean) {
    val colors = QcTheme.colors
    val tint = if (met) colors.ok else colors.ink2
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(QcSpacing.s1)) {
        Box(modifier = Modifier.size(QcSize.iconXs), contentAlignment = Alignment.Center) {
            if (met) {
                Icon(QcIcons.check, contentDescription = null, tint = tint, modifier = Modifier.size(QcSize.iconXs))
            } else {
                Box(modifier = Modifier.size(QcSpacing.s1).background(tint, QcShapes.full))
            }
        }
        Text(text = text, style = QcText.F1Regular, color = tint)
    }
}

/** A row whose supporting text wraps: icon and trailing align to the first line. */
@Composable
internal fun WrapRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    iconTint: Color = QcTheme.colors.ink2,
    onClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = QcTheme.colors
    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = QcSize.row2)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = QcSpacing.s4, vertical = QcSpacing.s3),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(QcSpacing.s4)
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.padding(top = QcSpacing.s1).size(QcSize.iconLg))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = QcText.F3, color = colors.ink)
            Text(text = subtitle, style = QcText.F2, color = colors.ink2)
        }
        if (trailing != null) Box(modifier = Modifier.padding(top = QcSpacing.s1)) { trailing() }
    }
}

/** The picked file in the import dialog: one-line name, its action on the right. */
@Composable
internal fun BackupFileRow(title: String, icon: ImageVector, subtitle: String, action: @Composable () -> Unit) {
    val colors = QcTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = QcSize.row2)
            .padding(top = QcSpacing.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(QcSpacing.s4)
    ) {
        Icon(icon, contentDescription = null, tint = colors.ink2, modifier = Modifier.size(QcSize.iconLg))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = QcText.F3, color = colors.ink, maxLines = 1, overflow = TextOverflow.Ellipsis)
            Text(text = subtitle, style = QcText.F2, color = colors.ink2, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        action()
    }
}

/** A count row on the check screen: status icon, label, the number on the right. */
@Composable
internal fun CountRow(icon: ImageVector, title: String, count: Int, iconTint: Color = QcTheme.colors.ink2) {
    val colors = QcTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = QcSize.row)
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = QcSpacing.s4, vertical = QcSpacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(QcSpacing.s4)
    ) {
        Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(QcSize.iconLg))
        Text(text = title, style = QcText.F3, color = colors.ink, modifier = Modifier.weight(1f))
        Text(text = count.toString(), style = QcText.F3.copy(fontFeatureSettings = "tnum"), color = colors.ink)
    }
}

/** A row in a dialog or sheet whose lead is a 40dp source tile. */
@Composable
internal fun SourceRow(
    title: String,
    subtitle: String?,
    onClick: () -> Unit,
    lead: @Composable () -> Unit
) {
    val colors = QcTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = if (subtitle == null) QcSize.row else QcSize.row2)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = QcSpacing.s6, vertical = QcSpacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(QcSpacing.s4)
    ) {
        lead()
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = QcText.F3, color = colors.ink)
            if (subtitle != null) Text(text = subtitle, style = QcText.F2, color = colors.ink2)
        }
        QcChevron()
    }
}

/** The QuickCards mark on its brand tile, lifted by a soft red shadow. */
@Composable
internal fun BackupSourceTile() {
    Image(
        painter = painterResource(R.drawable.qc_app_mark),
        contentDescription = null,
        modifier = Modifier
            .size(QcSpacing.s10)
            .shadow(QcElevation.e2, QcShapes.md, ambientColor = QcTheme.colors.brand, spotColor = QcTheme.colors.brand)
    )
}

/** The spreadsheet source: a table glyph in `ok` on a tonal tile with a hairline ring. */
@Composable
internal fun SheetSourceTile() {
    val colors = QcTheme.colors
    Box(
        modifier = Modifier
            .size(QcSpacing.s10)
            .background(colors.tonal, QcShapes.md)
            .border(QcSize.hairline, colors.line, QcShapes.md),
        contentAlignment = Alignment.Center
    ) {
        Icon(QcIcons.table, contentDescription = null, tint = colors.ok, modifier = Modifier.size(QcSize.iconLg))
    }
}

/** The imported cards stacked in their own colours, the newest in front. */
@Composable
internal fun ImportedFan(cards: List<FanCard>) {
    if (cards.isEmpty()) return
    val dark = QcTheme.colors.dark
    Box(modifier = Modifier.size(FanCardWidth + QcSpacing.s4, FanCardHeight + QcSpacing.s4)) {
        cards.reversed().forEachIndexed { index, card ->
            val step = QcSpacing.s2 * index
            CardArt(
                band = card.band,
                kind = ArtKind.SWATCH,
                modifier = Modifier
                    .offset(x = step, y = step)
                    .size(FanCardWidth, FanCardHeight)
            ) {
                val network = card.networkId
                if (network != null) {
                    NetworkMark(
                        networkId = network,
                        contentDescription = null,
                        tone = markTone(network, onArt = true, dark = dark),
                        fit = MarkFit.SLOT,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(end = QcSpacing.s2, bottom = QcSpacing.s1)
                    )
                }
            }
        }
    }
}

private val FanCardWidth = QcSpacing.s16 * 2
private val FanCardHeight = QcSpacing.s16 + QcSpacing.s4

/** A result screen's centre: the fan, one big line, an optional quiet line. */
@Composable
internal fun ResultHeading(fan: List<FanCard>, title: String, detail: String?, top: Dp = QcSpacing.s12) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = top),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ImportedFan(fan)
        Text(
            text = title,
            style = QcText.F5,
            color = QcTheme.colors.ink,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = if (fan.isEmpty()) QcSpacing.s4 else QcSpacing.s8)
        )
        if (detail != null) {
            Text(
                text = detail,
                style = QcText.F2,
                color = QcTheme.colors.ink2,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = QcSpacing.s1)
            )
        }
    }
}
