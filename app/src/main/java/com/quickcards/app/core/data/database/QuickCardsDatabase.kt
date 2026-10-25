package com.quickcards.app.core.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.quickcards.app.core.data.dao.BankDao
import com.quickcards.app.core.data.dao.CardDao
import com.quickcards.app.core.data.dao.CardNetworkDao
import com.quickcards.app.core.data.dao.CardRepairDao
import com.quickcards.app.core.data.dao.CardTagDao
import com.quickcards.app.core.data.dao.CardTypeDao
import com.quickcards.app.core.data.dao.HolderDao
import com.quickcards.app.core.data.dao.TagDao
import com.quickcards.app.core.data.model.Bank
import com.quickcards.app.core.data.model.Card
import com.quickcards.app.core.data.model.CardNetwork
import com.quickcards.app.core.data.model.CardRepairEntity
import com.quickcards.app.core.data.model.CardTag
import com.quickcards.app.core.data.model.CardType
import com.quickcards.app.core.data.model.Holder
import com.quickcards.app.core.data.model.SeedData
import com.quickcards.app.core.data.model.Tag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Database(
    entities = [
        Card::class,
        Bank::class,
        Holder::class,
        CardNetwork::class,
        CardType::class,
        Tag::class,
        CardTag::class,
        CardRepairEntity::class
    ],
    version = 13,
    exportSchema = true
)
abstract class QuickCardsDatabase : RoomDatabase() {

    abstract fun cardDao(): CardDao
    abstract fun cardRepairDao(): CardRepairDao
    abstract fun bankDao(): BankDao
    abstract fun holderDao(): HolderDao
    abstract fun networkDao(): CardNetworkDao
    abstract fun cardTypeDao(): CardTypeDao
    abstract fun tagDao(): TagDao
    abstract fun cardTagDao(): CardTagDao

    companion object {
        @Volatile
        private var INSTANCE: QuickCardsDatabase? = null

        val MIGRATION_6_7 = object : Migration(6, 7) {
            override fun migrate(database: SupportSQLiteDatabase) {
            }
        }

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE cards ADD COLUMN panHash TEXT")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_cards_panHash` ON `cards` (`panHash`)")
            }
        }

        val MIGRATION_8_9 = object : Migration(8, 9) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `cards_new` (
                        `id` TEXT NOT NULL,
                        `cardNumber` TEXT NOT NULL,
                        `expiryDate` TEXT NOT NULL,
                        `cvv` TEXT NOT NULL,
                        `bankName` TEXT NOT NULL,
                        `cardType` TEXT NOT NULL,
                        `cardIssuer` TEXT NOT NULL,
                        `cardVariant` TEXT NOT NULL,
                        `owner` TEXT NOT NULL,
                        `description` TEXT NOT NULL,
                        `cardColor` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `updatedAt` INTEGER NOT NULL,
                        `panHash` TEXT,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    INSERT INTO `cards_new` (
                        `id`, `cardNumber`, `expiryDate`, `cvv`, `bankName`, `cardType`,
                        `cardIssuer`, `cardVariant`, `owner`, `description`, `cardColor`,
                        `createdAt`, `updatedAt`, `panHash`
                    )
                    SELECT
                        `id`, `cardNumber`, `expiryDate`, `cvv`, `bankName`, `cardType`,
                        `cardIssuer`, `cardVariant`, `owner`, `description`, `cardColor`,
                        `createdAt`, `updatedAt`, `panHash`
                    FROM `cards`
                    """.trimIndent()
                )
                database.execSQL("DROP TABLE `cards`")
                database.execSQL("ALTER TABLE `cards_new` RENAME TO `cards`")
                database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_cards_panHash` ON `cards` (`panHash`)")
                database.execSQL("DROP TABLE IF EXISTS `tags`")
            }
        }

        val MIGRATION_9_10 = object : Migration(9, 10) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `holders` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `networks` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `code` TEXT NOT NULL,
                        `prefixes` TEXT NOT NULL,
                        `lengths` TEXT NOT NULL,
                        `cvvLength` INTEGER NOT NULL,
                        `luhn` INTEGER NOT NULL,
                        `groups` TEXT NOT NULL DEFAULT '',
                        `sortOrder` INTEGER NOT NULL,
                        `builtIn` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `card_types` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `sortOrder` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tags` (
                        `id` TEXT NOT NULL,
                        `name` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `card_tags` (
                        `cardId` TEXT NOT NULL,
                        `tagId` TEXT NOT NULL,
                        PRIMARY KEY(`cardId`, `tagId`),
                        FOREIGN KEY(`cardId`) REFERENCES `cards`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_card_tags_tagId` ON `card_tags` (`tagId`)"
                )

                database.execSQL("ALTER TABLE `cards` ADD COLUMN `holderId` TEXT")
                database.execSQL("ALTER TABLE `cards` ADD COLUMN `bankId` TEXT")
                database.execSQL("ALTER TABLE `cards` ADD COLUMN `networkId` TEXT")
                database.execSQL("ALTER TABLE `cards` ADD COLUMN `networkAuto` INTEGER NOT NULL DEFAULT 1")
                database.execSQL("ALTER TABLE `cards` ADD COLUMN `typeId` TEXT")
                database.execSQL("ALTER TABLE `cards` ADD COLUMN `nameOnCard` TEXT NOT NULL DEFAULT ''")
                database.execSQL("ALTER TABLE `cards` ADD COLUMN `bandColor` TEXT NOT NULL DEFAULT 'navy'")
                database.execSQL("ALTER TABLE `cards` ADD COLUMN `sortOrder` INTEGER NOT NULL DEFAULT 0")

                seedNetworks(database)
                seedCardTypes(database)

                database.execSQL(
                    """
                    UPDATE `cards` SET `sortOrder` = (
                        SELECT COUNT(*) FROM `cards` AS `earlier`
                        WHERE `earlier`.`createdAt` < `cards`.`createdAt`
                           OR (`earlier`.`createdAt` = `cards`.`createdAt`
                               AND `earlier`.`id` < `cards`.`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_10_11 = object : Migration(10, 11) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `cards` ADD COLUMN `pinned` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `cards` ADD COLUMN `pinnedAt` INTEGER")
            }
        }

        val MIGRATION_12_13 = object : Migration(12, 13) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `cards` ADD COLUMN `dueDay` INTEGER")
            }
        }

        val MIGRATION_11_12 = object : Migration(11, 12) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE `cards` ADD COLUMN `archived` INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE `cards` ADD COLUMN `archivedAt` INTEGER")
                database.execSQL("ALTER TABLE `cards` ADD COLUMN `archiveReason` TEXT")
                database.execSQL("ALTER TABLE `cards` ADD COLUMN `needsReview` INTEGER NOT NULL DEFAULT 0")
                database.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `card_repairs` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `cardId` TEXT NOT NULL,
                        `field` TEXT NOT NULL,
                        `before` TEXT NOT NULL,
                        `after` TEXT NOT NULL,
                        `at` INTEGER NOT NULL,
                        FOREIGN KEY(`cardId`) REFERENCES `cards`(`id`)
                            ON UPDATE NO ACTION ON DELETE CASCADE
                    )
                    """.trimIndent()
                )
                database.execSQL(
                    "CREATE INDEX IF NOT EXISTS `index_card_repairs_cardId` " +
                        "ON `card_repairs` (`cardId`)"
                )
            }
        }

        private fun seedNetworks(database: SupportSQLiteDatabase) {
            SeedData.networks.forEach { network ->
                database.execSQL(
                    """
                    INSERT OR IGNORE INTO `networks` (
                        `id`, `name`, `code`, `prefixes`, `lengths`,
                        `cvvLength`, `luhn`, `groups`, `sortOrder`, `builtIn`
                    ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """.trimIndent(),
                    arrayOf(
                        network.id,
                        network.name,
                        network.code,
                        network.prefixes,
                        network.lengths,
                        network.cvvLength,
                        if (network.luhn) 1 else 0,
                        network.groups,
                        network.sortOrder,
                        if (network.builtIn) 1 else 0
                    )
                )
            }
        }

        private fun seedCardTypes(database: SupportSQLiteDatabase) {
            SeedData.cardTypes.forEach { cardType ->
                database.execSQL(
                    """
                    INSERT OR IGNORE INTO `card_types` (`id`, `name`, `sortOrder`)
                    VALUES (?, ?, ?)
                    """.trimIndent(),
                    arrayOf(cardType.id, cardType.name, cardType.sortOrder)
                )
            }
        }

        fun getDatabase(
            context: Context,
            scope: CoroutineScope
        ): QuickCardsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    QuickCardsDatabase::class.java,
                    "quickcards_database"
                )
                    .addCallback(QuickCardsDatabaseCallback(scope))
                    .addMigrations(
                        MIGRATION_6_7,
                        MIGRATION_7_8,
                        MIGRATION_8_9,
                        MIGRATION_9_10,
                        MIGRATION_10_11,
                        MIGRATION_11_12,
                        MIGRATION_12_13
                    )
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class QuickCardsDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {

            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch {
                        populateDatabase(database)
                    }
                }
            }

            suspend fun populateDatabase(database: QuickCardsDatabase) {
                database.bankDao().insertBanks(Bank.getDefaultBanks())
                database.networkDao().insertNetworks(SeedData.networks)
                database.cardTypeDao().insertCardTypes(SeedData.cardTypes)
            }
        }
    }
}
