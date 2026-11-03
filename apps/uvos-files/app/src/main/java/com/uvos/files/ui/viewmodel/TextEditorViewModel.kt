package com.uvos.files.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.topjohnwu.superuser.Shell
import com.uvos.files.data.engine.StorageEngineRouter
import com.uvos.files.domain.model.FileItem
import com.uvos.files.domain.model.FileMetadata
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class TextEditorState(
    val fileItem: FileItem? = null,
    val content: String = "",
    val originalContent: String = "",
    val lineCount: Int = 1,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isModified: Boolean = false,
    val metadata: FileMetadata? = null,
    val errorMessage: String? = null,
    val statusMessage: String? = null
)

class TextEditorViewModel(
    private val engineRouter: StorageEngineRouter
) : ViewModel() {

    private val _state = MutableStateFlow(TextEditorState())
    val state: StateFlow<TextEditorState> = _state.asStateFlow()

    fun loadFile(fileItem: FileItem) {
        _state.value = TextEditorState(
            fileItem = fileItem,
            isLoading = true
        )

        viewModelScope.launch {
            try {
                val metadata = engineRouter.getMetadata(fileItem.absolutePath)
                val text = withContext(Dispatchers.IO) {
                    engineRouter.readFile(fileItem.absolutePath).use { stream ->
                        stream.bufferedReader(Charsets.UTF_8).readText()
                    }
                }
                val lines = text.lines().size
                _state.update {
                    it.copy(
                        content = text,
                        originalContent = text,
                        lineCount = lines,
                        metadata = metadata,
                        isLoading = false,
                        isModified = false
                    )
                }
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Ошибка чтения: ${e.localizedMessage}"
                    )
                }
            }
        }
    }

    fun updateContent(newContent: String) {
        val lines = newContent.lines().size
        _state.update {
            it.copy(
                content = newContent,
                lineCount = lines,
                isModified = newContent != it.originalContent
            )
        }
    }

    fun saveContent() {
        val file = _state.value.fileItem ?: return
        val textToSave = _state.value.content
        val meta = _state.value.metadata

        _state.update { it.copy(isSaving = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    engineRouter.writeFile(file.absolutePath, append = false).use { out ->
                        out.write(textToSave.toByteArray(Charsets.UTF_8))
                    }
                }

                // Restore original POSIX attributes if root path
                if (engineRouter.isRootPath(file.absolutePath) && engineRouter.isRootAvailable() && meta != null) {
                    meta.permissions?.let { perms ->
                        engineRouter.setPermissions(file.absolutePath, perms)
                    }
                    if (meta.owner.isNotEmpty() && meta.group.isNotEmpty()) {
                        Shell.cmd("chown ${meta.owner}:${meta.group} '${file.absolutePath}'").exec()
                    }
                    meta.selinuxContext?.let { ctx ->
                        Shell.cmd("chcon $ctx '${file.absolutePath}'").exec()
                    }
                }

                _state.update {
                    it.copy(
                        isSaving = false,
                        originalContent = textToSave,
                        isModified = false,
                        statusMessage = "Файл успешно сохранён с исходными POSIX-атрибутами"
                    )
                }
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
}
