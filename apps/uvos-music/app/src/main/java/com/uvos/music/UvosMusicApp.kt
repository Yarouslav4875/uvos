package com.uvos.music

import android.app.Application
import com.uvos.music.core.audio.engine.ExoPlayerHolder
import com.uvos.music.core.database.MusicDatabase

/**
 * UVOS Music Application.
 * Zero-Network Invariant strictly enforced: 100% Offline, no network capabilities.
 */
class UvosMusicApp : Application() {

    lateinit var playerHolder: ExoPlayerHolder
        private set

    lateinit var database: MusicDatabase
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        playerHolder = ExoPlayerHolder(this)
        database = MusicDatabase.getInstance(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        playerHolder.release()
    }

    companion object {
        lateinit var instance: UvosMusicApp
            private set
    }
}
