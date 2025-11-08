package io.github.mumu12641.dolphin.ui.page.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.mumu12641.dolphin.data.UserPreferencesRepository
import io.github.mumu12641.dolphin.model.BookingInfo
import io.github.mumu12641.dolphin.service.BookingService
import io.github.mumu12641.dolphin.util.Constant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onCompletion
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModelScope
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date

class MainViewModel : ViewModel() {

    val venues = Constant.VENUES
    val timeSlots = Constant.TIME_SLOTS

    var selectedVenue by mutableStateOf(venues.first())
        private set

    val selectedCourts = mutableStateListOf<Int>()

    var selectedTimeSlot by mutableStateOf(timeSlots[5])

    var showLog by mutableStateOf(false)
    val logMessages = mutableStateListOf<LogEntry>()

    var bookingState by mutableStateOf(BookingState.IDLE)
        private set

    fun getCourtCountForSelectedVenue(): Int {
        return Constant.VENUE_COURT_COUNTS[selectedVenue] ?: 0
    }

    fun onVenueSelected(venue: String) {
        if (selectedVenue != venue) {
            selectedVenue = venue
            selectedCourts.clear()
        }
    }

    fun onCourtClicked(courtNumber: Int) {
        if (selectedCourts.contains(courtNumber)) {
            selectedCourts.remove(courtNumber)
        } else {
            selectedCourts.add(courtNumber)
        }
    }

    fun startBooking() {
        logMessages.clear()
        showLog = true
        bookingState = BookingState.RUNNING

        viewModelScope.launch {
            val user = UserPreferencesRepository.userFlow.first()
            val username = user.first
            val password = user.second

            if (username.isBlank() || password.isBlank()) {
                addLog("❌ 用户信息未填写。请前往设置页面填写。", LogType.ERROR)
                withContext(Dispatchers.Main) {
                    bookingState = BookingState.STOPPED
                }
                return@launch
            }

            val courtCount = getCourtCountForSelectedVenue()
            val courts = selectedCourts.toMutableList()
            if (courtCount > 0 && courts.size < courtCount) {
                val remainingCourts = (1..courtCount).filter { it !in courts }.shuffled()
                courts.addAll(remainingCourts)
            }

            val priorityList =
                courts.mapNotNull { Constant.COURT_IDS[selectedVenue]?.get(it) }.joinToString(",")

            val executablePath =
                File(System.getProperty("user.dir"),"HUST_Booking_Assistant.exe").absolutePath


            val bookingInfo = BookingInfo(
                venueId = Constant.VENUE_IDS[selectedVenue]!!,
                startTime = selectedTimeSlot.split("-").first(),
                priorityList = priorityList,
                username = username,
                password = password
            )

            BookingService.start(
                executablePath = executablePath,
                bookingInfo = bookingInfo
            ).onCompletion {
                withContext(Dispatchers.Main) {
                    bookingState = BookingState.STOPPED
                }
            }.collect { logEntry ->
                logMessages.add(logEntry)
            }
        }
    }

    fun stopBooking() {
        BookingService.stop()
        addLog("预约已中止。", LogType.INFO)
        bookingState = BookingState.STOPPED
    }

    fun backToForm() {
        showLog = false
        bookingState = BookingState.IDLE
    }

    private fun addLog(message: String, type: LogType) {
        val timestamp = SimpleDateFormat("yyyy-mm-dd HH:mm:ss.SSS").format(Date())
        logMessages.add(LogEntry(message, timestamp, type))
    }
}

enum class BookingState {
    IDLE, RUNNING, STOPPED
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
