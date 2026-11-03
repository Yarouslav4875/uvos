package com.uvos.files.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvos.files.data.engine.StorageEngineRouter
import com.uvos.files.domain.model.FileItem
import com.uvos.files.domain.usecase.CalculateChecksumUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HashCalculatorState(
    val fileItem: FileItem? = null,
    val selectedAlgorithm: String = "SHA-256",
    val isCalculating: Boolean = false,
    val progress: Float = 0f,
    val bytesProcessed: Long = 0L,
    val totalBytes: Long = 0L,
    val speedBytesPerSec: Long = 0L,
    val calculatedHash: String? = null,
    val compareHash: String = "",
    val isMatch: Boolean? = null,
    val errorMessage: String? = null
)

class HashCalculatorViewModel(
    private val engineRouter: StorageEngineRouter,
    private val checksumUseCase: CalculateChecksumUseCase
) : ViewModel() {

    private val _state = MutableStateFlow(HashCalculatorState())
    val state: StateFlow<HashCalculatorState> = _state.asStateFlow()

    private var calculationJob: Job? = null

    fun setFile(fileItem: FileItem) {
        _state.value = HashCalculatorState(
            fileItem = fileItem,
            totalBytes = fileItem.sizeBytes
        )
    }

    fun setAlgorithm(algorithm: String) {
        _state.update {
            it.copy(
                selectedAlgorithm = algorithm,
                calculatedHash = null,
                isMatch = null
            )
        }
    }

    fun setCompareHash(hash: String) {
        val clean = hash.trim().lowercase()
        val currentHash = _state.value.calculatedHash?.lowercase()
        val match = if (currentHash != null && clean.isNotEmpty()) {
            clean == currentHash
        } else {
            null
        }
        _state.update { it.copy(compareHash = hash, isMatch = match) }
    }

    fun startCalculation() {
        val file = _state.value.fileItem ?: return
        val algo = _state.value.selectedAlgorithm

        calculationJob?.cancel()
        _state.update {
            it.copy(
                isCalculating = true,
                progress = 0f,
                bytesProcessed = 0L,
                speedBytesPerSec = 0L,
                calculatedHash = null,
                isMatch = null,
                errorMessage = null
            )
        }

        calculationJob = viewModelScope.launch {
            try {
                val stream = engineRouter.readFile(file.absolutePath)
                checksumUseCase(stream, file.sizeBytes, algo).collect { progressData ->
                    _state.update { curr ->
                        val finalHash = progressData.finalHash
                        val match = if (finalHash != null && curr.compareHash.isNotBlank()) {
                            curr.compareHash.trim().equals(finalHash, ignoreCase = true)
                        } else {
                            null
                        }

                        curr.copy(
                            progress = progressData.percentage,
                            bytesProcessed = progressData.bytesProcessed,
                            speedBytesPerSec = progressData.speedBytesPerSec,
                            calculatedHash = finalHash,
                            isCalculating = finalHash == null,
                            isMatch = match
                        )
                    }
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isCalculating = false,
                        errorMessage = "Ошибка вычисления: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun cancelCalculation() {
        calculationJob?.cancel()
        _state.update { it.copy(isCalculating = false) }
    }
}
