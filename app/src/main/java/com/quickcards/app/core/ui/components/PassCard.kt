package com.quickcards.app.core.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalViewConfiguration
import androidx.compose.ui.platform.ViewConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.onLongClick
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.quickcards.app.R
import com.quickcards.app.core.security.UnlockSession
import com.quickcards.app.core.ui.HomeCard
import com.quickcards.app.core.ui.theme.ExpiredPassColors
import com.quickcards.app.core.ui.theme.PassColors
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcType
import com.quickcards.domain.CardNumberRules
import com.quickcards.domain.ExpiryState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

enum class PassField { NUMBER, EXPIRY, CVV, NAME }

private val FieldShape = RoundedCornerShape(QcDimens.PassFieldCopyTarget)
private val FieldValueSpacingMasked = 0.18.em

private val TypePillStyle = TextStyle(
    fontSize = 10.sp,
    lineHeight = 14.sp,
    fontWeight = FontWeight(600),
    letterSpacing = 0.08.em
)

private val ExpiredChipStyle = QcType.Label.copy(
    fontSize = 10.sp,
    lineHeight = 14.sp,
    fontWeight = FontWeight(700),
    letterSpacing = 0.06.em
)

@OptIn(ExperimentalFoundationApi::class, ExperimentalLayoutApi::class)
@Composable
fun PassCard(
    card: HomeCard,
    maskNumbers: Boolean,
    showCardType: Boolean,
    tagsShow: Boolean,
    modifier: Modifier = Modifier,
    onCopy: suspend (PassField) -> Boolean = { false },
    onReveal: suspend (PassField) -> Boolean = { false },
    onActions: () -> Unit = {},
    preview: Boolean = false,
    compact: Boolean = false,
    expiryState: ExpiryState? = null,
    needsReview: Boolean = false,
    archivedLabel: String? = null,
    note: String? = null,
    showNote: Boolean = false,
    onCopyNote: suspend () -> Boolean = { false }
) {
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val expired = expiryState == ExpiryState.EXPIRED
    val expiredColors = PassColors.expired(dark)
    val brandBand = PassColors.band(card.card.bandColor)
    val band = if (expired) expiredColors.band else brandBand
    val body = if (expired) expiredColors.body else PassColors.bodyTint(brandBand, dark)
    val edge = if (expired) expiredColors.edge else PassColors.edge(brandBand, dark)
    val noteText = note?.takeIf { showNote && it.isNotBlank() }
    val density = LocalDensity.current
    val envelope = LocalViewConfiguration.current
    val isUnlocked by UnlockSession.isUnlocked.collectAsState()

    val showArchived = !archivedLabel.isNullOrBlank()
    val showExpired = !showArchived && expired
    val showSoon = !showArchived && !showExpired &&
        expiryState == ExpiryState.EXPIRES_SOON
    val showReview = !showArchived && !showExpired && !showSoon && needsReview

    var notchY by remember { mutableFloatStateOf(0f) }
    val cornerPx = with(density) { QcDimens.PassCorner.toPx() }
    val notchRadiusPx = with(density) { QcDimens.PassNotch.toPx() }
    val effectiveNotchY = if (notchY > 0f) notchY else with(density) { 124.dp.toPx() }
    val roundedShape = remember { RoundedCornerShape(QcDimens.PassCorner) }
    val shape: Shape = if (compact) {
        roundedShape
    } else {
        remember(effectiveNotchY, cornerPx, notchRadiusPx) {
            NotchedPassShape(effectiveNotchY, cornerPx, notchRadiusPx)
        }
    }
    val shadowColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.06f)

    CompositionLocalProvider(
        LocalViewConfiguration provides LongPressViewConfiguration(envelope)
    ) {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .then(
                    if (dark) {
                        Modifier
                    } else {
                        Modifier.shadow(
                            elevation = 3.dp,
                            shape = shape,
                            clip = false,
                            ambientColor = shadowColor,
                            spotColor = shadowColor
                        )
                    }
                )
                .clip(shape)
                .background(body)
                .drawWithContent {
                    drawContent()
                    val outline = shape.createOutline(size, layoutDirection, this)
                    if (outline is androidx.compose.ui.graphics.Outline.Generic) {
                        drawPath(
                            path = outline.path,
                            color = edge,
                            style = Stroke(width = 1.dp.toPx())
                        )
                    } else {
                        drawRoundRect(
                            color = edge,
                            cornerRadius = CornerRadius(cornerPx),
                            style = Stroke(width = 1.dp.toPx())
                        )
                    }
                }
        ) {
            Band(
                card = card,
                band = band,
                preview = preview,
                onActions = onActions,
                expiredColors = if (expired) expiredColors else null
            )

            BoxWithConstraints(
                modifier = Modifier.padding(
                    start = QcDimens.PassFieldInset,
                    end = QcDimens.PassFieldInset,
                    top = QcDimens.PassSectionTop,
                    bottom = QcDimens.PassSectionBottomNumber
                )
            ) {
                val narrowNumber = maxWidth < 252.dp
                val masked = maskNumbers && !preview
                val numberText = when {
                    preview && card.card.number.isBlank() ->
                        if (narrowNumber) {
                            stringResource(R.string.cards_pass_placeholder_narrow)
                        } else {
                            stringResource(R.string.cards_pass_placeholder_full)
                        }
                    masked && narrowNumber ->
                        stringResource(
                            R.string.cards_pass_masked_narrow,
                            card.card.number.takeLast(4)
                        )
                    masked -> CardNumberRules.format(card.card.number, card.network, masked = true)
                    else -> CardNumberRules.format(card.card.number, card.network, masked = false)
                }
                val hiddenReveal = if (masked) {
                    CardNumberRules.format(card.card.number, card.network, masked = false)
                } else {
                    null
                }
                PassCopyField(
                    label = stringResource(R.string.cards_field_number),
                    value = numberText,
                    valueStyle = if (masked) QcType.PassNumberMasked else QcType.PassNumber,
                    revealedValue = hiddenReveal,
                    hidden = masked,
                    canReveal = !preview,
                    isUnlocked = isUnlocked,
                    interactive = !preview,
                    placeholder = preview && card.card.number.isBlank(),
                    trailing = if (showCardType && !card.typeName.isNullOrBlank()) {
                        {
                            Box(
                                modifier = Modifier.fillMaxWidth(),
                                contentAlignment = Alignment.CenterEnd
                            ) {
                                TypePill(card.typeName.orEmpty(), band, dark)
                            }
                        }
                    } else {
                        null
                    },
                    onCopy = { onCopy(PassField.NUMBER) },
                    onReveal = { onReveal(PassField.NUMBER) }
                )
            }

            if (!compact) {
            var notchMeasured by remember { mutableStateOf(false) }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = QcDimens.PassPerforationInset)
                    .padding(top = QcDimens.Base)
                    .height(QcDimens.PassPerforation)
                    .drawBehind {
                        val dash = 4.dp.toPx()
                        drawPath(
                            path = Path().apply {
                                moveTo(0f, size.height / 2f)
                                lineTo(size.width, size.height / 2f)
                            },
                            color = PassColors.perforation(dark),
                            style = Stroke(
                                width = size.height,
                                pathEffect = PathEffect.dashPathEffect(
                                    floatArrayOf(dash, dash),
                                    0f
                                )
                            )
                        )
                    }
                    .onGloballyPositioned { coordinates ->
                        val measured = coordinates.positionInParent().y + coordinates.size.height / 2f
                        if (!notchMeasured || kotlin.math.abs(notchY - measured) > 0.5f) {
                            notchY = measured
                            notchMeasured = true
                        }
                    }
            )

            BoxWithConstraints(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = QcDimens.PassFieldInset,
                        end = QcDimens.PassFieldInset,
                        top = QcDimens.Base,
                        bottom = QcDimens.PassDetailsBottom
                    )
            ) {
                val adaptive = maxWidth < 340.dp
                val dense = maxWidth < 252.dp
                val expiryPlaceholder = stringResource(R.string.cards_pass_expiry_placeholder)
                val namePlaceholder = stringResource(R.string.cards_pass_name_placeholder)
                val valueStyle = when {
                    maxWidth < 221.dp -> QcType.StubValue.copy(
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        letterSpacing = 0.em
                    )
                    dense -> QcType.StubValue.copy(
                        fontSize = 13.sp,
                        lineHeight = 20.sp,
                        letterSpacing = 0.em
                    )
                    else -> QcType.StubValue
                }
                val holderName = card.holderName.ifBlank { card.card.nameOnCard }
                    .uppercase(Locale.ROOT)
                val expiryShown = card.card.expiry.ifBlank { expiryPlaceholder }
                val expiryDescription = when (expiryState) {
                    ExpiryState.EXPIRED -> stringResource(
                        R.string.cards_a11y_expiry_expired,
                        expiryShown
                    )
                    ExpiryState.EXPIRES_SOON -> stringResource(
                        R.string.cards_a11y_expiry_soon,
                        expiryShown
                    )
                    else -> null
                }
                Row(modifier = Modifier.fillMaxWidth()) {
                    PassCopyField(
                        modifier = Modifier.weight(if (dense) 1f else if (adaptive) 0.85f else 1f),
                        label = stringResource(R.string.cards_field_expiry),
                        value = expiryShown,
                        valueStyle = valueStyle,
                        revealedValue = null,
                        hidden = false,
                        canReveal = false,
                        isUnlocked = isUnlocked,
                        interactive = !preview,
                        placeholder = preview && card.card.expiry.isBlank(),
                        dense = dense,
                        accessibilityOverride = expiryDescription,
                        onCopy = { onCopy(PassField.EXPIRY) },
                        onReveal = null
                    )
                    val cvvLength = card.network?.cvvLength ?: 3
                    PassCopyField(
                        modifier = Modifier.weight(if (dense) 0.62f else if (adaptive) 0.55f else 1f),
                        label = stringResource(R.string.cards_field_cvv),
                        value = "•".repeat(cvvLength),
                        valueStyle = valueStyle.copy(letterSpacing = FieldValueSpacingMasked),
                        revealedValue = card.card.cvv,
                        revealedValueStyle = valueStyle,
                        hidden = true,
                        canReveal = !preview,
                        isUnlocked = isUnlocked,
                        interactive = !preview,
                        dense = dense,
                        onCopy = { onCopy(PassField.CVV) },
                        onReveal = { onReveal(PassField.CVV) }
                    )
                    PassCopyField(
                        modifier = Modifier.weight(if (dense) 2.0f else if (adaptive) 2.2f else 1.45f),
                        label = stringResource(R.string.cards_field_name),
                        value = holderName.ifBlank { namePlaceholder },
                        valueStyle = if (adaptive) valueStyle.copy(letterSpacing = 0.em) else valueStyle,
                        revealedValue = null,
                        hidden = false,
                        canReveal = false,
                        isUnlocked = isUnlocked,
                        interactive = !preview,
                        placeholder = preview && holderName.isBlank(),
                        dense = dense,
                        onCopy = { onCopy(PassField.NAME) },
                        onReveal = null
                    )
                }
            }

            val hasStatus = showArchived || showExpired || showSoon || showReview
            val hasTags = tagsShow && card.tagNames.isNotEmpty()
            if (preview || hasTags || hasStatus) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = QcDimens.Gutter,
                            end = QcDimens.Gutter,
                            bottom = if (noteText != null) QcDimens.Base else QcDimens.Gutter
                        ),
                    horizontalArrangement = Arrangement.spacedBy(QcDimens.Base),
                    verticalAlignment = Alignment.Top
                ) {
                    FlowRow(
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        if (tagsShow) {
                            card.tagNames.forEach { tag ->
                                Box(
                                    modifier = Modifier
                                        .height(QcDimens.TagHeight)
                                        .clip(RoundedCornerShape(QcDimens.CornerSm))
                                        .background(
                                            if (expired) expiredColors.tag else brandBand
                                        )
                                        .padding(horizontal = 10.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tag,
                                        style = QcType.Label,
                                        color = Color.White,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                    if (hasStatus) {
                        Box(
                            modifier = Modifier
                                .height(QcDimens.TagHeight)
                                .widthIn(max = QcDimens.PassStatusMax),
                            contentAlignment = Alignment.CenterEnd
                        ) {
                            when {
                                showArchived -> StatusLabel(
                                    icon = QcIcons.inventory2,
                                    text = archivedLabel.orEmpty()
                                )
                                showExpired -> ExpiredChip()
                                showReview -> StatusLabel(
                                    icon = QcIcons.info,
                                    text = stringResource(R.string.cards_status_check_details)
                                )
                                else -> StatusLabel(
                                    icon = null,
                                    text = stringResource(R.string.cards_status_expires_soon)
                                )
                            }
                        }
                    }
                }
            }

            if (noteText != null) {
                PassNote(
                    cardId = card.id,
                    note = noteText,
                    onCopy = onCopyNote
                )
            }
            }
        }
    }
}

@Composable
private fun Band(
    card: HomeCard,
    band: Color,
    preview: Boolean,
    onActions: () -> Unit,
    expiredColors: ExpiredPassColors?
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(QcDimens.PassBand)
            .drawBehind {
                if (expiredColors != null) {
                    cssLinearGradient(
                        118f,
                        listOf(
                            0f to band,
                            0.60f to band,
                            1f to lerp(band, expiredColors.bandEnd, 0.5f)
                        )
                    )
                    cssLinearGradient(
                        105f,
                        listOf(
                            0f to Color.Transparent,
                            0.38f to Color.Transparent,
                            0.52f to expiredColors.sheen,
                            0.66f to Color.Transparent,
                            1f to Color.Transparent
                        )
                    )
                } else {
                    cssLinearGradient(
                        118f,
                        listOf(
                            0f to band,
                            0.45f to band,
                            1f to lerp(band, Color.White, 0.22f)
                        )
                    )
                    cssLinearGradient(
                        105f,
                        listOf(
                            0f to Color.Transparent,
                            0.38f to Color.Transparent,
                            0.52f to Color.White.copy(alpha = 0.12f),
                            0.66f to Color.Transparent,
                            1f to Color.Transparent
                        )
                    )
                }
            }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(QcDimens.PassBand)
                .padding(start = QcDimens.Gutter, end = 2.dp),
            horizontalArrangement = Arrangement.spacedBy(QcDimens.Base),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = card.bankName,
                style = QcType.BandBank,
                color = Color.White,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (card.card.pinned) {
                Icon(
                    imageVector = QcIcons.pinFilled,
                    contentDescription = stringResource(R.string.cards_pass_pinned),
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }
            val code = card.network?.code.orEmpty()
            if (code.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .height(22.dp)
                        .border(
                            QcDimens.Divider,
                            Color.White.copy(alpha = 0.55f),
                            RoundedCornerShape(QcDimens.CornerBandFrame)
                        )
                        .padding(horizontal = 7.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = code,
                        style = QcType.BandNetwork,
                        color = Color.White,
                        maxLines = 1
                    )
                }
            }
            if (preview) {
                Spacer(modifier = Modifier.size(width = QcDimens.PassGap, height = QcDimens.Divider))
            } else {
                IconButton(
                    onClick = onActions,
                    modifier = Modifier.size(QcDimens.Touch)
                ) {
                    Icon(
                        imageVector = QcIcons.more,
                        contentDescription = stringResource(R.string.cards_pass_more, card.title),
                        tint = Color.White.copy(alpha = 0.9f)
                    )
                }
            }
        }
    }
}

@Composable
private fun TypePill(typeName: String, band: Color, dark: Boolean) {
    Box(
        modifier = Modifier
            .wrapContentHeight(unbounded = true)
            .height(QcDimens.TypePillHeight)
            .clip(RoundedCornerShape(QcDimens.CornerBadge))
            .background(PassColors.mix(band, 0.26f, dark))
            .padding(horizontal = QcDimens.Base),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = typeName.uppercase(),
            style = TypePillStyle,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1
        )
    }
}

@Composable
private fun ExpiredChip(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .height(18.dp)
                .border(
                    QcDimens.Divider,
                    MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(QcDimens.CornerBadge)
                )
                .padding(start = 4.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Icon(
                imageVector = QcIcons.eventBusy,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(16.dp)
            )
            Text(
                text = stringResource(R.string.cards_status_expired).uppercase(Locale.ROOT),
                style = ExpiredChipStyle,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun StatusLabel(
    icon: ImageVector?,
    text: String,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = text.uppercase(Locale.ROOT),
            style = QcType.FieldLabel,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
private fun PassNote(
    cardId: String,
    note: String,
    onCopy: suspend () -> Boolean
) {
    val scope = rememberCoroutineScope()
    val envelope = LocalViewConfiguration.current
    val noteConfiguration = remember(envelope) {
        LongPressViewConfiguration(envelope, NoteLongPressMillis)
    }
    var expanded by remember(cardId) { mutableStateOf(false) }
    var overflows by remember(cardId, note) { mutableStateOf(false) }
    val copyLabel = stringResource(R.string.cards_a11y_copy_note)
    val moreLabel = stringResource(R.string.cards_note_more)
    val lessLabel = stringResource(R.string.cards_note_less)
    val showMoreLabel = stringResource(R.string.cards_a11y_note_more)
    val showLessLabel = stringResource(R.string.cards_a11y_note_less)

    CompositionLocalProvider(LocalViewConfiguration provides noteConfiguration) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    start = QcDimens.Gutter,
                    end = QcDimens.Gutter,
                    bottom = QcDimens.PassGap
                )
        ) {
            Text(
                text = note,
                style = QcType.BodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = if (expanded) Int.MAX_VALUE else 2,
                overflow = TextOverflow.Ellipsis,
                onTextLayout = { result ->
                    if (!expanded) overflows = result.hasVisualOverflow
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .pointerInput(note) {
                        detectTapGestures(
                            onLongPress = { scope.launch { onCopy() } }
                        )
                    }
                    .semantics {
                        onLongClick(label = copyLabel) {
                            scope.launch { onCopy() }
                            true
                        }
                    }
            )
            if (overflows || expanded) {
                TextButton(
                    onClick = { expanded = !expanded },
                    modifier = Modifier
                        .offset(x = (-12).dp)
                        .height(QcDimens.Touch),
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.onSurface
                    )
                ) {
                    Text(
                        text = if (expanded) lessLabel else moreLabel,
                        style = QcType.Label,
                        modifier = Modifier.semantics {
                            contentDescription = if (expanded) showLessLabel else showMoreLabel
                        }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PassCopyField(
    label: String,
    value: String,
    valueStyle: TextStyle,
    revealedValue: String?,
    hidden: Boolean,
    canReveal: Boolean,
    isUnlocked: Boolean,
    modifier: Modifier = Modifier,
    revealedValueStyle: TextStyle = valueStyle,
    trailing: (@Composable () -> Unit)? = null,
    interactive: Boolean = true,
    placeholder: Boolean = false,
    dense: Boolean = false,
    accessibilityOverride: String? = null,
    onCopy: suspend () -> Boolean,
    onReveal: (suspend () -> Boolean)?
) {
    val scope = rememberCoroutineScope()
    val onSurface = MaterialTheme.colorScheme.onSurface
    val surface = MaterialTheme.colorScheme.background
    val fieldName = label.lowercase()
    val copyLabel = stringResource(R.string.cards_a11y_copy_field, fieldName)
    val showLabel = stringResource(R.string.cards_a11y_show_field, fieldName)
    var flashing by remember { mutableStateOf(false) }
    var copied by remember { mutableStateOf(false) }
    var copyEvent by remember { mutableIntStateOf(0) }
    var revealed by remember { mutableStateOf(false) }
    var drainFraction by remember { mutableFloatStateOf(0f) }
    val interactionSource = remember { MutableInteractionSource() }
    val pressed by interactionSource.collectIsPressedAsState()

    fun requestCopy() {
        scope.launch {
            if (onCopy()) copyEvent++
        }
    }

    fun requestReveal() {
        val reveal = onReveal ?: return
        if (!canReveal) return
        scope.launch {
            if (revealed) return@launch
            if (reveal()) revealed = true
        }
    }

    LaunchedEffect(copyEvent) {
        if (copyEvent == 0) return@LaunchedEffect
        flashing = true
        copied = true
        delay(QcDimens.CopyFlashMillis)
        flashing = false
        delay(1020)
        copied = false
    }

    // Frame-time based so the 5 s reveal ignores the animator scale: a tween
    // completes instantly when animations are off and would re-mask at once.
    LaunchedEffect(revealed) {
        if (!revealed) {
            drainFraction = 0f
            return@LaunchedEffect
        }
        drainFraction = 1f
        val startedAt = withFrameNanos { it }
        while (true) {
            val frame = withFrameNanos { it }
            val elapsedMs = (frame - startedAt) / 1_000_000f
            drainFraction = (1f - elapsedMs / QcDimens.RevealDurationMillis).coerceIn(0f, 1f)
            if (elapsedMs >= QcDimens.RevealDurationMillis) {
                drainFraction = 0f
                revealed = false
                break
            }
        }
    }

    LaunchedEffect(isUnlocked) {
        if (!isUnlocked) {
            revealed = false
            drainFraction = 0f
        }
    }

    val shownValue = if (revealed && revealedValue != null) revealedValue else value
    val shownStyle = if (revealed && revealedValue != null) revealedValueStyle else valueStyle
    val shownHidden = hidden && !revealed

    val accessibility = when {
        interactive && accessibilityOverride != null -> accessibilityOverride
        shownHidden && interactive && canReveal ->
            stringResource(R.string.cards_a11y_field_hidden_reveal, label)
        !shownHidden && interactive && canReveal ->
            stringResource(R.string.cards_a11y_field_value_reveal, label, shownValue)
        shownHidden && interactive ->
            stringResource(R.string.cards_a11y_field_hidden_action, label)
        !shownHidden && interactive ->
            stringResource(R.string.cards_a11y_field_value_action, label, shownValue)
        shownHidden ->
            stringResource(R.string.cards_a11y_field_hidden, label)
        else ->
            stringResource(R.string.cards_a11y_field_value, label, shownValue)
    }

    val contentColor = when {
        flashing -> surface
        placeholder && !revealed -> MaterialTheme.colorScheme.onSurfaceVariant
        else -> onSurface
    }
    val labelColor = when {
        flashing -> surface
        copied -> onSurface
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    val fieldBackground = when {
        flashing -> onSurface
        pressed -> onSurface.copy(alpha = 0.12f)
        else -> Color.Transparent
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = QcDimens.PassStubTarget)
            .clip(FieldShape)
            .background(fieldBackground)
            .then(
                if (interactive) {
                    Modifier.combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClickLabel = copyLabel,
                        onLongClickLabel = if (canReveal) showLabel else null,
                        onClick = ::requestCopy,
                        onLongClick = if (canReveal && onReveal != null) {
                            { requestReveal() }
                        } else {
                            null
                        }
                    )
                } else {
                    Modifier
                }
            )
            .clearAndSetSemantics {
                contentDescription = accessibility
                if (interactive) {
                    onClick(label = copyLabel) {
                        requestCopy()
                        true
                    }
                    if (canReveal && onReveal != null) {
                        onLongClick(label = showLabel) {
                            requestReveal()
                            true
                        }
                    }
                }
            }
    ) {
        Column(
            modifier = Modifier.padding(
                start = if (dense) QcDimens.PassFieldInsetDense else QcDimens.PassFieldInset,
                end = if (dense) QcDimens.PassFieldInsetDense else QcDimens.PassFieldInset,
                top = QcDimens.PassFieldTop,
                bottom = QcDimens.PassFieldBottom
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = if (dense) 12.dp else 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (copied) {
                    Icon(
                        imageVector = QcIcons.check,
                        contentDescription = null,
                        tint = labelColor,
                        modifier = Modifier.size(QcDimens.IconTiny)
                    )
                    Spacer(modifier = Modifier.size(QcDimens.Half))
                }
                Text(
                    text = if (copied) {
                        stringResource(R.string.cards_pass_copied)
                    } else {
                        label.uppercase(Locale.ROOT)
                    },
                    style = if (dense) {
                        QcType.FieldLabel.copy(
                            fontSize = 9.sp,
                            lineHeight = 12.sp,
                            letterSpacing = 0.06.em
                        )
                    } else {
                        QcType.FieldLabel
                    },
                    color = labelColor,
                    maxLines = 1
                )
                if (trailing != null) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterEnd
                    ) {
                        trailing()
                    }
                }
            }
            Spacer(modifier = Modifier.height(QcDimens.PassLabelGap))
            Text(
                text = shownValue,
                style = shownStyle,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.fillMaxWidth()
            )
        }
        if (canReveal) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(
                        start = QcDimens.PassFieldInset,
                        end = QcDimens.PassFieldInset,
                        bottom = QcDimens.Half
                    )
                    .fillMaxWidth()
                    .height(QcDimens.PassRevealLine)
                    .drawWithContent {
                        drawContent()
                        if (drainFraction > 0f) {
                            drawRoundRect(
                                color = onSurface,
                                cornerRadius = CornerRadius(1.dp.toPx()),
                                size = Size(size.width * drainFraction, size.height)
                            )
                        }
                    }
            )
        }
    }
}

private class LongPressViewConfiguration(
    private val base: ViewConfiguration,
    private val timeoutMillis: Long = QcDimens.LongPressMillis
) : ViewConfiguration by base {
    override val longPressTimeoutMillis: Long = timeoutMillis
}

private const val NoteLongPressMillis = 500L

private class NotchedPassShape(
    private val notchCenterY: Float,
    private val cornerRadius: Float,
    private val notchRadius: Float
) : Shape {
    override fun createOutline(
        size: Size,
        layoutDirection: LayoutDirection,
        density: Density
    ): androidx.compose.ui.graphics.Outline {
        val body = Path().apply {
            addRoundRect(
                RoundRect(
                    left = 0f,
                    top = 0f,
                    right = size.width,
                    bottom = size.height,
                    cornerRadius = CornerRadius(cornerRadius)
                )
            )
        }
        val notches = Path().apply {
            addOval(Rect(center = Offset(0f, notchCenterY), radius = notchRadius))
            addOval(Rect(center = Offset(size.width, notchCenterY), radius = notchRadius))
        }
        return androidx.compose.ui.graphics.Outline.Generic(
            Path.combine(PathOperation.Difference, body, notches)
        )
    }
}

private fun DrawScope.cssLinearGradient(angleDegrees: Float, stops: List<Pair<Float, Color>>) {
    val radians = angleDegrees * PI / 180.0
    val dx = sin(radians).toFloat()
    val dy = -cos(radians).toFloat()
    val length = abs(size.width * dx) + abs(size.height * dy)
    val center = Offset(size.width / 2f, size.height / 2f)
    val start = Offset(center.x - dx * length / 2f, center.y - dy * length / 2f)
    val end = Offset(center.x + dx * length / 2f, center.y + dy * length / 2f)
    drawRect(
        brush = Brush.linearGradient(
            *stops.toTypedArray(),
            start = start,
            end = end
        )
    )
}
