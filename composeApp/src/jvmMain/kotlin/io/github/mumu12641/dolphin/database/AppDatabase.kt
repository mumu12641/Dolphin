package io.github.mumu12641.dolphin.database

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import io.github.mumu12641.dolphin.model.HistoryEntry
import io.github.mumu12641.dolphin.model.UserHistory
import kotlinx.coroutines.Dispatchers
import java.io.File

@Database(entities = [UserHistory::class, HistoryEntry::class], version = 1)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun historyDao(): HistoryDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val dbFile = File(System.getProperty("user.home"), ".dolphin/dolphin_database.db")
                dbFile.parentFile.mkdirs() // Ensure the directory exists

                val builder = Room.databaseBuilder<AppDatabase>(
                    name = dbFile.absolutePath,
                )

                val instance = builder
                    .setDriver(BundledSQLiteDriver())
                    .setQueryCoroutineContext(Dispatchers.IO)
                    .build()

                INSTANCE = instance
                instance
            }
        }
    }
}
