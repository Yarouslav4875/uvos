package com.uvos.settings.domain.model

enum class AppFilterCategory(val label: String) {
    ALL("ВСЕ"),
    SYSTEM("СИСТЕМА"),
    ISOLATED("ИЗОЛИРОВАННЫЕ"),
    USER("СТОРОННИЕ"),
    THREATS("ПОТЕНЦИАЛЬНАЯ УГРОЗА")
}
