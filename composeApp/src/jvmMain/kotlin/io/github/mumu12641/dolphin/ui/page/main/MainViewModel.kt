package io.github.mumu12641.dolphin.ui.page.main

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import io.github.mumu12641.dolphin.data.UserPreferencesRepository
import io.github.mumu12641.dolphin.model.BookingInfo
import io.github.mumu12641.dolphin.network.Login
import io.github.mumu12641.dolphin.network.Network
import io.github.mumu12641.dolphin.network.Result
import io.github.mumu12641.dolphin.util.Constant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import moe.tlaster.precompose.viewmodel.ViewModel
import moe.tlaster.precompose.viewmodel.viewModelScope
import java.text.SimpleDateFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
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

            addLog("正在检查场地优先级设置...", LogType.INFO)
            val courtCount = getCourtCountForSelectedVenue()
            val courts = selectedCourts.toMutableList() // Create a mutable copy
            if (courtCount > 0 && courts.size < courtCount) {
                val remainingCourts = (1..courtCount).filter { it !in courts }.shuffled()
                courts.addAll(remainingCourts)
                addLog("部分或全部场地已随机设置优先级。", LogType.TIP)
            }
            addLog("最终场地优先级: ${courts.joinToString(" -> ")}", LogType.INFO)

            addLog("正在登录...", LogType.INFO)
            val cookies = when (val loginResult = Login.login(username, password)) {
                is Result.Success -> {
                    addLog("登录成功。", LogType.INFO)
                    loginResult.data
                }

                is Result.Error -> {
                    addLog("登录失败: ${loginResult.exception.message}", LogType.ERROR)
                    bookingState = BookingState.STOPPED
                    return@launch
                }
            }
            println(cookies)

            val (startTime, endTime) = selectedTimeSlot.split("-")
            val orderDate =
                LocalDate.now().plusDays(2).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"))

            val bookingInfo = BookingInfo(
                venueId = Constant.VENUE_IDS[selectedVenue]!!,
                courtId = Constant.COURT_IDS[selectedVenue]!![courts.first()]!!,
                orderDate = orderDate,
                startTime = startTime,
                endTime = endTime
            )

            val tokenResult = Network.testConnection(
                cookies,
                bookingInfo
            )

            val (csrfToken, token) = when (tokenResult) {
                is Result.Success -> {
                    val (csrf, t) = tokenResult.data
                    addLog("获取到 CSRF Token: $csrf", LogType.INFO)
                    addLog("获取到 Token: $t", LogType.INFO)
                    tokenResult.data
                }

                is Result.Error -> {
                    addLog("获取 Token 失败: ${tokenResult.exception.message}", LogType.ERROR)
                    bookingState = BookingState.STOPPED
                    return@launch
                }
            }
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
