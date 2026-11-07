package com.uvos.downloader.ui.main

import android.app.Application
import android.content.Context
import android.os.Environment
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uvos.downloader.DownloaderApplication
import com.uvos.downloader.domain.model.DownloadChunk
import com.uvos.downloader.domain.model.DownloadStatus
import com.uvos.downloader.domain.model.DownloadTask
import com.uvos.downloader.domain.model.NetworkSpeedMetric
import com.uvos.downloader.domain.usecase.CancelDownloadUseCase
import com.uvos.downloader.domain.usecase.PauseDownloadUseCase
import com.uvos.downloader.domain.usecase.StartDownloadUseCase
import com.uvos.downloader.service.DownloadForegroundService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File

data class DownloaderUiState(
    val urlInput: String = "",
    val customFileName: String = "",
    val numThreads: Int = 4,
    val onlyWifi: Boolean = false,
    val isAddDialogOpen: Boolean = false
)

class DownloaderViewModel(application: Application) : AndroidViewModel(application) {

    private val app = application as DownloaderApplication
    private val repository = app.downloadRepository
    private val engine = app.downloadEngine

    private val startUseCase = StartDownloadUseCase(repository)
    private val pauseUseCase = PauseDownloadUseCase(repository)
    private val cancelUseCase = CancelDownloadUseCase(repository)

    private val _uiState = MutableStateFlow(DownloaderUiState())
    val uiState: StateFlow<DownloaderUiState> = _uiState.asStateFlow()

    val activeTasks: StateFlow<List<DownloadTask>> = repository.getActiveTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val completedTasks: StateFlow<List<DownloadTask>> = repository.getCompletedTasks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val speedMetric: StateFlow<NetworkSpeedMetric> = engine.speedMetric

    fun updateUrlInput(url: String) {
        _uiState.value = _uiState.value.copy(urlInput = url)
    }

    fun updateFileName(name: String) {
        _uiState.value = _uiState.value.copy(customFileName = name)
    }

    fun updateThreads(threads: Int) {
        _uiState.value = _uiState.value.copy(numThreads = threads)
    }

    fun updateOnlyWifi(onlyWifi: Boolean) {
        _uiState.value = _uiState.value.copy(onlyWifi = onlyWifi)
    }

    fun setAddDialogOpen(open: Boolean) {
        _uiState.value = _uiState.value.copy(isAddDialogOpen = open)
    }

    fun startNewDownload(context: Context) {
        val state = _uiState.value
        val url = state.urlInput.trim()
        if (url.isBlank()) return

        val downloadsDir = getApplication<Application>().getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: getApplication<Application>().filesDir

        viewModelScope.launch {
            val task = startUseCase(
                url = url,
                destinationDir = downloadsDir,
                customFileName = state.customFileName.ifBlank { null },
                numThreads = state.numThreads,
                onlyWifi = state.onlyWifi
            )

            // Start foreground service
            DownloadForegroundService.start(context, task.id)

            // Reset dialog state
            _uiState.value = _uiState.value.copy(
                urlInput = "",
                customFileName = "",
                isAddDialogOpen = false
            )
        }
    }

    fun resumeDownload(context: Context, taskId: String) {
        DownloadForegroundService.start(context, taskId)
    }

    fun pauseDownload(context: Context, taskId: String) {
        DownloadForegroundService.pause(context, taskId)
    }

    fun cancelDownload(context: Context, taskId: String) {
        DownloadForegroundService.cancel(context, taskId)
    }

    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            cancelUseCase(taskId, deleteFile = false)
        }
    }

    fun addSampleDownload() {
        _uiState.value = _uiState.value.copy(
            urlInput = "https://speed.hetzner.de/100MB.bin",
            customFileName = "uvos_system_firmware_v1.0.bin",
            numThreads = 4,
            isAddDialogOpen = true
        )
    }
}
