package com.uvos.music.feature.library

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.uvos.music.UvosMusicApp
import com.uvos.music.domain.model.AudioTrack
import com.uvos.music.domain.usecase.ScanLocalLibraryUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LibraryUiState(
    val tracks: List<AudioTrack> = emptyList(),
    val filteredTracks: List<AudioTrack> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val selectedTab: LibraryTab = LibraryTab.ALL_TRACKS
)

enum class LibraryTab {
    ALL_TRACKS,
    HI_RES,
    ARTISTS,
    ALBUMS
}

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val scanner = ScanLocalLibraryUseCase(application)
    private val playerHolder = (application as UvosMusicApp).playerHolder

    private val _uiState = MutableStateFlow(LibraryUiState(isLoading = true))
    val uiState: StateFlow<LibraryUiState> = _uiState.asStateFlow()

    init {
        loadMedia()
    }

    fun loadMedia() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            val scannedTracks = scanner.execute()
            _uiState.value = _uiState.value.copy(
                tracks = scannedTracks,
                isLoading = false
            )
            applyFilter()
        }
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilter()
    }

    fun selectTab(tab: LibraryTab) {
        _uiState.value = _uiState.value.copy(selectedTab = tab)
        applyFilter()
    }

    private fun applyFilter() {
        val all = _uiState.value.tracks
        val query = _uiState.value.searchQuery.trim().lowercase()

        var result = when (_uiState.value.selectedTab) {
            LibraryTab.ALL_TRACKS -> all
            LibraryTab.HI_RES -> all.filter { it.formatInfo.isHiRes || it.formatInfo.isLossless }
            LibraryTab.ARTISTS -> all.sortedBy { it.artist.lowercase() }
            LibraryTab.ALBUMS -> all.sortedBy { it.album.lowercase() }
        }

        if (query.isNotEmpty()) {
            result = result.filter {
                it.title.lowercase().contains(query) ||
                it.artist.lowercase().contains(query) ||
                it.album.lowercase().contains(query)
            }
        }

        _uiState.value = _uiState.value.copy(filteredTracks = result)
    }

    fun playTrack(track: AudioTrack) {
        val tracks = _uiState.value.filteredTracks.ifEmpty { _uiState.value.tracks }
        val index = tracks.indexOf(track).coerceAtLeast(0)
        playerHolder.playQueue(tracks, index)
    }

    fun playAll() {
        val tracks = _uiState.value.filteredTracks.ifEmpty { _uiState.value.tracks }
        if (tracks.isNotEmpty()) {
            playerHolder.playQueue(tracks, 0)
        }
    }
}
