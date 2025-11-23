package io.github.mumu12641.dolphin.ui.page.main

import io.github.mumu12641.dolphin.data.UserPreferencesRepository
import io.github.mumu12641.dolphin.di.Graph
import io.github.mumu12641.dolphin.model.BookingInfo
import io.github.mumu12641.dolphin.model.HistoryEntry
import io.github.mumu12641.dolphin.service.BookingService
import io.github.mumu12641.dolphin.util.Constant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModelScope
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date

sealed class MainAction {
    data class SelectVenue(val venue: String) : MainAction()
    data class ClickCourt(val courtNumber: Int) : MainAction()
    data class SelectTimeSlot(val timeSlot: String) : MainAction()
    data object ClearSelectedCourts : MainAction()
    data object StartConfig : MainAction()
    data object StartBooking : MainAction()
    data object StopBooking : MainAction()
    data object BackToHome : MainAction()
    data object SaveLogToFile : MainAction()
}

data class MainUiState(
    val username: String = "",
    val password: String = "",
    val history: List<HistoryEntry> = emptyList(),
    val selectedVenue: String = Constant.VENUES.first(),
    val selectedCourts: List<Int> = emptyList(),
    val selectedTimeSlot: String = Constant.TIME_SLOTS[5],
    val logMessages: List<LogEntry> = emptyList(),
    val bookingState: BookingState = BookingState.IDLE,
    val venues: List<String> = Constant.VENUES,
    val courtCount: Int = Constant.VENUE_COURT_COUNTS[Constant.VENUES.first()]!!
)


@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel : ViewModel() {

    private val historyRepository = Graph.historyRepository

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()


    init {
        viewModelScope.launch {
            UserPreferencesRepository.userFlow
                .flatMapLatest { user ->
                    val username = user.first
                    val password = user.second
                    if (username.isNotBlank()) {
                        historyRepository.getHistoryByUsername(username).map { userHistory ->
                            _uiState.value.copy(
                                username = username,
                                password = password,
                                history = userHistory?.history ?: emptyList()
                            )
                        }
                    } else {
                        flowOf(
                            _uiState.value.copy(
                                username = "",
                                password = "",
                                history = emptyList()
                            )
                        )
                    }
                }
                .collect { state ->
                    _uiState.value = state
                }
        }
    }

    fun onAction(action: MainAction) {
        when (action) {
            is MainAction.SelectVenue -> onVenueSelected(action.venue)
            is MainAction.ClickCourt -> onCourtClicked(action.courtNumber)
            is MainAction.SelectTimeSlot -> _uiState.update { it.copy(selectedTimeSlot = action.timeSlot) }
            MainAction.ClearSelectedCourts -> _uiState.update { it.copy(selectedCourts = emptyList()) }
            MainAction.StartConfig -> _uiState.update { it.copy(bookingState = BookingState.CONFIG) }
            MainAction.StartBooking -> startBooking()
            MainAction.StopBooking -> stopBooking()
            MainAction.BackToHome -> _uiState.update {
                it.copy(
                    bookingState = BookingState.IDLE
                )
            }

            MainAction.SaveLogToFile -> saveLogToFile()
        }
    }


    fun onVenueSelected(venue: String) {
        if (_uiState.value.selectedVenue != venue) {
            _uiState.update {
                it.copy(
                    selectedVenue = venue,
                    selectedCourts = emptyList(),
                    courtCount = Constant.VENUE_COURT_COUNTS[venue] ?: 0
                )
            }
        }
    }

    fun onCourtClicked(courtNumber: Int) {
        val newSelectedCourts = _uiState.value.selectedCourts.toMutableList()
        if (newSelectedCourts.contains(courtNumber)) {
            newSelectedCourts.remove(courtNumber)
        } else {
            newSelectedCourts.add(courtNumber)
        }
        _uiState.update { it.copy(selectedCourts = newSelectedCourts) }
    }


    fun startBooking() {
        _uiState.update {
            it.copy(
                logMessages = emptyList(),
                bookingState = BookingState.RUNNING
            )
        }

        viewModelScope.launch {
//            val user = UserPreferencesRepository.userFlow.first()
//            val username = user.first
//            val password = user.second
            val username = _uiState.value.username
            val password = _uiState.value.password

            if (username.isBlank() || password.isBlank()) {
                addLog("❌ 用户信息未填写。请前往设置页面填写。", LogType.ERROR)
                _uiState.update { it.copy(bookingState = BookingState.STOPPED) }
                return@launch
            }

            val courts = _uiState.value.selectedCourts.toMutableList()
            if (_uiState.value.courtCount > 0 && courts.size < _uiState.value.courtCount) {
                val remainingCourts =
                    (1.._uiState.value.courtCount).filter { it !in courts }.shuffled()
                courts.addAll(remainingCourts)
                _uiState.update { it.copy(selectedCourts = courts) }
            }
            val priorityList =
                _uiState.value.selectedCourts.mapNotNull {
                    Constant.COURT_IDS[_uiState.value.selectedVenue]?.get(
                        it
                    )
                }
                    .joinToString(",")

//            val executablePath =
//                File(System.getProperty("user.dir"), "Goodminton.exe").absolutePath
            val executablePath = File("D:\\Softwares\\Dolphin", "Goodminton.exe").absolutePath


            val bookingInfo = BookingInfo(
                venueId = Constant.VENUE_IDS[_uiState.value.selectedVenue]!!,
                startTime = _uiState.value.selectedTimeSlot.split("-").first(),
                priorityList = priorityList,
                username = username,
                password = password
            )

            BookingService.start(
                executablePath = executablePath,
                bookingInfo = bookingInfo
            ).onCompletion {
                _uiState.update { it.copy(bookingState = BookingState.STOPPED) }
            }.collect { logEntry ->
                _uiState.update {
                    it.copy(logMessages = it.logMessages + logEntry)
                }
            }
        }
    }

    fun stopBooking() {
        BookingService.stop()
        addLog("🛑 预约已中止。", LogType.INFO)
        _uiState.update { it.copy(bookingState = BookingState.STOPPED) }
    }


    fun saveLogToFile() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val appDir = File(System.getProperty("user.dir"))
                val logDir = File(appDir, "log")
                if (!logDir.exists()) {
                    logDir.mkdirs()
                }
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
                val existingLogFiles = logDir.listFiles { _, name ->
                    name.startsWith("dolphin_") && name.endsWith(".log")
                } ?: emptyArray()
                val nextNumber = existingLogFiles.size + 1
                val fileNumber = String.format("%03d", nextNumber)
                val logFile = File(logDir, "dolphin_${fileNumber}_${timestamp}.log")
                logFile.bufferedWriter().use { writer ->
                    _uiState.value.logMessages.forEach {
                        writer.write("[${it.timestamp}] ${it.message}\n")
                    }
                }
                addLog("📝 日志已保存到: ${logFile.absolutePath}", LogType.INFO)
            } catch (e: IOException) {
                addLog("❌ 日志保存失败: ${e.message}", LogType.ERROR)
            }
        }
    }

    private fun addLog(message: String, type: LogType) {
        val timestamp = SimpleDateFormat("yyyy-mm-dd HH:mm:ss.SSS").format(Date())
        _uiState.update {
            it.copy(logMessages = it.logMessages + LogEntry(message, timestamp, type))
        }
    }
}

enum class BookingState {
    IDLE, RUNNING, STOPPED, CONFIG
}

enum class LogType {
    INFO, WARNING, DEBUG, ERROR
}

data class LogEntry(
    val message: String,
    val timestamp: String,
    val type: LogType,
    val exeLog: Boolean = false
)
