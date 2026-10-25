package com.quickcards.domain

/**
 * Format 2 of the QuickCards backup, as specified by `snapshot` in
 * prototype/v2.html and the `#backup` section of prototype/timeline.html.
 *
 * The JSON document is always wrapped by the app's ExportCrypto helper
 * before it is written to disk; nothing here touches the filesystem.
 */
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
    val groups: String = ""
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
 * A parsed file: the normalized content plus which list sections the file
 * actually carried. Format 1 backups show only cards and banks in the review;
 * their holders and types are derived for the import but not listed.
 */
data class DecodedBackup(
    val content: BackupContent,
    val sections: Set<BackupSection>,
    val format: Int
)

class BackupFormatException(message: String) : Exception(message)

class WrongPassphraseException : Exception("That passphrase doesn’t open this backup")
