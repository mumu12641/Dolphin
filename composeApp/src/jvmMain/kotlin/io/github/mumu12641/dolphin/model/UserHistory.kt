package io.github.mumu12641.dolphin.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class UserHistory(
    @PrimaryKey val username: String,
    val history: List<HistoryEntry>
)
