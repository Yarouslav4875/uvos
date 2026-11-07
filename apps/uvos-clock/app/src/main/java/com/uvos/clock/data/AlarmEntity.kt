package com.uvos.clock.data

import java.util.Calendar

data class AlarmEntity(
    val id: Long = 0,
    val hour: Int,
    val minute: Int,
    val isEnabled: Boolean = true,
    val daysOfWeek: Int = 127, // битовая маска 7 дней недели: 1=Пн, 2=Вт, 4=Ср, 8=Чт, 16=Пт, 32=Сб, 64=Вс
    val label: String = "Будильник",
    val isCrescendo: Boolean = true
) {
    fun getFormattedTime(): String {
        return "%02d:%02d".format(hour, minute)
    }

    fun isDayEnabled(dayIndex: Int): Boolean {
        // dayIndex: 0=Mon, 1=Tue, ..., 6=Sun
        return (daysOfWeek and (1 shl dayIndex)) != 0
    }

    fun getDaysDescription(): String {
        if (daysOfWeek == 127) return "Каждый день"
        if (daysOfWeek == 31) return "По будням (Пн-Пт)"
        if (daysOfWeek == 96) return "По выходным (Сб-Вс)"
        if (daysOfWeek == 0) return "Однократно"

        val names = listOf("Пн", "Вт", "Ср", "Чт", "Пт", "Сб", "Вс")
        val active = mutableListOf<String>()
        for (i in 0 until 7) {
            if (isDayEnabled(i)) active.add(names[i])
        }
        return active.joinToString(", ")
    }

    /**
     * Вычисляет следующее время срабатывания в миллисекундах.
     */
    fun getNextTriggerMillis(): Long {
        val now = Calendar.getInstance()
        val target = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }

        if (daysOfWeek == 0) {
            // Однократный будильник
            if (target.timeInMillis <= now.timeInMillis) {
                target.add(Calendar.DAY_OF_YEAR, 1)
            }
            return target.timeInMillis
        }

        // Поиск ближайшего активного дня недели (до 7 дней вперед)
        for (dayOffset in 0..7) {
            val candidate = (target.clone() as Calendar).apply {
                add(Calendar.DAY_OF_YEAR, dayOffset)
            }
            if (candidate.timeInMillis > now.timeInMillis) {
                // Calendar.DAY_OF_WEEK: Sunday = 1, Monday = 2, ..., Saturday = 7
                val calDay = candidate.get(Calendar.DAY_OF_WEEK)
                val dayIdx = when (calDay) {
                    Calendar.MONDAY -> 0
                    Calendar.TUESDAY -> 1
                    Calendar.WEDNESDAY -> 2
                    Calendar.THURSDAY -> 3
                    Calendar.FRIDAY -> 4
                    Calendar.SATURDAY -> 5
                    Calendar.SUNDAY -> 6
                    else -> 0
                }
                if (isDayEnabled(dayIdx)) {
                    return candidate.timeInMillis
                }
            }
        }

        return target.timeInMillis
    }
}
