package com.uvos.music.core.database.entity

data class TrackEntity(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUriString: String,
    val filePath: String,
    val container: String,
    val sampleRateHz: Int,
    val bitDepth: Int,
    val bitrateKbps: Int,
    val isFavorite: Boolean = false,
    val playCount: Int = 0,
    val lastPlayedTimestamp: Long = 0L
)
