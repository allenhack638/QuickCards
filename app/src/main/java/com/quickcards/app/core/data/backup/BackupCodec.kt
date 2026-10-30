package com.quickcards.app.core.data.backup

import com.quickcards.domain.BACKUP_FORMAT
import com.quickcards.domain.BACKUP_APP_NAME
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonElement
import com.google.gson.JsonNull
import com.google.gson.JsonObject
import com.google.gson.JsonParser
import com.google.gson.reflect.TypeToken
import com.quickcards.app.core.data.model.BandColor
import com.quickcards.app.core.security.ExportCrypto
import com.quickcards.domain.BackupBank
import com.quickcards.domain.BackupCard
import com.quickcards.domain.BackupContent
import com.quickcards.domain.BackupFormatException
import com.quickcards.domain.BackupMeta
import com.quickcards.domain.BackupNamed
import com.quickcards.domain.BackupNetwork
import com.quickcards.domain.BackupSection
import com.quickcards.domain.DecodedBackup
import com.quickcards.domain.Monogram
import com.quickcards.domain.WrongPassphraseException
import java.util.Locale

/**
 * Payload inside the AES-256-GCM envelope. Format 2 is a JSON object; format 1 is a bare
 * `Card` array mapped like [com.quickcards.app.core.data.repository.LegacyBackfill].
 */
object BackupCodec {

    private const val MAX_CARDS = 10_000
    private const val MAX_LIST_ITEMS = 10_000
    private const val LEGACY_ME = "Me"

    private val WHITESPACE = Regex("""\s+""")

    fun encode(content: BackupContent): String {
        val root = JsonObject()
        content.meta?.let { meta ->
            root.add("meta", JsonObject().apply {
                addProperty("app", meta.app)
                addProperty("format", meta.format)
                addProperty("version", meta.version)
                addProperty("exportedAt", meta.exportedAt)
            })
        }
        root.add("holders", namedArray(content.holders))
        root.add("banks", JsonArray().apply {
            content.banks.forEach { bank ->
                add(JsonObject().apply {
                    addProperty("id", bank.id)
                    addProperty("name", bank.name)
                    addProperty("mono", bank.mono)
                })
            }
        })
        root.add("types", namedArray(content.types))
        root.add("tags", namedArray(content.tags))
        root.add("networks", JsonArray().apply {
            content.networks.forEach { network ->
                add(JsonObject().apply {
                    addProperty("id", network.id)
                    addProperty("name", network.name)
                    addProperty("code", network.code)
                    addProperty("prefixes", network.prefixes)
                    addProperty("lengths", network.lengths)
                    addProperty("cvv", network.cvv)
                    addProperty("luhn", network.luhn)
                    addProperty("groups", network.groups)
                    addProperty("cvvOptional", network.cvvOptional)
                })
            }
        })
        root.add("cards", JsonArray().apply {
            content.cards.forEach { card ->
                add(JsonObject().apply {
                    addProperty("id", card.id)
                    addNullableString("holderId", card.holderId)
                    addNullableString("bankId", card.bankId)
                    addProperty("networkAuto", card.networkAuto)
                    addNullableString("networkId", card.networkId)
                    addNullableString("typeId", card.typeId)
                    addProperty("variant", card.variant)
                    addProperty("number", card.number)
                    addProperty("exp", card.exp)
                    addProperty("cvv", card.cvv)
                    addProperty("nameOnCard", card.nameOnCard)
                    addProperty("color", card.color)
                    addProperty("note", card.note)
                    card.dueDay?.let { addProperty("dueDay", it) }
                    add("tagIds", JsonArray().apply { card.tagIds.forEach(::add) })
                    addProperty("sortOrder", card.sortOrder)
                    addProperty("pinned", card.pinned)
                    addNullableLong("pinnedAt", if (card.pinned) card.pinnedAt else null)
                    addProperty("archived", card.archived)
                    addNullableLong("archivedAt", if (card.archived) card.archivedAt else null)
                    addProperty("archiveReason", card.archiveReason)
                    addProperty("needsReview", card.needsReview)
                })
            }
        })
        content.settings?.let { settings ->
            root.add("cfg", Gson().toJsonTree(settings))
        }
        return Gson().toJson(root)
    }

    fun isEncrypted(fileText: String): Boolean {
        val trimmed = fileText.trimStart()
        return trimmed.startsWith("{") && ExportCrypto.isEnvelope(fileText)
    }

    /** Opens an encrypted envelope, or a plaintext legacy file ([passphrase] may be null). */
    fun decode(fileText: String, passphrase: CharArray?): DecodedBackup {
        if (isEncrypted(fileText)) {
            val pass = passphrase ?: throw WrongPassphraseException()
            val plaintext = try {
                ExportCrypto.decrypt(fileText, pass)
            } catch (_: Exception) {
                throw WrongPassphraseException()
            } catch (_: StackOverflowError) {
                throw WrongPassphraseException()
            }
            return decodePayload(plaintext)
        }
        return decodePayload(fileText)
    }

    fun decodePayload(payload: String): DecodedBackup {
        val root = try {
            JsonParser.parseString(payload)
        } catch (_: Exception) {
            throw BackupFormatException("Invalid backup file")
        } catch (_: StackOverflowError) {
            throw BackupFormatException("Invalid backup file")
        }

        if (root.isJsonArray) return decodeLegacyCards(root.asJsonArray)

        if (root.isJsonObject) {
            val objectRoot = root.asJsonObject
            val metaElement = objectRoot.get("meta")
            if (metaElement != null && metaElement.isJsonObject) {
                val meta = metaElement.asJsonObject
                val app = meta.stringOrNull("app")
                if (app != BACKUP_APP_NAME) {
                    throw BackupFormatException("Not a QuickCards backup")
                }
                val format = meta.intOrNull("format") ?: 0
                if (format != BACKUP_FORMAT) {
                    throw BackupFormatException("Unsupported backup format")
                }
                return decodeFormat2(objectRoot, meta)
            }
            val cards = objectRoot.get("cards")
            if (cards != null && cards.isJsonArray) {
                return decodeLegacyCards(cards.asJsonArray)
            }
        }
        throw BackupFormatException("Not a QuickCards backup")
    }

    private fun decodeFormat2(root: JsonObject, meta: JsonObject): DecodedBackup {
        val holders = namedItems(root.array("holders"), "holder")
        val banks = mutableListOf<BackupBank>()
        val bankArray = root.array("banks")
        if (bankArray.size() > MAX_LIST_ITEMS) throw BackupFormatException("Too many banks")
        bankArray.forEachIndexed { index, element ->
            if (!element.isJsonObject) return@forEachIndexed
            val item = element.asJsonObject
            val name = item.string("name").trim()
            if (name.isEmpty()) return@forEachIndexed
            banks += BackupBank(
                id = item.stringOrNull("id") ?: "bank-$index",
                name = name,
                mono = item.stringOrNull("mono").orEmpty()
            )
        }

        val types = namedItems(root.array("types"), "type")
        val tags = namedItems(root.array("tags"), "tag")

        val networks = mutableListOf<BackupNetwork>()
        val networkArray = root.array("networks")
        if (networkArray.size() > MAX_LIST_ITEMS) throw BackupFormatException("Too many networks")
        networkArray.forEachIndexed { index, element ->
            if (!element.isJsonObject) return@forEachIndexed
            val item = element.asJsonObject
            val name = item.string("name").trim()
            if (name.isEmpty()) return@forEachIndexed
            networks += BackupNetwork(
                id = item.stringOrNull("id") ?: "network-$index",
                name = name,
                code = item.string("code").trim().uppercase(Locale.ROOT),
                prefixes = item.string("prefixes").replace(WHITESPACE, ""),
                lengths = item.string("lengths").replace(WHITESPACE, ""),
                cvv = item.intOrNull("cvv")
                    ?: item.intOrNull("cvvLength")
                    ?: 3,
                luhn = item.booleanOrNull("luhn") ?: true,
                groups = item.string("groups"),
                cvvOptional = item.booleanOrNull("cvvOptional") ?: false
            )
        }

        val cards = mutableListOf<BackupCard>()
        val cardArray = root.array("cards")
        if (cardArray.size() > MAX_CARDS) throw BackupFormatException("Too many cards")
        cardArray.forEachIndexed { index, element ->
            if (!element.isJsonObject) return@forEachIndexed
            val item = element.asJsonObject
            val number = item.string("number").filter { it in '0'..'9' }
            if (number.isEmpty()) return@forEachIndexed
            val tagIds = mutableListOf<String>()
            val tagsElement = item.get("tagIds")
            if (tagsElement != null && tagsElement.isJsonArray) {
                tagsElement.asJsonArray.forEach { tag ->
                    if (tag.isJsonPrimitive) {
                        val id = tag.asString
                        if (id.isNotBlank()) tagIds += id
                    }
                }
            }
            cards += BackupCard(
                id = item.stringOrNull("id") ?: "card-$index",
                number = number,
                exp = item.string("exp"),
                cvv = item.string("cvv"),
                nameOnCard = item.string("nameOnCard"),
                holderId = item.stringOrNull("holderId"),
                bankId = item.stringOrNull("bankId"),
                networkAuto = item.booleanOrNull("networkAuto") ?: true,
                networkId = item.stringOrNull("networkId"),
                typeId = item.stringOrNull("typeId"),
                variant = item.string("variant"),
                note = item.string("note"),
                dueDay = item.intOrNull("dueDay")?.takeIf { it in 1..31 },
                color = normalizeBand(item.stringOrNull("color")),
                tagIds = tagIds.distinct(),
                sortOrder = item.intOrNull("sortOrder") ?: index,
                pinned = item.booleanOrNull("pinned") ?: false,
                pinnedAt = if (item.booleanOrNull("pinned") == true) {
                    item.longOrNull("pinnedAt")
                } else {
                    null
                },
                archived = item.booleanOrNull("archived") ?: false,
                archivedAt = if (item.booleanOrNull("archived") == true) {
                    item.longOrNull("archivedAt")
                } else {
                    null
                },
                archiveReason = item.string("archiveReason"),
                needsReview = item.intOrNull("needsReview") ?: 0
            )
        }

        val settings = root.get("cfg")
            ?.takeIf { it.isJsonObject }
            ?.let { gsonMap(it.asJsonObject) }

        return DecodedBackup(
            content = BackupContent(
                meta = BackupMeta(
                    app = BACKUP_APP_NAME,
                    format = BACKUP_FORMAT,
                    version = meta.string("version"),
                    exportedAt = meta.string("exportedAt")
                ),
                holders = holders,
                banks = banks,
                types = types,
                tags = tags,
                networks = networks,
                cards = cards,
                settings = settings
            ),
            sections = BackupSection.entries.toSet(),
            format = BACKUP_FORMAT
        )
    }

    /** Format 1: a bare array of Room `Card` objects, mapped like LegacyBackfill. */
    private fun decodeLegacyCards(array: JsonArray): DecodedBackup {
        if (array.size() > MAX_CARDS) throw BackupFormatException("Too many cards")

        val holders = LinkedHashMap<String, BackupNamed>()
        val banks = LinkedHashMap<String, BackupBank>()
        val types = LinkedHashMap<String, BackupNamed>()
        val cards = mutableListOf<BackupCard>()

        array.forEachIndexed { index, element ->
            if (!element.isJsonObject) return@forEachIndexed
            val item = element.asJsonObject
            val number = item.string("cardNumber").filter { it in '0'..'9' }
            if (number.isEmpty()) return@forEachIndexed

            val owner = normalizeOwner(item.string("owner"))
            val holderKey = owner.lowercase(Locale.ROOT)
            val holder = holders.getOrPut(holderKey) {
                BackupNamed(id = "legacy-holder-${holders.size + 1}", name = owner)
            }

            val bankName = normalizeName(item.string("bankName"))
            val bank = if (bankName.isEmpty()) {
                null
            } else {
                banks.getOrPut(bankName.lowercase(Locale.ROOT)) {
                    BackupBank(
                        id = "legacy-bank-${banks.size + 1}",
                        name = bankName,
                        mono = Monogram.monoFor(bankName)
                    )
                }
            }

            val typeName = normalizeName(item.string("cardType"))
            val type = if (typeName.isEmpty()) {
                null
            } else {
                types.getOrPut(typeName.lowercase(Locale.ROOT)) {
                    BackupNamed(id = "legacy-type-${types.size + 1}", name = typeName)
                }
            }

            cards += BackupCard(
                id = "legacy-card-${index + 1}",
                number = number,
                exp = item.string("expiryDate"),
                cvv = item.string("cvv"),
                nameOnCard = legacyNameOnCard(owner),
                holderId = holder.id,
                bankId = bank?.id,
                networkAuto = true,
                networkId = null,
                typeId = type?.id,
                variant = item.string("cardVariant"),
                note = item.string("description"),
                color = BandColor.importValue(item.stringOrNull("cardColor")),
                tagIds = emptyList(),
                sortOrder = index,
                legacyIssuer = item.string("cardIssuer").trim().ifEmpty { null }
            )
        }

        return DecodedBackup(
            content = BackupContent(
                meta = null,
                holders = holders.values.toList(),
                banks = banks.values.toList(),
                types = types.values.toList(),
                tags = emptyList(),
                networks = emptyList(),
                cards = cards,
                settings = null
            ),
            sections = setOf(BackupSection.BANKS),
            format = 1
        )
    }

    private fun namedItems(array: JsonArray, prefix: String): List<BackupNamed> {
        if (array.size() > MAX_LIST_ITEMS) throw BackupFormatException("Too many $prefix entries")
        val items = mutableListOf<BackupNamed>()
        array.forEachIndexed { index, element ->
            if (!element.isJsonObject) return@forEachIndexed
            val item = element.asJsonObject
            val name = normalizeName(item.string("name"))
            if (name.isEmpty()) return@forEachIndexed
            items += BackupNamed(
                id = item.stringOrNull("id") ?: "$prefix-$index",
                name = name
            )
        }
        return items
    }

    private fun namedArray(items: List<BackupNamed>): JsonArray = JsonArray().apply {
        items.forEach { item ->
            add(JsonObject().apply {
                addProperty("id", item.id)
                addProperty("name", item.name)
            })
        }
    }

    private fun gsonMap(objectRoot: JsonObject): Map<String, Any?>? = try {
        Gson().fromJson(objectRoot, object : TypeToken<Map<String, Any?>>() {}.type)
    } catch (_: Exception) {
        null
    }

    private fun normalizeBand(value: String?): String = BandColor.importValue(value)

    private fun normalizeOwner(owner: String): String {
        val trimmed = normalizeName(owner)
        return when {
            trimmed.isEmpty() -> LEGACY_ME
            trimmed.equals("me", ignoreCase = true) -> LEGACY_ME
            else -> trimmed
        }
    }

    private fun legacyNameOnCard(owner: String): String =
        if (owner.equals(LEGACY_ME, ignoreCase = true)) "" else owner.uppercase(Locale.ROOT)

    private fun normalizeName(value: String): String =
        value.trim().replace(WHITESPACE, " ")

    private fun JsonObject.string(key: String): String =
        stringOrNull(key).orEmpty()

    private fun JsonObject.stringOrNull(key: String): String? {
        val element = get(key) ?: return null
        if (element.isJsonNull) return null
        return runCatching { element.asString }.getOrNull()?.takeIf { it.isNotEmpty() }
    }

    private fun JsonObject.intOrNull(key: String): Int? {
        val element = get(key) ?: return null
        if (element.isJsonNull) return null
        return runCatching {
            if (element.isJsonPrimitive) element.asJsonPrimitive.asInt else null
        }.getOrNull()
    }

    private fun JsonObject.booleanOrNull(key: String): Boolean? {
        val element = get(key) ?: return null
        if (element.isJsonNull) return null
        return runCatching {
            if (element.isJsonPrimitive) element.asJsonPrimitive.asBoolean else null
        }.getOrNull()
    }

    private fun JsonObject.longOrNull(key: String): Long? {
        val element = get(key) ?: return null
        if (element.isJsonNull) return null
        return runCatching {
            if (element.isJsonPrimitive) element.asJsonPrimitive.asLong else null
        }.getOrNull()
    }

    private fun JsonObject.array(key: String): JsonArray {
        val element: JsonElement? = get(key)
        return if (element != null && element.isJsonArray) element.asJsonArray else JsonArray()
    }

    private fun JsonObject.addNullableString(key: String, value: String?) {
        if (value == null) {
            add(key, JsonNull.INSTANCE)
        } else {
            addProperty(key, value)
        }
    }

    private fun JsonObject.addNullableLong(key: String, value: Long?) {
        if (value == null) {
            add(key, JsonNull.INSTANCE)
        } else {
            addProperty(key, value)
        }
    }
}
