package com.quickcards.app.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.quickcards.app.R

val Geist = FontFamily(
    Font(R.font.geist_regular, FontWeight.Normal),
    Font(R.font.geist_medium, FontWeight.Medium),
    Font(R.font.geist_semibold, FontWeight.SemiBold)
)

val GeistMono = FontFamily(
    Font(R.font.geist_mono_regular, FontWeight.Normal),
    Font(R.font.geist_mono_medium, FontWeight.Medium)
)

// Half-leading centred and kept, as in the CSS spec, so text boxes match the prototype.
private val qcLineHeight = LineHeightStyle(
    alignment = LineHeightStyle.Alignment.Center,
    trim = LineHeightStyle.Trim.None
)

private fun style(
    size: Int,
    lineHeight: Int,
    weight: FontWeight,
    family: FontFamily = Geist,
    trackingEm: Float = 0f,
    features: String? = null
) = TextStyle(
    fontFamily = family,
    fontWeight = weight,
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = trackingEm.em,
    fontFeatureSettings = features,
    platformStyle = PlatformTextStyle(includeFontPadding = false),
    lineHeightStyle = qcLineHeight
)

/** The five Daylight sizes (DESIGN.md › Typography); weights vary only within each size's range. */
object QcText {
    val F5 = style(24, 32, FontWeight.SemiBold, trackingEm = -0.02f)
    val F4 = style(20, 28, FontWeight.SemiBold)
    val F3 = style(15, 20, FontWeight.Normal)
    val F3Medium = style(15, 20, FontWeight.Medium)
    val F3Semibold = style(15, 20, FontWeight.SemiBold)
    val F2 = style(13, 20, FontWeight.Normal)
    val F2Medium = style(13, 20, FontWeight.Medium)
    val F2Semibold = style(13, 20, FontWeight.SemiBold)
    val F1 = style(11, 16, FontWeight.Medium)
    val F1Regular = style(11, 16, FontWeight.Normal)
    val F1Semibold = style(11, 16, FontWeight.SemiBold)

    /** Card number, expiry and CVV: Geist Mono 500 with tabular figures. */
    val MonoF5 = style(24, 32, FontWeight.Medium, GeistMono, 0.04f, "tnum")
    val MonoF4 = style(20, 28, FontWeight.Medium, GeistMono, 0.04f, "tnum")
    val MonoF3 = style(15, 20, FontWeight.Medium, GeistMono, 0.04f, "tnum")
    val MonoF1 = style(11, 16, FontWeight.Medium, GeistMono, 0.04f, "tnum")
    val MonoDotsF5 = MonoF5.copy(letterSpacing = 0.12f.em)
    val MonoDotsF4 = MonoF4.copy(letterSpacing = 0.12f.em)
    val MonoDotsF3 = MonoF3.copy(letterSpacing = 0.12f.em)
}

val Typography = Typography(
    displayLarge = QcText.F5,
    displayMedium = QcText.F5,
    displaySmall = QcText.F5,
    headlineLarge = QcText.F5,
    headlineMedium = QcText.F5,
    headlineSmall = QcText.F5,
    titleLarge = QcText.F4,
    titleMedium = QcText.F3Semibold,
    titleSmall = QcText.F2Semibold,
    bodyLarge = QcText.F3,
    bodyMedium = QcText.F2,
    bodySmall = QcText.F1Regular,
    labelLarge = QcText.F2Semibold,
    labelMedium = QcText.F1Semibold,
    labelSmall = QcText.F1
)
