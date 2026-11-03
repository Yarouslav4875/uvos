package com.uvos.music.feature.main

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.content.ContextCompat
import com.uvos.music.core.ui.theme.M4200ChassisBg
import com.uvos.music.core.ui.theme.UvosMusicTheme
import com.uvos.music.feature.equalizer.EqualizerScreen
import com.uvos.music.feature.equalizer.EqualizerViewModel
import com.uvos.music.feature.library.LibraryScreen
import com.uvos.music.feature.library.LibraryViewModel
import com.uvos.music.feature.player.PlayerScreen
import com.uvos.music.feature.player.PlayerViewModel
import com.uvos.music.service.MusicPlaybackService

enum class AppDestination {
    PLAYER,
    EQUALIZER,
    LIBRARY
}

class MainActivity : ComponentActivity() {

    private val playerViewModel: PlayerViewModel by viewModels()
    private val equalizerViewModel: EqualizerViewModel by viewModels()
    private val libraryViewModel: LibraryViewModel by viewModels()

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) {
            libraryViewModel.loadMedia()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start background MediaSessionService
        startPlaybackService()

        checkAndRequestMediaPermissions()

        setContent {
            UvosMusicTheme {
                Surface(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(M4200ChassisBg),
                    color = M4200ChassisBg
                ) {
                    MainAppContent(
                        playerViewModel = playerViewModel,
                        equalizerViewModel = equalizerViewModel,
                        libraryViewModel = libraryViewModel
                    )
                }
            }
        }
    }

    private fun startPlaybackService() {
        try {
            val serviceIntent = Intent(this, MusicPlaybackService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent)
            } else {
                startService(serviceIntent)
            }
        } catch (_: Exception) {
            // Service startup error handling
        }
    }

    private fun checkAndRequestMediaPermissions() {
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            arrayOf(Manifest.permission.READ_MEDIA_AUDIO)
        } else {
            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
        }

        val neededPermissions = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }

        if (neededPermissions.isNotEmpty()) {
            requestPermissionLauncher.launch(neededPermissions.toTypedArray())
        }
    }
}

@Composable
fun MainAppContent(
    playerViewModel: PlayerViewModel,
    equalizerViewModel: EqualizerViewModel,
    libraryViewModel: LibraryViewModel
) {
    var currentDestination by remember { mutableStateOf(AppDestination.PLAYER) }

    when (currentDestination) {
        AppDestination.PLAYER -> {
            PlayerScreen(
                viewModel = playerViewModel,
                onNavigateToEqualizer = { currentDestination = AppDestination.EQUALIZER },
                onNavigateToLibrary = { currentDestination = AppDestination.LIBRARY }
            )
        }
        AppDestination.EQUALIZER -> {
            EqualizerScreen(
                viewModel = equalizerViewModel,
                onNavigateBack = { currentDestination = AppDestination.PLAYER }
            )
        }
        AppDestination.LIBRARY -> {
            LibraryScreen(
                viewModel = libraryViewModel,
                onNavigateToPlayer = { currentDestination = AppDestination.PLAYER }
            )
        }
    }
}
