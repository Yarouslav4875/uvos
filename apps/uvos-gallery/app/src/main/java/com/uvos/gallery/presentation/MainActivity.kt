package com.uvos.gallery.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import com.uvos.gallery.UVOSGalleryApplication
import com.uvos.gallery.domain.model.MediaItem
import com.uvos.gallery.presentation.main.GalleryMainScreen
import com.uvos.gallery.presentation.theme.UVOSGalleryTheme
import com.uvos.gallery.presentation.theme.UvosBackground
import com.uvos.gallery.presentation.viewer.MediaViewerScreen

sealed interface GalleryScreenState {
    object Grid : GalleryScreenState
    data class Viewer(val mediaItem: MediaItem) : GalleryScreenState
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val appContainer = (application as UVOSGalleryApplication).appContainer

        setContent {
            UVOSGalleryTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = UvosBackground
                ) {
                    var screenState by remember { mutableStateOf<GalleryScreenState>(GalleryScreenState.Grid) }

                    when (val current = screenState) {
                        is GalleryScreenState.Grid -> {
                            GalleryMainScreen(
                                mediaRepository = appContainer.mediaRepository,
                                onMediaSelected = { item ->
                                    screenState = GalleryScreenState.Viewer(item)
                                },
                                onInspectExif = { item ->
                                    screenState = GalleryScreenState.Viewer(item)
                                }
                            )
                        }
                        is GalleryScreenState.Viewer -> {
                            MediaViewerScreen(
                                item = current.mediaItem,
                                onBack = { screenState = GalleryScreenState.Grid },
                                onInspectExif = { },
                                onScrubInPlace = { },
                                onMoveToVault = { },
                                onShareClean = { }
                            )
                        }
                    }
                }
            }
        }
    }
}
