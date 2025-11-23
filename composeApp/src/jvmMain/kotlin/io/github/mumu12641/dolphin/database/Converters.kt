package io.github.mumu12641.dolphin.database

import androidx.room.TypeConverter
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.mumu12641.dolphin.model.HistoryEntry

class Converters {
    @TypeConverter
    fun fromHistoryEntryList(value: List<HistoryEntry>?): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toHistoryEntryList(value: String): List<HistoryEntry>? {
        val listType = object : TypeToken<List<HistoryEntry>>() {}.type
        return Gson().fromJson(value, listType)
    }

    @TypeConverter
    fun fromIntList(value: List<Int>?): String {
        return Gson().toJson(value)
    }

    @TypeConverter
    fun toIntList(value: String): List<Int>? {
        val listType = object : TypeToken<List<Int>>() {}.type
        return Gson().fromJson(value, listType)
    }
}
