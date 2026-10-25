package com.quickcards.app.core.data.repository

import androidx.room.withTransaction
import com.quickcards.app.core.data.database.QuickCardsDatabase
import com.quickcards.app.core.data.model.CardType
import com.quickcards.app.core.data.model.VaultCard
import com.quickcards.domain.CardNumberRules
import java.util.Locale

/** Debug-only sample data. Wipes the vault first. */
object DemoSeeder {

    private val BANK_NAMES = listOf(
        "American Express",
        "AU Small Finance Bank",
        "Axis Bank",
        "Bank of Baroda",
        "Federal Bank",
        "HDFC Bank",
        "ICICI Bank",
        "IDFC FIRST Bank",
        "IndusInd Bank",
        "Kotak Mahindra Bank",
        "State Bank of India",
        "Yes Bank"
    )

    private val TYPE_NAMES = listOf("Credit", "Debit", "Prepaid", "Forex")

    private val TAG_NAMES = listOf("Travel", "Online", "Fuel", "Bills", "Business")

    private data class DemoCard(
        val holder: String,
        val bank: String,
        val variant: String,
        val prefix: String,
        val expiry: String,
        val cvv: String,
        val band: String,
        val type: String?,
        val tags: List<String>,
        val note: String
    )

    private val CARDS = listOf(
        DemoCard("Aarav Mehta", "HDFC Bank", "Regalia", "453277810349221", "12/29", "381", "navy", "Credit", listOf("Travel", "Online"), "Everyday spending"),
        DemoCard("Aarav Mehta", "ICICI Bank", "Amazon Pay", "431581026674093", "08/28", "902", "rust", "Credit", listOf("Online"), ""),
        DemoCard("Aarav Mehta", "HDFC Bank", "Diners Black", "3622720627166", "05/30", "447", "graphite", null, listOf("Travel"), "Lounge access"),
        DemoCard("Aarav Mehta", "State Bank of India", "SimplySAVE", "652188047712993", "03/27", "117", "forest", "Credit", listOf("Fuel"), ""),
        DemoCard("Priya Mehta", "American Express", "Platinum", "37828224631000", "09/28", "4821", "plum", "Credit", listOf("Business", "Travel"), ""),
        DemoCard("Priya Mehta", "Kotak Mahindra Bank", "811 Debit", "416622095583107", "06/29", "228", "navy", "Debit", listOf("Bills"), ""),
        DemoCard("Priya Mehta", "Axis Bank", "Flipkart", "524199001137448", "11/30", "645", "rust", "Credit", emptyList(), "")
    )

    suspend fun seed(database: QuickCardsDatabase, repository: VaultRepository, cardCount: Int = 0) {
        val bankDao = database.bankDao()
        val holderDao = database.holderDao()
        val typeDao = database.cardTypeDao()
        val tagDao = database.tagDao()

        database.withTransaction {
            database.cardTagDao().deleteAllLinks()
            database.cardDao().deleteAllCards()
            holderDao.deleteAllHolders()
            tagDao.deleteAllTags()
            bankDao.deleteAllBanks()
        }

        val holders = listOf("Aarav Mehta", "Priya Mehta")
            .associateWith { repository.createHolder(it) }
        val banks = BANK_NAMES.associateWith { repository.createBank(it) }
        val tags = TAG_NAMES.associateWith { repository.createTag(it) }

        val typesByName = typeDao.getAllCardTypesSync()
            .associateBy { it.name.lowercase(Locale.ROOT) }
            .toMutableMap()
        val types = TYPE_NAMES.associateWith { name ->
            typesByName[name.lowercase(Locale.ROOT)] ?: run {
                val created = CardType(name = name, sortOrder = typeDao.maxSortOrder() + 1)
                typeDao.insertCardType(created)
                typesByName[name.lowercase(Locale.ROOT)] = created
                created
            }
        }

        for (demo in CARDS) {
            val holder = holders.getValue(demo.holder)
            val bank = banks.getValue(demo.bank)
            val type = demo.type?.let { types.getValue(it) }
            val number = demo.prefix + CardNumberRules.luhnCheckDigit(demo.prefix)
            repository.insertCard(
                VaultCard(
                    number = number,
                    expiry = demo.expiry,
                    cvv = demo.cvv,
                    holderId = holder.id,
                    bankId = bank.id,
                    networkId = null,
                    networkAuto = true,
                    typeId = type?.id,
                    nameOnCard = holder.name.uppercase(Locale.ROOT),
                    cardName = demo.variant,
                    note = demo.note,
                    bandColor = demo.band,
                    tagIds = demo.tags.map { tags.getValue(it).id }
                )
            )
        }

        if (cardCount > CARDS.size) {
            val holderList = holders.values.toList()
            val bankList = banks.values.toList()
            val typeList = types.values.toList()
            val bands = listOf("navy", "rust", "forest", "plum", "graphite")
            for (index in CARDS.size until cardCount) {
                val prefix = "411111111111" + (index % 1000).toString().padStart(3, '0')
                val number = prefix + CardNumberRules.luhnCheckDigit(prefix)
                val holder = holderList[index % holderList.size]
                repository.insertCard(
                    VaultCard(
                        number = number,
                        expiry = "%02d/%02d".format((index % 12) + 1, 26 + (index % 5)),
                        cvv = (100 + index % 900).toString(),
                        holderId = holder.id,
                        bankId = bankList[index % bankList.size].id,
                        networkId = null,
                        networkAuto = true,
                        typeId = typeList[index % typeList.size].id,
                        nameOnCard = holder.name.uppercase(Locale.ROOT),
                        cardName = "",
                        note = "",
                        bandColor = bands[index % bands.size],
                        tagIds = emptyList()
                    )
                )
            }
        }
    }
}
