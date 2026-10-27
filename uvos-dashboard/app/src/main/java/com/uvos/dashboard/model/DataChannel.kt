package com.uvos.dashboard.model

/**
 * 13 каналов данных UVOS (docs/LIMITERS.md §3).
 */
enum class DataChannel(val displayName: String, val allowsCoarsening: Boolean) {
    TIME("Время", true),
    BATTERY("Батарея", true),
    NETWORK("Сеть", false),
    GEOLOCATION("Геолокация", true),
    CAMERA("Камера", false),
    MICROPHONE("Микрофон", false),
    CONTACTS("Контакты", false),
    SMS("СМС", false),
    CALLS("Звонки", false),
    STORAGE_FILES("Файлы", false),
    SENSORS("Датчики", false),
    IDENTIFIERS("Идентификаторы", true),
    APP_LIST("Список приложений", false)
}

enum class ChannelMode {
    CLOSED,    // ЗАКРЫТО (по умолчанию)
    COARSED,   // ОГРУБЛЕНО (для времени, батареи, гео, идентификаторов)
    OPEN       // ОТКРЫТО
}

enum class TimeCoarseness {
    HOUR, MINUTE, EXACT
}
