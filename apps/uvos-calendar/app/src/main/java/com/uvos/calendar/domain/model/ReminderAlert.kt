package com.uvos.calendar.domain.model

data class ReminderAlert(
    val reminderId: Long = 0,
    val eventId: Long = 0,
    val minutesBefore: Int, // 5, 15, 30, 60, 1440
    val isFired: Boolean = false
) {
    val displayLabel: String
        get() = when (minutesBefore) {
            0 -> "В момент события"
            5 -> "За 5 минут"
            15 -> "За 15 минут"
            30 -> "За 30 минут"
            60 -> "За 1 час"
            120 -> "За 2 часа"
            1440 -> "За 1 день"
            else -> "За $minutesBefore мин."
        }
}
