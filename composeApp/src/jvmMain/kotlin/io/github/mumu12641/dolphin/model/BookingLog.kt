package io.github.mumu12641.dolphin.model

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class LogLevel { DEBUG, INFO, WARNING, ERROR }

data class LogEntry(
    val message: String,
    val level: LogLevel = LogLevel.INFO,
    val timestamp: LocalDateTime = LocalDateTime.now(),
    val emoji: String? = null
)

private val logTimestampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
private val displayTimeFormat = DateTimeFormatter.ofPattern("HH:mm:ss")

fun LogEntry.formattedTimestamp(): String = timestamp.format(logTimestampFormat)

fun LogEntry.displayTime(): String = timestamp.format(displayTimeFormat)

fun LogEntry.displayEmoji(): String = emoji ?: "💬"

fun LogEntry.formattedLine(): String =
    "${formattedTimestamp()} | ${level.name.padEnd(7)} | ${displayEmoji()} $message"
