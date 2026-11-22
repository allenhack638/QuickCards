package com.quickcards.app.core.ui.entity

import androidx.annotation.StringRes
import com.quickcards.app.R
import com.quickcards.domain.EntityKind
import com.quickcards.domain.UsageCard

/** One affected card, listed by the rename confirm. */
data class ManageCardItem(
    val id: String,
    val bankName: String,
    val typeName: String,
    val lastFour: String,
    val bandColor: String,
    val networkId: String?
)

fun UsageCard.toManageCardItem(): ManageCardItem = ManageCardItem(
    id = id,
    bankName = bankName,
    typeName = typeName,
    lastFour = lastFour,
    bandColor = bandColor,
    networkId = networkId
)

/** List title on the Manage root and the list screen. */
@get:StringRes
val EntityKind.titleRes: Int
    get() = when (this) {
        EntityKind.HOLDER -> R.string.manage_list_holders
        EntityKind.BANK -> R.string.manage_list_banks
        EntityKind.NETWORK -> R.string.manage_list_networks
        EntityKind.TYPE -> R.string.manage_list_types
        EntityKind.TAG -> R.string.manage_list_tags
    }

/** Lowercase name used inside sentences: "Add card holder". */
@get:StringRes
val EntityKind.oneRes: Int
    get() = when (this) {
        EntityKind.HOLDER -> R.string.manage_kind_holder
        EntityKind.BANK -> R.string.manage_kind_bank
        EntityKind.NETWORK -> R.string.manage_kind_network
        EntityKind.TYPE -> R.string.manage_kind_type
        EntityKind.TAG -> R.string.manage_kind_tag
    }

/** Capitalised name used as a sentence subject: "Card holder added". */
@get:StringRes
val EntityKind.oneTitleRes: Int
    get() = when (this) {
        EntityKind.HOLDER -> R.string.manage_kind_holder_title
        EntityKind.BANK -> R.string.manage_kind_bank_title
        EntityKind.NETWORK -> R.string.manage_kind_network_title
        EntityKind.TYPE -> R.string.manage_kind_type_title
        EntityKind.TAG -> R.string.manage_kind_tag_title
    }

/** Empty-state title for the list editor. */
@get:StringRes
val EntityKind.emptyTitleRes: Int
    get() = when (this) {
        EntityKind.HOLDER -> R.string.manage_empty_holders
        EntityKind.BANK -> R.string.manage_empty_banks
        EntityKind.NETWORK -> R.string.manage_empty_networks
        EntityKind.TYPE -> R.string.manage_empty_types
        EntityKind.TAG -> R.string.manage_empty_tags
    }
