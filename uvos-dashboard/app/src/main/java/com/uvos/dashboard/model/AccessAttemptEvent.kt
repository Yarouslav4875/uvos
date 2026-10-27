package com.uvos.dashboard.model

enum class AccessVerdict(val isBlocked: Boolean, val logLabel: String) {
    DENIED_CLOSED(true, "отказано (канал закрыт)"),
    DENIED_WHITELIST(true, "вне белого списка"),
    DENIED_LIMIT(true, "лимит превышен"),
    DENIED_SCHEDULE(true, "окно доступа закрыто"),
    COARSED_APPLIED(false, "выдано огрубленно"),
    ALLOWED(false, "разрешено")
}

data class AccessAttemptEvent(
    val id: Long,
    val timestampMillis: Long,
    val packageName: String,
    val appName: String,
    val channel: DataChannel,
    val targetResource: String,
    val verdict: AccessVerdict,
    var isViewed: Boolean = false // false -> статичная красная лампа/точка #E02020
)
