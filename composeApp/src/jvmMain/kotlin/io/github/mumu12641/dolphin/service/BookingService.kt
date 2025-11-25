package io.github.mumu12641.dolphin.service

import io.github.mumu12641.dolphin.ui.page.main.LogEntry
import io.github.mumu12641.dolphin.ui.page.main.LogType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import java.text.SimpleDateFormat
import java.util.Date

object BookingService {

    private var process: Process? = null
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")

    fun start(
        executablePath: String,
        venueId: String,
        startTime: String,
        priorityList: String,
        username: String,
        password: String
    ): Flow<LogEntry> = flow {
        val command = mutableListOf(
            executablePath,
            "--cdbh", venueId,
            "--start_time", startTime,
            "--order_date_after_today", "2",
            "--schedule_time", "08:00:01",
            "--select_pay_type", "-1",
            "--priority_list", priorityList,
            "--user_name", username,
            "--password", password
        )


        try {
            val processBuilder = ProcessBuilder(command)
            processBuilder.redirectErrorStream(true)
            process = processBuilder.start()
            emit(
                LogEntry(
                    "💻 运行命令: [${process?.pid()}]${command.joinToString(" ")}",
                    timeFormat.format(Date()),
                    LogType.INFO
                )
            )
            process?.inputStream?.bufferedReader()?.useLines { lines ->
                lines.forEach { line ->
                    val logEntry = if ("DEBUG" in line) {
                        LogEntry(line, timeFormat.format(Date()), LogType.DEBUG, true)
                    } else if ("INFO" in line) {
                        LogEntry(line, timeFormat.format(Date()), LogType.INFO, true)
                    } else if ("WARNING" in line) {
                        LogEntry(line, timeFormat.format(Date()), LogType.WARNING, true)
                    } else if ("ERROR" in line) {
                        LogEntry(line, timeFormat.format(Date()), LogType.ERROR, true)
                    } else {
                        LogEntry(line, timeFormat.format(Date()), LogType.INFO, true)
                    }
                    emit(logEntry)
                }
            }
            val status = process?.waitFor()
            when (status) {
                0 -> emit(LogEntry(type = LogType.SUCCESS))
                1 -> emit(LogEntry(type = LogType.FAILED))
                else -> emit(LogEntry(type = LogType.ABORT))
            }
        } catch (e: Exception) {
            emit(
                LogEntry(
                    "❌ 执行预约程序时出错: ${e.message}",
                    timeFormat.format(Date()),
                    LogType.ERROR
                )
            )
            emit(LogEntry(type = LogType.FAILED))
        }
    }.flowOn(Dispatchers.IO)

    fun stop() {
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
