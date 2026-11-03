package com.uvos.files.domain.usecase

import com.uvos.files.data.engine.StorageEngineRouter
import com.uvos.files.domain.model.FileItem
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

enum class SortField { NAME, SIZE, DATE, TYPE }

class BrowseDirectoryUseCase(
    private val router: StorageEngineRouter
) {
    operator fun invoke(
        path: String,
        sortBy: SortField = SortField.NAME,
        ascending: Boolean = true,
        showHidden: Boolean = false
    ): Flow<List<FileItem>> {
        return router.listFiles(path).map { list ->
            val filtered = if (showHidden) list else list.filter { !it.isHidden }
            sort(filtered, sortBy, ascending)
        }
    }

    private fun sort(list: List<FileItem>, sortBy: SortField, ascending: Boolean): List<FileItem> {
        val comparator = when (sortBy) {
            SortField.NAME -> compareBy<FileItem> { !it.isDirectory }.thenBy { it.name.lowercase() }
            SortField.SIZE -> compareBy<FileItem> { !it.isDirectory }.thenBy { it.sizeBytes }
            SortField.DATE -> compareBy<FileItem> { !it.isDirectory }.thenBy { it.lastModifiedEpochMs }
            SortField.TYPE -> compareBy<FileItem> { !it.isDirectory }.thenBy { it.fileType.name }
        }
        return if (ascending) list.sortedWith(comparator) else list.sortedWith(comparator.reversed())
    }
}
