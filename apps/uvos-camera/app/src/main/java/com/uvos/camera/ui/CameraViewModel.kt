package com.uvos.camera.ui

import android.app.Application
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uvos.camera.data.CameraHardwareManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CameraUiState(
    val lensFacing: Int = CameraSelector.LENS_FACING_BACK,
    val flashMode: Int = ImageCapture.FLASH_MODE_AUTO,
    val isCapturing: Boolean = false,
    val lastSavedPath: String? = null,
    val isExifPurged: Boolean = true,
    val statusNotification: String? = null
)

class CameraViewModel(application: Application) : AndroidViewModel(application) {

    val cameraManager = CameraHardwareManager(application)

    private val _uiState = MutableStateFlow(CameraUiState())
    val uiState: StateFlow<CameraUiState> = _uiState.asStateFlow()

    fun toggleFlash() {
        val nextMode = when (_uiState.value.flashMode) {
            ImageCapture.FLASH_MODE_AUTO -> ImageCapture.FLASH_MODE_ON
            ImageCapture.FLASH_MODE_ON -> ImageCapture.FLASH_MODE_OFF
            else -> ImageCapture.FLASH_MODE_AUTO
        }
        cameraManager.setFlashMode(nextMode)
        _uiState.value = _uiState.value.copy(flashMode = nextMode)
    }

    fun capturePhoto() {
        if (_uiState.value.isCapturing) return

        _uiState.value = _uiState.value.copy(isCapturing = true, statusNotification = "ФИКСАЦИЯ КАДРА...")

        cameraManager.capturePhoto(
            onSuccess = { path, purged ->
                _uiState.value = _uiState.value.copy(
                    isCapturing = false,
                    lastSavedPath = path,
                    isExifPurged = purged,
                    statusNotification = if (purged) "КАДР СОХРАНЕН // EXIF ОЧИЩЕН" else "КАДР СОХРАНЕН"
                )
            },
            onError = { exc ->
                _uiState.value = _uiState.value.copy(
                    isCapturing = false,
                    statusNotification = "ОШИБКА СЪЕМКИ: ${exc.message}"
                )
            }
        )
    }

    fun clearNotification() {
        _uiState.value = _uiState.value.copy(statusNotification = null)
    }
}
