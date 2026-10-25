@file:OptIn(ExperimentalTextApi::class)

package com.quickcards.app.core.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.quickcards.app.R

private fun robotoFlex(weight: Int): Font = Font(
    R.font.roboto_flex,
    weight = FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

val RobotoFlex = FontFamily(
    robotoFlex(400),
    robotoFlex(500),
    robotoFlex(560),
    robotoFlex(600),
    robotoFlex(650),
    robotoFlex(700),
    robotoFlex(800)
)

private fun qcStyle(
    weight: Int,
    size: Int,
    lineHeight: Int,
    trackingEm: Double = 0.0
): TextStyle = TextStyle(
    fontFamily = RobotoFlex,
    fontWeight = FontWeight(weight),
    fontSize = size.sp,
    lineHeight = lineHeight.sp,
    letterSpacing = trackingEm.em,
    fontFeatureSettings = "tnum"
)

object QcType {
    val ScreenTitle = qcStyle(650, 24, 32, -0.01)
    val PassNumber = qcStyle(560, 24, 32, 0.045)
    val PassNumberMasked = qcStyle(560, 24, 32, 0.07)
    val Headline = qcStyle(600, 22, 28, -0.005)
    val DialogTitle = qcStyle(500, 22, 28)
    val NestedTitle = qcStyle(600, 20, 28)
    val StubValue = qcStyle(650, 16, 26, 0.02)
    val Title = qcStyle(600, 16, 24)
    val BodyLarge = qcStyle(500, 16, 24)
    val MenuItem = qcStyle(400, 15, 20)
    val Body = qcStyle(400, 14, 20)
    val BodySmall = qcStyle(400, 12, 16)
    val Label = qcStyle(600, 12, 16)
    val Chip = qcStyle(600, 13, 18)
    val FieldLabel = qcStyle(600, 11, 16, 0.06)
    val BandNetwork = qcStyle(800, 10, 14, 0.1)
    val BandBank = qcStyle(700, 15, 20)
}

val Typography = Typography(
    displayLarge = QcType.ScreenTitle,
    displayMedium = QcType.ScreenTitle,
    displaySmall = QcType.Headline,
    headlineLarge = QcType.Headline,
    headlineMedium = QcType.Headline,
    headlineSmall = QcType.NestedTitle,
    titleLarge = QcType.NestedTitle,
    titleMedium = QcType.Title,
    titleSmall = QcType.Body.copy(fontWeight = FontWeight(600)),
    bodyLarge = QcType.BodyLarge,
    bodyMedium = QcType.Body,
    bodySmall = QcType.BodySmall,
    labelLarge = QcType.Body.copy(fontWeight = FontWeight(600)),
    labelMedium = QcType.Label,
    labelSmall = QcType.Label
)
