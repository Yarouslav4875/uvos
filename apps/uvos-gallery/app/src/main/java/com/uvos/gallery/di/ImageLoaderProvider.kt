package com.uvos.gallery.di

import android.content.Context
import coil.ImageLoader
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import java.io.File

object ImageLoaderProvider {
    /**
     * Creates an air-gapped, zero-network Coil ImageLoader instance.
     * Memory cache is limited to 25% heap, and thumbnail disk cache is isolated
     * inside the app sandbox with network policies completely disabled.
     */
    fun createLocalImageLoader(context: Context): ImageLoader {
        return ImageLoader.Builder(context)
            .memoryCache {
                MemoryCache.Builder(context)
                    .maxSizePercent(0.25) // Max 25% heap for RAM optimization
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(File(context.cacheDir, "thumbnail_cache"))
                    .maxSizeBytes(128L * 1024 * 1024) // 128 MB disk cache
                    .build()
            }
            .networkCachePolicy(CachePolicy.DISABLED)
            .respectCacheHeaders(false)
            .build()
    }
}
