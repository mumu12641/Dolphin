package io.github.mumu12641.dolphin.service

import io.github.mumu12641.dolphin.model.LogEntry
import io.github.mumu12641.dolphin.model.LogLevel
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.IOException

enum class BookingOutcome { SUCCESS, FAILED, ABORT }

sealed interface BookingEvent {
    data class Message(val entry: LogEntry) : BookingEvent
    data class Finished(val outcome: BookingOutcome) : BookingEvent
}

object BookingService {

    @Volatile private var process: Process? = null
    @Volatile private var stopRequested = false

    fun start(
        venueId: String,
        startTime: String,
        priorityList: String,
        username: String,
        password: String,
        scheduleTime: String
    ): Flow<BookingEvent> {
        stopRequested = false
        return flow {
            try {
                val runtime = findBookingRuntime(System.getProperty("compose.application.resources.dir"))
                val command = listOf(
                    runtime.python.absolutePath,
                    "-I", "-u", runtime.script.absolutePath,
                    "--cdbh", venueId,
                    "--start_time", startTime,
                    "--order_date_after_today", "2",
                    "--schedule_time", scheduleTime,
                    "--select_pay_type", "-1",
                    "--priority_list", priorityList
                )
                if (stopRequested) {
                    emit(BookingEvent.Message(LogEntry("预约程序已停止。", emoji = "🛑")))
                    emit(BookingEvent.Finished(BookingOutcome.ABORT))
                    return@flow
                }
                val processBuilder = ProcessBuilder(command)
                processBuilder.directory(runtime.root)
                processBuilder.redirectErrorStream(true)
                processBuilder.environment().apply {
                    remove("PYTHONHOME")
                    remove("PYTHONPATH")
                    remove("PYTHONUSERBASE")
                    put("PYTHONUTF8", "1")
                    put("PYTHONIOENCODING", "utf-8")
                    put("DOLPHIN_CREDENTIALS_STDIN", "1")
                }
                val running = processBuilder.start()
                process = running
                if (stopRequested) running.destroyForcibly()
                try {
                    running.outputStream.bufferedWriter(Charsets.UTF_8).use { writer ->
                        writer.appendLine(Json.encodeToString(mapOf("username" to username, "password" to password)))
                    }
                } catch (error: IOException) {
                    if (running.isAlive && !stopRequested) throw error
                }
                emit(
                    BookingEvent.Message(
                        LogEntry("预约程序已启动", emoji = "🚀")
                    )
                )
                val parser = BookingOutputParser()
                running.inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                    lines.forEach { line ->
                        parser.parse(line)?.let { entry ->
                            val safeMessage = if (password.isEmpty()) entry.message else entry.message.replace(password, "••••")
                            emit(BookingEvent.Message(entry.copy(message = safeMessage)))
                        }
                    }
                }
                val status = running.waitFor()
                val outcome = when {
                    stopRequested -> BookingOutcome.ABORT
                    status == 0 -> BookingOutcome.SUCCESS
                    else -> BookingOutcome.FAILED
                }
                val resultMessage = when (outcome) {
                    BookingOutcome.SUCCESS -> LogEntry("预约程序已退出", emoji = "🏁")
                    BookingOutcome.FAILED -> LogEntry("程序异常退出，退出码 $status", LogLevel.ERROR, emoji = "🔌")
                    BookingOutcome.ABORT -> LogEntry("预约程序已停止。", emoji = "🛑")
                }
                emit(BookingEvent.Message(resultMessage))
                emit(BookingEvent.Finished(outcome))
            } catch (e: Exception) {
                if (e is CancellationException) throw e
                val outcome = if (stopRequested) BookingOutcome.ABORT else BookingOutcome.FAILED
                if (outcome == BookingOutcome.ABORT) {
                    emit(BookingEvent.Message(LogEntry("预约程序已停止。", emoji = "🛑")))
                } else {
                    emit(BookingEvent.Message(LogEntry("程序运行异常：${e.message}", LogLevel.ERROR, emoji = "🧯")))
                }
                emit(BookingEvent.Finished(outcome))
            } finally {
                process?.takeIf { it.isAlive }?.destroyForcibly()
                process = null
            }
        }.flowOn(Dispatchers.IO)
    }

    fun stop() {
        stopRequested = true
        process?.let { proc ->
            try {
                proc.toHandle().descendants().forEach { it.destroyForcibly() }
                proc.destroyForcibly()
                proc.waitFor(3, java.util.concurrent.TimeUnit.SECONDS)
                process = null
            } catch (e: Exception) {
                proc.destroyForcibly()
                process = null
            }
        }
    }
}
