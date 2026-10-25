package com.quickcards.app.core.ui.theme

import androidx.compose.ui.unit.dp

/**
 * Every design size in one place. Values come from prototype/quickcards-v2.css
 * and DESIGN.md; screens and components read them here instead of repeating
 * literal dp values.
 */
object QcDimens {

    // Spacing
    val Half = 4.dp
    val Base = 8.dp
    val PassGap = 12.dp
    val ListPassGap = 16.dp
    val Gutter = 16.dp
    val Section = 24.dp
    val Xl = 32.dp
    val Touch = 48.dp

    // Rounded
    val CornerXs = 4.dp
    val CornerBandFrame = 5.dp
    val CornerBadge = 6.dp
    val CornerSm = 8.dp
    val CornerMd = 12.dp
    val PassCorner = 18.dp
    val DialogCorner = 28.dp

    // App chrome
    val AppBar = 64.dp
    val FooterAction = 52.dp
    val Divider = 1.dp

    // Rows, fields and icons
    val LeadSlot = 56.dp
    val FieldHeight = 56.dp
    val HelperLine = 20.dp
    val ListRowOneLine = 56.dp
    val ListRowTwoLine = 72.dp
    val Icon = 24.dp
    val IconSmall = 18.dp
    val IconTiny = 14.dp
    val Monogram = 40.dp
    val NetworkBadgeWidth = 56.dp
    val NetworkBadgeHeight = 28.dp
    val ChipHeight = 32.dp
    val TagHeight = 24.dp
    val TypePillHeight = 20.dp

    // Pass
    val PassBand = 48.dp
    val PassNotch = 9.dp
    val PassPerforationInset = 18.dp
    val PassPerforation = 2.dp
    val PassFieldCopyTarget = 10.dp
    val PassStubTarget = 56.dp
    val PassFieldInset = 8.dp
    val PassFieldInsetDense = 6.dp
    val PassFieldTop = 7.dp
    val PassLabelGap = 2.dp
    val PassSectionTop = 12.dp
    val PassSectionBottomNumber = 6.dp
    val PassDetailsBottom = 12.dp
    val PassStatusMax = 200.dp
    /** Gap after a field, whose 20dp supporting line is already reserved. */
    val FormFieldGap = 16.dp
    /** Gap after a group without a supporting line; matches field-to-field. */
    val FormGroupGap = 36.dp
    val PassFieldBottom = 6.dp
    val PassRevealLine = 2.dp

    // Dialogs and sheets
    val DialogHeaderGap = 16.dp
    val DialogActionGap = 8.dp
    val SheetMaxHeightFraction = 0.86f

    // Search and settings
    val SearchBox = 48.dp
    val SubheadTop = 24.dp
    val SubheadBottom = 8.dp
    val SettingsRowVertical = 8.dp

    // Adaptive layout
    val CardMinPane = 240.dp
    val CardMaxWidth = 360.dp
    val GridBreakpoint = 600.dp
    val GridBreakpointWide = 840.dp

    // Lock screen
    val LockMark = 72.dp
    val LockMarkCompact = 60.dp
    val LockDisc = 88.dp
    val LockDiscCompact = 72.dp
    val LockGlow = 128.dp
    val LockGlowCompact = 104.dp

    // Interaction
    val DisabledAlpha = 0.38f
    val RevealDurationMillis = 5_000f
    val LongPressMillis = 450L
    val CopyFlashMillis = 180L
}
