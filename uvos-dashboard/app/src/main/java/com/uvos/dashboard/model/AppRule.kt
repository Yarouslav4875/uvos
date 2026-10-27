package com.uvos.dashboard.model

/**
 * Профили ограничений (CONTROL-SCHEME.md §4).
 */
enum class ProfileType(val title: String, val description: String) {
    STERILE("СТЕРИЛЬНО", "Все 13 каналов заблокированы"),
    COMM("СВЯЗЬ", "Базовая связь владельцу, приложениям закрыто"),
    CUSTOM("СВОЙ", "Индивидуальная настройка каналов")
}

data class AppRule(
    val packageName: String,
    val appName: String,
    val uid: Int,
    val channelModes: MutableMap<DataChannel, ChannelMode> = DataChannel.values().associateWith { ChannelMode.CLOSED }.toMutableMap(),
    var networkConfig: NetworkConfig = NetworkConfig(),
    var timeCoarseness: TimeCoarseness = TimeCoarseness.HOUR
) {
    val openChannelsCount: Int
        get() = channelModes.count { it.value == ChannelMode.OPEN }
}
