package com.uvos.clock.data

/**
 * Модель задачи для матрицы Эйзенхауэра UnderVoltOS.
 *
 * Приоритеты:
 * 1: Срочно и Важно (Критично, Lamp Red)
 * 2: Важно, но не срочно (Стратегически, Lamp Yellow)
 * 3: Срочно, но не важно (Делегировать/Быстро, Lamp Orange)
 * 4: Не срочно и не важно (Рутина/Архив, Lamp Gray/Cyan)
 */
data class TaskEntity(
    val id: Long = 0,
    val title: String,
    val description: String? = null,
    val priority: Int,
    val isCompleted: Boolean = false,
    val dueTimestamp: Long? = null,
    val reminderTimestamp: Long? = null,
    val createdAt: Long = System.currentTimeMillis()
) {
    val isUrgent: Boolean
        get() = priority == 1 || priority == 3

    val isImportant: Boolean
        get() = priority == 1 || priority == 2

    companion object {
        fun priorityFromToggles(isUrgent: Boolean, isImportant: Boolean): Int {
            return when {
                isUrgent && isImportant -> 1
                !isUrgent && isImportant -> 2
                isUrgent && !isImportant -> 3
                else -> 4
            }
        }
    }
}
