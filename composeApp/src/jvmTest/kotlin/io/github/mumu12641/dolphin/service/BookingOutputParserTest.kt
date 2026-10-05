package io.github.mumu12641.dolphin.service

import io.github.mumu12641.dolphin.model.LogEntry
import io.github.mumu12641.dolphin.model.LogLevel
import io.github.mumu12641.dolphin.model.displayEmoji
import io.github.mumu12641.dolphin.model.displayTime
import io.github.mumu12641.dolphin.model.formattedLine
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class BookingOutputParserTest {
    @Test
    fun parsesCurrentPythonLogWithoutDuplicatingTimestampOrLevel() {
        val entry = BookingOutputParser().parse(
            "2026-10-01 16:33:14.832 | INFO    | ⏰ | 定时执行：10-02 08:00:03",
            LocalDateTime.of(2026, 10, 1, 16, 33, 15)
        )!!

        assertEquals(LogLevel.INFO, entry.level)
        assertEquals(LocalDateTime.of(2026, 10, 1, 16, 33, 14, 832_000_000), entry.timestamp)
        assertEquals("定时执行：10-02 08:00:03", entry.message)
        assertEquals("⏰", entry.displayEmoji())
        assertEquals("16:33:14", entry.displayTime())
        assertEquals(
            "2026-10-01 16:33:14.832 | INFO    | ⏰ 定时执行：10-02 08:00:03",
            entry.formattedLine()
        )
    }

    @Test
    fun parsesLegacyLogAcrossMidnight() {
        val entry = BookingOutputParser().parse(
            "23:59:59 | WARNING | 场地已被占用",
            LocalDateTime.of(2026, 10, 2, 0, 0, 1)
        )!!

        assertEquals(LogLevel.WARNING, entry.level)
        assertEquals(LocalDateTime.of(2026, 10, 1, 23, 59, 59), entry.timestamp)
        assertEquals("场地已被占用", entry.message)
    }

    @Test
    fun messageWordsDoNotChangeLevelAndTracebackRemainsError() {
        val parser = BookingOutputParser()
        val time = LocalDateTime.of(2026, 10, 1, 16, 33)
        assertEquals(LogLevel.INFO, parser.parse("2026-10-01 16:33:00.000 | INFO    | ERROR 字样只是正文", time)?.level)
        assertEquals(LogLevel.ERROR, parser.parse("2026-10-01 16:33:01.000 | ERROR   | 出现异常", time)?.level)
        assertEquals(LogLevel.ERROR, parser.parse("Traceback (most recent call last):", time)?.level)
        assertNull(parser.parse("", time))
    }

    @Test
    fun mapsEveryPythonLevelAndPreservesUnstructuredOutput() {
        val parser = BookingOutputParser()
        val time = LocalDateTime.of(2026, 10, 1, 16, 33)
        assertEquals(LogLevel.DEBUG, parser.parse("2026-10-01 16:33:00.000 | DEBUG   | 调试", time)?.level)
        assertEquals(LogLevel.WARNING, parser.parse("2026-10-01 16:33:00.000 | WARNING | 注意", time)?.level)
        assertEquals(LogLevel.ERROR, parser.parse("2026-10-01 16:33:00.000 | CRITICAL | 严重错误", time)?.level)
        assertEquals("普通输出", BookingOutputParser().parse("普通输出", time)?.message)
        assertEquals(LogLevel.INFO, BookingOutputParser().parse("普通输出", time)?.level)
    }

    @Test
    fun preservesLongMessagesAndEventSpecificEmojiAtTheSameLevel() {
        val timestamp = LocalDateTime.of(2026, 10, 1, 16, 33)
        val parser = BookingOutputParser()
        val login = parser.parse("2026-10-01 16:33:00.000 | INFO    | 🔐 | 正在登录统一认证", timestamp)!!
        val court = parser.parse("2026-10-01 16:33:01.000 | INFO    | 🏸 | 尝试 1 号场", timestamp)!!
        assertEquals("🔐", login.displayEmoji())
        assertEquals("🏸", court.displayEmoji())
        assertEquals(login.level, court.level)
        val details = LogEntry("第一行\n第二行", timestamp = timestamp, emoji = "🧵")
        assertEquals("2026-10-01 16:33:00.000 | INFO    | 🧵 第一行\n第二行", details.formattedLine())
    }

    @Test
    fun acceptsOlderLogsWithoutMistakingBodyTextForEmojiMetadata() {
        val entry = BookingOutputParser().parse("2026-10-01 16:33:00.000 | INFO    | 消息 | 正文")!!
        assertEquals("消息 | 正文", entry.message)
        assertEquals("💬", entry.displayEmoji())
    }
}
