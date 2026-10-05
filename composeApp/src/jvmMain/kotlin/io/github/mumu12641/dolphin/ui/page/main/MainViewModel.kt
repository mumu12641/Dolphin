package io.github.mumu12641.dolphin.ui.page.main

import io.github.mumu12641.dolphin.data.PreferencesRepository
import io.github.mumu12641.dolphin.di.DatabaseModule
import io.github.mumu12641.dolphin.model.HistoryEntry
import io.github.mumu12641.dolphin.model.LogEntry
import io.github.mumu12641.dolphin.model.LogLevel
import io.github.mumu12641.dolphin.model.formattedLine
import io.github.mumu12641.dolphin.service.BookingEvent
import io.github.mumu12641.dolphin.service.BookingOutcome
import io.github.mumu12641.dolphin.service.BookingService
import io.github.mumu12641.dolphin.util.Constant
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModelScope
import java.io.File
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

enum class BookingStep { VENUE, TIME, COURT, REVIEW }
enum class BookingState { IDLE, CONFIG, RUNNING, SUCCESS, FAILED, ABORT }

sealed class MainAction {
    data class SelectVenue(val venue: String) : MainAction()
    data class ClickCourt(val courtNumber: Int) : MainAction()
    data class SelectTimeSlot(val timeSlot: String) : MainAction()
    data class StartConfigWithHistory(val historyEntry: HistoryEntry) : MainAction()
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
    val configStep: BookingStep = BookingStep.VENUE,
    val executionTime: String = "08:00:03",
    val targetDate: LocalDate = targetBookingDate(LocalDateTime.now(), "08:00:03"),
    val venues: List<String> = Constant.VENUES,
    val courtCount: Int = Constant.VENUE_COURT_COUNTS[Constant.VENUES.first()] ?: 0,
    val logMessages: List<LogEntry> = emptyList(),
    val bookingState: BookingState = BookingState.IDLE
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel : ViewModel() {
    private val historyRepository = DatabaseModule.historyRepository
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState = _uiState.asStateFlow()
    @Volatile private var bookingActive = false
    @Volatile private var stopPending = false

    init {
        viewModelScope.launch {
            PreferencesRepository.userFlow.flatMapLatest { (username, password) ->
                if (username.isBlank()) {
                    flowOf(Triple("", "", emptyList<HistoryEntry>()))
                } else {
                    historyRepository.getHistoryByUsername(username).map {
                        Triple(username, password, it?.history ?: emptyList())
                    }
                }
            }.collect { (username, password, history) ->
                _uiState.update { it.copy(username = username, password = password, history = history) }
            }
        }
        viewModelScope.launch {
            PreferencesRepository.executionTimeFlow.collect { time ->
                _uiState.update {
                    it.copy(
                        executionTime = time,
                        targetDate = targetBookingDate(LocalDateTime.now(), time)
                    )
                }
            }
        }
    }

    fun onAction(action: MainAction) {
        when (action) {
            is MainAction.SelectVenue -> onVenueSelected(action.venue)
            is MainAction.ClickCourt -> onCourtClicked(action.courtNumber)
            is MainAction.SelectTimeSlot -> {
                if (action.timeSlot in Constant.TIME_SLOTS) {
                    _uiState.update { it.copy(selectedTimeSlot = action.timeSlot) }
                }
            }
            is MainAction.StartConfigWithHistory -> {
                val entry = action.historyEntry
                val count = Constant.VENUE_COURT_COUNTS[entry.venue] ?: return
                if (entry.timeSlot !in Constant.TIME_SLOTS || bookingActive) return
                _uiState.update {
                    it.copy(
                        selectedVenue = entry.venue,
                        selectedCourts = entry.priority.distinct().filter { court -> court in 1..count },
                        selectedTimeSlot = entry.timeSlot,
                        courtCount = count,
                        configStep = BookingStep.REVIEW,
                        targetDate = targetBookingDate(LocalDateTime.now(), it.executionTime),
                        bookingState = BookingState.CONFIG
                    )
                }
            }
            MainAction.ClearSelectedCourts -> _uiState.update { it.copy(selectedCourts = emptyList()) }
            MainAction.StartConfig -> {
                if (bookingActive) return
                _uiState.update {
                    it.copy(
                        selectedVenue = Constant.VENUES.first(),
                        selectedCourts = emptyList(),
                        selectedTimeSlot = Constant.TIME_SLOTS[5],
                        courtCount = Constant.VENUE_COURT_COUNTS[Constant.VENUES.first()] ?: 0,
                        configStep = BookingStep.VENUE,
                        bookingState = BookingState.CONFIG
                    )
                }
            }
            MainAction.StartBooking -> startBooking()
            MainAction.StopBooking -> stopBooking()
            MainAction.BackToHome -> {
                if (!bookingActive) _uiState.update { it.copy(bookingState = BookingState.IDLE) }
            }
            MainAction.SaveLogToFile -> saveLogToFile()
        }
    }

    fun nextConfigStep() {
        _uiState.update {
            val next = when (it.configStep) {
                BookingStep.VENUE -> BookingStep.TIME
                BookingStep.TIME -> BookingStep.COURT
                BookingStep.COURT, BookingStep.REVIEW -> BookingStep.REVIEW
            }
            it.copy(
                configStep = next,
                targetDate = if (next == BookingStep.REVIEW) {
                    targetBookingDate(LocalDateTime.now(), it.executionTime)
                } else it.targetDate
            )
        }
    }

    fun previousConfigStep() {
        _uiState.update {
            it.copy(
                configStep = when (it.configStep) {
                    BookingStep.VENUE -> BookingStep.VENUE
                    BookingStep.TIME -> BookingStep.VENUE
                    BookingStep.COURT -> BookingStep.TIME
                    BookingStep.REVIEW -> BookingStep.COURT
                }
            )
        }
    }

    fun onVenueSelected(venue: String) {
        if (venue !in Constant.VENUES) return
        _uiState.update {
            if (it.selectedVenue == venue) it else it.copy(
                selectedVenue = venue,
                selectedCourts = emptyList(),
                courtCount = Constant.VENUE_COURT_COUNTS[venue] ?: 0
            )
        }
    }

    fun onCourtClicked(courtNumber: Int) {
        _uiState.update {
            if (courtNumber !in 1..it.courtCount) it else it.copy(
                selectedCourts = if (courtNumber in it.selectedCourts) {
                    it.selectedCourts - courtNumber
                } else {
                    it.selectedCourts + courtNumber
                }
            )
        }
    }

    fun startBooking() {
        if (bookingActive) return
        val config = _uiState.value
        if (config.bookingState != BookingState.CONFIG || config.configStep != BookingStep.REVIEW) return
        val currentTarget = targetBookingDate(LocalDateTime.now(), config.executionTime)
        if (currentTarget != config.targetDate) {
            _uiState.update { it.copy(targetDate = currentTarget) }
            return
        }
        if (config.username.isBlank() || config.password.isBlank()) {
            _uiState.update {
                it.copy(
                    bookingState = BookingState.FAILED,
                    logMessages = listOf(log("请在设置中填写账号和密码", LogLevel.ERROR, "👤"))
                )
            }
            return
        }

        val venueId = Constant.VENUE_IDS[config.selectedVenue] ?: return
        val courtIds = Constant.COURT_IDS[config.selectedVenue] ?: return
        val selected = config.selectedCourts.distinct().filter { it in 1..config.courtCount }
        val courts = selected + (1..config.courtCount).filter { it !in selected }.shuffled()
        val runFlow = BookingService.start(
            venueId = venueId,
            startTime = config.selectedTimeSlot.substringBefore("-"),
            priorityList = courts.mapNotNull(courtIds::get).joinToString(","),
            username = config.username,
            password = config.password,
            scheduleTime = config.executionTime
        )
        bookingActive = true
        stopPending = false
        _uiState.update {
            it.copy(
                bookingState = BookingState.RUNNING,
                logMessages = emptyList(),
                selectedCourts = courts
            )
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                runFlow.collect { event ->
                    when (event) {
                        is BookingEvent.Message -> _uiState.update {
                            it.copy(logMessages = it.logMessages + event.entry)
                        }
                        is BookingEvent.Finished -> {
                            val result = when (event.outcome) {
                                BookingOutcome.SUCCESS -> BookingState.SUCCESS
                                BookingOutcome.FAILED -> BookingState.FAILED
                                BookingOutcome.ABORT -> BookingState.ABORT
                            }
                            _uiState.update { it.copy(bookingState = result) }
                            val status = when (result) {
                                BookingState.SUCCESS -> 0
                                BookingState.FAILED -> 1
                                else -> 2
                            }
                            try {
                                historyRepository.addHistoryEntry(
                                    config.username,
                                    HistoryEntry(
                                        venue = config.selectedVenue,
                                        priority = courts,
                                        timeSlot = config.selectedTimeSlot,
                                        status = status
                                    )
                                )
                            } catch (error: Exception) {
                                addLog("记录保存失败：${error.message}", LogLevel.ERROR, "🗂️")
                            }
                        }
                    }
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                addLog("预约运行失败：${error.message}", LogLevel.ERROR, "🧨")
                _uiState.update { it.copy(bookingState = BookingState.FAILED) }
            } finally {
                bookingActive = false
                stopPending = false
            }
        }
    }

    fun stopBooking() {
        if (!bookingActive || stopPending || _uiState.value.bookingState != BookingState.RUNNING) return
        stopPending = true
        addLog("正在停止预约…", LogLevel.INFO, "✋")
        viewModelScope.launch(Dispatchers.IO) { BookingService.stop() }
    }

    fun saveLogToFile() {
        val messages = _uiState.value.logMessages
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val directory = File(System.getProperty("user.home"), "Dolphin/logs")
                check(directory.isDirectory || directory.mkdirs()) { "无法创建日志目录：$directory" }
                val filename = "dolphin_${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss_SSS"))}.log"
                val file = File(directory, filename)
                file.bufferedWriter().use { writer ->
                    messages.forEach { writer.appendLine(it.formattedLine()) }
                }
                addLog("日志已保存：${file.absolutePath}", LogLevel.INFO, "💾")
            } catch (error: Exception) {
                addLog("日志保存失败：${error.message}", LogLevel.ERROR, "📝")
            }
        }
    }

    private fun log(message: String, level: LogLevel, emoji: String) =
        LogEntry(message, level, emoji = emoji)

    private fun addLog(message: String, level: LogLevel, emoji: String) {
        _uiState.update { it.copy(logMessages = it.logMessages + log(message, level, emoji)) }
    }
}
