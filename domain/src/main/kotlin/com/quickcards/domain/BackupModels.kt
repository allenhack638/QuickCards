package com.quickcards.domain

/** Backup format 2. Always wrapped by ExportCrypto before it reaches disk. */
const val BACKUP_APP_NAME = "QuickCards"
const val BACKUP_FORMAT = 2

enum class BackupSection {
    HOLDERS,
    BANKS,
    NETWORKS,
    TYPES,
    TAGS
}

data class BackupMeta(
    val app: String = BACKUP_APP_NAME,
    val format: Int = BACKUP_FORMAT,
    val version: String = "",
    val exportedAt: String = ""
)

data class BackupNamed(
    val id: String,
    val name: String
)

data class BackupBank(
    val id: String,
    val name: String,
    val mono: String = ""
)

data class BackupNetwork(
    val id: String,
    val name: String,
    val code: String,
    val prefixes: String,
    val lengths: String,
    val cvv: Int = 3,
    val luhn: Boolean = true,
    val groups: String = "",
    val cvvOptional: Boolean = false
)

data class BackupCard(
    val id: String = "",
    val number: String,
    val exp: String = "",
    val cvv: String = "",
    val nameOnCard: String = "",
    val holderId: String? = null,
    val bankId: String? = null,
    val networkAuto: Boolean = true,
    val networkId: String? = null,
    val typeId: String? = null,
    val variant: String = "",
    val note: String = "",
    /** Payment due day of the month; older backups omit it. */
    val dueDay: Int? = null,
    val color: String = "navy",
    val tagIds: List<String> = emptyList(),
    val sortOrder: Int = 0,
    val pinned: Boolean = false,
    val pinnedAt: Long? = null,
    /** Format 2 (optional): archive state, reason and review mask. */
    val archived: Boolean = false,
    val archivedAt: Long? = null,
    val archiveReason: String = "",
    val needsReview: Int = 0,
    /** Format 1 only: the legacy cardIssuer, resolved against the phone's networks. */
    val legacyIssuer: String? = null
)

data class BackupContent(
    val meta: BackupMeta? = null,
    val holders: List<BackupNamed> = emptyList(),
    val banks: List<BackupBank> = emptyList(),
    val types: List<BackupNamed> = emptyList(),
    val tags: List<BackupNamed> = emptyList(),
    val networks: List<BackupNetwork> = emptyList(),
    val cards: List<BackupCard> = emptyList(),
    val settings: Map<String, Any?>? = null
)

/**
 * Normalized content plus the list sections the file carried. Format 1 reviews list only
 * cards and banks; its holders and types are derived but not listed.
 */
data class DecodedBackup(
    val content: BackupContent,
    val sections: Set<BackupSection>,
    val format: Int
)

class BackupFormatException(message: String) : Exception(message)

class WrongPassphraseException : Exception("That passphrase doesn’t open this backup")
