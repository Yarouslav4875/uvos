package com.uvos.music.domain.usecase

import android.content.ContentUris
import android.content.Context
import android.media.MediaExtractor
import android.media.MediaFormat
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.uvos.music.domain.model.AudioFormatInfo
import com.uvos.music.domain.model.AudioTrack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class ScanLocalLibraryUseCase(
    private val context: Context
) {
    suspend fun execute(): List<AudioTrack> = withContext(Dispatchers.IO) {
        val tracks = mutableListOf<AudioTrack>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.DATA,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.MIME_TYPE
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA)
                val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val mimeCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.MIME_TYPE)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val title = c.getString(titleCol) ?: "Unknown Track"
                    val artist = c.getString(artistCol) ?: "Unknown Artist"
                    val album = c.getString(albumCol) ?: "Unknown Album"
                    val durationMs = c.getLong(durCol)
                    val filePath = c.getString(dataCol) ?: ""
                    val albumId = c.getLong(albumIdCol)
                    val mimeType = c.getString(mimeCol) ?: ""

                    // Ignore tracks under 5 seconds (ringtones / system notifications)
                    if (durationMs < 5000) continue

                    val contentUri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val albumArtUri = ContentUris.withAppendedId(
                        Uri.parse("content://media/external/audio/albumart"),
                        albumId
                    )

                    val formatInfo = detectAudioFormat(filePath, mimeType)

                    tracks.add(
                        AudioTrack(
                            id = id,
                            title = title,
                            artist = if (artist == "<unknown>") "Unknown Artist" else artist,
                            album = if (album == "<unknown>") "Unknown Album" else album,
                            durationMs = durationMs,
                            contentUri = contentUri,
                            filePath = filePath,
                            formatInfo = formatInfo,
                            albumArtUri = albumArtUri
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.e("ScanLocalLibrary", "Error scanning media store: ${e.message}", e)
        }

        tracks
    }

    private fun detectAudioFormat(filePath: String, mimeType: String): AudioFormatInfo {
        val ext = File(filePath).extension.uppercase()
        var container = when {
            ext.isNotEmpty() -> ext
            mimeType.contains("flac") -> "FLAC"
            mimeType.contains("wav") -> "WAV"
            mimeType.contains("mp3") || mimeType.contains("mpeg") -> "MP3"
            mimeType.contains("ogg") -> "OGG"
            mimeType.contains("opus") -> "OPUS"
            mimeType.contains("aac") || mimeType.contains("m4a") -> "AAC"
            else -> "PCM"
        }

        var sampleRate = 44100
        var bitDepth = 16
        var bitrate = 1411
        var isLossless = container in listOf("FLAC", "WAV", "ALAC", "AIFF", "APE", "DFF", "DSF")

        // Try fast metadata extraction
        try {
            val extractor = MediaExtractor()
            extractor.setDataSource(filePath)
            val trackCount = extractor.trackCount
            for (i in 0 until trackCount) {
                val format = extractor.getTrackFormat(i)
                val mime = format.getString(MediaFormat.KEY_MIME) ?: ""
                if (mime.startsWith("audio/")) {
                    if (format.containsKey(MediaFormat.KEY_SAMPLE_RATE)) {
                        sampleRate = format.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    }
                    if (format.containsKey(MediaFormat.KEY_BIT_RATE)) {
                        bitrate = format.getInteger(MediaFormat.KEY_BIT_RATE) / 1000
                    }
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && format.containsKey(MediaFormat.KEY_PCM_ENCODING)) {
                        val pcmEncoding = format.getInteger(MediaFormat.KEY_PCM_ENCODING)
                        if (pcmEncoding == 4) bitDepth = 32 // ENCODING_PCM_FLOAT
                    }
                    break
                }
            }
            extractor.release()
        } catch (_: Exception) {}

        if (container == "FLAC" && sampleRate >= 96000) {
            bitDepth = 24
        }

        return AudioFormatInfo(
            container = container,
            sampleRateHz = sampleRate,
            bitDepth = bitDepth,
            bitrateKbps = bitrate,
            isLossless = isLossless,
            isHiRes = sampleRate >= 48000 || bitDepth > 16
        )
    }
}
