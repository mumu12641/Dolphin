package io.github.mumu12641.dolphin.ui.page.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.mumu12641.dolphin.data.UserPreferencesRepository
import io.github.mumu12641.dolphin.network.Login
import io.github.mumu12641.dolphin.util.Constant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModelScope
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

        viewModelScope.launch(Dispatchers.IO) {
            bookingState = BookingState.RUNNING
            val user = UserPreferencesRepository.userFlow.first()
            val username = user.first
            val password = user.second

            if (username.isBlank() || password.isBlank()) {
                addLog("用户信息未填写。请前往设置页面填写。", LogType.ERROR)
                bookingState = BookingState.STOPPED
                return@launch
            }
            addLog("用户信息校验通过。", LogType.INFO)
            addLog("正在登录...", LogType.INFO)
            val cookies = Login.login(username, password)
            addLog("登录成功。Cookies: $cookies", LogType.INFO)

            addLog("正在检查场地优先级设置...", LogType.INFO)
            val courtCount = getCourtCountForSelectedVenue()
            if (courtCount > 0 && selectedCourts.size < courtCount) {
                val remainingCourts = (1..courtCount).filter { it !in selectedCourts }.shuffled()
                selectedCourts.addAll(remainingCourts)
                addLog("部分或全部场地已随机设置优先级。", LogType.TIP)
            }
            addLog("最终场地优先级: ${selectedCourts.joinToString(" -> ")}", LogType.INFO)
            addLog("开始预约...", LogType.INFO)
        }
    }

    fun stopBooking() {
        addLog("预约已中止。", LogType.INFO)
        bookingState = BookingState.STOPPED
    }

    fun backToForm() {
        showLog = false
        bookingState = BookingState.IDLE
    }

    fun saveSettings() {
        // 在这里实现保存逻辑
        addLog("设置已保存!", LogType.INFO)
    }

    private fun addLog(message: String, type: LogType) {
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS").format(Date())
        logMessages.add(LogEntry(message, timestamp, type))
    }
}

enum class BookingState {
    IDLE, RUNNING, STOPPED
}

enum class LogType {
    INFO, ERROR, TIP
}

data class LogEntry(val message: String, val timestamp: String, val type: LogType)
