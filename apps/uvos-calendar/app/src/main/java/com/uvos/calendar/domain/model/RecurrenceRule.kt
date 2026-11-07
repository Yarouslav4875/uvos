package com.uvos.calendar.domain.model

enum class RecurrenceFrequency(val titleRes: String) {
    NONE("Без повторения"),
    DAILY("Ежедневно"),
    WEEKLY("Еженедельно"),
    MONTHLY("Ежемесячно"),
    YEARLY("Ежегодно")
}

data class RecurrenceRule(
    val frequency: RecurrenceFrequency = RecurrenceFrequency.NONE,
    val interval: Int = 1
) {
    fun toRRuleString(): String? {
        return when (frequency) {
            RecurrenceFrequency.NONE -> null
            RecurrenceFrequency.DAILY -> "RRULE:FREQ=DAILY;INTERVAL=$interval"
            RecurrenceFrequency.WEEKLY -> "RRULE:FREQ=WEEKLY;INTERVAL=$interval"
            RecurrenceFrequency.MONTHLY -> "RRULE:FREQ=MONTHLY;INTERVAL=$interval"
            RecurrenceFrequency.YEARLY -> "RRULE:FREQ=YEARLY;INTERVAL=$interval"
        }
    }

    companion object {
        fun fromRRuleString(rrule: String?): RecurrenceRule {
            if (rrule.isNullOrBlank()) return RecurrenceRule(RecurrenceFrequency.NONE)
            val freq = when {
                rrule.contains("FREQ=DAILY", ignoreCase = true) -> RecurrenceFrequency.DAILY
                rrule.contains("FREQ=WEEKLY", ignoreCase = true) -> RecurrenceFrequency.WEEKLY
                rrule.contains("FREQ=MONTHLY", ignoreCase = true) -> RecurrenceFrequency.MONTHLY
                rrule.contains("FREQ=YEARLY", ignoreCase = true) -> RecurrenceFrequency.YEARLY
                else -> RecurrenceFrequency.NONE
            }
            return RecurrenceRule(frequency = freq)
        }
    }
}
