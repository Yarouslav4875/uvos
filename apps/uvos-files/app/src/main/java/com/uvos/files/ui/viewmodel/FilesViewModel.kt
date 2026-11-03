package com.uvos.files.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.uvos.files.data.engine.StorageEngineRouter
import com.uvos.files.data.repository.StoragePartitionRepository
import com.uvos.files.domain.model.FileItem
import com.uvos.files.domain.model.StoragePartitionInfo
import com.uvos.files.domain.usecase.BrowseDirectoryUseCase
import com.uvos.files.domain.usecase.FileOperationsUseCase
import com.uvos.files.domain.usecase.SortField
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class FileBrowserState(
    val currentPath: String = "/sdcard",
    val files: List<FileItem> = emptyList(),
    val selectedFiles: Set<FileItem> = emptySet(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val sortBy: SortField = SortField.NAME,
    val sortAscending: Boolean = true,
    val showHidden: Boolean = false
)

class FilesViewModel(
    private val engineRouter: StorageEngineRouter,
    private val partitionRepo: StoragePartitionRepository,
    private val browseUseCase: BrowseDirectoryUseCase,
    private val fileOpsUseCase: FileOperationsUseCase
) : ViewModel() {

    private val _leftPaneState = MutableStateFlow(FileBrowserState(currentPath = "/sdcard"))
    val leftPaneState: StateFlow<FileBrowserState> = _leftPaneState.asStateFlow()

    private val _rightPaneState = MutableStateFlow(FileBrowserState(currentPath = "/system"))
    val rightPaneState: StateFlow<FileBrowserState> = _rightPaneState.asStateFlow()

    private val _isDualPaneActive = MutableStateFlow(false)
    val isDualPaneActive: StateFlow<Boolean> = _isDualPaneActive.asStateFlow()

    val storagePartitions: StateFlow<List<StoragePartitionInfo>> = partitionRepo
        .observePartitions()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        loadDirectory(isLeftPane = true, path = "/sdcard")
        loadDirectory(isLeftPane = false, path = "/system")
    }

    fun setDualPane(active: Boolean) {
        _isDualPaneActive.value = active
    }

    fun isRootPath(path: String): Boolean = engineRouter.isRootPath(path)
    fun isRootAvailable(): Boolean = engineRouter.isRootAvailable()

    fun navigateTo(isLeftPane: Boolean, path: String) {
        loadDirectory(isLeftPane, path)
    }

    fun navigateUp(isLeftPane: Boolean) {
        val currentState = if (isLeftPane) _leftPaneState.value else _rightPaneState.value
        val parent = currentState.currentPath.substringBeforeLast('/', "")
        val targetPath = if (parent.isEmpty()) "/" else parent
        loadDirectory(isLeftPane, targetPath)
    }

    fun refresh(isLeftPane: Boolean) {
        val path = if (isLeftPane) _leftPaneState.value.currentPath else _rightPaneState.value.currentPath
        loadDirectory(isLeftPane, path)
    }

    fun toggleFileSelection(isLeftPane: Boolean, item: FileItem) {
        val targetFlow = if (isLeftPane) _leftPaneState else _rightPaneState
        targetFlow.update { state ->
            val newSelected = state.selectedFiles.toMutableSet()
            if (newSelected.contains(item)) {
                newSelected.remove(item)
            } else {
                newSelected.add(item)
            }
            state.copy(selectedFiles = newSelected)
        }
    }

    fun clearSelection(isLeftPane: Boolean) {
        val targetFlow = if (isLeftPane) _leftPaneState else _rightPaneState
        targetFlow.update { it.copy(selectedFiles = emptySet()) }
    }

    fun setSort(isLeftPane: Boolean, sortField: SortField) {
        val targetFlow = if (isLeftPane) _leftPaneState else _rightPaneState
        val current = targetFlow.value
        val newAscending = if (current.sortBy == sortField) !current.sortAscending else true
        targetFlow.update { it.copy(sortBy = sortField, sortAscending = newAscending) }
        loadDirectory(isLeftPane, current.currentPath)
    }

    fun toggleShowHidden(isLeftPane: Boolean) {
        val targetFlow = if (isLeftPane) _leftPaneState else _rightPaneState
        val newState = !targetFlow.value.showHidden
        targetFlow.update { it.copy(showHidden = newState) }
        loadDirectory(isLeftPane, targetFlow.value.currentPath)
    }

    fun deleteItem(item: FileItem, isLeftPane: Boolean) {
        viewModelScope.launch {
            val success = fileOpsUseCase.delete(item.absolutePath)
            if (success) {
                refresh(isLeftPane)
            }
        }
    }

    fun copySelectedToOtherPane(fromLeft: Boolean) {
        val sourceState = if (fromLeft) _leftPaneState.value else _rightPaneState.value
        val destState = if (fromLeft) _rightPaneState.value else _leftPaneState.value
        val targetDir = destState.currentPath

        viewModelScope.launch {
            sourceState.selectedFiles.forEach { item ->
                val destPath = if (targetDir == "/") "/${item.name}" else "$targetDir/${item.name}"
                fileOpsUseCase.copy(item.absolutePath, destPath)
            }
            clearSelection(fromLeft)
            refresh(!fromLeft)
        }
    }

    fun moveSelectedToOtherPane(fromLeft: Boolean) {
        val sourceState = if (fromLeft) _leftPaneState.value else _rightPaneState.value
        val destState = if (fromLeft) _rightPaneState.value else _leftPaneState.value
        val targetDir = destState.currentPath

        viewModelScope.launch {
            sourceState.selectedFiles.forEach { item ->
                val destPath = if (targetDir == "/") "/${item.name}" else "$targetDir/${item.name}"
                fileOpsUseCase.move(item.absolutePath, destPath)
            }
            clearSelection(fromLeft)
            refresh(fromLeft)
            refresh(!fromLeft)
        }
    }

    private fun loadDirectory(isLeftPane: Boolean, path: String) {
        val targetFlow = if (isLeftPane) _leftPaneState else _rightPaneState
        targetFlow.update { it.copy(isLoading = true, errorMessage = null) }

        viewModelScope.launch {
            try {
                val state = targetFlow.value
                browseUseCase(
                    path = path,
                    sortBy = state.sortBy,
                    ascending = state.sortAscending,
                    showHidden = state.showHidden
                ).collect { fileList ->
                    targetFlow.update {
                        it.copy(
                            currentPath = path,
                            files = fileList,
                            isLoading = false
                        )
                    }
                }
            } catch (e: Exception) {
                targetFlow.update { it.copy(isLoading = false, errorMessage = e.localizedMessage) }
            }
        }
    }
}
