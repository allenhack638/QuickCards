package com.quickcards.app.core.ui.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.unit.dp

private fun symbol(name: String, path: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 960f,
        viewportHeight = 960f
    ).addGroup(translationY = 960f)
        .addPath(pathData = addPathNodes(path), fill = SolidColor(Color.Black))
        .clearGroup()
        .build()

/**
 * Material Symbols Rounded. Rows, sheets, dialogs, banners and pickers use the filled glyphs;
 * bar actions, in-field icons and unselected navigation items use the outlined ones.
 */
object QcIcons {
    val search: ImageVector by lazy { symbol("search", QcSymbolPaths.search) }
    val tune: ImageVector by lazy { symbol("tune", QcSymbolPaths.tune) }
    val filter: ImageVector by lazy { symbol("filter", QcSymbolPaths.tune) }
    val back: ImageVector by lazy { symbol("back", QcSymbolPaths.arrowBack) }
    val close: ImageVector by lazy { symbol("close", QcSymbolPaths.close) }
    val more: ImageVector by lazy { symbol("more", QcSymbolPaths.moreVert) }
    val add: ImageVector by lazy { symbol("add", QcSymbolPaths.add) }
    val check: ImageVector by lazy { symbol("check", QcSymbolPaths.check) }
    val dropDown: ImageVector by lazy { symbol("dropDown", QcSymbolPaths.expandMore) }
    val openExternal: ImageVector by lazy { symbol("openExternal", QcSymbolPaths.openInNew) }
    val visibility: ImageVector by lazy { symbol("visibility", QcSymbolPaths.visibility) }
    val visibilityOff: ImageVector by lazy { symbol("visibilityOff", QcSymbolPaths.visibilityOff) }
    val colorize: ImageVector by lazy { symbol("colorize", QcSymbolPaths.colorize) }
    val nfc: ImageVector by lazy { symbol("nfc", QcSymbolPaths.contactless) }
    val pin: ImageVector by lazy { symbol("pin", QcSymbolPaths.pushPin) }
    val offline: ImageVector by lazy { symbol("offline", QcSymbolPaths.cloudOff) }
    val warning: ImageVector by lazy { symbol("warning", QcSymbolPaths.warning) }
    val help: ImageVector by lazy { symbol("help", QcSymbolPaths.help) }
    val card: ImageVector by lazy { symbol("card", QcSymbolPaths.creditCard) }
    val manage: ImageVector by lazy { symbol("manage", QcSymbolPaths.category) }
    val settings: ImageVector by lazy { symbol("settings", QcSymbolPaths.settings) }

    val chevronRight: ImageVector by lazy { symbol("chevronRight", QcSymbolPaths.chevronRightFill) }
    val copy: ImageVector by lazy { symbol("copy", QcSymbolPaths.contentCopyFill) }
    val edit: ImageVector by lazy { symbol("edit", QcSymbolPaths.editFill) }
    val delete: ImageVector by lazy { symbol("delete", QcSymbolPaths.deleteFill) }
    val share: ImageVector by lazy { symbol("share", QcSymbolPaths.shareFill) }
    val pinFilled: ImageVector by lazy { symbol("pinFilled", QcSymbolPaths.pushPinFill) }
    val lock: ImageVector by lazy { symbol("lock", QcSymbolPaths.lockFill) }
    val contrast: ImageVector by lazy { symbol("contrast", QcSymbolPaths.contrastFill) }
    val bell: ImageVector by lazy { symbol("bell", QcSymbolPaths.notificationsFill) }
    val timer: ImageVector by lazy { symbol("timer", QcSymbolPaths.timerFill) }
    val payments: ImageVector by lazy { symbol("payments", QcSymbolPaths.paymentsFill) }
    val upload: ImageVector by lazy { symbol("upload", QcSymbolPaths.uploadFill) }
    val download: ImageVector by lazy { symbol("download", QcSymbolPaths.downloadFill) }
    val shield: ImageVector by lazy { symbol("shield", QcSymbolPaths.shieldFill) }
    val info: ImageVector by lazy { symbol("info", QcSymbolPaths.infoFill) }
    val eventBusy: ImageVector by lazy { symbol("eventBusy", QcSymbolPaths.eventBusyFill) }
    val eventUpcoming: ImageVector by lazy { symbol("eventUpcoming", QcSymbolPaths.eventUpcomingFill) }
    val hourglassBottom: ImageVector by lazy { symbol("hourglassBottom", QcSymbolPaths.hourglassEmptyFill) }
    val notes: ImageVector by lazy { symbol("notes", QcSymbolPaths.notesFill) }
    val tag: ImageVector by lazy { symbol("tag", QcSymbolPaths.sellFill) }
    val person: ImageVector by lazy { symbol("person", QcSymbolPaths.personFill) }
    val bank: ImageVector by lazy { symbol("bank", QcSymbolPaths.accountBalanceFill) }
    val network: ImageVector by lazy { symbol("network", QcSymbolPaths.creditCardFill) }
    val type: ImageVector by lazy { symbol("type", QcSymbolPaths.styleFill) }
    val inventory2: ImageVector by lazy { symbol("inventory2", QcSymbolPaths.inventory2Fill) }
    val unarchive: ImageVector by lazy { symbol("unarchive", QcSymbolPaths.unarchiveFill) }
    val clipboard: ImageVector by lazy { symbol("clipboard", QcSymbolPaths.contentPasteOffFill) }
    val autofill: ImageVector by lazy { symbol("autofill", QcSymbolPaths.passwordFill) }
    val password: ImageVector by lazy { symbol("password", QcSymbolPaths.passwordFill) }
    val fingerprint: ImageVector by lazy { symbol("fingerprint", QcSymbolPaths.fingerprintFill) }
    val update: ImageVector by lazy { symbol("update", QcSymbolPaths.systemUpdateFill) }
    val file: ImageVector by lazy { symbol("file", QcSymbolPaths.descriptionFill) }
    val table: ImageVector by lazy { symbol("table", QcSymbolPaths.tableChartFill) }
    val autoFixHigh: ImageVector by lazy { symbol("autoFixHigh", QcSymbolPaths.autoFixHighFill) }
    val touchApp: ImageVector by lazy { symbol("touchApp", QcSymbolPaths.touchAppFill) }
    val lockClock: ImageVector by lazy { symbol("lockClock", QcSymbolPaths.lockClockFill) }
    val phonelinkLock: ImageVector by lazy { symbol("phonelinkLock", QcSymbolPaths.phonelinkLockFill) }
    val contactless: ImageVector by lazy { symbol("contactless", QcSymbolPaths.contactlessFill) }
    val hideNumbers: ImageVector by lazy { symbol("hideNumbers", QcSymbolPaths.visibilityOffFill) }
    val error: ImageVector by lazy { symbol("error", QcSymbolPaths.errorFill) }

    /** Navigation items when selected. */
    object Selected {
        val card: ImageVector by lazy { symbol("cardSelected", QcSymbolPaths.creditCardFill) }
        val manage: ImageVector by lazy { symbol("manageSelected", QcSymbolPaths.categoryFill) }
        val settings: ImageVector by lazy { symbol("settingsSelected", QcSymbolPaths.settingsFill) }
    }
}
