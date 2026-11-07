package com.uvos.downloader.domain.model

enum class DownloadStatus {
    QUEUED,
    DOWNLOADING,
    PAUSED,
    COMPLETED,
    ERROR;

    companion object {
        fun fromString(value: String): DownloadStatus {
            return entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: ERROR
        }
    }
}
