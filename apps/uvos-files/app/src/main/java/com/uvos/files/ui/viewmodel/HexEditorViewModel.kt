package com.uvos.files.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvos.files.data.engine.StorageEngineRouter
import com.uvos.files.domain.model.FileItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class HexLine(
    val offset: Long,
    val offsetHex: String,
    val bytes: List<Byte>,
    val hexGroup1: String, // 8 bytes
    val hexGroup2: String, // 8 bytes
    val asciiString: String
)

data class HexEditorState(
    val fileItem: FileItem? = null,
    val totalSize: Long = 0L,
    val currentOffset: Long = 0L,
    val pageSize: Int = 256,
    val lines: List<HexLine> = emptyList(),
    val modifiedBytes: Map<Long, Byte> = emptyMap(),
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val errorMessage: String? = null,
    val statusMessage: String? = null
)

class HexEditorViewModel(
    private val engineRouter: StorageEngineRouter
) : ViewModel() {

    private val _state = MutableStateFlow(HexEditorState())
    val state: StateFlow<HexEditorState> = _state.asStateFlow()

    fun loadFile(fileItem: FileItem) {
        _state.value = HexEditorState(
            fileItem = fileItem,
            totalSize = fileItem.sizeBytes,
            currentOffset = 0L,
            isLoading = true
        )
        loadPage(0L)
    }

    fun nextPage() {
        val next = _state.value.currentOffset + _state.value.pageSize
        if (next < _state.value.totalSize) {
            loadPage(next)
        }
    }

    fun previousPage() {
        val prev = (_state.value.currentOffset - _state.value.pageSize).coerceAtLeast(0L)
        if (prev != _state.value.currentOffset) {
            loadPage(prev)
        }
    }

    fun jumpToOffset(offset: Long) {
        val clamped = (offset / 16) * 16
        loadPage(clamped.coerceIn(0L, _state.value.totalSize))
    }

    fun editByte(globalOffset: Long, newValue: Byte) {
        _state.update { curr ->
            val updatedMap = curr.modifiedBytes.toMutableMap()
            updatedMap[globalOffset] = newValue
            curr.copy(
                modifiedBytes = updatedMap,
                statusMessage = "Модифицирован байт по смещению 0x%08X".format(globalOffset)
            )
        }
        // Re-render lines with modified byte
        refreshLinesFromMemory()
    }

    fun saveModifications() {
        val file = _state.value.fileItem ?: return
        val modifications = _state.value.modifiedBytes
        if (modifications.isEmpty()) {
            _state.update { it.copy(statusMessage = "Нет изменений для сохранения") }
            return
        }

        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            try {
                // Group contiguous modifications
                for ((offset, byteVal) in modifications) {
                    engineRouter.writeChunk(file.absolutePath, offset, byteArrayOf(byteVal))
                }
                _state.update {
                    it.copy(
                        isSaving = false,
                        modifiedBytes = emptyMap(),
                        statusMessage = "Успешно сохранено ${modifications.size} байт"
                    )
                }
                loadPage(_state.value.currentOffset)
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = "Ошибка сохранения: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    private fun loadPage(offset: Long) {
        val file = _state.value.fileItem ?: return
        _state.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val bytes = engineRouter.readChunk(file.absolutePath, offset, _state.value.pageSize)
                val lines = formatHexLines(offset, bytes, _state.value.modifiedBytes)
                _state.update {
                    it.copy(
                        currentOffset = offset,
                        lines = lines,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.localizedMessage
                    )
                }
            }
        }
    }

    private fun refreshLinesFromMemory() {
        val file = _state.value.fileItem ?: return
        viewModelScope.launch {
            val bytes = engineRouter.readChunk(file.absolutePath, _state.value.currentOffset, _state.value.pageSize)
            val lines = formatHexLines(_state.value.currentOffset, bytes, _state.value.modifiedBytes)
            _state.update { it.copy(lines = lines) }
        }
    }

    private fun formatHexLines(startOffset: Long, bytes: ByteArray, modified: Map<Long, Byte>): List<HexLine> {
        val lines = mutableListOf<HexLine>()
        var offset = startOffset
        var index = 0

        while (index < bytes.size) {
            val lineLength = minOf(16, bytes.size - index)
            val lineBytes = mutableListOf<Byte>()

            for (i in 0 until lineLength) {
                val globalPos = offset + i
                val b = modified[globalPos] ?: bytes[index + i]
                lineBytes.add(b)
            }

            val hex1 = lineBytes.take(8).joinToString(" ") { "%02X".format(it) }
            val hex2 = lineBytes.drop(8).joinToString(" ") { "%02X".format(it) }
            val ascii = lineBytes.map { b ->
                val c = b.toInt().toChar()
                if (c in ' '..'~') c else '.'
            }.joinToString("")

            lines.add(
                HexLine(
                    offset = offset,
                    offsetHex = "%08X".format(offset),
                    bytes = lineBytes,
                    hexGroup1 = hex1.padEnd(23, ' '),
                    hexGroup2 = hex2.padEnd(23, ' '),
                    asciiString = ascii.padEnd(16, ' ')
                )
            )

            offset += lineLength
            index += lineLength
        }

        return lines
    }
}
