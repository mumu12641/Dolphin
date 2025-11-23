package io.github.mumu12641.dolphin.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import io.github.mumu12641.dolphin.model.HistoryEntry
import io.github.mumu12641.dolphin.model.UserHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface HistoryDao {

    @Query("SELECT * FROM UserHistory WHERE username = :username")
    fun getHistoryByUsername(username: String): Flow<UserHistory?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(userHistory: UserHistory)

    @Query("SELECT * FROM UserHistory WHERE username = :username")
    suspend fun findUserHistory(username: String): UserHistory?

    @Transaction
    suspend fun addHistoryEntry(username: String, historyEntry: HistoryEntry) {
        val userHistory = findUserHistory(username)
        if (userHistory == null) {
            insertHistory(UserHistory(username, listOf(historyEntry)))
        } else {
            val updatedHistory = userHistory.history.toMutableList()
            updatedHistory.add(0, historyEntry) // Add to the beginning of the list
            insertHistory(userHistory.copy(history = updatedHistory))
        }
    }
}
