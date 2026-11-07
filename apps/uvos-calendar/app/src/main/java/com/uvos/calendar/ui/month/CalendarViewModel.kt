package com.uvos.calendar.ui.month

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uvos.calendar.UvosCalendarApplication
import com.uvos.calendar.domain.model.CalendarEvent
import com.uvos.calendar.domain.model.RecurrenceFrequency
import com.uvos.calendar.domain.model.RecurrenceRule
import com.uvos.calendar.domain.repository.CalendarRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class CalendarUiState(
    val currentYear: Int = 2026,
    val currentMonth: Int = Calendar.NOVEMBER, // 0-based Calendar.MONTH
    val selectedDay: Int = 7,
    val todayYear: Int = 2026,
    val todayMonth: Int = Calendar.NOVEMBER,
    val todayDay: Int = 7,
    val todayDayOfWeek: Int = Calendar.SATURDAY,
    val daysInMonth: Int = 30,
    val firstDayOfWeekInMonth: Int = Calendar.SUNDAY, // Day of week of day 1
    val daysWithEvents: Set<Int> = emptySet(),
    val selectedDayEvents: List<CalendarEvent> = emptyList(),
    val editingEvent: CalendarEvent? = null,
    val isEditSheetOpen: Boolean = false,
    val statusMessage: String? = null
)

class CalendarViewModel(application: Application) : AndroidViewModel(application) {

    private val repository: CalendarRepository =
        (application as UvosCalendarApplication).repository

    private val calendarInstance = Calendar.getInstance()

    private val _selectedYear = MutableStateFlow(calendarInstance.get(Calendar.YEAR))
    private val _selectedMonth = MutableStateFlow(calendarInstance.get(Calendar.MONTH))
    private val _selectedDay = MutableStateFlow(calendarInstance.get(Calendar.DAY_OF_MONTH))

    private val _editingEvent = MutableStateFlow<CalendarEvent?>(null)
    private val _isEditSheetOpen = MutableStateFlow(false)
    private val _statusMessage = MutableStateFlow<String?>(null)

    val allEvents: StateFlow<List<CalendarEvent>> = repository.observeAllEvents()
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    val uiState: StateFlow<CalendarUiState> = combine(
        _selectedYear,
        _selectedMonth,
        _selectedDay,
        allEvents,
        _editingEvent,
        _isEditSheetOpen,
        _statusMessage
    ) { year, month, day, events, editingEvent, isSheetOpen, statusMsg ->
        val cal = Calendar.getInstance()
        val todayYear = cal.get(Calendar.YEAR)
        val todayMonth = cal.get(Calendar.MONTH)
        val todayDay = cal.get(Calendar.DAY_OF_MONTH)
        val todayDow = cal.get(Calendar.DAY_OF_WEEK)

        cal.set(Calendar.YEAR, year)
        cal.set(Calendar.MONTH, month)
        cal.set(Calendar.DAY_OF_MONTH, 1)
        val daysInMonth = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        val firstDow = cal.get(Calendar.DAY_OF_WEEK)

        // Find which days have events (including recurring events)
        val activeDays = mutableSetOf<Int>()
        val dayEvents = mutableListOf<CalendarEvent>()

        for (d in 1..daysInMonth) {
            cal.set(Calendar.DAY_OF_MONTH, d)
            cal.set(Calendar.HOUR_OF_DAY, 0)
            cal.set(Calendar.MINUTE, 0)
            cal.set(Calendar.SECOND, 0)
            cal.set(Calendar.MILLISECOND, 0)
            val dayStart = cal.timeInMillis
            val dayEnd = dayStart + 86400_000L - 1L

            for (event in events) {
                if (isEventActiveOnDay(event, dayStart, dayEnd, year, month, d)) {
                    activeDays.add(d)
                    if (d == day) {
                        dayEvents.add(event)
                    }
                }
            }
        }

        CalendarUiState(
            currentYear = year,
            currentMonth = month,
            selectedDay = day.coerceIn(1, daysInMonth),
            todayYear = todayYear,
            todayMonth = todayMonth,
            todayDay = todayDay,
            todayDayOfWeek = todayDow,
            daysInMonth = daysInMonth,
            firstDayOfWeekInMonth = firstDow,
            daysWithEvents = activeDays,
            selectedDayEvents = dayEvents.sortedBy { it.startTimestamp },
            editingEvent = editingEvent,
            isEditSheetOpen = isSheetOpen,
            statusMessage = statusMsg
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        CalendarUiState()
    )

    fun selectDay(day: Int) {
        _selectedDay.value = day
    }

    fun nextMonth() {
        val m = _selectedMonth.value
        val y = _selectedYear.value
        if (m == Calendar.DECEMBER) {
            _selectedMonth.value = Calendar.JANUARY
            _selectedYear.value = y + 1
        } else {
            _selectedMonth.value = m + 1
        }
        adjustSelectedDay()
    }

    fun previousMonth() {
        val m = _selectedMonth.value
        val y = _selectedYear.value
        if (m == Calendar.JANUARY) {
            _selectedMonth.value = Calendar.DECEMBER
            _selectedYear.value = y - 1
        } else {
            _selectedMonth.value = m - 1
        }
        adjustSelectedDay()
    }

    fun jumpToToday() {
        val cal = Calendar.getInstance()
        _selectedYear.value = cal.get(Calendar.YEAR)
        _selectedMonth.value = cal.get(Calendar.MONTH)
        _selectedDay.value = cal.get(Calendar.DAY_OF_MONTH)
    }

    fun openNewEventSheet() {
        // Prepopulate start time at 10:00 AM on selected day
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _selectedYear.value)
            set(Calendar.MONTH, _selectedMonth.value)
            set(Calendar.DAY_OF_MONTH, _selectedDay.value)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startTs = cal.timeInMillis
        val endTs = startTs + 3600_000L

        _editingEvent.value = CalendarEvent(
            id = 0,
            title = "",
            startTimestamp = startTs,
            endTimestamp = endTs,
            colorHex = "#E02020"
        )
        _isEditSheetOpen.value = true
    }

    fun openEditEventSheet(event: CalendarEvent) {
        _editingEvent.value = event
        _isEditSheetOpen.value = true
    }

    fun closeEditSheet() {
        _editingEvent.value = null
        _isEditSheetOpen.value = false
    }

    fun saveEvent(event: CalendarEvent) {
        viewModelScope.launch {
            repository.saveEvent(event)
            closeEditSheet()
            _statusMessage.value = "Событие сохранено в защищённую базу"
        }
    }

    fun deleteEvent(id: Long) {
        viewModelScope.launch {
            repository.deleteEvent(id)
            closeEditSheet()
            _statusMessage.value = "Событие удалено"
        }
    }

    fun clearStatusMessage() {
        _statusMessage.value = null
    }

    suspend fun exportIcs(): String {
        return repository.exportEventsToIcs()
    }

    fun importIcs(content: String) {
        viewModelScope.launch {
            val count = repository.importEventsFromIcs(content)
            _statusMessage.value = "Импортировано событий: $count"
        }
    }

    private fun adjustSelectedDay() {
        val cal = Calendar.getInstance().apply {
            set(Calendar.YEAR, _selectedYear.value)
            set(Calendar.MONTH, _selectedMonth.value)
            set(Calendar.DAY_OF_MONTH, 1)
        }
        val maxDays = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
        if (_selectedDay.value > maxDays) {
            _selectedDay.value = maxDays
        }
    }

    private fun isEventActiveOnDay(
        event: CalendarEvent,
        dayStartMs: Long,
        dayEndMs: Long,
        year: Int,
        month: Int,
        day: Int
    ): Boolean {
        // Direct overlap
        if (event.startTimestamp <= dayEndMs && event.endTimestamp >= dayStartMs) {
            return true
        }

        // Recurrence evaluation
        val rrule = RecurrenceRule.fromRRuleString(event.recurrenceRule)
        if (rrule.frequency == RecurrenceFrequency.NONE) return false

        // Do not project before event start date
        if (dayEndMs < event.startTimestamp) return false

        val eventCal = Calendar.getInstance().apply { timeInMillis = event.startTimestamp }

        return when (rrule.frequency) {
            RecurrenceFrequency.DAILY -> true
            RecurrenceFrequency.WEEKLY -> eventCal.get(Calendar.DAY_OF_WEEK) == getDayOfWeek(year, month, day)
            RecurrenceFrequency.MONTHLY -> eventCal.get(Calendar.DAY_OF_MONTH) == day
            RecurrenceFrequency.YEARLY -> eventCal.get(Calendar.MONTH) == month && eventCal.get(Calendar.DAY_OF_MONTH) == day
            RecurrenceFrequency.NONE -> false
        }
    }

    private fun getDayOfWeek(year: Int, month: Int, day: Int): Int {
        val cal = Calendar.getInstance()
        cal.set(year, month, day)
        return cal.get(Calendar.DAY_OF_WEEK)
    }
}
