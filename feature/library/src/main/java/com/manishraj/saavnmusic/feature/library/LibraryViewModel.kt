package com.manishraj.saavnmusic.feature.library

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.manishraj.saavnmusic.data.repository.MusicRepository
import com.manishraj.saavnmusic.domain.Song
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LibraryViewModel
    @Inject
    constructor(
        private val repo: MusicRepository,
    ) : ViewModel() {
        val favorites = repo.favorites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        val downloads = repo.downloads.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        val history = repo.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        val playlists = repo.localPlaylists.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
        val sortDescending = MutableStateFlow(true)

        fun createPlaylist(name: String) {
            viewModelScope.launch { repo.createPlaylist(name) }
        }

        fun deletePlaylist(id: Long) {
            viewModelScope.launch { repo.deletePlaylist(id) }
        }

        fun clearHistory() {
            viewModelScope.launch { repo.clearHistory() }
        }

        fun deleteDownload(id: String) {
            viewModelScope.launch { repo.deleteDownload(id) }
        }

        fun addToPlaylist(
            pid: Long,
            s: Song,
        ) {
            viewModelScope.launch { repo.addToPlaylist(pid, s) }
        }
    }
