package com.uvos.music.domain.model

import android.net.Uri

/**
 * Domain model representing a local audio track in UVOS Music library.
 */
data class AudioTrack(
    val id: Long,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val contentUri: Uri,
    val filePath: String,
    val formatInfo: AudioFormatInfo = AudioFormatInfo(),
    val albumArtUri: Uri? = null,
    val isFavorite: Boolean = false
) {
    val durationFormatted: String
        get() {
            val totalSeconds = durationMs / 1000
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}
