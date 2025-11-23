package io.github.mumu12641.dolphin.database

import io.github.mumu12641.dolphin.model.HistoryEntry
import io.github.mumu12641.dolphin.model.UserHistory
import kotlinx.coroutines.flow.Flow

class HistoryRepository(private val historyDao: HistoryDao) {

    fun getHistoryByUsername(username: String): Flow<UserHistory?> {
        return historyDao.getHistoryByUsername(username)
    }

    suspend fun addHistoryEntry(username: String, historyEntry: HistoryEntry) {
        historyDao.addHistoryEntry(username, historyEntry)
    }
}
