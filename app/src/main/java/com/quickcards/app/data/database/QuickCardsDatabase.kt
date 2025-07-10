package com.quickcards.app.data.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import android.content.Context
import com.quickcards.app.data.dao.BankDao
import com.quickcards.app.data.dao.CardDao
import com.quickcards.app.data.dao.TagDao
import com.quickcards.app.data.model.Bank
import com.quickcards.app.data.model.Card
import com.quickcards.app.data.model.Converters
import com.quickcards.app.data.model.Tag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@Database(
    entities = [Card::class, Bank::class, Tag::class],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class QuickCardsDatabase : RoomDatabase() {
    
    abstract fun cardDao(): CardDao
    abstract fun bankDao(): BankDao
    abstract fun tagDao(): TagDao
    
    companion object {
        @Volatile
        private var INSTANCE: QuickCardsDatabase? = null
        
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
                .fallbackToDestructiveMigration() // ✅ Allow database recreation on schema changes
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
                        populateDatabase(database.bankDao(), database.tagDao())
                    }
                }
            }
            
            suspend fun populateDatabase(bankDao: BankDao, tagDao: TagDao) {
                // Insert default banks
                bankDao.insertBanks(Bank.getDefaultBanks())
                
                // Insert default tags
                tagDao.insertTags(Tag.getDefaultTags())
            }
        }
    }
}