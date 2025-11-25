package io.github.mumu12641.dolphin.di

import io.github.mumu12641.dolphin.database.AppDatabase
import io.github.mumu12641.dolphin.database.HistoryRepository

object DatabaseModule {
    val database: AppDatabase by lazy {
        AppDatabase.getDatabase()
    }
    val historyRepository: HistoryRepository by lazy {
        HistoryRepository(database.historyDao())
    }
}
