package com.uvos.music.core.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import com.uvos.music.core.database.dao.TrackDao

class MusicDatabase private constructor(context: Context) : SQLiteOpenHelper(
    context.applicationContext,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    val trackDao: TrackDao = TrackDao { writableDatabase }

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE IF NOT EXISTS tracks (
                id INTEGER PRIMARY KEY,
                title TEXT NOT NULL,
                artist TEXT NOT NULL,
                album TEXT NOT NULL,
                duration_ms INTEGER NOT NULL,
                content_uri TEXT NOT NULL,
                file_path TEXT NOT NULL,
                container TEXT NOT NULL,
                sample_rate INTEGER NOT NULL,
                bit_depth INTEGER NOT NULL,
                bitrate_kbps INTEGER NOT NULL,
                is_favorite INTEGER DEFAULT 0,
                play_count INTEGER DEFAULT 0,
                last_played INTEGER DEFAULT 0
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS tracks")
        onCreate(db)
    }

    companion object {
        private const val DATABASE_NAME = "uvos_music.db"
        private const val DATABASE_VERSION = 1

        @Volatile
        private var instance: MusicDatabase? = null

        fun getInstance(context: Context): MusicDatabase {
            return instance ?: synchronized(this) {
                instance ?: MusicDatabase(context).also { instance = it }
            }
        }
    }
}
