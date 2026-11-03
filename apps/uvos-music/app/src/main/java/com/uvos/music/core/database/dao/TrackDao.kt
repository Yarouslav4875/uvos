package com.uvos.music.core.database.dao

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import com.uvos.music.core.database.entity.TrackEntity

class TrackDao(private val dbHelper: () -> SQLiteDatabase) {

    fun insertOrUpdate(track: TrackEntity) {
        val db = dbHelper()
        val values = ContentValues().apply {
            put("id", track.id)
            put("title", track.title)
            put("artist", track.artist)
            put("album", track.album)
            put("duration_ms", track.durationMs)
            put("content_uri", track.contentUriString)
            put("file_path", track.filePath)
            put("container", track.container)
            put("sample_rate", track.sampleRateHz)
            put("bit_depth", track.bitDepth)
            put("bitrate_kbps", track.bitrateKbps)
            put("is_favorite", if (track.isFavorite) 1 else 0)
            put("play_count", track.playCount)
            put("last_played", track.lastPlayedTimestamp)
        }
        db.insertWithOnConflict("tracks", null, values, SQLiteDatabase.CONFLICT_REPLACE)
    }

    fun getAllTracks(): List<TrackEntity> {
        val db = dbHelper()
        val list = mutableListOf<TrackEntity>()
        val cursor = db.rawQuery("SELECT * FROM tracks ORDER BY title ASC", null)
        cursor.use {
            while (it.moveToNext()) {
                list.add(
                    TrackEntity(
                        id = it.getLong(it.getColumnIndexOrThrow("id")),
                        title = it.getString(it.getColumnIndexOrThrow("title")),
                        artist = it.getString(it.getColumnIndexOrThrow("artist")),
                        album = it.getString(it.getColumnIndexOrThrow("album")),
                        durationMs = it.getLong(it.getColumnIndexOrThrow("duration_ms")),
                        contentUriString = it.getString(it.getColumnIndexOrThrow("content_uri")),
                        filePath = it.getString(it.getColumnIndexOrThrow("file_path")),
                        container = it.getString(it.getColumnIndexOrThrow("container")),
                        sampleRateHz = it.getInt(it.getColumnIndexOrThrow("sample_rate")),
                        bitDepth = it.getInt(it.getColumnIndexOrThrow("bit_depth")),
                        bitrateKbps = it.getInt(it.getColumnIndexOrThrow("bitrate_kbps")),
                        isFavorite = it.getInt(it.getColumnIndexOrThrow("is_favorite")) == 1,
                        playCount = it.getInt(it.getColumnIndexOrThrow("play_count")),
                        lastPlayedTimestamp = it.getLong(it.getColumnIndexOrThrow("last_played"))
                    )
                )
            }
        }
        return list
    }

    fun setFavorite(trackId: Long, isFavorite: Boolean) {
        val db = dbHelper()
        val values = ContentValues().apply {
            put("is_favorite", if (isFavorite) 1 else 0)
        }
        db.update("tracks", values, "id = ?", arrayOf(trackId.toString()))
    }
}
