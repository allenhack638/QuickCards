package com.quickcards.app.core.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.unit.dp

/** DESIGN.md › Layout: the 4dp grid. Paddings, margins and gaps use only these (or 0). */
object QcSpacing {
    val s1 = 4.dp
    val s2 = 8.dp
    val s3 = 12.dp
    val s4 = 16.dp
    val s5 = 20.dp
    val s6 = 24.dp
    val s8 = 32.dp
    val s10 = 40.dp
    val s12 = 48.dp
    val s16 = 64.dp
}

object QcRadius {
    val xs = 4.dp
    val sm = 8.dp
    val md = 12.dp
    val lg = 20.dp
    val xl = 28.dp
}

/** Control, row and icon sizes. Controls use these as minimums so large text grows them. */
object QcSize {
    val hit = 48.dp
    val field = 48.dp
    val bar = 64.dp
    val row = 56.dp
    val row2 = 64.dp
    val nav = 80.dp
    val ctlSm = 24.dp
    val ctl = 32.dp
    val ctlMd = 36.dp
    val ctlLg = 44.dp
    val iconXs = 16.dp
    val iconSm = 18.dp
    val icon = 20.dp
    val iconLg = 24.dp
    val statusChip = 20.dp
    val hairline = 1.dp
    val focus = 2.dp
    val switchWidth = 44.dp
    val dialogMaxWidth = 560.dp
}

/** e1 groups and fields, e2 menus, e3 sheets and dialogs. Shadows draw in the light theme only. */
object QcElevation {
    val e1 = 1.dp
    val e2 = 8.dp
    val e3 = 24.dp
}

object QcMotion {
    const val STATE_MS = 150
    const val NAV_MS = 200
    const val TOAST_MS = 3_000L
    const val TOAST_A11Y_MS = 4_500L
    val ease = CubicBezierEasing(0.2f, 0f, 0f, 1f)
}

/** Legacy names kept so features compile; each resolves to a Daylight token. */
object QcDimens {

    // Spacing
    val Half = QcSpacing.s1
    val Base = QcSpacing.s2
    val PassGap = QcSpacing.s3
    val NavBarHeight = QcSize.nav
    val Gutter = QcSpacing.s4
    val Section = QcSpacing.s6
    val Touch = QcSize.hit

    // Rounded
    val PassCorner = QcRadius.lg

    // App chrome
    val AppBar = QcSize.bar
    val Divider = QcSize.hairline

    // Rows, fields and icons
    val LeadSlot = 56.dp
    val Icon = QcSize.iconLg
    val Monogram = QcSpacing.s10

    // Dialogs and sheets
    val SheetHandleTop = QcSpacing.s3
    val SheetHandleBottom = QcSpacing.s2
    val SheetHandleWidth = QcSize.ctl
    val SheetHandleHeight = QcSpacing.s1
    val SheetTitleTop = QcSpacing.s2
    val SheetBottom = QcSpacing.s6
    val SheetMaxHeightFraction = 0.86f

    // Search
    val SearchBox = QcSpacing.s10

    // Adaptive layout (DESIGN.md › Adaptive)
    val MediumWidth = 600.dp
    val ExpandedWidth = 840.dp
    val CompactHeight = 480.dp
    val Rail = QcSize.nav
    val ListPane = 360.dp
    val ListPaneMedium = 328.dp
    val PageMax = 600.dp
    val DetailMax = 840.dp
    val HingeZone = QcSpacing.s6
    /** How far a pushed screen slides in (Material shared axis). */
    val SharedAxisOffset = QcSpacing.s8
    /** The hero's design width; narrower panes scale it as one object. */
    val HeroDesign = 328.dp
    /** The hero keeps the ISO card ratio (328 x 207) at any width. */
    const val HeroRatio = 328f / 207f
    val CardMaxWidth = 360.dp
    /** Panes narrower than this put each details key above its value. */
    val NarrowPane = 320.dp
    /** A loading tile: the height of a tile with a meta line and no note. */
    val SkeletonTile = 148.dp

    // Lock screen
    val LockMark = 72.dp
    val LockGlyph = 56.dp
    val LockMarkTop = QcSpacing.s16 * 2 + QcSpacing.s12
    val LockMarkTopCompact = QcSpacing.s12

    // Interaction
    val RevealDurationMillis = 5_000f

    /** How long a row or tile keeps its "Copied" mark. */
    const val CopiedMillis = 1_600L
}
