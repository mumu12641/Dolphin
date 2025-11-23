package io.github.mumu12641.dolphin.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class HistoryEntry(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val venue: String,
    val priority: List<Int>,
    val timeSlot: String,
    val status: Int
)
