package com.quickcards.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.setProgress
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import com.quickcards.app.R
import com.quickcards.app.core.ui.LocalAppLocked
import com.quickcards.app.core.ui.art.ArtKind
import com.quickcards.app.core.ui.art.CardArt
import com.quickcards.app.core.ui.theme.PassColors
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcShapes
import com.quickcards.app.core.ui.theme.QcSize
import com.quickcards.app.core.ui.theme.QcSpacing
import com.quickcards.app.core.ui.theme.QcText
import com.quickcards.app.core.ui.theme.QcTheme
import com.quickcards.domain.ArtRules
import java.util.Locale
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/** Swatch order in the Colour sheet: the palette order, graphite last. */
val ColourOrder: List<String> = ArtRules.PALETTE_ORDER + ArtRules.GRAPHITE

/**
 * Custom card colours (DESIGN.md › Card colour): a hue and a lightness on fixed saturation give
 * the light stop; white card text must keep 7:1 on it.
 */
object CustomColour {
    private const val SATURATION = 0.58
    private const val L0 = 0.14
    private const val L1 = 0.72
    const val DEFAULT_HUE = 248f
    const val DEFAULT_LIGHTNESS = 0.4f

    /** The light stop `#RRGGBB` for a hue (0–360) and a lightness position (0–1). */
    fun hex(hue: Float, t: Float): String = format(hsl(hue.toDouble(), SATURATION, L0 + (L1 - L0) * t))

    /** Strip colours: the hue strip at 40% lightness, the lightness strip at its ends and middle. */
    fun hueStop(hue: Float): String = format(hsl(hue.toDouble(), SATURATION, 0.40))

    fun readable(hex: String): Boolean = parse(hex)?.let { ArtRules.contrastWithWhite(it) >= ArtRules.MIN_DIGIT_CONTRAST } ?: false

    /** The lightness position where white text drops below 7:1 for this hue. */
    fun limit(hue: Float): Float {
        for (i in 0..STEPS) {
            if (!readable(hex(hue, i / STEPS.toFloat()))) return ((i - 1) / STEPS.toFloat()).coerceAtLeast(0f)
        }
        return 1f
    }

    /** Hue and lightness position of any hex, for the strip thumbs. */
    fun position(hex: String): Pair<Float, Float> {
        val rgb = parse(hex) ?: return DEFAULT_HUE to DEFAULT_LIGHTNESS
        val r = (rgb shr 16 and 0xFF) / 255.0
        val g = (rgb shr 8 and 0xFF) / 255.0
        val b = (rgb and 0xFF) / 255.0
        val hi = max(r, max(g, b))
        val lo = min(r, min(g, b))
        val d = hi - lo
        val h = when {
            d == 0.0 -> DEFAULT_HUE.toDouble()
            hi == r -> 60 * (((g - b) / d).mod(6.0))
            hi == g -> 60 * ((b - r) / d + 2)
            else -> 60 * ((r - g) / d + 4)
        }
        val t = (((hi + lo) / 2 - L0) / (L1 - L0)).coerceIn(0.0, 1.0)
        return h.toFloat() to t.toFloat()
    }

    fun parse(hex: String?): Int? {
        val digits = hex?.trim()?.removePrefix("#") ?: return null
        if (!digits.matches(HEX)) return null
        return digits.toInt(16)
    }

    private fun format(rgb: Int): String = String.format(Locale.ROOT, "#%06X", rgb)

    private fun hsl(h: Double, s: Double, l: Double): Int {
        val a = s * min(l, 1 - l)
        fun f(n: Int): Int {
            val k = (n + h / 30) % 12
            val v = l - a * max(-1.0, min(k - 3, min(9 - k, 1.0)))
            return (v * 255).roundToInt().coerceIn(0, 255)
        }
        return (f(0) shl 16) or (f(8) shl 8) or f(4)
    }

    private const val STEPS = 200
    private val HEX = Regex("[0-9a-fA-F]{6}")
}

/** What the sheet's preview card shows. */
data class ColourPreview(val title: String, val networkId: String? = null, val last4: String? = null)

@Composable
fun colourName(key: String): String = stringResource(
    when (key.lowercase(Locale.ROOT)) {
        "navy" -> R.string.form_band_navy
        "ocean" -> R.string.form_band_ocean
        "teal" -> R.string.form_band_teal
        "forest" -> R.string.form_band_forest
        "moss" -> R.string.form_band_moss
        "olive" -> R.string.form_band_olive
        "burnt-orange" -> R.string.form_band_burnt_orange
        "rust" -> R.string.form_band_rust
        "crimson" -> R.string.form_band_crimson
        "wine" -> R.string.form_band_wine
        "magenta" -> R.string.form_band_magenta
        "plum" -> R.string.form_band_plum
        "violet" -> R.string.form_band_violet
        "indigo" -> R.string.form_band_indigo
        "slate" -> R.string.form_band_slate
        ArtRules.GRAPHITE -> R.string.form_band_graphite
        else -> R.string.form_band_custom
    }
)

/** A small card-shaped colour chip: 32×20 in fields and bank rows, 40×24 for recent customs. */
@Composable
fun ColourChip(key: String, modifier: Modifier = Modifier, large: Boolean = false) {
    CardArt(
        band = key,
        kind = ArtKind.MINI,
        modifier = modifier
            .size(if (large) QcSpacing.s10 else QcSpacing.s8, if (large) QcSize.iconLg else QcSpacing.s5)
            .clearAndSetSemantics { }
    )
}

/** The Colour picker field; an empty [key] shows the "Bank colour" placeholder. */
@Composable
fun ColourField(
    key: String?,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    label: String = stringResource(R.string.form_group_band),
    helper: String = ""
) {
    QcLookupField(
        label = label,
        value = key?.let { colourName(it) }.orEmpty(),
        helper = helper,
        error = null,
        onClick = onClick,
        placeholder = stringResource(R.string.form_placeholder_colour),
        lead = key?.let { { ColourChip(it) } },
        modifier = modifier
    )
}

/** The bank editor's Colour row: the field, whether another bank shares it, and its sheet. */
@Composable
fun BankColourField(colour: String, shared: Boolean, bankName: String, onPick: (String) -> Unit) {
    var open by remember { mutableStateOf(false) }
    val locked = LocalAppLocked.current
    LaunchedEffect(locked) { if (locked) open = false }
    ColourField(
        key = colour,
        onClick = { open = true },
        label = stringResource(R.string.manage_bank_colour),
        helper = stringResource(if (shared) R.string.manage_bank_colour_shared else R.string.manage_bank_colour_unused)
    )
    if (open && !locked) {
        CardColourSheet(
            selected = colour,
            bankColour = null,
            preview = ColourPreview(bankName),
            recent = emptyList(),
            onPick = onPick,
            onBankColour = {},
            onDismiss = { open = false }
        )
    }
}

/**
 * The Colour sheet: live preview, the sixteen card swatches with names in four columns, then
 * Custom. A pick applies at once; [bankColour] adds the "Bank colour" chip and marks that swatch.
 */
@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CardColourSheet(
    selected: String,
    bankColour: String?,
    preview: ColourPreview,
    recent: List<String>,
    onPick: (String) -> Unit,
    onBankColour: () -> Unit,
    onDismiss: () -> Unit
) {
    var custom by remember { mutableStateOf(false) }
    QcModalBottomSheet(onDismissRequest = onDismiss) {
        QcSheetBackHandler(enabled = custom) { custom = false }
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            val maxBody = maxHeight
            Column(modifier = Modifier.fillMaxWidth().heightIn(max = maxBody)) {
                if (custom) {
                    CustomColourView(
                        initial = selected.takeIf { CustomColour.parse(it) != null },
                        preview = preview,
                        recent = recent,
                        onBack = { custom = false },
                        onUse = { hex ->
                            onPick(hex)
                            onDismiss()
                        }
                    )
                } else {
                    PaletteView(
                        selected = selected,
                        bankColour = bankColour,
                        preview = preview,
                        onPick = onPick,
                        onBankColour = onBankColour,
                        onCustom = { custom = true },
                        onDone = onDismiss
                    )
                }
            }
        }
    }
}

@Composable
private fun SheetTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = QcText.F4,
        color = QcTheme.colors.ink,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = modifier.semantics { heading() }
    )
}

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.PaletteView(
    selected: String,
    bankColour: String?,
    preview: ColourPreview,
    onPick: (String) -> Unit,
    onBankColour: () -> Unit,
    onCustom: () -> Unit,
    onDone: () -> Unit
) {
    val isCustom = CustomColour.parse(selected) != null
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = QcSpacing.s6, end = QcSpacing.s6, top = QcSpacing.s2, bottom = QcSpacing.s3),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SheetTitle(stringResource(R.string.form_group_band), Modifier.weight(1f))
        if (bankColour != null) {
            BankColourChip(on = selected == bankColour, onClick = onBankColour)
        }
    }
    PreviewCard(selected, preview)
    Column(
        modifier = Modifier
            .weight(1f, fill = false)
            .verticalScroll(rememberScrollState())
            .padding(start = QcSpacing.s6, end = QcSpacing.s6, top = QcSpacing.s4)
            .selectableGroup(),
        verticalArrangement = Arrangement.spacedBy(QcSpacing.s2)
    ) {
        (ColourOrder + CUSTOM).chunked(COLUMNS).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(QcSpacing.s2)) {
                row.forEach { key ->
                    Box(modifier = Modifier.weight(1f)) {
                        if (key == CUSTOM) {
                            CustomSwatch(on = isCustom, onClick = onCustom)
                        } else {
                            Swatch(
                                key = key,
                                on = key == selected,
                                bank = key == bankColour,
                                onClick = { onPick(key) }
                            )
                        }
                    }
                }
                repeat(COLUMNS - row.size) { Spacer(modifier = Modifier.weight(1f)) }
            }
        }
    }
    QcSheetActions(primary = stringResource(R.string.form_action_done), onPrimary = onDone)
}

@Composable
private fun BankColourChip(on: Boolean, onClick: () -> Unit) {
    val colors = QcTheme.colors
    val label = stringResource(R.string.form_band_bank_chip)
    Box(
        modifier = Modifier
            .heightIn(min = QcSize.hit)
            .clip(QcShapes.full)
            .selectable(selected = on, role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .heightIn(min = QcSize.ctl)
                .then(
                    if (on) {
                        Modifier.background(colors.select, QcShapes.full)
                    } else {
                        Modifier
                            .background(colors.surface, QcShapes.full)
                            .border(QcSize.hairline, colors.line, QcShapes.full)
                    }
                )
                .padding(horizontal = QcSpacing.s3, vertical = QcSpacing.s1),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(QcSpacing.s1)
        ) {
            Icon(
                imageVector = QcIcons.bank,
                contentDescription = null,
                tint = if (on) colors.onSelect else colors.ink,
                modifier = Modifier.size(QcSize.iconSm)
            )
            Text(text = label, style = QcText.F2Medium, color = if (on) colors.onSelect else colors.ink, maxLines = 1)
        }
    }
}

private const val COLUMNS = 4
private const val CUSTOM = "custom"
private const val CARD_RATIO = 1.586f

/** 64×40 card swatch; selected: 2dp gap ring + 2dp ink ring and a check; the bank's own colour carries the bank glyph. */
@Composable
private fun Swatch(key: String, on: Boolean, bank: Boolean, onClick: () -> Unit) {
    val name = colourName(key)
    val a11y = if (bank) stringResource(R.string.form_band_bank_a11y, name) else name
    SwatchCell(name = name, on = on, a11y = a11y, onClick = onClick) {
        CardArt(band = key, kind = ArtKind.SWATCH) {
            if (bank) {
                Icon(
                    imageVector = QcIcons.bank,
                    contentDescription = null,
                    tint = PassColors.OnArt,
                    modifier = Modifier
                        .padding(QcSpacing.s1)
                        .size(QcSize.iconXs)
                )
            }
            if (on) {
                Icon(
                    imageVector = QcIcons.check,
                    contentDescription = null,
                    tint = PassColors.OnArt,
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(QcSize.icon)
                )
            }
        }
    }
}

@Composable
private fun CustomSwatch(on: Boolean, onClick: () -> Unit) {
    val colors = QcTheme.colors
    val name = stringResource(R.string.form_band_custom)
    SwatchCell(name = name, on = on, a11y = stringResource(R.string.form_band_custom_title), onClick = onClick) {
        Box(
            modifier = Modifier
                .size(QcSpacing.s16, QcSpacing.s10)
                .background(colors.tonal, QcShapes.sm)
                .border(QcSize.hairline, colors.line, QcShapes.sm),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = QcIcons.palette,
                contentDescription = null,
                tint = colors.ink2,
                modifier = Modifier.size(QcSize.icon)
            )
        }
    }
}

@Composable
private fun SwatchCell(
    name: String,
    on: Boolean,
    a11y: String,
    onClick: () -> Unit,
    swatch: @Composable () -> Unit
) {
    val colors = QcTheme.colors
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(QcShapes.sm)
            .selectable(selected = on, role = Role.RadioButton, onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = a11y }
            .padding(vertical = QcSpacing.s1),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(QcSpacing.s1)
    ) {
        Box(
            modifier = if (on) {
                Modifier
                    .border(QcSize.focus, colors.ink, QcShapes.md)
                    .padding(QcSize.focus * 2)
            } else {
                Modifier.padding(QcSize.focus * 2)
            }
        ) {
            swatch()
        }
        Text(
            text = name,
            style = if (on) QcText.F1 else QcText.F1Regular,
            color = if (on) colors.ink else colors.ink2,
            textAlign = TextAlign.Center,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.clearAndSetSemantics { }
        )
    }
}

/** The 192-wide live preview pass at the top of the sheet and the bank editor. */
@Composable
fun PreviewCard(key: String, preview: ColourPreview, modifier: Modifier = Modifier) {
    val width = QcSpacing.s16 * 3
    Box(modifier = modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        CardArt(
            band = key,
            kind = ArtKind.TILE,
            modifier = Modifier
                .size(width, width / CARD_RATIO)
                .clearAndSetSemantics { }
        ) {
            Column(modifier = Modifier.matchParentSize(), verticalArrangement = Arrangement.SpaceBetween) {
                Row(verticalAlignment = Alignment.Top) {
                    Text(
                        text = preview.title,
                        style = QcText.F2Semibold,
                        color = PassColors.OnArt,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    preview.networkId?.let { id ->
                        NetworkMark(
                            networkId = id,
                            contentDescription = null,
                            tone = markTone(id, onArt = true, dark = false),
                            fit = MarkFit.SLOT
                        )
                    }
                }
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = DOTS,
                        style = QcText.MonoDotsF3,
                        color = PassColors.OnArt.copy(alpha = DOTS_ALPHA),
                        modifier = Modifier.padding(end = QcSpacing.s1)
                    )
                    Text(
                        text = preview.last4 ?: stringResource(R.string.form_preview_digits),
                        style = QcText.MonoF3,
                        color = if (preview.last4 == null) PassColors.OnArtSecondary else PassColors.OnArt
                    )
                }
            }
        }
    }
}

private const val DOTS = "••••"
private const val DOTS_ALPHA = 0.72f

@Composable
private fun androidx.compose.foundation.layout.ColumnScope.CustomColourView(
    initial: String?,
    preview: ColourPreview,
    recent: List<String>,
    onBack: () -> Unit,
    onUse: (String) -> Unit
) {
    val colors = QcTheme.colors
    val start = remember(initial) { initial?.let(CustomColour::position) ?: (CustomColour.DEFAULT_HUE to CustomColour.DEFAULT_LIGHTNESS) }
    var hue by remember { mutableFloatStateOf(start.first) }
    var t by remember { mutableFloatStateOf(start.second) }
    var hex by remember { mutableStateOf(initial?.uppercase(Locale.ROOT) ?: CustomColour.hex(hue, t)) }
    var hexText by remember { mutableStateOf(TextFieldValue(hex.removePrefix("#"))) }
    val readable = CustomColour.readable(hex)

    fun fromStrips(nextHue: Float, nextT: Float) {
        hue = nextHue
        t = nextT
        hex = CustomColour.hex(nextHue, nextT)
        hexText = TextFieldValue(hex.removePrefix("#"), androidx.compose.ui.text.TextRange(hex.length - 1))
    }

    fun fromHex(value: String) {
        hex = "#" + value.uppercase(Locale.ROOT)
        val (h, l) = CustomColour.position(hex)
        hue = h
        t = l
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = QcSpacing.s3, end = QcSpacing.s6, top = QcSpacing.s1, bottom = QcSpacing.s2),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(QcSpacing.s2)
    ) {
        QcIconButton(QcIcons.back, stringResource(R.string.form_band_back), onBack)
        SheetTitle(stringResource(R.string.form_band_custom_title), Modifier.weight(1f))
    }
    Column(
        modifier = Modifier
            .weight(1f, fill = false)
            .verticalScroll(rememberScrollState())
            .padding(top = QcSpacing.s3),
        verticalArrangement = Arrangement.spacedBy(QcSpacing.s2)
    ) {
        PreviewCard(hex, preview, Modifier.padding(bottom = QcSpacing.s2))
        ColourStrip(
            label = stringResource(R.string.form_band_hue),
            value = hue / HUE_MAX,
            stops = HUE_STOPS.map { colourOf(CustomColour.hueStop(it)) },
            thumb = colourOf(CustomColour.hueStop(hue)),
            limit = null,
            onChange = { fromStrips(it * HUE_MAX, t) }
        )
        ColourStrip(
            label = stringResource(R.string.form_band_lightness),
            value = t,
            stops = listOf(0f, 0.5f, 1f).map { colourOf(CustomColour.hex(hue, it)) },
            thumb = colourOf(hex),
            limit = CustomColour.limit(hue),
            onChange = { fromStrips(hue, it) }
        )
        if (!readable) {
            Row(
                modifier = Modifier.padding(horizontal = QcSpacing.s6),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(QcSpacing.s2)
            ) {
                Icon(
                    imageVector = QcIcons.error,
                    contentDescription = null,
                    tint = colors.danger,
                    modifier = Modifier.size(QcSize.iconSm)
                )
                Text(text = stringResource(R.string.form_band_too_light), style = QcText.F2, color = colors.danger)
            }
        }
        Row(
            modifier = Modifier.padding(start = QcSpacing.s6, end = QcSpacing.s6, top = QcSpacing.s1),
            horizontalArrangement = Arrangement.spacedBy(QcSpacing.s4),
            verticalAlignment = Alignment.Top
        ) {
            FormTextField(
                value = hexText,
                onValueChange = { input ->
                    val next = input.text.filter { it.isDigit() || it.lowercaseChar() in 'a'..'f' }.take(HEX_DIGITS).uppercase(Locale.ROOT)
                    hexText = TextFieldValue(next, androidx.compose.ui.text.TextRange(next.length))
                    if (next.length == HEX_DIGITS) fromHex(next)
                },
                label = stringResource(R.string.form_band_hex),
                fieldTag = "band_hex",
                error = if (readable) null else "",
                mono = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters, autoCorrectEnabled = false),
                visualTransformation = HashPrefix(stringResource(R.string.form_band_hash)),
                modifier = Modifier.weight(1f)
            )
            if (recent.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(QcSpacing.s1)) {
                    Text(text = stringResource(R.string.form_band_recent), style = QcText.F2Medium, color = colors.ink2)
                    Row(
                        modifier = Modifier.height(QcSize.field).offset(x = QcSpacing.s1),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        recent.take(RECENT_MAX).forEach { item ->
                            val label = stringResource(R.string.form_band_custom_desc, item)
                            Box(
                                modifier = Modifier
                                    .size(QcSize.hit)
                                    .clip(QcShapes.sm)
                                    .clickable(role = Role.Button, onClickLabel = label) {
                                        hexText = TextFieldValue(item.removePrefix("#"))
                                        fromHex(item.removePrefix("#"))
                                    }
                                    .semantics { contentDescription = label },
                                contentAlignment = Alignment.Center
                            ) {
                                ColourChip(item, large = true)
                            }
                        }
                    }
                }
            }
        }
    }
    QcSheetActions(
        primary = stringResource(R.string.form_band_use),
        onPrimary = { onUse(hex) },
        primaryEnabled = readable && hexText.text.length == HEX_DIGITS,
        filled = false
    )
}

private const val HUE_MAX = 360f
private const val HEX_DIGITS = 6
private const val RECENT_MAX = 3
private val HUE_STOPS = (0..12).map { it * 30f }

private fun colourOf(hex: String): Color = Color((CustomColour.parse(hex) ?: 0) or OPAQUE)

private const val OPAQUE = -0x1000000

/** Shows "# " before the hex digits without making it part of the value. */
private class HashPrefix(private val hash: String) : VisualTransformation {
    override fun filter(text: AnnotatedString): TransformedText {
        val prefix = "$hash "
        return TransformedText(
            AnnotatedString(prefix) + text,
            object : OffsetMapping {
                override fun originalToTransformed(offset: Int) = offset + prefix.length
                override fun transformedToOriginal(offset: Int) = (offset - prefix.length).coerceIn(0, text.length)
            }
        )
    }
}

/** A 48-tall strip: 12dp gradient track, 24dp thumb inside a 3dp white ring and a hairline, an optional 7:1 limit tick. */
@Composable
private fun ColourStrip(
    label: String,
    value: Float,
    stops: List<Color>,
    thumb: Color,
    limit: Float?,
    onChange: (Float) -> Unit
) {
    val colors = QcTheme.colors
    var widthPx by remember { mutableFloatStateOf(1f) }
    fun at(x: Float) = (x / widthPx).coerceIn(0f, 1f)
    BoxWithConstraints(
        modifier = Modifier
            .padding(horizontal = QcSpacing.s6)
            .fillMaxWidth()
            .height(QcSize.hit)
            .onSizeChanged { widthPx = it.width.toFloat().coerceAtLeast(1f) }
            .pointerInput(Unit) { detectTapGestures { onChange(at(it.x)) } }
            .pointerInput(Unit) {
                detectDragGestures { change, _ ->
                    change.consume()
                    onChange(at(change.position.x))
                }
            }
            .semantics {
                contentDescription = label
                progressBarRangeInfo = ProgressBarRangeInfo(value, 0f..1f, steps = 0)
                setProgress { target ->
                    onChange(target.coerceIn(0f, 1f))
                    true
                }
            },
        contentAlignment = Alignment.CenterStart
    ) {
        val full = maxWidth
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(QcSpacing.s3)
                .clip(QcShapes.full)
                .background(Brush.horizontalGradient(stops))
                .border(QcSize.hairline, colors.line, QcShapes.full)
        )
        if (limit != null) {
            Box(
                modifier = Modifier
                    .offset(x = position(full, limit, QcSize.focus))
                    .size(QcSize.focus, QcSpacing.s5)
                    .background(colors.ink, QcShapes.full)
            )
        }
        Box(
            modifier = Modifier
                .offset(x = position(full, value, QcSize.ctl))
                .size(QcSize.ctl)
                .border(QcSize.hairline, colors.ink.copy(alpha = THUMB_RING_ALPHA), QcShapes.full)
                .padding(QcSize.hairline)
                .background(PassColors.OnArt, QcShapes.full)
                .padding(QcSize.focus + QcSize.hairline)
                .background(thumb, QcShapes.full)
        )
    }
}

private const val THUMB_RING_ALPHA = 0.28f

private fun position(full: Dp, fraction: Float, size: Dp): Dp = full * fraction - size / 2
