package com.quickcards.app.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.github.skydoves.colorpicker.compose.BrightnessSlider
import com.github.skydoves.colorpicker.compose.HsvColorPicker
import com.github.skydoves.colorpicker.compose.rememberColorPickerController
import com.quickcards.app.R
import com.quickcards.app.core.data.model.BandColor
import com.quickcards.app.core.ui.LocalAppLocked
import com.quickcards.app.core.ui.theme.BrandOnRed
import com.quickcards.app.core.ui.theme.PassColors
import com.quickcards.app.core.ui.theme.QcDimens
import com.quickcards.app.core.ui.theme.QcIcons
import com.quickcards.app.core.ui.theme.QcType
import java.util.Locale
import kotlin.math.roundToInt

/** Curated swatches plus Custom; each cell is a 48dp target around a 40dp disc. */
@Composable
fun BandColorGroup(
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var sheetOpen by remember { mutableStateOf(false) }
    var recent by rememberSaveable { mutableStateOf<List<String>>(emptyList()) }
    val locked = LocalAppLocked.current

    LaunchedEffect(locked) {
        if (locked) sheetOpen = false
    }

    BandSwatchGrid(
        selected = selected,
        onSelect = onSelect,
        onCustom = { sheetOpen = true },
        modifier = modifier
    )

    if (sheetOpen) {
        CustomColorSheet(
            initialHex = BandColor.hexFor(selected),
            recent = recent,
            onDismiss = { sheetOpen = false },
            onUse = { hex ->
                onSelect(hex)
                recent = (listOf(hex) + recent.filterNot { it == hex }).take(5)
                sheetOpen = false
            }
        )
    }
}

@Composable
private fun BandSwatchGrid(
    selected: String,
    onSelect: (String) -> Unit,
    onCustom: () -> Unit,
    modifier: Modifier = Modifier
) {
    val items = remember { BandColor.ALL + CUSTOM_KEY }
    BoxWithConstraints(modifier = modifier.fillMaxWidth()) {
        val columns = if (maxWidth < 376.dp) 6 else 8
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .selectableGroup()
        ) {
            items.chunked(columns).forEach { row ->
                Row(modifier = Modifier.fillMaxWidth()) {
                    row.forEach { key ->
                        if (key == CUSTOM_KEY) {
                            CustomSwatch(
                                selectedHex = selected,
                                onClick = onCustom,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            BandSwatch(
                                key = key,
                                selected = selected == key,
                                onClick = { onSelect(key) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                    repeat(columns - row.size) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun BandSwatch(
    key: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val band = PassColors.band(key)
    val ring = MaterialTheme.colorScheme.onSurface
    val name = stringResource(R.string.form_band_color_desc, bandName(key))
    Box(
        modifier = modifier
            .height(QcDimens.Touch)
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .semantics { contentDescription = name },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .drawBehind {
                    if (selected) {
                        drawCircle(
                            color = ring,
                            radius = size.minDimension / 2f - 1.dp.toPx(),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(band),
                contentAlignment = Alignment.Center
            ) {
                if (selected) {
                    Icon(
                        imageVector = QcIcons.check,
                        contentDescription = null,
                        tint = BrandOnRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomSwatch(
    selectedHex: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val customSelected = !BandColor.isCurated(selectedHex)
    val ring = MaterialTheme.colorScheme.onSurface
    val description = if (customSelected) {
        stringResource(R.string.form_band_custom_desc, selectedHex)
    } else {
        stringResource(R.string.form_band_custom)
    }
    Box(
        modifier = modifier
            .height(QcDimens.Touch)
            .selectable(
                selected = customSelected,
                role = Role.RadioButton,
                onClick = onClick
            )
            .semantics { contentDescription = description }
            .testTag("band_custom"),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(46.dp)
                .drawBehind {
                    if (customSelected) {
                        drawCircle(
                            color = ring,
                            radius = size.minDimension / 2f - 1.dp.toPx(),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            if (customSelected) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(PassColors.band(selectedHex)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = QcIcons.check,
                        contentDescription = null,
                        tint = BrandOnRed,
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                        .drawBehind {
                            val stroke = 2.dp.toPx()
                            drawCircle(
                                brush = Brush.sweepGradient(PassColors.CustomRing),
                                radius = size.minDimension / 2f - stroke / 2f,
                                style = Stroke(width = stroke)
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = QcIcons.colorize,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomColorSheet(
    initialHex: String,
    recent: List<String>,
    onDismiss: () -> Unit,
    onUse: (String) -> Unit
) {
    val initial = remember(initialHex) { PassColors.band(initialHex) }
    val controller = rememberColorPickerController()
    var hex by remember { mutableStateOf(initialHex.removePrefix("#")) }
    var color by remember { mutableStateOf(initial) }
    val scrollState = rememberScrollState()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val valid = hex.length == 6 && hex.all { it in '0'..'9' || it in 'A'..'F' }
    val candidate = if (valid) "#$hex" else null
    val contrast = BandColor.contrastWithWhite(candidate)
    val tooLight = contrast != null && contrast < BandColor.MIN_CONTRAST
    val canUse = valid && !tooLight
    val dark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val error = if (tooLight) stringResource(R.string.form_band_too_light) else null

    QcModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.surfaceContainer
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = maxHeight * QcDimens.SheetMaxHeightFraction)
            ) {
                Text(
                    text = stringResource(R.string.form_band_custom_title),
                    style = QcType.Headline,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(
                        start = QcDimens.Section,
                        end = QcDimens.Section,
                        top = QcDimens.SheetTitleTop,
                        bottom = QcDimens.Base
                    )
                )
                Column(
                    modifier = Modifier
                        .weight(1f, fill = false)
                        .verticalScroll(scrollState)
                ) {
                    HsvColorPicker(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(200.dp),
                        controller = controller,
                        initialColor = initial,
                        onColorChanged = { envelope ->
                            val next = envelope.color.bandHex()
                            if (next != "#$hex") {
                                hex = next.removePrefix("#")
                                color = envelope.color
                            }
                        }
                    )
                    Box(modifier = Modifier.padding(
                        start = QcDimens.Section,
                        end = QcDimens.Section,
                        top = QcDimens.Base,
                        bottom = QcDimens.Base
                    )) {
                        BrightnessSlider(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(40.dp),
                            controller = controller,
                            initialColor = initial,
                            borderColor = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                    FormTextField(
                        value = hex,
                        onValueChange = { input ->
                            val next = input
                                .uppercase(Locale.ROOT)
                                .filter { it in '0'..'9' || it in 'A'..'F' }
                                .take(6)
                            hex = next
                            if (next.length == 6) {
                                val parsed = PassColors.band("#$next")
                                color = parsed
                                controller.selectByColor(parsed, fromUser = false)
                            }
                        },
                        label = stringResource(R.string.form_band_hex),
                        helper = "",
                        error = error,
                        counterMax = 6,
                        keyboardOptions = KeyboardOptions(
                            capitalization = KeyboardCapitalization.Characters,
                            keyboardType = KeyboardType.Ascii
                        ),
                        fieldTag = "band_hex",
                        modifier = Modifier.padding(horizontal = QcDimens.Section)
                    )
                    ColorPreview(
                        band = color,
                        dark = dark,
                        modifier = Modifier.padding(
                            start = QcDimens.Section,
                            end = QcDimens.Section,
                            top = QcDimens.Touch
                        )
                    )
                    if (recent.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.form_band_recent),
                            style = QcType.Body.copy(fontWeight = FontWeight(600)),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(
                                start = QcDimens.Section,
                                end = QcDimens.Section,
                                top = QcDimens.Section
                            )
                        )
                        Row(
                            modifier = Modifier.padding(
                                start = QcDimens.Section - QcDimens.Base,
                                end = QcDimens.Section - QcDimens.Base
                            )
                        ) {
                            recent.forEach { recentHex ->
                                RecentSwatch(
                                    hex = recentHex,
                                    selected = candidate == recentHex,
                                    onClick = {
                                        hex = recentHex.removePrefix("#")
                                        val picked = PassColors.band(recentHex)
                                        color = picked
                                        controller.selectByColor(picked, fromUser = false)
                                    }
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(QcDimens.Section))
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(
                            start = QcDimens.Section,
                            end = QcDimens.Section,
                            top = QcDimens.PassGap,
                            bottom = QcDimens.SheetBottom
                        ),
                    horizontalArrangement = Arrangement.spacedBy(QcDimens.Base, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = onDismiss,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(stringResource(R.string.common_cancel))
                    }
                    TextButton(
                        onClick = { candidate?.takeIf { canUse }?.let(onUse) },
                        enabled = canUse,
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = MaterialTheme.colorScheme.onSurface
                        )
                    ) {
                        Text(stringResource(R.string.form_band_use))
                    }
                }
            }
        }
    }
}

@Composable
private fun ColorPreview(band: Color, dark: Boolean, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(QcDimens.PassCorner))
            .background(PassColors.bodyTint(band, dark))
            .border(
                width = 1.dp,
                color = PassColors.edge(band, dark),
                shape = RoundedCornerShape(QcDimens.PassCorner)
            )
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(QcDimens.PassBand)
                .background(
                    Brush.linearGradient(
                        listOf(band, lerp(band, BrandOnRed, 0.28f))
                    )
                )
                .padding(start = QcDimens.Gutter),
            contentAlignment = Alignment.CenterStart
        ) {
            Text(
                text = stringResource(R.string.form_band_preview),
                style = QcType.BandBank,
                color = BrandOnRed,
                maxLines = 1
            )
        }
        Spacer(modifier = Modifier.height(36.dp))
    }
}

@Composable
private fun RecentSwatch(hex: String, selected: Boolean, onClick: () -> Unit) {
    val description = stringResource(R.string.form_band_custom_desc, hex)
    val ring = MaterialTheme.colorScheme.onSurface
    Box(
        modifier = Modifier
            .size(QcDimens.Touch)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .drawBehind {
                    if (selected) {
                        drawCircle(
                            color = ring,
                            radius = size.minDimension / 2f + 2.dp.toPx(),
                            style = Stroke(width = 2.dp.toPx())
                        )
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(PassColors.band(hex))
            )
        }
    }
}

@Composable
private fun bandName(key: String): String = stringResource(
    when (key) {
        BandColor.NAVY -> R.string.form_band_navy
        BandColor.RUST -> R.string.form_band_rust
        BandColor.FOREST -> R.string.form_band_forest
        BandColor.PLUM -> R.string.form_band_plum
        BandColor.GRAPHITE -> R.string.form_band_graphite
        BandColor.TEAL -> R.string.form_band_teal
        BandColor.OCEAN -> R.string.form_band_ocean
        BandColor.INDIGO -> R.string.form_band_indigo
        BandColor.VIOLET -> R.string.form_band_violet
        BandColor.MAGENTA -> R.string.form_band_magenta
        BandColor.WINE -> R.string.form_band_wine
        BandColor.CRIMSON -> R.string.form_band_crimson
        BandColor.BURNT_ORANGE -> R.string.form_band_burnt_orange
        BandColor.OLIVE -> R.string.form_band_olive
        BandColor.MOSS -> R.string.form_band_moss
        else -> R.string.form_band_slate
    }
)

private fun Color.bandHex(): String = BandColor.formatHex(
    (red * 255f).roundToInt(),
    (green * 255f).roundToInt(),
    (blue * 255f).roundToInt()
)

private const val CUSTOM_KEY = "custom"
