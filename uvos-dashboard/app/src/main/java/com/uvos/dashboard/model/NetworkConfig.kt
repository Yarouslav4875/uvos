package com.uvos.dashboard.model

/**
 * Режимы Сетевого регулятора (docs/NETWORK-REGULATOR.md §2).
 */
enum class NetworkMode(val displayName: String) {
    FULLY_CLOSED("ПОЛНОСТЬЮ ЗАКРЫТО"),
    WHITELIST("БЕЛЫЙ СПИСОК"),
    OPEN("ОТКРЫТО")
}

data class WhitelistDomain(
    val domain: String,
    val isEnabled: Boolean = true
)

data class TimeWindow(
    val startMinute: Int = 0,
    val endMinute: Int = 1439
) {
    fun isCurrentTimeWithin(currentMinuteOfDay: Int): Boolean {
        return if (startMinute <= endMinute) {
            currentMinuteOfDay in startMinute..endMinute
        } else {
            currentMinuteOfDay >= startMinute || currentMinuteOfDay <= endMinute
        }
    }
}

data class NetworkConfig(
    val mode: NetworkMode = NetworkMode.FULLY_CLOSED,
    val whitelist: List<WhitelistDomain> = listOf(
        WhitelistDomain("api.telegram.org", true),
        WhitelistDomain("auth.bank.ru", true)
    ),
    val dailyLimitBytes: Long = 500L * 1024L * 1024L, // 500 МБ по умолчанию
    val usedBytesToday: Long = 120L * 1024L * 1024L,  // Демо-значение: 120 МБ
    val chargingOnly: Boolean = false,
    val timeWindow: TimeWindow? = null
) {
    val isLimitExceeded: Boolean
        get() = dailyLimitBytes > 0L && usedBytesToday >= dailyLimitBytes
}
