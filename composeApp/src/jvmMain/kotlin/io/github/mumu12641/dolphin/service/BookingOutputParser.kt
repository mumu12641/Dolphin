package io.github.mumu12641.dolphin.service

import io.github.mumu12641.dolphin.model.LogEntry
import io.github.mumu12641.dolphin.model.LogLevel
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter

/** Reads event-specific emoji metadata while accepting older console log formats. */
internal class BookingOutputParser {
    private val currentFormat = Regex("""^(\d{4}-\d{2}-\d{2} \d{2}:\d{2}:\d{2}\.\d{3}) \| (DEBUG|INFO|WARNING|ERROR|CRITICAL)\s*\| (.*)$""")
    private val legacyFormat = Regex("""^(\d{2}:\d{2}:\d{2}) \| (DEBUG|INFO|WARNING|ERROR|CRITICAL)\s*\| (.*)$""")
    private val timestampFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    private val emojiField = Regex("""^([^\p{L}\p{N}\s|]+) \| (.*)$""")
    private var previousLevel = LogLevel.INFO

    fun parse(line: String, receivedAt: LocalDateTime = LocalDateTime.now()): LogEntry? {
        if (line.isBlank()) return null
        currentFormat.matchEntire(line)?.let { match ->
            val timestamp = runCatching { LocalDateTime.parse(match.groupValues[1], timestampFormat) }
                .getOrDefault(receivedAt)
            val level = levelOf(match.groupValues[2])
            previousLevel = level
            return parseMessage(match.groupValues[3], level, timestamp)
        }
        legacyFormat.matchEntire(line)?.let { match ->
            val time = runCatching { LocalTime.parse(match.groupValues[1]) }.getOrNull()
            val timestamp = time?.let { nearestDateTime(it, receivedAt) } ?: receivedAt
            val level = levelOf(match.groupValues[2])
            previousLevel = level
            return parseMessage(match.groupValues[3], level, timestamp)
        }
        // Python traceback lines have no prefix; retain their error level.
        return LogEntry(
            line,
            if (previousLevel == LogLevel.ERROR) LogLevel.ERROR else LogLevel.INFO,
            receivedAt,
            emoji = if (previousLevel == LogLevel.ERROR) "🧵" else "💬"
        )
    }

    private fun parseMessage(message: String, level: LogLevel, timestamp: LocalDateTime): LogEntry {
        val metadata = emojiField.matchEntire(message)
        return LogEntry(
            message = metadata?.groupValues?.get(2) ?: message,
            level = level,
            timestamp = timestamp,
            emoji = metadata?.groupValues?.get(1)
        )
    }

    private fun levelOf(value: String): LogLevel = when (value) {
        "DEBUG" -> LogLevel.DEBUG
        "WARNING" -> LogLevel.WARNING
        "ERROR", "CRITICAL" -> LogLevel.ERROR
        else -> LogLevel.INFO
    }

    private fun nearestDateTime(time: LocalTime, receivedAt: LocalDateTime): LocalDateTime =
        (-1L..1L).map { offset -> LocalDateTime.of(receivedAt.toLocalDate().plusDays(offset), time) }
            .minBy { Duration.between(it, receivedAt).abs() }
}
