package com.uvos.downloader

import android.app.Application
import com.uvos.downloader.data.local.db.DownloadDatabase
import com.uvos.downloader.data.network.DownloadEngine
import com.uvos.downloader.data.network.OkHttpDownloadClient
import com.uvos.downloader.data.repository.DownloadRepositoryImpl
import com.uvos.downloader.domain.repository.DownloadRepository

class DownloaderApplication : Application() {

    lateinit var database: DownloadDatabase
        private set

    lateinit var downloadRepository: DownloadRepository
        private set

    lateinit var downloadEngine: DownloadEngine
        private set

    override fun onCreate() {
        super.onCreate()
        database = DownloadDatabase.getInstance(this)
        downloadRepository = DownloadRepositoryImpl(database)
        downloadEngine = DownloadEngine(downloadRepository, OkHttpDownloadClient())
    }
}
