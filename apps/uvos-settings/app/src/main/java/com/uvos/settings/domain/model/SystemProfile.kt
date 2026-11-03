package com.uvos.settings.domain.model

enum class SystemProfile(val displayName: String, val description: String) {
    STERILE("СТЕРИЛЬНО", "Полная радиотишина. Авиарежим, датчики заглушены, iptables DROP ALL"),
    COMMUNICATION("СВЯЗЬ", "Минимальный доступ. Только голосовой RIL и SMS, Wi-Fi и BT отключены"),
    CUSTOM("СВОЙ", "Пользовательский режим. Выборочный доступ по правилам щитка")
}
