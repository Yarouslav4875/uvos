package com.uvos.gallery

import android.app.Application
import coil.ImageLoader
import coil.ImageLoaderFactory
import com.uvos.gallery.di.AppContainer
import com.uvos.gallery.di.ImageLoaderProvider

class UVOSGalleryApplication : Application(), ImageLoaderFactory {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }

    override fun newImageLoader(): ImageLoader {
        return ImageLoaderProvider.createLocalImageLoader(this)
    }
}
